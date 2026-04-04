import numpy as np
import matplotlib.pyplot as plt
from scipy import signal

# Your filter design here
# firls() can be called via signal.firls()
fs=48000
nyquist = fs/2
num_taps = 101

bands = [0, 800, 1000, 2000, 2200, nyquist]
bands_norms = [b/nyquist for b in bands]
desired = [1, 1, 0.1, 0.1, 1, 1]
b = signal.firls(num_taps, bands_norms, desired)
# Signal analysis
w, h = signal.freqz(b)

t= np.linspace(0, 2, 2*fs, endpoint=False)
test_data = signal.chirp(t, 1, 2, 24000, method='logarithmic')
filtered_data = signal.lfilter(b, 1, test_data)

plt.figure(figsize=(12,6))
plt.plot(t[:2000], test_data[:2000], label = "Original Chirp Signal", alpha=0.7)
plt.plot(t[:2000], filtered_data[:2000], label = "Filtered Chirp Signal", alpha=0.7)
plt.title("Time-Domain Comparison")
plt.xlabel("Time (s)")
plt.ylabel("Amplitude")
plt.legend()
plt.grid()
plt.show()

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

plot_fft(test_data, fs, "FFT of Original Chirp Signal")
plot_fft(filtered_data, fs, "FFT of Filtered Chirp Signal")



plt.figure()
plt.subplot(2, 1, 1)
plt.title('Digital filter frequency response, N = ' + str(len(b)))
plt.plot(w / np.pi, 20 * np.log10(abs(h)), 'b')
plt.ylabel('Amplitude [dB]', color='b')
plt.grid()
plt.axis('tight')

plt.subplot(2, 1, 2)
angles = np.unwrap(np.angle(h))
plt.plot(w / np.pi, angles, 'g')
plt.ylabel('Angle (radians)', color='g')
plt.grid()
plt.axis('tight')
plt.xlabel('Frequency [0 to Nyquist Hz, normalized]')
plt.show()