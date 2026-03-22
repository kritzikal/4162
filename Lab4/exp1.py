import numpy as np
import matplotlib.pyplot as plt
from scipy.signal import convolve
from scipy import ndimage
from scipy.fft import fft


fs = 20000
T = 0.01
t = np.linspace(0, T, int(T*fs), endpoint=False)

seg1 = np.sin(2*np.pi*500*t)
seg2 = np.cos(2*np.pi*500*t)

# Plot signals
plt.figure()
plt.plot(t, seg1, label = 'Sine')
plt.plot(t, seg2, label = 'Cosine')
plt.legend()
plt.show()

# Manual convolution
def linear_convolution(x, h):
    N = len(x)
    M = len(h)
    y = np.zeros(N+M-1)
    for n in range(N+M-1):
        for k in range(N):
            if 0 <= n-k < M:
                y[n] += x[k] * h[n-k]
    return y
def circular_convolution(x,h):
    N = len(x)
    if len(h)!=N:
        h = np.pad(h, (0,N-len(h)))
    y = np.zeros(N)
    for n in range(N):
        for k in range(N):
            y[n] += x[k] * h[(n-k)%N]
    return y
def scipy_convolution(x, h):
    N = max(len(x),len(h))
    X = np.fft.fft(x)
    H = np.fft.fft(h)
    Y = X*H
    y = np.fft.ifft(Y)
    return np.real(y)
dt = 1/fs

manual = linear_convolution(seg1, seg2) * dt
conv_np = convolve(seg1, seg2, mode='full') * dt
manual_circular = circular_convolution(seg1, seg2) * dt
circ_np = scipy_convolution(seg1, seg2) * dt
# Time axes
t_conv = np.arange(0, len(manual)) * dt

# Plot convolution
plt.figure()
plt.plot(t_conv[:2000], conv_np[:2000])
plt.title("Scipy Convolution (zoomed)")
plt.show()

plt.figure()
plt.plot(t_conv[:2000], manual[:2000])
plt.title("Manual Convolution (zoomed)")
plt.show()

plt.figure()
plt.plot(t[:4000], manual_circular[:4000])
plt.title("Manual Circular Convolution (zoomed)")
plt.savefig("manual_circular.png")
plt.show()

plt.figure()
plt.plot(t[:4000], circ_np[:4000])
plt.title("Scipy Circular Convolution (zoomed)")
plt.savefig("scipy_circular.png")
plt.show()