import numpy as np
import matplotlib.pyplot as plt
import scipy.signal as signal
from scipy.signal import butter, firwin, freqz, lfilter
fs = 48000
t = np.linspace(0, 1, fs)
signal1 = np.sin(2 * np.pi * 1000 * t) + 0.5 * np.sin(2 * np.pi * 3000 * t)

plt.figure()
plt.plot(t[:300], signal1[:300])
plt.title("Original Sine Wave")
plt.xlabel("Time")
plt.ylabel("Amplitude")
plt.show()

cutoff = 2000
order = 4
nyq = 0.5 * fs
normal_cutoff = cutoff/nyq
b_iir, a_iir = butter(order, normal_cutoff, btype='low', analog=False)

w,h = freqz(b_iir, a_iir, worN = 8000)
plt.figure()
plt.plot((w * fs) / (2 * np.pi), np.abs(h))
plt.title("Frequency Response")
plt.xlabel("Frequency (Hz)")
plt.ylabel("Gain")
plt.show()

numtaps = 101
b_fir = firwin(numtaps, cutoff, fs=fs)

w,h = freqz(b_fir, worN=8000)
plt.plot(0.5*fs*w/np.pi, np.abs(h), label='FIR')
plt.title("FIR Filter Frequency Response")
plt.xlabel("Frequency (Hz)")
plt.ylabel("Gain")
plt.grid()
plt.show()

# Apply IIR filter
filtered_iir = signal.lfilter(b_iir, a_iir, signal1)

# Apply FIR filter (a = 1 for FIR)
filtered_fir = signal.lfilter(b_fir, 1.0, signal1)

def plot_fft(data, F_s, title):
    N = len(data)
    yf = np.fft.fft(data)
    xf = np.fft.fftfreq(N, 1/F_s)[:N//2]
    plt.figure(figsize=(12,6))
    plt.plot(xf, 2.0/N * np.abs(yf[:N//2]))
    plt.title(title)
    plt.xlabel("Frequency (Hz)")
    plt.ylabel("Amplitude")
    plt.grid()
    plt.show()


plot_fft(signal1, fs, "FFT of Original Sine Signal")
plot_fft(filtered_iir, fs, "FFT using IIR Filter")
plot_fft(filtered_fir, fs, "FFT using FIR Filter")

n_samples = int(0.005 * fs)

plt.figure(figsize=(12, 10))

# Subplot 1: Original Signal
plt.subplot(3, 1, 1)
plt.plot(t[:n_samples] * 1000, signal1[:n_samples], label="Original (1kHz + 3kHz)")
plt.title("Time Domain: Original Signal")
plt.ylabel("Amplitude")
plt.grid(True)
plt.legend(loc='upper right')

# Subplot 2: IIR Filtered (Butterworth)
plt.subplot(3, 1, 2)
plt.plot(t[:n_samples] * 1000, filtered_iir[:n_samples], color='orange', label="IIR Low-pass (2kHz Cutoff)")
plt.title("Time Domain: IIR Filtered")
plt.ylabel("Amplitude")
plt.grid(True)
plt.legend(loc='upper right')

# Subplot 3: FIR Filtered (Windowed)
plt.subplot(3, 1, 3)
plt.plot(t[:n_samples] * 1000, filtered_fir[:n_samples], color='green', label="FIR Low-pass (2kHz Cutoff)")
plt.title("Time Domain: FIR Filtered")
plt.xlabel("Time (ms)")
plt.ylabel("Amplitude")
plt.grid(True)
plt.legend(loc='upper right')

plt.tight_layout()
plt.show()
