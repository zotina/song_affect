package generator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


public class NoisyWavGenerator {
    
    
    private static final float SAMPLE_RATE = 44100.0f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    
    
    private static final double FREQUENCY = 440.0; 
    private static final double DURATION = 5.0;    
    private static final double AMPLITUDE = 0.8;   
    
    
    private static final double NOISE_LEVEL = 0.2; 
    private static final double NOISE_TYPE = 1.0;  
    
    public static void main(String[] args) {
        try {
            
            File outputFile = new File("noisy_test.wav");
            generateNoisyWav(outputFile);
            
            System.out.println("Noisy WAV file generated successfully: " + outputFile.getAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("Error generating WAV file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    public static void generateNoisyWav(File outputFile) throws IOException {
        
        AudioFormat format = new AudioFormat(
            SAMPLE_RATE,
            SAMPLE_SIZE_BITS,
            CHANNELS,
            SIGNED,
            BIG_ENDIAN
        );
        
        
        int totalSamples = (int)(SAMPLE_RATE * DURATION);
        
        
        byte[] audioData = new byte[totalSamples * (SAMPLE_SIZE_BITS / 8)];
        ByteBuffer buffer = ByteBuffer.wrap(audioData);
        buffer.order(ByteOrder.LITTLE_ENDIAN); 
        
        
        Random random = new Random(42); 
        
        
        for (int i = 0; i < totalSamples; i++) {
            double time = i / SAMPLE_RATE;
            
            
            double sineValue = Math.sin(2.0 * Math.PI * FREQUENCY * time);
            
            
            sineValue *= AMPLITUDE;
            
            
            double noise = (random.nextDouble() * 2.0 - 1.0) * NOISE_LEVEL;
            
            
            if (NOISE_TYPE > 1.0) {
                
                noise = noise / NOISE_TYPE;
            }
            
            
            double sample = sineValue + noise;
            
            
            sample = Math.max(-1.0, Math.min(1.0, sample));
            
            
            short shortSample = (short)(sample * 32767);
            
            
            buffer.putShort(i * 2, shortSample);
        }
        
        
        AudioInputStream audioInputStream = new AudioInputStream(
            new ByteArrayInputStream(audioData),
            format,
            totalSamples
        );
        
        
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
    }
}