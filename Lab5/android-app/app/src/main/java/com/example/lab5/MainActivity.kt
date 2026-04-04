package com.example.lab5

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.lab5.ui.theme.Lab5Theme
import kotlinx.coroutines.*
import kotlin.math.abs

// TarsosDSP Core Imports
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.filters.LowPassFS
import be.tarsos.dsp.filters.HighPass

enum class FilterMode { NONE, LOW_PASS, HIGH_PASS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Lab5Theme {
                AudioRecorderScreen()
            }
        }
    }
}

@Composable
fun AudioRecorderScreen() {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var amplitude by remember { mutableIntStateOf(0) }
    var filterMode by remember { mutableStateOf(FilterMode.NONE) }
    var alphaValue by remember { mutableFloatStateOf(0.5f) }
    var audioBuffer by remember { mutableStateOf(ShortArray(0)) }

    val scope = rememberCoroutineScope()
    var recordingJob by remember { mutableStateOf<Job?>(null) }

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (hasPermission) {
            Text("Signal Level: $amplitude")
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.Black)) {
                WaveformVisualizer(audioBuffer)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row {
                FilterOption("None", FilterMode.NONE, filterMode) { filterMode = it }
                FilterOption("Low-Pass", FilterMode.LOW_PASS, filterMode) { filterMode = it }
                FilterOption("High-Pass", FilterMode.HIGH_PASS, filterMode) { filterMode = it }
            }

            Text("Cutoff Frequency: ${"%.0f".format(20f + alphaValue * 8000f)} Hz")
            Slider(value = alphaValue, onValueChange = { alphaValue = it })

            Button(onClick = {
                if (isRecording) {
                    isRecording = false
                    recordingJob?.cancel()
                } else {
                    isRecording = true
                    recordingJob = scope.launch(Dispatchers.IO) {
                        startTarsosRecording(filterMode, { alphaValue }) { data ->
                            scope.launch(Dispatchers.Main) {
                                audioBuffer = data
                                amplitude = data.maxOfOrNull { abs(it.toInt()) } ?: 0
                            }
                        }
                    }
                }
            }) { Text(if (isRecording) "Stop" else "Start") }
        } else {
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
fun WaveformVisualizer(buffer: ShortArray) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (buffer.isEmpty()) return@Canvas
        val centerY = size.height / 2
        val step = (buffer.size / size.width.toInt()).coerceAtLeast(1)
        for (i in 0 until size.width.toInt()) {
            val idx = i * step
            if (idx >= buffer.size) break
            val valNorm = (buffer[idx] / 32768f) * centerY
            drawLine(Color.Cyan, Offset(i.toFloat(), centerY), Offset(i.toFloat(), centerY - valNorm))
        }
    }
}

@Composable
fun FilterOption(label: String, mode: FilterMode, current: FilterMode, onSelected: (FilterMode) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = mode == current, onClick = { onSelected(mode) })
        Text(label)
    }
}

private suspend fun startTarsosRecording(
    mode: FilterMode,
    getAlpha: () -> Float,
    onDataReceived: (ShortArray) -> Unit
) {
    val sampleRate = 44100
    val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, 1024, 0)

    dispatcher.addAudioProcessor(object : AudioProcessor {
        override fun process(audioEvent: AudioEvent): Boolean {
            val cutoff = 20f + (getAlpha() * 8000f)
            when (mode) {
                FilterMode.LOW_PASS -> LowPassFS(cutoff, sampleRate.toFloat()).process(audioEvent)
                FilterMode.HIGH_PASS -> HighPass(cutoff, sampleRate.toFloat()).process(audioEvent)
                else -> {}
            }
            val floatBuf = audioEvent.floatBuffer
            val shortBuf = ShortArray(floatBuf.size) { i -> (floatBuf[i] * 32767).toInt().toShort() }
            onDataReceived(shortBuf)
            return true
        }
        override fun processingFinished() {}
    })

    try {
        withContext(Dispatchers.IO) { dispatcher.run() }
    } finally {
        if (!dispatcher.isStopped) dispatcher.stop()
    }
}