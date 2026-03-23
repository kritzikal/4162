package com.example.lab42

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlin.concurrent.thread

class AudioCaptureService : Service() {

    companion object {
        const val CHANNEL_ID = "AudioCaptureChannel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"

        const val SAMPLE_RATE = 22050
        const val BUFFER_SIZE = 1024
    }

    private val binder = LocalBinder()
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var isCapturing = false
    private var captureThread: Thread? = null

    var onPitchDetected: ((pitch: Float, highest: Float) -> Unit)? = null
    var onWaveformData: ((FloatArray) -> Unit)? = null
    private var highestPitch = -1f

    inner class LocalBinder : Binder() {
        fun getService(): AudioCaptureService = this@AudioCaptureService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    @Suppress("DEPRECATION")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_DATA)
        }

        if (resultCode != -1 && data != null) {
            val notification = createNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            startAudioCapture()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audio Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Captures system audio for pitch detection"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pitch Detection")
            .setContentText("Capturing system audio...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startAudioCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(config)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(maxOf(minBufferSize, BUFFER_SIZE * 2))
            .build()

        isCapturing = true
        captureThread = thread(name = "SystemAudioCapture") {
            processAudio()
        }
    }

    private fun processAudio() {
        val audioFormat = TarsosDSPAudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
        val byteBuffer = ByteArray(BUFFER_SIZE * 2) // 16-bit = 2 bytes per sample
        val floatBuffer = FloatArray(BUFFER_SIZE)

        val pdh = PitchDetectionHandler { result, _ ->
            val pitchInHz = result.pitch
            if (pitchInHz > 0) {
                if (pitchInHz > highestPitch) {
                    highestPitch = pitchInHz
                }
                onPitchDetected?.invoke(pitchInHz, highestPitch)
            }
        }

        val pitchProcessor = PitchProcessor(
            PitchProcessor.PitchEstimationAlgorithm.YIN,
            SAMPLE_RATE.toFloat(),
            BUFFER_SIZE,
            pdh
        )

        audioRecord?.startRecording()

        while (isCapturing) {
            val bytesRead = audioRecord?.read(byteBuffer, 0, byteBuffer.size) ?: 0
            if (bytesRead > 0) {
                // Convert bytes to floats (-1.0 to 1.0)
                for (i in 0 until BUFFER_SIZE) {
                    val sample = (byteBuffer[i * 2].toInt() and 0xFF) or
                            (byteBuffer[i * 2 + 1].toInt() shl 8)
                    floatBuffer[i] = sample.toShort() / 32768f
                }

                // Send waveform data
                onWaveformData?.invoke(floatBuffer.copyOf())

                // Create audio event for pitch processing
                val audioEvent = AudioEvent(audioFormat)
                audioEvent.floatBuffer = floatBuffer
                pitchProcessor.process(audioEvent)
            }
        }

        audioRecord?.stop()
    }

    fun resetHighestPitch() {
        highestPitch = -1f
    }

    fun stopCapture() {
        isCapturing = false
        captureThread?.join(1000)
        audioRecord?.release()
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }
}
