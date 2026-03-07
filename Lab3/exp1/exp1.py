import numpy as np
import matplotlib.pyplot as plt
import scipy.signal as signal
import librosa
import librosa.display
import sounddevice as sd
import scipy.io.wavfile as wav

fs = 5000
T = 10

# First half: 100Hz (0-5s)
t1 = np.linspace(0, 5, fs*5)
seg1 = np.sin(2*np.pi*100*t1)

# Second half: 500Hz (5-10s)
t2 = np.linspace(5, 10, fs*5)
seg2 = np.sin(2*np.pi*500*t2)

# Full time axis
t = np.linspace(0, T, fs*T)

# 200+700Hz mix throughout
ta = np.sin(2*np.pi*200*t) + np.sin(2*np.pi*700*t)

# Combine using concatenate, then add ta
c = np.concatenate([seg1, seg2]) + ta

# Plot 100Hz segment
plt.figure(figsize=(10, 4))
plt.plot(t1[:500], seg1[:500])
plt.title('100 Hz signal')
plt.xlabel('Time (s)')
plt.ylabel('Amplitude')
plt.savefig('100Hz.png')

# Plot 500Hz segment
plt.figure(figsize=(10, 4))
plt.plot(t2[:500], seg2[:500])
plt.title('500 Hz signal')
plt.xlabel('Time (s)')
plt.ylabel('Amplitude')
plt.savefig('500Hz.png')

# Plot 200+700Hz mix
plt.figure(figsize=(10, 4))
plt.plot(t[:500], ta[:500])
plt.title('Mix of 200 and 700 Hz signal')
plt.xlabel('Time (s)')
plt.ylabel('Amplitude')
plt.savefig('200_700Hz.png')

# Plot combined signal
plt.figure(figsize=(10, 4))
plt.plot(t[:500], c[:500])
plt.title('Combined signal (first 0.1s)')
plt.xlabel('Time (s)')
plt.ylabel('Amplitude')
plt.savefig('combined.png')

# STFT Spectrogram
frequencies, time, Zxx = signal.stft(c, fs=fs, nperseg=64)
plt.figure(figsize=(10, 6))
plt.pcolormesh(time, frequencies, np.abs(Zxx), shading='gouraud')
plt.colorbar(label="Magnitude")
plt.ylabel("Frequency (Hz)")
plt.xlabel("Time (s)")
plt.title("STFT Spectrogram")
plt.savefig('spectrogram.png')
