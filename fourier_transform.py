import numpy as np
import matplotlib.pyplot as plt
from scipy.fft import fft, fftfreq

csv_filename = 'sample_sensor_data.csv'
data = np.genfromtxt(csv_filename, delimiter=',').T

timestamps = (data[0] - data[0, 0]) / 1000

accel_data = data[1:4]
gyro_data = data[4:-1]
dt = np.diff(timestamps,prepend=timestamps[0]) #time intervals
velocity = np.cumsum(accel_data*dt,axis=1) #integration of acceleration
displacement = np.cumsum(velocity*dt,axis=1) #integration of velocity
axis = 1




fig, axes = plt.subplots(3, 1, sharex=True, figsize=(10, 8))
axes[0].plot(timestamps, accel_data[axis])
axes[0].set_ylabel('Accel ')

axes[1].plot(timestamps, velocity[axis])
axes[1].set_ylabel('Velocity ')

axes[2].plot(timestamps, displacement[axis])
axes[2].set_ylabel('Displacement ')

axes[-1].set_xlabel('Time')
plt.tight_layout()
plt.show()

fig,axes = plt.subplots(3,1,figsize=(10,8))
labels = ['X','Y','Z']
N=accel_data.shape[1] #label for 3 axes
sampling_rate = 1/ np.mean(np.diff(timestamps)) #sampling freq(Hz)
freqs = fftfreq(N, d=1/sampling_rate) #frequency bins
#plot all fourier transform on 3 axes
for i in range(3):
    fft_magnitude = np.abs(fft(accel_data[i])) #compute FFT
    axes[i].plot(freqs[:N // 2], fft_magnitude[:N // 2]) #plot positive freq
    axes[i].set_ylabel(f'{labels[i]} Amplitude')
    axes[i].grid(True)
axes[0].set_title('Frequency Spectrum - All Axes')
axes[-1].set_xlabel('Frequency (Hz)')
plt.tight_layout()
plt.show()