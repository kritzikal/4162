package com.example.pitchdetector

import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.jvm.JVMAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.sound.sampled.*
import javax.swing.*

class WaveformPanel : JPanel() {
    var samples: FloatArray = FloatArray(0)
        set(value) {
            field = value
            repaint()
        }

    init {
        preferredSize = Dimension(600, 150)
        background = Color(45, 45, 50)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val centerY = height / 2

        // Draw center line
        g2.color = Color(100, 100, 120)
        g2.drawLine(0, centerY, width, centerY)

        if (samples.isEmpty()) return

        // Draw waveform
        g2.color = Color(100, 200, 255)
        g2.stroke = BasicStroke(1.5f)

        val amplitudeScale = height / 2 * 0.9f
        val stepX = width.toFloat() / samples.size

        var prevX = 0
        var prevY = centerY - (samples[0] * amplitudeScale).toInt()

        for (i in 1 until samples.size) {
            val x = (i * stepX).toInt()
            val y = centerY - (samples[i] * amplitudeScale).toInt()
            g2.drawLine(prevX, prevY, x, y)
            prevX = x
            prevY = y
        }
    }
}

class PitchDetectorApp : JFrame("Pitch Detector - Desktop") {
    private val waveformPanel = WaveformPanel()
    private val currentPitchLabel = JLabel("Current Pitch: ---")
    private val highestPitchLabel = JLabel("Highest Pitch: ---")
    private val mixerComboBox = JComboBox<String>()
    private val startButton = JButton("Start Detection")
    private val resetButton = JButton("Reset")
    private val statusLabel = JLabel("Select audio source and click Start")

    private var dispatcher: AudioDispatcher? = null
    private var audioThread: Thread? = null
    private var isRunning = false
    private var highestPitch = -1f

