
package entite;

public interface AmplitudeModifier {
    byte[] modifyAmplitude(byte[] audioData, float amplificationFactor);
    
    static AmplitudeModifier createForFormat(javax.sound.sampled.AudioFormat format) {
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        boolean isBigEndian = format.isBigEndian();
        
        return new OptimizedAmplitudeModifier(bytesPerSample, isBigEndian);
    }
}

class OptimizedAmplitudeModifier implements AmplitudeModifier {
    private final int bytesPerSample;
    private final boolean isBigEndian;
    
    public OptimizedAmplitudeModifier(int bytesPerSample, boolean isBigEndian) {
        this.bytesPerSample = bytesPerSample;
        this.isBigEndian = isBigEndian;
    }
    
    @Override
    public byte[] modifyAmplitude(byte[] audioData, float amplificationFactor) {
        if (Math.abs(amplificationFactor - 1.0f) < 0.001f) {
            return audioData.clone();
        }
        
        byte[] modifiedData = new byte[audioData.length];
        
        final int CHUNK_SIZE = 1024 * bytesPerSample;
        
        for (int offset = 0; offset < audioData.length; offset += CHUNK_SIZE) {
            int chunkEnd = Math.min(offset + CHUNK_SIZE, audioData.length);
            processChunk(audioData, modifiedData, offset, chunkEnd, amplificationFactor);
        }
        
        return modifiedData;
    }
    
    private void processChunk(byte[] audioData, byte[] modifiedData, int start, int end, float amplificationFactor) {
        for (int i = start; i < end; i += bytesPerSample) {
            if (bytesPerSample == 1) {
                processSample8Bit(audioData, modifiedData, i, amplificationFactor);
            } else if (bytesPerSample == 2) {
                processSample16Bit(audioData, modifiedData, i, amplificationFactor);
            } else if (bytesPerSample == 3) {
                processSample24Bit(audioData, modifiedData, i, amplificationFactor);
            }
        }
    }
    
    private void processSample8Bit(byte[] audioData, byte[] modifiedData, int index, float amplificationFactor) {
        int sample = audioData[index] & 0xFF; 
        sample = sample - 128; 
        sample = Math.round(sample * amplificationFactor); 
        sample = Math.min(127, Math.max(-128, sample));
        sample = sample + 128; 
        modifiedData[index] = (byte) sample;
    }
    
    private void processSample16Bit(byte[] audioData, byte[] modifiedData, int index, float amplificationFactor) {
        int sample;
        if (isBigEndian) {
            sample = (audioData[index] << 8) | (audioData[index + 1] & 0xFF);
        } else {
            sample = (audioData[index + 1] << 8) | (audioData[index] & 0xFF);
        }
        
        if ((sample & 0x8000) != 0) {
            sample |= 0xFFFF0000;
        }
        
        sample = Math.round(sample * amplificationFactor); 
        sample = Math.min(32767, Math.max(-32768, sample));
        
        if (isBigEndian) {
            modifiedData[index] = (byte) (sample >> 8);
            modifiedData[index + 1] = (byte) sample;
        } else {
            modifiedData[index] = (byte) sample;
            modifiedData[index + 1] = (byte) (sample >> 8);
        }
    }
    
    private void processSample24Bit(byte[] audioData, byte[] modifiedData, int index, float amplificationFactor) {
        int sample;
        if (isBigEndian) {
            sample = (audioData[index] << 16) | ((audioData[index + 1] & 0xFF) << 8) | (audioData[index + 2] & 0xFF);
        } else {
            sample = (audioData[index + 2] << 16) | ((audioData[index + 1] & 0xFF) << 8) | (audioData[index] & 0xFF);
        }
        
        if ((sample & 0x800000) != 0) {
            sample |= 0xFF000000;
        }
        
        sample = Math.round(sample * amplificationFactor); 
        sample = Math.min(8388607, Math.max(-8388608, sample));
        
        if (isBigEndian) {
            modifiedData[index] = (byte) (sample >> 16);
            modifiedData[index + 1] = (byte) (sample >> 8);
            modifiedData[index + 2] = (byte) sample;
        } else {
            modifiedData[index] = (byte) sample;
            modifiedData[index + 1] = (byte) (sample >> 8);
            modifiedData[index + 2] = (byte) (sample >> 16);
        }
    }
}