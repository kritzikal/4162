import numpy as np
import matplotlib.pyplot as plt

csv_filename = 'sample_sensor_data.csv'
data = np.genfromtxt(csv_filename, delimiter=',').T

timestamps = (data[0] - data[0, 0]) / 1000

accel_data = data[1:4]
gyro_data = data[4:-1]


def peak_detection(t, sig, thresh, tol = 1):
    peaks = []
    max_val = -np.inf
    N = len(sig)
    for i in range(1,N-1):
        #save signal by checking neighboring signal
        if sig[i] > sig[i-1] and sig[i] >= sig[i+1]:
            if sig[i] > max_val:
                max_val = sig[i]
                position = t[i]
        #check if signal drops sigificantly
        if max_val > thresh and sig[i] < max_val - tol:
            peaks.append((position, max_val))
            max_val = -np.inf

    #peaks.append((position, max_val))
    return np.array(peaks)


max_peaks = peak_detection(timestamps, accel_data[0], thresh = -15)

plt.plot(timestamps, accel_data[0])
plt.title("First axis of accelerometer data")
plt.xlabel("Time")
plt.ylabel("Meters per second")
plt.scatter(max_peaks[:, 0], max_peaks[:, 1], color='red')
plt.show()
print(accel_data[0].max())