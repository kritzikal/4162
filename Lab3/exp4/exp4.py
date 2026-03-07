import numpy as np
import matplotlib.pyplot as plt
import soundfile as sf
from scipy.signal import correlate

fs = 1000 #Sampling rate
t = np.arange(0, 1, 1/fs) #1 sec time interval with 1 ms time samples
#5 Hz Sine wave
f1 = 5
signal1 = np.sin(2*np.pi*f1*t)

#Delayed 5 Hz sine wave
delay = 50 #delay in samples
signal2 = np.roll(signal1, delay) #shift by 50 samples to right

#Random noise
signal3 = np.random.randn(len(t))

def compute_autocorrelation(signal):
    n = len(signal)
    autocorr = np.zeros(n)
    for k in range(n):
        for i in (range(n-k)):
            autocorr[k] += signal[i]*signal[i+k]
    return autocorr
#Compute autocorrelation
autocorr1 = compute_autocorrelation(signal1)
autocorr3 = compute_autocorrelation(signal3)
#Plot autocorrelation
plt.figure(figsize=(10,4))
plt.plot(autocorr1, label="Autocorrelation of Sine Wave")
plt.plot(autocorr3, label="Correlation of Random Noise", linestyle='--')
plt.xlabel("Lag")
plt.ylabel("Normalized Autocorrelation")
plt.title("Autocorrelation of Signals")
plt.legend()
plt.savefig("autocorrelation.png")
plt.show()

def cross_correlation(signal_a, signal_b):
    N = len(signal_a)
    M = len(signal_b)
    lags = np.arange(-(M-1),N)
    cross_corr = []
    norm = np.sqrt(np.sum(signal_a**2)*np.sum(signal_b**2))
    for k in lags:
        cur_sum = 0
        for i in range(N):
            if 0<=i-k<M:
                cur_sum+= signal_a[i]*signal_b[i-k]
        cross_corr.append(cur_sum/norm)

    return np.array(cross_corr), lags
cross_corr, lags = cross_correlation(signal1, signal2)

plt.figure(figsize=(10,4))
plt.plot(lags,cross_corr, label="Cross-Correlation (Signal 1 vs. Signal 2)")
plt.xlabel("Lag samples")
plt.ylabel("Normalized Correlation")
plt.title("Cross-Correlation between Two Signals")
plt.legend()
plt.savefig("cross_correlation.png")
plt.show()