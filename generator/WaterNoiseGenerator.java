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


public class WaterNoiseGenerator {
    
    
    private static final float SAMPLE_RATE = 44100.0f;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    
    
    private static final double DURATION = 6.0;    
    private static final double SIGNAL_AMPLITUDE = 0.7;   
    
    
    private static final int NOISE_SEED = 98765; 
    
    
    private static final double[] MELODY_NOTES = {
        392.00, 
        440.00, 
        349.23, 
        392.00, 
        523.25, 
        493.88, 
        440.00, 
        392.00, 
    };
    
    
    private static final double NOTE_DURATION = 0.75; 
    
    public static void main(String[] args) {
        try {
            
            File cleanFile = new File("clean_melody_water.wav");
            generateCleanSound(cleanFile);
            System.out.println("Son original généré: " + cleanFile.getAbsolutePath());
            
            
            File noiseFile = new File("water_noise.wav");
            generateWaterNoise(noiseFile);
            System.out.println("Profil de bruit d'eau généré: " + noiseFile.getAbsolutePath());
            
            
            File noisyFile = new File("water_noisy_melody.wav");
            generateNoisySound(noisyFile);
            System.out.println("Son bruité généré: " + noisyFile.getAbsolutePath());
            
            System.out.println("\nFichiers de test générés avec succès!");
            System.out.println("\nPour tester la réduction de bruit:");
            System.out.println("1. Chargez 'water_noisy_melody.wav' dans l'application");
            System.out.println("2. Allez dans l'onglet Réduction de bruit");
            System.out.println("3. Cliquez sur 'Apprendre le bruit depuis un fichier' et sélectionnez 'water_noise.wav'");
            System.out.println("4. Activez la réduction de bruit et ajustez les paramètres");
            System.out.println("5. Comparez avec le son original 'clean_melody_water.wav'");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération des fichiers de test: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private static void generateCleanSound(File outputFile) throws IOException {
        
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
        
        
        double[] cleanSamples = generateHarmonicMelody(totalSamples);
        
        
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
    
    
    private static void generateWaterNoise(File outputFile) throws IOException {
        
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
        
        
        double baseNoiseFactor = 0.35;  
        
        
        double[] lastValues = new double[8];  
        
        
        double bubbleChance = 0.002;  
        double splashChance = 0.0005; 
        
        
        double flowRate = 1.0;
        double flowModFreq = 0.2; 
        
        
        for (int i = 0; i < totalSamples; i++) {
            
            flowRate = 0.8 + 0.4 * Math.sin(2.0 * Math.PI * flowModFreq * (i / SAMPLE_RATE));
            
            
            double whiteNoise = random.nextDouble() * 2.0 - 1.0;
            
            
            
            
            
            lastValues[0] = (lastValues[0] * 0.6) + (whiteNoise * 0.4);
            
            
            lastValues[1] = (lastValues[1] * 0.85) + (whiteNoise * 0.15);
            
            
            lastValues[2] = (lastValues[2] * 0.92) + (whiteNoise * 0.08);
            
            
            lastValues[3] = (lastValues[3] * 0.95) + (whiteNoise * 0.05);
            
            
            lastValues[4] = (lastValues[4] * 0.98) + (whiteNoise * 0.02);
            
            
            double waterNoise = 0;
            waterNoise += lastValues[0] * 0.08 * flowRate;        
            waterNoise += lastValues[1] * 0.25 * flowRate;        
            waterNoise += lastValues[2] * 0.35 * flowRate;        
            waterNoise += lastValues[3] * 0.20 * flowRate;        
            waterNoise += lastValues[4] * 0.12 * flowRate;        
            
            
            if (random.nextDouble() < bubbleChance * flowRate) {
                
                double bubbleAmp = (random.nextDouble() * 0.15 + 0.1) * flowRate;
                int bubbleDuration = (int)(SAMPLE_RATE * (0.02 + random.nextDouble() * 0.03));
                double bubbleFreq = 400 + random.nextDouble() * 800;
                
                
                for (int j = 0; j < bubbleDuration && i + j < totalSamples; j++) {
                    double fadeOut = Math.exp(-j / (SAMPLE_RATE * 0.01));
                    double bubbleValue = bubbleAmp * fadeOut * Math.sin(2.0 * Math.PI * bubbleFreq * (j / SAMPLE_RATE));
                    
                    
                    if (j == 0) {
                        waterNoise += bubbleValue;
                    } else {
                        int index = i + j;
                        double existingValue = buffer.getShort(index * 2) / 32767.0;
                        double newValue = existingValue + bubbleValue;
                        
                        
                        newValue = Math.max(-1.0, Math.min(1.0, newValue));
                        
                        
                        buffer.putShort(index * 2, (short)(newValue * 32767));
                    }
                }
            }
            
            
            if (random.nextDouble() < splashChance * flowRate) {
                double splashAmp = (random.nextDouble() * 0.2 + 0.1) * flowRate;
                int splashDuration = (int)(SAMPLE_RATE * 0.1);
                
                for (int j = 0; j < splashDuration && i + j < totalSamples; j++) {
                    double fadeOut = Math.exp(-j / (SAMPLE_RATE * 0.02));
                    
                    double splashNoise = (random.nextDouble() * 2.0 - 1.0) * splashAmp * fadeOut;
                    
                    if (j == 0) {
                        waterNoise += splashNoise;
                    } else {
                        int index = i + j;
                        double existingValue = buffer.getShort(index * 2) / 32767.0;
                        double newValue = existingValue + splashNoise * 0.7; 
                        
                        
                        newValue = Math.max(-1.0, Math.min(1.0, newValue));
                        
                        
                        buffer.putShort(index * 2, (short)(newValue * 32767));
                    }
                }
            }
            
            
            waterNoise *= baseNoiseFactor;
            
            
            waterNoise = Math.max(-1.0, Math.min(1.0, waterNoise));
            
            
            short sample = (short)(waterNoise * 32767);
            buffer.putShort(i * 2, sample);
        }
        
        
        AudioInputStream audioInputStream = new AudioInputStream(
            new ByteArrayInputStream(audioData),
            format,
            totalSamples
        );
        
        
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
    }
    
    
    private static void generateNoisySound(File outputFile) throws IOException {
        
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
        
        
        double[] cleanSamples = generateHarmonicMelody(totalSamples);
        
        
        double baseNoiseFactor = 0.35;  
        
        
        double[] lastValues = new double[8];  
        
        
        double bubbleChance = 0.002;  
        double splashChance = 0.0005; 
        
        
        double flowRate = 1.0;
        double flowModFreq = 0.2; 
        
        
        for (int i = 0; i < totalSamples; i++) {
            
            double cleanSample = cleanSamples[i];
            
            
            flowRate = 0.8 + 0.4 * Math.sin(2.0 * Math.PI * flowModFreq * (i / SAMPLE_RATE));
            
            
            double whiteNoise = random.nextDouble() * 2.0 - 1.0;
            
            
            
            
            
            lastValues[0] = (lastValues[0] * 0.6) + (whiteNoise * 0.4);
            
            
            lastValues[1] = (lastValues[1] * 0.85) + (whiteNoise * 0.15);
            
            
            lastValues[2] = (lastValues[2] * 0.92) + (whiteNoise * 0.08);
            
            
            lastValues[3] = (lastValues[3] * 0.95) + (whiteNoise * 0.05);
            
            
            lastValues[4] = (lastValues[4] * 0.98) + (whiteNoise * 0.02);
            
            
            double waterNoise = 0;
            waterNoise += lastValues[0] * 0.08 * flowRate;        
            waterNoise += lastValues[1] * 0.25 * flowRate;        
            waterNoise += lastValues[2] * 0.35 * flowRate;        
            waterNoise += lastValues[3] * 0.20 * flowRate;        
            waterNoise += lastValues[4] * 0.12 * flowRate;        
            
            
            if (random.nextDouble() < bubbleChance * flowRate) {
                
                double bubbleAmp = (random.nextDouble() * 0.15 + 0.1) * flowRate;
                waterNoise += bubbleAmp;
            }
            
            
            if (random.nextDouble() < splashChance * flowRate) {
                
                double splashAmp = (random.nextDouble() * 0.2 + 0.1) * flowRate;
                waterNoise += splashAmp;
            }
            
            
            waterNoise *= baseNoiseFactor;
            
            
            double noisySample = cleanSample + waterNoise;
            
            
            noisySample = Math.max(-1.0, Math.min(1.0, noisySample));
            
            
            short sample = (short)(noisySample * 32767);
            buffer.putShort(i * 2, sample);
        }
        
        
        AudioInputStream audioInputStream = new AudioInputStream(
            new ByteArrayInputStream(audioData),
            format,
            totalSamples
        );
        
        
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
    }
    
    
    private static double[] generateHarmonicMelody(int totalSamples) {
        double[] samples = new double[totalSamples];
        
        int samplesPerNote = (int)(SAMPLE_RATE * NOTE_DURATION);
        
        for (int i = 0; i < totalSamples; i++) {
            
            int noteIndex = (i / samplesPerNote) % MELODY_NOTES.length;
            double frequency = MELODY_NOTES[noteIndex];
            
            
            double noteTime = (i % samplesPerNote) / SAMPLE_RATE;
            
            
            double envelope = 1.0;
            double attackTime = 0.08; 
            double decayTime = 0.12;  
            double sustainLevel = 0.7; 
            double releaseTime = 0.2; 
            
            if (noteTime < attackTime) {
                
                envelope = noteTime / attackTime;
            } else if (noteTime < attackTime + decayTime) {
                
                double decayProgress = (noteTime - attackTime) / decayTime;
                envelope = 1.0 - ((1.0 - sustainLevel) * decayProgress);
            } else if (noteTime > NOTE_DURATION - releaseTime) {
                
                envelope = sustainLevel * (NOTE_DURATION - noteTime) / releaseTime;
            } else {
                
                envelope = sustainLevel;
            }
            
            
            double sample = Math.sin(2.0 * Math.PI * frequency * (i / SAMPLE_RATE));
            
            
            double harmonic2 = 0.5 * Math.sin(2.0 * Math.PI * (frequency * 2) * (i / SAMPLE_RATE));
            double harmonic3 = 0.25 * Math.sin(2.0 * Math.PI * (frequency * 3) * (i / SAMPLE_RATE));
            double harmonic4 = 0.125 * Math.sin(2.0 * Math.PI * (frequency * 4) * (i / SAMPLE_RATE));
            
            
            double vibrato = 1.0 + 0.005 * Math.sin(2.0 * Math.PI * 6 * (i / SAMPLE_RATE));
            
            
            sample = (sample + harmonic2 + harmonic3 + harmonic4) * vibrato;
            
            
            sample /= 1.875;
            
            
            samples[i] = sample * SIGNAL_AMPLITUDE * envelope;
        }
        
        return samples;
    }
}