package entite;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

public class NoiseReductionProcessor {

    private static final int FFT_SIZE = 2048; 
    private static final int HOP_SIZE = FFT_SIZE / 4; 
    
    private float reductionFactor = 0.9f;    
    private float noiseFloor = 0.05f;        
    private float smoothingFactor = 0.7f;    
    
    private float[] noiseProfile = null;
    private boolean noiseProfileEstimated = false;
    private boolean hasLearnedNoiseProfile = false;
    
    private float[] inputBuffer = new float[FFT_SIZE];
    private float[] outputBuffer = new float[FFT_SIZE];
    private float[] window = new float[FFT_SIZE];
    private float[] prevPhase = new float[FFT_SIZE / 2 + 1];
    private float[] prevMagnitude = new float[FFT_SIZE / 2 + 1];
    
    public NoiseReductionProcessor() {
        for (int i = 0; i < FFT_SIZE; i++) {
            window[i] = 0.5f * (1 - (float) Math.cos(2 * Math.PI * i / (FFT_SIZE - 1)));
        }
    }
    
    public void setParameters(float reductionFactor, float noiseFloor, float smoothingFactor) {
        this.reductionFactor = reductionFactor;
        this.noiseFloor = noiseFloor;
        this.smoothingFactor = smoothingFactor;
    }

    public byte[] processAudio(byte[] audioData, AudioFormat format) {
        if (audioData == null) {
            return null;
        }
        
        float[] samples = convertToFloatSamples(audioData, format);
        
        if (!noiseProfileEstimated && !hasLearnedNoiseProfile) {
            estimateNoiseProfile(samples);
        }
        
        float[] processedSamples = applyNoiseReduction(samples);
        
        return convertToByteArray(processedSamples, format);
    }
    

