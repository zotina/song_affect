package entite;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import util.AudioUtils;

public class WavFileHandler {

    public static AudioLoadResult loadWavFile(File file) throws UnsupportedAudioFileException, IOException {
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
        AudioFormat format = audioInputStream.getFormat();
        
        
        byte[] rawAudioData = readAllBytes(audioInputStream);
        
        
        float[] normalizedSamples = convertToNormalizedSamples(rawAudioData, format);
        
        return new AudioLoadResult(rawAudioData, format, normalizedSamples);
    }
    
    
    public static void saveWavFile(File file, byte[] audioData, AudioFormat format) 
            throws IOException {
        AudioInputStream audioStream = new AudioInputStream(
                new ByteArrayInputStream(audioData),
                format,
                audioData.length / format.getFrameSize());
        
        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, file);
    }
    
    private static byte[] readAllBytes(AudioInputStream audioInputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384]; 
        
        while ((nRead = audioInputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        
        return buffer.toByteArray();
    }
    
    public static float[] convertToNormalizedSamples(byte[] audioData, AudioFormat format) {
        int bytesPerSample = format.getSampleSizeInBits() / 8;
        int numSamples = audioData.length / bytesPerSample;
        
        
        int downsampleFactor = Math.max(1, numSamples / 10000);
        ArrayList<Float> downsampledList = new ArrayList<>();
        
        ByteBuffer bb = ByteBuffer.wrap(audioData);
        if (format.isBigEndian()) {
            bb.order(ByteOrder.BIG_ENDIAN);
        } else {
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        
        for (int i = 0; i < numSamples; i++) {
            float sample = 0;
            
            if (bytesPerSample == 1) {
                
                sample = ((audioData[i] & 0xFF) - 128) / 128.0f;
            } else if (bytesPerSample == 2) {
                
                if (i * 2 + 1 < audioData.length) {
                    short shortSample = bb.getShort(i * 2);
                    sample = shortSample / 32768.0f;
                }
            } else if (bytesPerSample == 3) {
                
                if (i * 3 + 2 < audioData.length) {
                    int intSample = AudioUtils.read24BitSample(audioData, i * 3, format.isBigEndian());
                    sample = intSample / 8388608.0f;
                }
            }
            
            
            if (i % downsampleFactor == 0) {
                downsampledList.add(sample);
            }
        }
        
        
        float[] downsampledArray = new float[downsampledList.size()];
        for (int i = 0; i < downsampledList.size(); i++) {
            downsampledArray[i] = downsampledList.get(i);
        }
        
        return downsampledArray;
    }
    
    
    public static class AudioLoadResult {
        private final byte[] rawAudioData;
        private final AudioFormat format;
        private final float[] normalizedSamples;
        
        public AudioLoadResult(byte[] rawAudioData, AudioFormat format, float[] normalizedSamples) {
            this.rawAudioData = rawAudioData;
            this.format = format;
            this.normalizedSamples = normalizedSamples;
        }
        
        public byte[] getRawAudioData() {
            return rawAudioData;
        }
        
        public AudioFormat getFormat() {
            return format;
        }
        
        public float[] getNormalizedSamples() {
            return normalizedSamples;
        }
    }
}