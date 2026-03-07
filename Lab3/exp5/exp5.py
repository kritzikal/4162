import numpy as np
import scipy.signal as signal
import sounddevice as sd
from scipy.signal import correlate

fs = 16000 #Sampling rate
chunks = 2048 #128ms of audio
low, high = int(fs/500), int(fs/50) #500 Hz repeats every 32 samples, 50 Hz repeats very 320 samples

#Enable real-time processing
def callback(indata, frames, time, status):
    #Detect primary microphone
    y = indata[:,0]
    #Autocorrelation
    corr = correlate(y,y,mode="full")[len(y)-1:]
    #Define threshold for isolating noise
    #Compute pitch using peaks
    if np.max(corr) > 0.1:
        peak = np.argmax(corr[low:high])+ low
        pitch = fs/peak
        print(f"Pitch {pitch:.1f}Hz", end='\r')
#Process sound from microphone in real-time
with sd.InputStream(samplerate=fs, blocksize=chunks, callback=callback, channels=1):
    print("Press Ctrl+C to exit")
    sd.sleep(1000000) #Run for 10 seconds