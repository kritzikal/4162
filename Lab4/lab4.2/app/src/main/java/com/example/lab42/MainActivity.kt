package com.example.lab42

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchProcessor
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm

class MainActivity : AppCompatActivity() {

    private lateinit var waveformView: WaveformView
    private lateinit var currentPitchText: TextView
    private lateinit var highestPitchText: TextView
    private lateinit var startStopButton: Button
    private lateinit var resetButton: Button

    private var dispatcher: be.tarsos.dsp.AudioDispatcher? = null
    private var audioThread: Thread? = null
    private var isRunning = false
    private var highestPitch = -1f

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) setupUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) {
            setupUI()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun setupUI() {
        setContentView(R.layout.activity_main)

        waveformView = findViewById(R.id.waveformView)
        currentPitchText = findViewById(R.id.currentPitchText)
        highestPitchText = findViewById(R.id.highestPitchText)
        startStopButton = findViewById(R.id.startStopButton)
        resetButton = findViewById(R.id.resetButton)

        startStopButton.setOnClickListener {
            if (isRunning) stopDetection() else startDetection()
        }

        resetButton.setOnClickListener {
            highestPitch = -1f
            highestPitchText.text = "Highest: ---"
            currentPitchText.text = "Current: ---"
        }
    }

    private fun startDetection() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0).apply {
            // Waveform capture
            addAudioProcessor(object : AudioProcessor {
                override fun process(e: AudioEvent): Boolean {
                    val samples = e.floatBuffer.copyOf()
                    runOnUiThread { waveformView.setSamples(samples) }
                    return true
                }
                override fun processingFinished() {}
            })

            // Pitch detection
            addAudioProcessor(PitchProcessor(PitchEstimationAlgorithm.YIN, 22050f, 1024) { result, _ ->
                if (result.pitch > 0) {
                    val pitch = result.pitch
                    if (pitch > highestPitch) highestPitch = pitch
                    runOnUiThread {
                        currentPitchText.text = "Current: %.1f Hz".format(pitch)
                        highestPitchText.text = "Highest: %.1f Hz".format(highestPitch)
                    }
                }
            })
        }

        audioThread = Thread(dispatcher, "Audio").apply { start() }
        isRunning = true
        startStopButton.text = "Stop"
    }

    private fun stopDetection() {
        dispatcher?.stop()
        audioThread = null
        dispatcher = null
        isRunning = false
        startStopButton.text = "Start"
        currentPitchText.text = "Current: ---"
        waveformView.setSamples(FloatArray(0))
    }

    override fun onDestroy() {
        super.onDestroy()
        dispatcher?.stop()
    }
}
