package com.example.lab5

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.UniversalAudioInputStream
import java.io.InputStream

object AudioDispatcherFactory {
    @SuppressLint("MissingPermission")
    fun fromDefaultMicrophone(sampleRate: Int, bufferSize: Int, overlap: Int): AudioDispatcher {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val actualBufferSize = minBufferSize.coerceAtLeast(bufferSize * 2)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            actualBufferSize
        )
        audioRecord.startRecording()

        val inputStream = object : InputStream() {
            private val buf = ByteArray(actualBufferSize)
            override fun read(): Int {
                val b = ByteArray(1)
                val r = read(b, 0, 1)
                return if (r == -1) -1 else b[0].toInt() and 0xFF
            }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                return audioRecord.read(b, off, len)
            }
            override fun close() {
                audioRecord.stop()
                audioRecord.release()
            }
        }

        val format = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        val audioStream = UniversalAudioInputStream(inputStream, format)
        return AudioDispatcher(audioStream, bufferSize, overlap)
    }
}