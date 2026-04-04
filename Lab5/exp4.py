import numpy as np
import matplotlib.pyplot as plt
import scipy.signal as signal
from scipy.io import wavfile
from scipy.signal import spectrogram, butter, filtfilt, freqz

fs, data = wavfile.read("../Lab5/with_hum.wav")
frequencies, time, Sxx = signal.stft(data, fs, nperseg=1024)

plt.figure()
plt.pcolormesh(time, frequencies, np.abs(Sxx), shading='gouraud')
plt.colorbar(label="Magnitude")
plt.ylabel('Frequency (Hz)')
plt.xlabel('Time (s)')
plt.title('Original Spectrogram')
plt.savefig("Original_Spectrogram.png")
plt.show()

def design_filter(filter_type, cutoff, fs, order=5, band=None):
    nyq = 0.5 * fs
    if filter_type == 'high':
        normal_cutoff = cutoff / nyq
        b, a = butter(order, normal_cutoff, btype='high', analog=False)

    elif filter_type == 'band':
        normal_band = [b/nyq for b in band]
        b, a = butter(order, normal_band, btype='bandpass', analog=False)
    return b, a

cutoff = 800
b_hp, a_hp = design_filter(filter_type='high', cutoff=cutoff, fs=fs, order=5)
filtered = filtfilt(b_hp,a_hp,data)
frequencies1, time1, Sxx1 = signal.stft(filtered, fs, nperseg=512)
plt.figure()
plt.pcolormesh(time1, frequencies1, np.abs(Sxx1), shading='gouraud')
plt.colorbar(label="Magnitude")
plt.ylabel('Frequency (Hz)')
plt.xlabel('Time (s)')
plt.title('HPF Spectrogram')
plt.savefig("HPF_Spectrogram.png")
plt.show()

band = [800, 3500]
b_bp, a_bp = design_filter(filter_type='band', cutoff=None, fs=fs, order=5, band=band)
filtered1 = filtfilt(b_bp,a_bp,data)
frequencies2, time2, Sxx2 = signal.stft(filtered1, fs, nperseg=512)
plt.figure()
plt.pcolormesh(time2, frequencies2, np.abs(Sxx2), shading='gouraud')
plt.colorbar(label="Magnitude")
plt.ylabel('Frequency (Hz)')
plt.xlabel('Time (s)')
plt.title('BPF Spectrogram')
plt.savefig("BPF_Spectrogram.png")
plt.show()

