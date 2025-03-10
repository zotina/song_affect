package traitement;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;


public interface AmplitudeModifier {
    byte[] modifyAmplitude(byte[] audioData, float amplificationFactor);
    
    
    static AmplitudeModifier createForFormat(AudioFormat format) {
        int sampleSizeInBits = format.getSampleSizeInBits();
        
        switch (sampleSizeInBits) {
            case 8:
                return new EightBitAmplitudeModifier();
            case 16:
                return new SixteenBitAmplitudeModifier();
            case 24:
                return new TwentyFourBitAmplitudeModifier();
            default:
                throw new IllegalArgumentException("Unsupported sample size: " + sampleSizeInBits);
        }
    }
}


class EightBitAmplitudeModifier implements AmplitudeModifier {
    @Override
    public byte[] modifyAmplitude(byte[] audioData, float amplificationFactor) {
        byte[] modifiedData = new byte[audioData.length];
        
        for (int i = 0; i < audioData.length; i++) {
            
            int sample = audioData[i] & 0xFF;
            int centered = sample - 128;
            
            
            centered = Math.round(centered * amplificationFactor);
            
            
            sample = Math.min(255, Math.max(0, centered + 128));
            
            modifiedData[i] = (byte) sample;
        }
        
        return modifiedData;
    }
}


class SixteenBitAmplitudeModifier implements AmplitudeModifier {
    @Override
    public byte[] modifyAmplitude(byte[] audioData, float amplificationFactor) {
        byte[] modifiedData = new byte[audioData.length];
        
        
        ByteBuffer inputBuffer = ByteBuffer.wrap(audioData);
        ByteBuffer outputBuffer = ByteBuffer.wrap(modifiedData);
        
        
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN);
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        for (int i = 0; i < audioData.length; i += 2) {
            if (i + 1 < audioData.length) {
                
                short sample = inputBuffer.getShort(i);
                
                
                float amplified = sample * amplificationFactor;
                
                
                short clampedSample = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, amplified));
                
                
                outputBuffer.putShort(i, clampedSample);
            }
        }
        
        return modifiedData;
    }
}


class TwentyFourBitAmplitudeModifier implements AmplitudeModifier {
    @Override
    public byte[] modifyAmplitude(byte[] audioData, float amplificationFactor) {
        byte[] modifiedData = new byte[audioData.length];
        
        for (int i = 0; i < audioData.length; i += 3) {
            if (i + 2 < audioData.length) {
                
                int sample = (audioData[i] & 0xFF) | 
                             ((audioData[i + 1] & 0xFF) << 8) | 
                             ((audioData[i + 2] & 0xFF) << 16);
                
                
                if ((sample & 0x800000) != 0) {
                    sample |= 0xFF000000;
                }
                
                
                float amplified = sample * amplificationFactor;
                
                
                int clampedSample = (int) Math.max(-8388608, Math.min(8388607, amplified));
                
                
                modifiedData[i] = (byte) (clampedSample & 0xFF);
                modifiedData[i + 1] = (byte) ((clampedSample >> 8) & 0xFF);
                modifiedData[i + 2] = (byte) ((clampedSample >> 16) & 0xFF);
            }
        }
        
        return modifiedData;
    }
}