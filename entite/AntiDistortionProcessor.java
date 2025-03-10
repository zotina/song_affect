
package entite;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;


public class AntiDistortionProcessor {
    
    
    private float threshold = 0.7f;
    private float ratio = 4.0f;
    private float makeupGain = 1.0f;
    private boolean useTanhSoftClipper = true;
    
    private static final int LOOK_AHEAD_MS = 5; 
    private int lookAheadSamples;
    
    private float[] lookAheadBuffer;
    private int bufferSize = 1024; 
    
    public AntiDistortionProcessor() {
        lookAheadBuffer = null;
    }
    
    public void setParameters(float threshold, float ratio, float makeupGain) {
        this.threshold = threshold;
        this.ratio = ratio;
        this.makeupGain = makeupGain;
    }
    
    public void setUseTanhSoftClipper(boolean useTanh) {
        this.useTanhSoftClipper = useTanh;
    }
    
   
    public byte[] processAudio(byte[] audioData, AudioFormat format) {
        if (audioData == null) {
            return null;
        }
        
        int sampleRate = (int) format.getSampleRate();
        lookAheadSamples = (sampleRate * LOOK_AHEAD_MS) / 1000;
        
        int numChannels = format.getChannels();
        
        float[][] samples = convertToFloatSamples(audioData, format);
        
        for (int channel = 0; channel < numChannels; channel++) {
            processChannelWithLookAhead(samples[channel]);
        }
        
        return convertToByteArray(samples, format);
    }
    
   
    private void processChannelWithLookAhead(float[] samples) {
        if (lookAheadBuffer == null || lookAheadBuffer.length < samples.length + lookAheadSamples) {
            lookAheadBuffer = new float[samples.length + lookAheadSamples];
        }
        
        System.arraycopy(samples, 0, lookAheadBuffer, 0, samples.length);
        for (int i = 0; i < lookAheadSamples; i++) {
            lookAheadBuffer[samples.length + i] = 0.0f; 
        }
        
        for (int i = 0; i < samples.length; i++) {
            float maxValue = Math.abs(lookAheadBuffer[i]);
            for (int j = 1; j <= lookAheadSamples; j++) {
                float value = Math.abs(lookAheadBuffer[i + j]);
                if (value > maxValue) {
                    maxValue = value;
                }
            }
            
            float gain = 1.0f;
            if (maxValue > threshold) {
                float overThreshold = maxValue - threshold;
                float compressionFactor = calculateDynamicCurve(overThreshold);
                gain = (threshold + (overThreshold / compressionFactor)) / maxValue;
            }
            
            float processedSample = lookAheadBuffer[i] * gain * makeupGain;
            
            if (useTanhSoftClipper) {
                processedSample = applyTanhSoftClipper(processedSample);
            } else {
                processedSample = Math.max(-1.0f, Math.min(1.0f, processedSample));
            }
            
            samples[i] = processedSample;
        }
    }
    
    private float calculateDynamicCurve(float overThreshold) {
        float baseRatio = ratio;
        float dynamicRatio = baseRatio * (1.0f + (overThreshold * 2.0f));
        
        return Math.min(20.0f, dynamicRatio);
    }
    
    private float applyTanhSoftClipper(float sample) {
        float drive = 1.5f; 
        
        return (float) Math.tanh(sample * drive) / drive;
    }
    
    private float[][] convertToFloatSamples(byte[] audioData, AudioFormat format) {
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        int numChannels = format.getChannels();
        int samplesPerChannel = audioData.length / (bytesPerSample * numChannels);
        
        float[][] samples = new float[numChannels][samplesPerChannel];
        
        ByteBuffer bb = ByteBuffer.wrap(audioData);
        if (format.isBigEndian()) {
            bb.order(ByteOrder.BIG_ENDIAN);
        } else {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        
        for (int i = 0; i < samplesPerChannel; i++) {
            for (int channel = 0; channel < numChannels; channel++) {
                int sampleIndex = i * numChannels + channel;
                
                if (bytesPerSample == 1) {
                    int byteIndex = sampleIndex;
                    if (byteIndex < audioData.length) {
                        samples[channel][i] = ((audioData[byteIndex] & 0xFF) - 128) / 128.0f;
                    }
                } else if (bytesPerSample == 2) {
                    int byteIndex = sampleIndex * 2;
                    if (byteIndex + 1 < audioData.length) {
                        short shortSample = bb.getShort(byteIndex);
                        samples[channel][i] = shortSample / 32768.0f;
                    }
                } else if (bytesPerSample == 3) {
                    int byteIndex = sampleIndex * 3;
                    if (byteIndex + 2 < audioData.length) {
                        int intSample;
                        if (format.isBigEndian()) {
                            intSample = ((audioData[byteIndex] & 0xFF) << 16) | 
                                       ((audioData[byteIndex + 1] & 0xFF) << 8) | 
                                       (audioData[byteIndex + 2] & 0xFF);
                        } else {
                            intSample = (audioData[byteIndex] & 0xFF) | 
                                       ((audioData[byteIndex + 1] & 0xFF) << 8) | 
                                       ((audioData[byteIndex + 2] & 0xFF) << 16);
                        }
                        if ((intSample & 0x800000) != 0) {
                            intSample |= 0xFF000000;
                        }
                        samples[channel][i] = intSample / 8388608.0f;
                    }
                }
            }
        }
        
        return samples;
    }
    
    private byte[] convertToByteArray(float[][] samples, AudioFormat format) {
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        int numChannels = format.getChannels();
        int samplesPerChannel = samples[0].length;
        
        byte[] audioData = new byte[samplesPerChannel * numChannels * bytesPerSample];
        
        ByteBuffer bb = ByteBuffer.wrap(audioData);
        if (format.isBigEndian()) {
            bb.order(ByteOrder.BIG_ENDIAN);
        } else {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        
        for (int i = 0; i < samplesPerChannel; i++) {
            for (int channel = 0; channel < numChannels; channel++) {
                int sampleIndex = i * numChannels + channel;
                float floatSample = samples[channel][i];
                
                if (bytesPerSample == 1) {
                    int sample = Math.round(floatSample * 128.0f) + 128;
                    sample = Math.min(255, Math.max(0, sample));
                    audioData[sampleIndex] = (byte) sample;
                } else if (bytesPerSample == 2) {
                    int sample = Math.round(floatSample * 32768.0f);
                    sample = Math.min(32767, Math.max(-32768, sample));
                    bb.putShort(sampleIndex * 2, (short) sample);
                } else if (bytesPerSample == 3) {
                    int sample = Math.round(floatSample * 8388608.0f);
                    sample = Math.min(8388607, Math.max(-8388608, sample));
                    
                    int byteIndex = sampleIndex * 3;
                    if (format.isBigEndian()) {
                        audioData[byteIndex] = (byte) (sample >> 16);
                        audioData[byteIndex + 1] = (byte) (sample >> 8);
                        audioData[byteIndex + 2] = (byte) sample;
                    } else {
                        audioData[byteIndex] = (byte) sample;
                        audioData[byteIndex + 1] = (byte) (sample >> 8);
                        audioData[byteIndex + 2] = (byte) (sample >> 16);
                    }
                }
            }
        }
        
        return audioData;
    }
}