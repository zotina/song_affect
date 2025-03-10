package generator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


public class DistortedWavGenerator {
   
   
   private static final float SAMPLE_RATE = 44100.0f;
   private static final int SAMPLE_SIZE_BITS = 16;
   private static final int CHANNELS = 1;
   private static final boolean SIGNED = true;
   private static final boolean BIG_ENDIAN = false;
   
   
   private static final double FREQUENCY = 440.0; 
   private static final double DURATION = 5.0;    
   private static final double AMPLITUDE = 0.8;   
   
   
   private static final double DISTORTION_AMOUNT = 3.0; 
   
   public static void main(String[] args) {
       try {
           
           File outputFile = new File("distorted_test.wav");
           generateDistortedWav(outputFile);
           
           System.out.println("Distorted WAV file generated successfully: " + outputFile.getAbsolutePath());
           
       } catch (Exception e) {
           System.err.println("Error generating WAV file: " + e.getMessage());
           e.printStackTrace();
       }
   }
   
   
   public static void generateDistortedWav(File outputFile) throws IOException {
       
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
       
       
       for (int i = 0; i < totalSamples; i++) {
           double time = i / SAMPLE_RATE;
           
           
           double sineValue = Math.sin(2.0 * Math.PI * FREQUENCY * time);
           
           
           sineValue *= AMPLITUDE;
           
           
           sineValue = applyDistortion(sineValue, DISTORTION_AMOUNT);
           
           
           short sample = (short)(sineValue * 32767);
           
           
           buffer.putShort(i * 2, sample);
       }
       
       
       AudioInputStream audioInputStream = new AudioInputStream(
           new ByteArrayInputStream(audioData),
           format,
           totalSamples
       );
       
       
       AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
   }
   
   
   private static double applyDistortion(double sample, double amount) {
       
       sample *= amount;
       
       
       if (sample > 1.0) {
           sample = 1.0;
       } else if (sample < -1.0) {
           sample = -1.0;
       }
       
       return sample;
   }
   
   
   private static double applyWaveshaping(double sample, double amount) {
       
       return Math.tanh(sample * amount);
   }
}