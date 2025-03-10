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


public class NoiseReductionTestGenerator {
    
    
    private static final float SAMPLE_RATE = 44100.0f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    
    
    private static final double DURATION = 5.0;    
    private static final double SIGNAL_AMPLITUDE = 0.7;   
    
    
    private static final double NOISE_LEVEL = 0.15; 
    private static final int NOISE_SEED = 12345; 
    
    
    private static final double[] MELODY_NOTES = {
        440.0,  
        493.88, 
        523.25, 
        587.33, 
        659.25, 
        587.33, 
        523.25, 
        493.88  
    };
    
    
    private static final double NOTE_DURATION = 0.5; 
    
    public static void main(String[] args) {
        try {
            
            File cleanFile = new File("clean_melody.wav");
            generateCleanMelody(cleanFile);
            System.out.println("Clean melody generated: " + cleanFile.getAbsolutePath());
            
            
            File noiseFile = new File("noise_sample.wav");
            generateNoiseSample(noiseFile);
            System.out.println("Noise sample generated: " + noiseFile.getAbsolutePath());
            
            
            File noisyFile = new File("noisy_melody.wav");
            generateNoisyMelody(noisyFile);
            System.out.println("Noisy melody generated: " + noisyFile.getAbsolutePath());
            
            System.out.println("\nTest files generated successfully!");
            System.out.println("\nTo test noise reduction:");
            System.out.println("1. Load 'noisy_melody.wav' in the application");
            System.out.println("2. Go to the Noise Reduction tab");
            System.out.println("3. Click 'Learn Noise from File' and select 'noise_sample.wav'");
            System.out.println("4. Enable noise reduction and adjust parameters");
            System.out.println("5. Compare with the original 'clean_melody.wav'");
            
        } catch (Exception e) {
            System.err.println("Error generating test files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private static void generateCleanMelody(File outputFile) throws IOException {
        
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
        
        
        double[] cleanSamples = generateMelodySamples(totalSamples);
        
        
        for (int i = 0; i < totalSamples; i++) {
            short sample = (short)(cleanSamples[i] * 32767);
            buffer.putShort(i * 2, sample);
        }
        
        
        AudioInputStream audioInputStream = new AudioInputStream(
            new ByteArrayInputStream(audioData),
            format,
            totalSamples
        );
        
        
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
    }
    
    
    private static void generateNoiseSample(File outputFile) throws IOException {
        
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
        
        
        Random random = new Random(NOISE_SEED);
        
        
        for (int i = 0; i < totalSamples; i++) {
            
            double noise = (random.nextDouble() * 2.0 - 1.0) * NOISE_LEVEL;
            
            
            if (i % 100 == 0) {
                double rumble = (random.nextDouble() * 2.0 - 1.0) * NOISE_LEVEL * 0.5;
                for (int j = 0; j < 100 && i + j < totalSamples; j++) {
                    double fadeOut = 1.0 - (j / 100.0);
                    int index = i + j;
                    noise = (random.nextDouble() * 2.0 - 1.0) * NOISE_LEVEL + (rumble * fadeOut * 0.5);
                    
                    
                    short sample = (short)(noise * 32767);
                    buffer.putShort(index * 2, sample);
                }
            } else {
                
                short sample = (short)(noise * 32767);
                buffer.putShort(i * 2, sample);
            }
        }
        
        
        AudioInputStream audioInputStream = new AudioInputStream(
            new ByteArrayInputStream(audioData),
            format,
            totalSamples
        );
        
        
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
    }
    
    
    private static void generateNoisyMelody(File outputFile) throws IOException {
        
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
        
        
        Random random = new Random(NOISE_SEED);
        
        
        double[] cleanSamples = generateMelodySamples(totalSamples);
        
        
        for (int i = 0; i < totalSamples; i++) {
            
            double cleanSample = cleanSamples[i];
            
            
            double noise = (random.nextDouble() * 2.0 - 1.0) * NOISE_LEVEL;
            
            
            if (i % 100 == 0) {
                double rumble = (random.nextDouble() * 2.0 - 1.0) * NOISE_LEVEL * 0.5;
                for (int j = 0; j < 100 && i + j < totalSamples; j++) {
                    double fadeOut = 1.0 - (j / 100.0);
                    int index = i + j;
                    if (index < totalSamples) {
                        double noisySample = cleanSamples[index] + 
                                           (random.nextDouble() * 2.0 - 1.0) * NOISE_LEVEL + 
                                           (rumble * fadeOut * 0.5);
                        
                        
                        noisySample = Math.max(-1.0, Math.min(1.0, noisySample));
                        
                        
                        short sample = (short)(noisySample * 32767);
                        buffer.putShort(index * 2, sample);
                    }
                }
                i += 99; 
            } else {
                
                double noisySample = cleanSample + noise;
                
                
                noisySample = Math.max(-1.0, Math.min(1.0, noisySample));
                
                
                short sample = (short)(noisySample * 32767);
                buffer.putShort(i * 2, sample);
            }
        }
        
        
        AudioInputStream audioInputStream = new AudioInputStream(
            new ByteArrayInputStream(audioData),
            format,
            totalSamples
        );
        
        
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
    }
    
    
    private static double[] generateMelodySamples(int totalSamples) {
        double[] samples = new double[totalSamples];
        
        int samplesPerNote = (int)(SAMPLE_RATE * NOTE_DURATION);
        int noteIndex = 0;
        
        for (int i = 0; i < totalSamples; i++) {
            
            noteIndex = (i / samplesPerNote) % MELODY_NOTES.length;
            double frequency = MELODY_NOTES[noteIndex];
            
            
            double noteTime = (i % samplesPerNote) / SAMPLE_RATE;
            
            
            double envelope = 1.0;
            double attackTime = 0.05; 
            double releaseTime = 0.1; 
            
            if (noteTime < attackTime) {
                
                envelope = noteTime / attackTime;
            } else if (noteTime > NOTE_DURATION - releaseTime) {
                
                envelope = (NOTE_DURATION - noteTime) / releaseTime;
            }
            
            
            double sample = Math.sin(2.0 * Math.PI * frequency * (i / SAMPLE_RATE));
            
            
            samples[i] = sample * SIGNAL_AMPLITUDE * envelope;
        }
        
        return samples;
    }
    
    
    private static double[] generateComplexMelodySamples(int totalSamples) {
        double[] samples = new double[totalSamples];
        
        int samplesPerNote = (int)(SAMPLE_RATE * NOTE_DURATION);
        int noteIndex = 0;
        
        for (int i = 0; i < totalSamples; i++) {
            
            noteIndex = (i / samplesPerNote) % MELODY_NOTES.length;
            double frequency = MELODY_NOTES[noteIndex];
            
            
            double noteTime = (i % samplesPerNote) / SAMPLE_RATE;
            
            
            double envelope = 1.0;
            double attackTime = 0.05; 
            double releaseTime = 0.1; 
            
            if (noteTime < attackTime) {
                
                envelope = noteTime / attackTime;
            } else if (noteTime > NOTE_DURATION - releaseTime) {
                
                envelope = (NOTE_DURATION - noteTime) / releaseTime;
            }
            
            
            double sample = Math.sin(2.0 * Math.PI * frequency * (i / SAMPLE_RATE));
            
            
            sample += 0.5 * Math.sin(2.0 * Math.PI * (frequency * 2) * (i / SAMPLE_RATE)); 
            sample += 0.25 * Math.sin(2.0 * Math.PI * (frequency * 3) * (i / SAMPLE_RATE)); 
            
            
            sample /= 1.75;
            
            
            samples[i] = sample * SIGNAL_AMPLITUDE * envelope;
        }
        
        return samples;
    }
}