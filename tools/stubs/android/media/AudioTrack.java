package android.media;
public class AudioTrack { public static final int MODE_STATIC = 0;
    public int write(short[] d, int o, int n) { return n; } public void setVolume(float v) {} public int setLoopPoints(int a, int b, int c) { return 0; }
    public void stop() {} public void play() {} public void pause() {} public void release() {} public void reloadStaticData() {}
    public static class Builder { public Builder setAudioAttributes(AudioAttributes a) { return this; } public Builder setAudioFormat(AudioFormat f) { return this; } public Builder setBufferSizeInBytes(int b) { return this; } public Builder setTransferMode(int m) { return this; } public AudioTrack build() { return null; } } }
