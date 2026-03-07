import numpy as np
import matplotlib.pyplot as plt
import soundfile as sf
import librosa
filename = '../exp3/test_vector.wav'
#signal, sample_rate = sf.read(filename)
y, sr = librosa.load(filename)

#if len(signal.shape) > 1:
    #signal = np.mean(signal, axis=1)
#Frame settings
FRAME_SIZE = int(0.01*sr)
HOP_SIZE = FRAME_SIZE//2
#Definite Zero-Crossing-Rate and Energy using librosa and frame settings
#Flatten to get 1D array
zcr=librosa.feature.zero_crossing_rate(y, frame_length=FRAME_SIZE, hop_length=HOP_SIZE, center = False)[0]
energy = librosa.feature.rms(y=y, frame_length=FRAME_SIZE, hop_length=HOP_SIZE, center = False)[0]
#Define thresholds as mean for simplicity
energy_threshold = np.mean(energy)
zcr_threshold = np.mean(zcr)
#Extract total number of frames
num_frames = 1 + (len(y) - FRAME_SIZE) // HOP_SIZE
print(num_frames)
#Label voiced and unvoiced based on thresholds
labels = []
for e,z in zip(energy, zcr):
    if e > energy_threshold:
        if z < zcr_threshold:
            labels.append("Voiced")
        else:
            labels.append("Unvoiced")
    else:
        labels.append("Unvoiced")
#print(labels)
#threshold = np.mean(energy_values)
#voiced_labels = energy_values > threshold

#Separate voiced and unvoiced frames into separate arrays
voiced_labels = []
unvoiced_labels = []
for i in range(len(labels)):
    if labels[i] == "Voiced":
        voiced_labels.append("Voiced")
    if labels[i] == "Unvoiced":
        unvoiced_labels.append("Unvoiced")
print("Voiced:",len(voiced_labels))
print("Unvoiced",len(unvoiced_labels))

# Plot Energy with Voiced/Unvoiced Classification
is_voiced = (energy > energy_threshold) & (zcr < zcr_threshold)
plt.figure(figsize=(10, 4))
plt.plot(energy, label="Frame Energy")
plt.axhline(energy_threshold, color="red", linestyle="--", label="Energy Threshold")
plt.fill_between(range(num_frames), energy, energy_threshold, where=is_voiced,
                 color='green', alpha=0.3, label="Voiced Frames")
plt.xlabel("Frame Index")
plt.ylabel("Energy")
plt.title("Voiced/Unvoiced Classification Using Energy")
plt.legend()
plt.savefig("Energy.png")
plt.show()
# Plot ZCR with Voiced/Unvoiced Classification
plt.figure(figsize=(10, 4))
plt.plot(zcr, label="ZCR")
plt.axhline(zcr_threshold, color="red", linestyle="--", label="ZCR Threshold")
plt.fill_between(range(len(zcr)), zcr, zcr_threshold, where= is_voiced,
                 color='green', alpha=0.3, label="Voiced Frames")
plt.xlabel("Frame Index")
plt.ylabel("ZCR Rate")
plt.title("Voiced/Unvoiced Classification Using ZCR")
plt.legend()
plt.savefig("ZCR.png")
plt.show()

# Print Results
#num_voiced = np.sum(voiced_labels)
#num_unvoiced = len(voiced_labels) - num_voiced
print(f"Total Frames: {num_frames}")
print(f"Voiced Frames: {len(voiced_labels)} ({len(voiced_labels)/num_frames:.2%})")
print(f"Unvoiced Frames: {len(unvoiced_labels)} ({len(unvoiced_labels)/num_frames:.2%})")
print(f"Energy Threshold Used: {energy_threshold:.5f}")
print(f"ZCR Threshold Used: {zcr_threshold:.5f}")

