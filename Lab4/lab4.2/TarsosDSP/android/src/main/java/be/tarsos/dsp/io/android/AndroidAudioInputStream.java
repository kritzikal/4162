package be.tarsos.dsp.io.android;

import android.media.AudioRecord;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import java.io.IOException;

public class AndroidAudioInputStream implements TarsosDSPAudioInputStream {
    private final AudioRecord audioRecord;
    private final TarsosDSPAudioFormat format;

    public AndroidAudioInputStream(AudioRecord audioRecord, TarsosDSPAudioFormat format) {
        this.audioRecord = audioRecord;
        this.format = format;
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        throw new IOException("Skip not supported");
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int samplesRead = audioRecord.read(b, off, len);
        if (samplesRead == AudioRecord.ERROR_INVALID_OPERATION || samplesRead == AudioRecord.ERROR_BAD_VALUE) {
            throw new IOException("Error reading audio data");
        }
        return samplesRead;
    }

    @Override
    public void close() throws IOException {
        audioRecord.stop();
        audioRecord.release();
    }

    @Override
    public TarsosDSPAudioFormat getFormat() {
        return format;
    }

    @Override
    public long getFrameLength() {
        return -1;
    }
}
