package com.example.lab4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
import org.jtransforms.fft.FloatFFT_1D

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}

@Composable
fun App() {
    var frequencyA by remember { mutableFloatStateOf(1f) }
    var frequencyB by remember { mutableFloatStateOf(1f) }
    val numPoints = 256

    val signalA = remember(frequencyA) {
        FloatArray(numPoints) { i -> sin(2 * PI * frequencyA * i / numPoints).toFloat() }
    }
    val signalB = remember(frequencyB) {
        FloatArray(numPoints) { i -> cos(2 * PI * frequencyB * i / numPoints).toFloat() }
    }
    
    val convolutionResult = remember(signalA, signalB) {
        computeCircularConvolution(signalA, signalB)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Frequency A: ${frequencyA.toInt()} Hz",
            style = MaterialTheme.typography.bodyLarge
        )

        Slider(
            value = frequencyA,
            onValueChange = { frequencyA = it },
            valueRange = 1f..50f,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Text(
            text = "Frequency B: ${frequencyB.toInt()} Hz",
            style = MaterialTheme.typography.bodyLarge
        )

        Slider(
            value = frequencyB,
            onValueChange = { frequencyB = it },
            valueRange = 1f..50f,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Text("Sine Wave (Signal A)")
        SignalCanvas(data = signalA, color = Color.Red)

        Text("Cosine Wave (Signal B)")
        SignalCanvas(data = signalB, color = Color.Blue)

        Text("Circular Convolution Result")
        SignalCanvas(data = convolutionResult, color = Color.Magenta)
    }
}

@Composable
fun SignalCanvas(data: FloatArray, color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(8.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        val maxAmp = data.maxOfOrNull { kotlin.math.abs(it) }?.takeIf { it > 0 } ?: 1f
        val scale = (height / 2.5f) / maxAmp

        drawLine(
            color = Color.LightGray,
            start = androidx.compose.ui.geometry.Offset(0f, centerY),
            end = androidx.compose.ui.geometry.Offset(width, centerY),
            strokeWidth = 1f
        )

        if (data.isNotEmpty()) {
            val step = width / (data.size - 1)
            for (i in 0 until data.size - 1) {
                val x1 = i * step
                val y1 = centerY - data[i] * scale
                val x2 = (i + 1) * step
                val y2 = centerY - data[i + 1] * scale
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(x1, y1),
                    end = androidx.compose.ui.geometry.Offset(x2, y2),
                    strokeWidth = 2f
                )
            }
        }
    }
}

fun computeCircularConvolution(signalA: FloatArray, signalB: FloatArray): FloatArray {
    val n = signalA.size
    val fft = FloatFFT_1D(n.toLong())
    val dataA = signalA.copyOf()
    val dataB = signalB.copyOf()
    
    fft.realForward(dataA)
    fft.realForward(dataB)
    
    val result = FloatArray(n)
    result[0] = dataA[0] * dataB[0] //DC
    if (n > 1) {
        result[1] = dataA[1] * dataB[1] //Nyquist
    }
    for (i in 1 until n/2) {
        val reA = dataA[2 * i]
        val imA = dataA[2 * i + 1]
        val reB = dataB[2 * i]
        val imB = dataB[2 * i + 1]

        result[2*i] = (reA * reB) - (imA * imB)
        result[2 * i + 1] = (reA * imB) + (imA * reB)
    }
    
    fft.realInverse(result, true)
    
    return result
}