    public boolean learnNoiseProfile(File noiseFile) {
        try {
            WavFileHandler.AudioLoadResult loadResult = WavFileHandler.loadWavFile(noiseFile);
            byte[] noiseData = loadResult.getRawAudioData();
            AudioFormat format = loadResult.getFormat();
            
            float[] noiseSamples = convertToFloatSamples(noiseData, format);
            
            learnNoiseProfileFromSamples(noiseSamples);
            
            return true;
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void learnNoiseProfileFromSamples(float[] noiseSamples) {
        noiseProfile = new float[FFT_SIZE / 2 + 1];
        Arrays.fill(noiseProfile, 0.0f);
        
        int numFrames = Math.max(1, (noiseSamples.length - FFT_SIZE) / HOP_SIZE + 1);
        
        for (int frameIndex = 0; frameIndex < numFrames; frameIndex++) {
            int startIndex = frameIndex * HOP_SIZE;
            if (startIndex + FFT_SIZE > noiseSamples.length) break;
            
            for (int i = 0; i < FFT_SIZE; i++) {
                inputBuffer[i] = noiseSamples[startIndex + i] * window[i];
            }
            
            float[] fftReal = new float[FFT_SIZE];
            float[] fftImag = new float[FFT_SIZE];
            System.arraycopy(inputBuffer, 0, fftReal, 0, FFT_SIZE);
            Arrays.fill(fftImag, 0.0f);
            computeFFT(fftReal, fftImag);
            
            float[] magnitude = new float[FFT_SIZE / 2 + 1];
            for (int i = 0; i <= FFT_SIZE / 2; i++) {
                magnitude[i] = (float) Math.sqrt(fftReal[i] * fftReal[i] + fftImag[i] * fftImag[i]);
            }
            
            for (int i = 0; i <= FFT_SIZE / 2; i++) {
                noiseProfile[i] += magnitude[i] / numFrames;
            }
        }
        
        for (int i = 0; i <= FFT_SIZE / 2; i++) {
            noiseProfile[i] = Math.max(noiseProfile[i], 1e-6f);
        }
        
        hasLearnedNoiseProfile = true;
        noiseProfileEstimated = true;
    }
    

    public void learnNoiseProfileFromSection(byte[] audioData, AudioFormat format, 
                                            int startSample, int endSample) {
        float[] allSamples = convertToFloatSamples(audioData, format);
        
        int sectionLength = endSample - startSample + 1;
        if (sectionLength <= 0 || startSample < 0 || endSample >= allSamples.length) {
            return;
        }
        
        float[] noiseSamples = new float[sectionLength];
        System.arraycopy(allSamples, startSample, noiseSamples, 0, sectionLength);
        
        learnNoiseProfileFromSamples(noiseSamples);
    }
    

    private void estimateNoiseProfile(float[] samples) {
        noiseProfile = new float[FFT_SIZE / 2 + 1];
        Arrays.fill(noiseProfile, 0.0f);
        
        int framesToUse = Math.min(10, samples.length / HOP_SIZE);
        
        for (int frameIndex = 0; frameIndex < framesToUse; frameIndex++) {
            int startIndex = frameIndex * HOP_SIZE;
            if (startIndex + FFT_SIZE > samples.length) break;
            
            for (int i = 0; i < FFT_SIZE; i++) {
                inputBuffer[i] = samples[startIndex + i] * window[i];
            }
            
            float[] fftReal = new float[FFT_SIZE];
            float[] fftImag = new float[FFT_SIZE];
            System.arraycopy(inputBuffer, 0, fftReal, 0, FFT_SIZE);
            Arrays.fill(fftImag, 0.0f);
            computeFFT(fftReal, fftImag);
            
            float[] magnitude = new float[FFT_SIZE / 2 + 1];
            for (int i = 0; i <= FFT_SIZE / 2; i++) {
                magnitude[i] = (float) Math.sqrt(fftReal[i] * fftReal[i] + fftImag[i] * fftImag[i]);
            }
            
            for (int i = 0; i <= FFT_SIZE / 2; i++) {
                noiseProfile[i] += magnitude[i] / framesToUse;
            }
        }
        
        for (int i = 0; i <= FFT_SIZE / 2; i++) {
            noiseProfile[i] = Math.max(noiseProfile[i], 1e-6f);
        }
        
        noiseProfileEstimated = true;
    }
    
    private float[] applyNoiseReduction(float[] samples) {
        float[] output = new float[samples.length];
        
        for (int frameIndex = 0; frameIndex < samples.length / HOP_SIZE; frameIndex++) {
            int startIndex = frameIndex * HOP_SIZE;
            if (startIndex + FFT_SIZE > samples.length) break;
            
            for (int i = 0; i < FFT_SIZE; i++) {
                if (startIndex + i < samples.length) {
                    inputBuffer[i] = samples[startIndex + i] * window[i];
                } else {
                    inputBuffer[i] = 0;
                }
            }
            
            float[] fftReal = new float[FFT_SIZE];
            float[] fftImag = new float[FFT_SIZE];
            System.arraycopy(inputBuffer, 0, fftReal, 0, FFT_SIZE);
            Arrays.fill(fftImag, 0.0f);
            computeFFT(fftReal, fftImag);
            
            float[] magnitude = new float[FFT_SIZE / 2 + 1];
            float[] phase = new float[FFT_SIZE / 2 + 1];
            for (int i = 0; i <= FFT_SIZE / 2; i++) {
                magnitude[i] = (float) Math.sqrt(fftReal[i] * fftReal[i] + fftImag[i] * fftImag[i]);
                phase[i] = (float) Math.atan2(fftImag[i], fftReal[i]);
            }
            
            for (int i = 0; i <= FFT_SIZE / 2; i++) {
                float snr = magnitude[i] / noiseProfile[i];
                float gain = Math.max(0, 1 - ((5.0f * reductionFactor) * noiseProfile[i] / Math.max(magnitude[i], noiseFloor)));
                if (frameIndex > 0) {
                    gain = smoothingFactor * gain + (1 - smoothingFactor) * (prevMagnitude[i] / Math.max(magnitude[i], 1e-6f));
                }
                
                magnitude[i] *= gain;
                
                prevMagnitude[i] = magnitude[i];
                prevPhase[i] = phase[i];
            }
            
            for (int i = 0; i <= FFT_SIZE / 2; i++) {
                fftReal[i] = magnitude[i] * (float) Math.cos(phase[i]);
                fftImag[i] = magnitude[i] * (float) Math.sin(phase[i]);
            }
            
            for (int i = 1; i < FFT_SIZE / 2; i++) {
                fftReal[FFT_SIZE - i] = fftReal[i];
                fftImag[FFT_SIZE - i] = -fftImag[i];
            }
            
            computeIFFT(fftReal, fftImag);
            
            for (int i = 0; i < FFT_SIZE; i++) {
                if (startIndex + i < output.length) {
                    output[startIndex + i] += fftReal[i] * window[i] / (FFT_SIZE / HOP_SIZE / 2);
                }
            }
        }
        
        return output;
    }
    
    private float[] convertToFloatSamples(byte[] audioData, AudioFormat format) {
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        int numSamples = audioData.length / bytesPerSample;
        float[] samples = new float[numSamples];
        
        ByteBuffer bb = ByteBuffer.wrap(audioData);
        if (format.isBigEndian()) {
            bb.order(ByteOrder.BIG_ENDIAN);
        } else {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        
        for (int i = 0; i < numSamples; i++) {
            if (bytesPerSample == 1) {
                samples[i] = ((audioData[i] & 0xFF) - 128) / 128.0f;
            } else if (bytesPerSample == 2) {
                if (i * 2 + 1 < audioData.length) {
                    short shortSample = bb.getShort(i * 2);
                    samples[i] = shortSample / 32768.0f;
                }
            } else if (bytesPerSample == 3) {
                if (i * 3 + 2 < audioData.length) {
                    int intSample;
                    if (format.isBigEndian()) {
                        intSample = ((audioData[i * 3] & 0xFF) << 16) | 
                                   ((audioData[i * 3 + 1] & 0xFF) << 8) | 
                                   (audioData[i * 3 + 2] & 0xFF);
                    } else {
                        intSample = (audioData[i * 3] & 0xFF) | 
                                   ((audioData[i * 3 + 1] & 0xFF) << 8) | 
                                   ((audioData[i * 3 + 2] & 0xFF) << 16);
                    }
                    if ((intSample & 0x800000) != 0) {
                        intSample |= 0xFF000000;
                    }
                    samples[i] = intSample / 8388608.0f;
                }
            }
        }
        
        return samples;
    }
    
    private byte[] convertToByteArray(float[] samples, AudioFormat format) {
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        byte[] audioData = new byte[samples.length * bytesPerSample];
        
        ByteBuffer bb = ByteBuffer.wrap(audioData);
        if (format.isBigEndian()) {
            bb.order(ByteOrder.BIG_ENDIAN);
        } else {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        
        for (int i = 0; i < samples.length; i++) {
            if (bytesPerSample == 1) {
                int sample = Math.round(samples[i] * 128.0f) + 128;
                sample = Math.min(255, Math.max(0, sample));
                audioData[i] = (byte) sample;
            } else if (bytesPerSample == 2) {
                int sample = Math.round(samples[i] * 32768.0f);
                sample = Math.min(32767, Math.max(-32768, sample));
                bb.putShort(i * 2, (short) sample);
            } else if (bytesPerSample == 3) {
                int sample = Math.round(samples[i] * 8388608.0f);
                sample = Math.min(8388607, Math.max(-8388608, sample));
                
                if (format.isBigEndian()) {
                    audioData[i * 3] = (byte) (sample >> 16);
                    audioData[i * 3 + 1] = (byte) (sample >> 8);
                    audioData[i * 3 + 2] = (byte) sample;
                } else {
                    audioData[i * 3] = (byte) sample;
                    audioData[i * 3 + 1] = (byte) (sample >> 8);
                    audioData[i * 3 + 2] = (byte) (sample >> 16);
                }
            }
        }
        
        return audioData;
    }
    
    private void computeFFT(float[] real, float[] imag) {
        int n = real.length;
        
        int shift = 1 + Integer.numberOfLeadingZeros(n);
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> shift;
            if (j > i) {
                float tempReal = real[i];
                float tempImag = imag[i];
                real[i] = real[j];
                imag[i] = imag[j];
                real[j] = tempReal;
                imag[j] = tempImag;
            }
        }
        
        for (int size = 2; size <= n; size *= 2) {
            float angle = (float) (-2 * Math.PI / size);
            float wReal = (float) Math.cos(angle);
            float wImag = (float) Math.sin(angle);
            
            for (int i = 0; i < n; i += size) {
                float tReal = 1.0f;
                float tImag = 0.0f;
                
                for (int j = 0; j < size / 2; j++) {
                    int a = i + j;
                    int b = i + j + size / 2;
                    
                    float aReal = real[a];
                    float aImag = imag[a];
                    float bReal = real[b] * tReal - imag[b] * tImag;
                    float bImag = real[b] * tImag + imag[b] * tReal;
                    
                    real[a] = aReal + bReal;
                    imag[a] = aImag + bImag;
                    real[b] = aReal - bReal;
                    imag[b] = aImag - bImag;
                    
                    float nextTReal = tReal * wReal - tImag * wImag;
                    float nextTImag = tReal * wImag + tImag * wReal;
                    tReal = nextTReal;
                    tImag = nextTImag;
                }
            }
        }
    }
    
    private void computeIFFT(float[] real, float[] imag) {
        int n = real.length;
        
        for (int i = 0; i < n; i++) {
            imag[i] = -imag[i];
        }
        
        computeFFT(real, imag);
        
        for (int i = 0; i < n; i++) {
            imag[i] = -imag[i] / n;
            real[i] = real[i] / n;
        }
    }
    
    public void resetNoiseProfile() {
        noiseProfileEstimated = false;
        hasLearnedNoiseProfile = false;
        noiseProfile = null;
    }
    
    public boolean hasLearnedNoiseProfile() {
        return hasLearnedNoiseProfile;
    }
}