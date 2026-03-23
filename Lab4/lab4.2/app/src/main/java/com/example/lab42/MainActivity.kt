package com.example.lab42

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import com.example.lab42.ui.theme.Lab42Theme

enum class AudioSource {
    MICROPHONE,
    SYSTEM_AUDIO
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab42Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PitchDetectionScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PitchDetectionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasAudioPermission = granted }
    )

    if (hasAudioPermission) {
        PitchDetector(modifier)
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Audio permission is required for pitch detection.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text(text = "Grant Permission")
            }
        }
    }
}

@Composable
fun AudioSourceSelector(
    selectedSource: AudioSource,
    onSourceSelected: (AudioSource) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        if (selectedSource == AudioSource.MICROPHONE) {
            Button(
                onClick = { },
                enabled = enabled
            ) {
                Text("Microphone")
            }
        } else {
            OutlinedButton(
                onClick = { onSourceSelected(AudioSource.MICROPHONE) },
                enabled = enabled
            ) {
                Text("Microphone")
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (selectedSource == AudioSource.SYSTEM_AUDIO) {
            Button(
                onClick = { },
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("System Audio")
            }
        } else {
            OutlinedButton(
                onClick = { onSourceSelected(AudioSource.SYSTEM_AUDIO) },
                enabled = enabled
            ) {
                Text("System Audio")
            }
        }
    }
}

@Composable
fun Waveform(
    samples: FloatArray,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        if (samples.isEmpty()) {
            drawLine(
                color = lineColor.copy(alpha = 0.3f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2f
            )
            return@Canvas
        }

        val centerY = size.height / 2
        val amplitudeScale = size.height / 2 * 0.9f
        val stepX = size.width / samples.size

        val path = Path()
        path.moveTo(0f, centerY - samples[0] * amplitudeScale)

        for (i in 1 until samples.size) {
            val x = i * stepX
            val y = centerY - samples[i] * amplitudeScale
            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2f)
        )

        drawLine(
            color = lineColor.copy(alpha = 0.2f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1f
        )
    }
}

@Composable
fun PitchDetector(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var currentPitch by remember { mutableFloatStateOf(-1f) }
    var highestPitch by remember { mutableFloatStateOf(-1f) }
    var isRunning by remember { mutableStateOf(false) }
    var waveformData by remember { mutableStateOf(FloatArray(0)) }
    var audioSource by remember { mutableStateOf(AudioSource.MICROPHONE) }

    // Service binding state
    var audioCaptureService by remember { mutableStateOf<AudioCaptureService?>(null) }
    var isBound by remember { mutableStateOf(false) }

    val serviceConnection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as AudioCaptureService.LocalBinder
                audioCaptureService = binder.getService().apply {
                    onPitchDetected = { pitch, highest ->
                        currentPitch = pitch
                        highestPitch = highest
                    }
                    onWaveformData = { data ->
                        waveformData = data
                    }
                }
                isBound = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                audioCaptureService = null
                isBound = false
            }
        }
    }

    // MediaProjection launcher
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(context, AudioCaptureService::class.java).apply {
                putExtra(AudioCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(AudioCaptureService.EXTRA_DATA, result.data)
            }
            context.startForegroundService(serviceIntent)
            context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
            isRunning = true
        }
    }

    // Microphone capture effect
    DisposableEffect(isRunning, audioSource) {
        var dispatcher: AudioDispatcher? = null

        if (isRunning && audioSource == AudioSource.MICROPHONE) {
            val sampleRate = 22050
            val bufferSize = 1024
            val overlap = 0

            val pdh = PitchDetectionHandler { result, _ ->
                val pitchInHz = result.pitch
                if (pitchInHz != -1f) {
                    currentPitch = pitchInHz
                    if (pitchInHz > highestPitch) {
                        highestPitch = pitchInHz
                    }
                }
            }

            val pitchProcessor: AudioProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.YIN,
                sampleRate.toFloat(),
                bufferSize,
                pdh
            )

            val waveformProcessor = object : AudioProcessor {
                override fun process(audioEvent: AudioEvent): Boolean {
                    waveformData = audioEvent.floatBuffer.copyOf()
                    return true
                }
                override fun processingFinished() {}
            }

            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, bufferSize, overlap)
            dispatcher?.addAudioProcessor(waveformProcessor)
            dispatcher?.addAudioProcessor(pitchProcessor)

            Thread(dispatcher, "Audio Dispatcher").start()
        }

        onDispose {
            dispatcher?.stop()
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            if (isBound) {
                audioCaptureService?.stopCapture()
                try {
                    context.unbindService(serviceConnection)
                } catch (_: Exception) {}
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Audio source selector
        AudioSourceSelector(
            selectedSource = audioSource,
            onSourceSelected = { newSource ->
                if (isRunning) {
                    // Stop current capture before switching
                    if (audioSource == AudioSource.SYSTEM_AUDIO && isBound) {
                        audioCaptureService?.stopCapture()
                        try {
                            context.unbindService(serviceConnection)
                        } catch (_: Exception) {}
                        isBound = false
                    }
                    isRunning = false
                    currentPitch = -1f
                    waveformData = FloatArray(0)
                }
                audioSource = newSource
            },
            enabled = !isRunning
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Source info text
        Text(
            text = if (audioSource == AudioSource.SYSTEM_AUDIO)
                "Captures audio from YouTube, Spotify, etc."
            else
                "Captures audio from device microphone",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Waveform display
        Waveform(
            samples = waveformData,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            lineColor = if (audioSource == AudioSource.SYSTEM_AUDIO)
                MaterialTheme.colorScheme.secondary
            else
                MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Current Pitch: ${if (currentPitch > 0) "%.2f Hz".format(currentPitch) else "---"}",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Highest Pitch: ${if (highestPitch > 0) "%.2f Hz".format(highestPitch) else "---"}",
            style = MaterialTheme.typography.headlineLarge,
            color = if (audioSource == AudioSource.SYSTEM_AUDIO)
                MaterialTheme.colorScheme.secondary
            else
                MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isRunning) {
                    // Stop
                    if (audioSource == AudioSource.SYSTEM_AUDIO && isBound) {
                        audioCaptureService?.stopCapture()
                        try {
                            context.unbindService(serviceConnection)
                        } catch (_: Exception) {}
                        isBound = false
                    }
                    isRunning = false
                    currentPitch = -1f
                    waveformData = FloatArray(0)
                } else {
                    // Start
                    if (audioSource == AudioSource.SYSTEM_AUDIO) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                        }
                    } else {
                        isRunning = true
                    }
                }
            },
            colors = if (isRunning)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            else
                ButtonDefaults.buttonColors()
        ) {
            Text(text = if (isRunning) "Stop Detection" else "Start Detection")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            highestPitch = -1f
            currentPitch = -1f
            audioCaptureService?.resetHighestPitch()
        }) {
            Text(text = "Reset")
        }
    }
}