    private val sampleRate = 44100
    private val bufferSize = 2048

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                stopDetection()
                dispose()
                System.exit(0)
            }
        })

        setupUI()
        populateMixers()
        pack()
        setLocationRelativeTo(null)
    }

    private fun setupUI() {
        layout = BorderLayout(10, 10)
        (contentPane as JPanel).border = BorderFactory.createEmptyBorder(15, 15, 15, 15)

        // Top panel - audio source selection
        val topPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JLabel("Audio Source:"))
            add(mixerComboBox.apply { preferredSize = Dimension(300, 25) })
            add(Box.createHorizontalStrut(10))
            add(JButton("Refresh").apply {
                addActionListener { populateMixers() }
            })
        }

        // Center panel - waveform
        val waveformContainer = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(80, 80, 90), 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            )
            add(waveformPanel, BorderLayout.CENTER)
        }

        // Pitch display panel
        val pitchPanel = JPanel(GridLayout(2, 1, 5, 5)).apply {
            currentPitchLabel.font = Font("SansSerif", Font.BOLD, 24)
            currentPitchLabel.horizontalAlignment = SwingConstants.CENTER

            highestPitchLabel.font = Font("SansSerif", Font.BOLD, 32)
            highestPitchLabel.foreground = Color(70, 130, 220)
            highestPitchLabel.horizontalAlignment = SwingConstants.CENTER

            add(currentPitchLabel)
            add(highestPitchLabel)
        }

        // Button panel
        val buttonPanel = JPanel(FlowLayout()).apply {
            startButton.preferredSize = Dimension(150, 35)
            resetButton.preferredSize = Dimension(100, 35)

            startButton.addActionListener { toggleDetection() }
            resetButton.addActionListener { resetHighest() }

            add(startButton)
            add(Box.createHorizontalStrut(10))
            add(resetButton)
        }

        // Status panel
        val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            statusLabel.foreground = Color.GRAY
            add(statusLabel)
        }

        // Main center panel
        val centerPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(waveformContainer)
            add(Box.createVerticalStrut(15))
            add(pitchPanel)
            add(Box.createVerticalStrut(15))
            add(buttonPanel)
        }

        add(topPanel, BorderLayout.NORTH)
        add(centerPanel, BorderLayout.CENTER)
        add(statusPanel, BorderLayout.SOUTH)
    }

    private fun populateMixers() {
        mixerComboBox.removeAllItems()

        val mixerInfos = AudioSystem.getMixerInfo()
        for (info in mixerInfos) {
            val mixer = AudioSystem.getMixer(info)
            val targetLines = mixer.targetLineInfo

            // Check if this mixer supports audio input (TargetDataLine)
            for (lineInfo in targetLines) {
                if (lineInfo is DataLine.Info && lineInfo.lineClass == TargetDataLine::class.java) {
                    mixerComboBox.addItem(info.name)
                    break
                }
            }
        }

        if (mixerComboBox.itemCount == 0) {
            mixerComboBox.addItem("No audio inputs found")
            startButton.isEnabled = false
        } else {
            startButton.isEnabled = true
            // Try to select Stereo Mix if available
            for (i in 0 until mixerComboBox.itemCount) {
                val name = mixerComboBox.getItemAt(i).lowercase()
                if (name.contains("stereo mix") || name.contains("what u hear") || name.contains("loopback")) {
                    mixerComboBox.selectedIndex = i
                    break
                }
            }
        }
    }

    private fun getMixerByName(name: String): Mixer? {
        val mixerInfos = AudioSystem.getMixerInfo()
        for (info in mixerInfos) {
            if (info.name == name) {
                return AudioSystem.getMixer(info)
            }
        }
        return null
    }

    private fun toggleDetection() {
        if (isRunning) {
            stopDetection()
        } else {
            startDetection()
        }
    }

    private fun startDetection() {
        val selectedMixer = mixerComboBox.selectedItem as? String ?: return
        val mixer = getMixerByName(selectedMixer)

        if (mixer == null) {
            statusLabel.text = "Error: Could not find selected audio source"
            return
        }

        try {
            val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            val info = DataLine.Info(TargetDataLine::class.java, format)

            if (!mixer.isLineSupported(info)) {
                statusLabel.text = "Error: Audio format not supported by this source"
                return
            }

            // Open the target data line from the selected mixer
            val line = mixer.getLine(info) as TargetDataLine
            line.open(format, bufferSize * 2)
            line.start()

            // Create AudioDispatcher from the line
            val audioStream = AudioInputStream(line)
            val jvmAudioStream = JVMAudioInputStream(audioStream)
            dispatcher = AudioDispatcher(jvmAudioStream, bufferSize, 0)

            // Waveform processor
            dispatcher?.addAudioProcessor(object : AudioProcessor {
                override fun process(audioEvent: AudioEvent): Boolean {
                    SwingUtilities.invokeLater {
                        waveformPanel.samples = audioEvent.floatBuffer.copyOf()
                    }
                    return true
                }
                override fun processingFinished() {}
            })

            // Pitch processor
            val pitchHandler = PitchDetectionHandler { result, _ ->
                val pitch = result.pitch
                if (pitch > 0) {
                    if (pitch > highestPitch) {
                        highestPitch = pitch
                    }
                    SwingUtilities.invokeLater {
                        currentPitchLabel.text = "Current Pitch: %.2f Hz".format(pitch)
                        highestPitchLabel.text = "Highest Pitch: %.2f Hz".format(highestPitch)
                    }
                }
            }

            dispatcher?.addAudioProcessor(
                PitchProcessor(
                    PitchProcessor.PitchEstimationAlgorithm.YIN,
                    sampleRate.toFloat(),
                    bufferSize,
                    pitchHandler
                )
            )

            audioThread = Thread(dispatcher, "Audio Dispatcher")
            audioThread?.start()

            isRunning = true
            startButton.text = "Stop Detection"
            startButton.background = Color(200, 80, 80)
            mixerComboBox.isEnabled = false
            statusLabel.text = "Detecting pitch from: $selectedMixer"

        } catch (e: Exception) {
            statusLabel.text = "Error: ${e.message}"
            e.printStackTrace()
        }
    }

    private fun stopDetection() {
        dispatcher?.stop()
        audioThread?.join(1000)
        dispatcher = null
        audioThread = null

        isRunning = false
        startButton.text = "Start Detection"
        startButton.background = null
        mixerComboBox.isEnabled = true
        waveformPanel.samples = FloatArray(0)
        currentPitchLabel.text = "Current Pitch: ---"
        statusLabel.text = "Detection stopped"
    }

    private fun resetHighest() {
        highestPitch = -1f
        highestPitchLabel.text = "Highest Pitch: ---"
    }
}

fun main() {
    System.setProperty("sun.java2d.uiScale", "1.0")

    SwingUtilities.invokeLater {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (_: Exception) {}

        PitchDetectorApp().isVisible = true
    }
}
