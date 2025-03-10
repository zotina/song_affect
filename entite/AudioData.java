package entite;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;

public class AudioData {
    private byte[] rawAudioData;
    private float[] normalizedSamples;
    private AudioFormat audioFormat;
    private float amplificationFactor = 1.0f;
    
    private boolean antiDistortionEnabled = false;
    private float distortionThreshold = 0.7f;
    private float distortionRatio = 4.0f;
    private float distortionMakeupGain = 1.0f;
    private boolean useTanhSoftClipper = true;
    
    private boolean noiseReductionEnabled = false;
    private float noiseReductionAmount = 0.9f;
    private float noiseFloor = 0.05f;
    private float smoothingFactor = 0.7f;
    
    private List<AudioDataListener> listeners = new ArrayList<>();
    
    public interface AudioDataListener {
        void onAudioDataChanged();
        void onAmplificationChanged(float factor);
        void onAntiDistortionChanged(boolean enabled, float threshold, float ratio, float makeupGain);
        void onNoiseReductionChanged(boolean enabled, float amount, float floor, float smoothing);
    }
    
    public void addListener(AudioDataListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(AudioDataListener listener) {
        listeners.remove(listener);
    }
    
    public void setAudioData(byte[] rawData, AudioFormat format, float[] samples) {
        this.rawAudioData = rawData;
        this.audioFormat = format;
        this.normalizedSamples = samples;
        notifyDataChanged();
    }
    
    public void setAmplificationFactor(float factor) {
        this.amplificationFactor = factor;
        notifyAmplificationChanged();
    }
    
    public void setAntiDistortionEnabled(boolean enabled) {
        this.antiDistortionEnabled = enabled;
        notifyAntiDistortionChanged();
    }
    
    public void setAntiDistortionParameters(float threshold, float ratio, float makeupGain) {
        this.distortionThreshold = threshold;
        this.distortionRatio = ratio;
        this.distortionMakeupGain = makeupGain;
        notifyAntiDistortionChanged();
    }
    
    public void setUseTanhSoftClipper(boolean useTanh) {
        this.useTanhSoftClipper = useTanh;
    }
    
    public boolean getUseTanhSoftClipper() {
        return useTanhSoftClipper;
    }
    
    public void setNoiseReductionEnabled(boolean enabled) {
        this.noiseReductionEnabled = enabled;
        notifyNoiseReductionChanged();
    }
    
    public void setNoiseReductionParameters(float amount, float floor, float smoothing) {
        this.noiseReductionAmount = amount;
        this.noiseFloor = floor;
        this.smoothingFactor = smoothing;
        notifyNoiseReductionChanged();
    }
    
    private void notifyDataChanged() {
        for (AudioDataListener listener : listeners) {
            listener.onAudioDataChanged();
        }
    }
    
    private void notifyAmplificationChanged() {
        for (AudioDataListener listener : listeners) {
            listener.onAmplificationChanged(amplificationFactor);
        }
    }
    
    private void notifyAntiDistortionChanged() {
        for (AudioDataListener listener : listeners) {
            listener.onAntiDistortionChanged(
                antiDistortionEnabled, 
                distortionThreshold, 
                distortionRatio, 
                distortionMakeupGain
            );
        }
    }
    
    private void notifyNoiseReductionChanged() {
        for (AudioDataListener listener : listeners) {
            listener.onNoiseReductionChanged(
                noiseReductionEnabled,
                noiseReductionAmount,
                noiseFloor,
                smoothingFactor
            );
        }
    }
    
    public byte[] getRawAudioData() {
        return rawAudioData;
    }
    
    public float[] getNormalizedSamples() {
        return normalizedSamples;
    }
    
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }
    
    public float getAmplificationFactor() {
        return amplificationFactor;
    }
    
    public boolean isAntiDistortionEnabled() {
        return antiDistortionEnabled;
    }
    
    public float getDistortionThreshold() {
        return distortionThreshold;
    }
    
    public float getDistortionRatio() {
        return distortionRatio;
    }
    
    public float getDistortionMakeupGain() {
        return distortionMakeupGain;
    }
    
    public boolean isNoiseReductionEnabled() {
        return noiseReductionEnabled;
    }
    
    public float getNoiseReductionAmount() {
        return noiseReductionAmount;
    }
    
    public float getNoiseFloor() {
        return noiseFloor;
    }
    
    public float getSmoothingFactor() {
        return smoothingFactor;
    }
    
    public boolean hasAudioData() {
        return rawAudioData != null && rawAudioData.length > 0;
    }
}