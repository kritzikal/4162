import numpy as np
import matplotlib.pyplot as plt
from scipy.io.wavfile import read, write
from scipy.signal import resample
import sounddevice as sd
from scipy.signal import butter, freqz, filtfilt
# Load audio file
sampling_rate, audio = read("../Lab5/with_hum.wav") # Replace with your audio file
audio = audio / np.max(np.abs(audio)) # Normalise the audio signal
print(f"Original Sampling Rate: {sampling_rate} Hz")
print(f"Audio Duration: {len(audio) / sampling_rate:.2f} seconds")

sampling_rate1, audio1 = read("../Lab5/without_hum.wav") # Replace with your audio file
audio1 = audio1 / np.max(np.abs(audio)) # Normalise the audio signal
print(f"Original Sampling Rate: {sampling_rate1} Hz")
print(f"Audio Duration: {len(audio1) / sampling_rate1:.2f} seconds")

# Play the original audio
#print("Playing original audio:")
#sd.play(audio, samplerate=sampling_rate)
#sd.wait() # Wait for playback to finish

# Plotting Time-Domain and Frequency-Domain Signals
def plot_signals_and_spectra(audio_signal, f_s, title):
    # Time-domain plot
    plt.figure(figsize=(12, 5))
    t = np.linspace(0, len(audio_signal) / f_s, len(audio_signal), endpoint=False)
    plt.plot(t, audio_signal)
    plt.title(f"Time-Domain Signal ({title})")
    plt.xlabel("Time (s)")
    plt.ylabel("Amplitude")
    plt.grid()
    plt.show()

    # Frequency-domain plot
    N = len(audio_signal)
    yf = np.fft.fft(audio_signal)
    xf = np.fft.fftfreq(N, 1 / f_s)[:N // 2]  # Positive frequencies
    plt.figure(figsize=(12, 5))
    plt.plot(xf, np.abs(yf[:N // 2]))
    plt.title(f"Frequency Spectrum ({title})")
    plt.xlabel("Frequency (Hz)")
    plt.ylabel("Amplitude")
    plt.grid()
    plt.show()

def design_notch_filter(freq, sampling_rate, Q=30):
    nyquist = 0.5 * sampling_rate
    w0 = freq / nyquist
    b, a = butter(N=2, Wn=[w0 - 0.005, w0 + 0.005], btype='bandstop')
    return b, a
notch_freq = 400
b,a = design_notch_filter(notch_freq, sampling_rate)

w,h = freqz(b, a, worN=8000, fs=sampling_rate)
plt.figure(figsize=(12, 6))
plt.plot(w, 20 * np.log10(abs(h)), label="Notch Filter")
plt.title("Frequency Response of the Notch Filter")
plt.xlabel("Frequency (Hz)")
plt.ylabel("Magnitude (dB)")
plt.grid()
plt.legend()
plt.show()

filtered_audio = filtfilt(b, a, audio)
print("Playing filtered audio:")
sd.play(filtered_audio, samplerate=sampling_rate)
sd.wait()

write("filtered_audio.wav", sampling_rate, (filtered_audio * 32767).astype(np.int16))

def plot_frequency_response(audio_signal, f_s, title):
    N = len(audio_signal)
    yf = np.fft.fft(audio_signal)
    xf = np.fft.fftfreq(N, 1/f_s)[:N // 2]
    magnitude = np.abs(yf[:N // 2])

    plt.figure(figsize=(12, 6))
    plt.plot (xf, magnitude)
    plt.title(title)
    plt.xlabel("Frequency (Hz)")
    plt.ylabel("Magnitude")
    plt.grid()
    plt.show()


plot_frequency_response(audio, sampling_rate, "Frequency Spectrum of Original Audio")
plot_frequency_response(filtered_audio, sampling_rate, "Frequency Spectrum of Filtered Audio")
#plot_signals_and_spectra(audio, sampling_rate, "with_hum.wav")
#plot_signals_and_spectra(audio1, sampling_rate1, "without_hum.wav")