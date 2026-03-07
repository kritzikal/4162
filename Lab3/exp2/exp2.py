import numpy as np
import matplotlib.pyplot as plt
import scipy.signal as signal
import librosa
#import sounddevice as sd
import scipy.io.wavfile as wav

fs = 16000
duration = 5
'''
print("Recording...")
audio = sd.rec(int(duration * fs), samplerate=fs, channels=1, dtype='float32')
sd.wait()
print("Recording Complete!")

wav.write("speech_recorded.wav", fs, (audio*32767).astype(np.int16))
'''
y,sr = librosa.load("../exp2/1.wav", sr=fs)
frequencies, time, Zxx = signal.stft(y, fs=fs, nperseg=512)

plt.figure(figsize=(10, 5))
plt.pcolormesh(time, frequencies, np.abs(Zxx), shading='gouraud')
plt.colorbar(label="Magnitude")
plt.ylabel('Frequency (Hz)')
plt.xlabel('Time (s)')
plt.title('Spectrogram')
plt.savefig('spectrogram.png')
