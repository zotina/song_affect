package generator;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.UnsupportedAudioFileException;

import entite.AmplitudeModifier;
import entite.AntiDistortionProcessor;
import entite.NoiseReductionProcessor;
import entite.WavFileHandler;
import entite.WavFileHandler.AudioLoadResult;


public class TestFileProcessor {
    
    public static void main(String[] args) {
        try {
            
            processDistortedFile();
            
            
            processNoisyFile();
            
            System.out.println("Processing complete! Check the output files.");
            
        } catch (Exception e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private static void processDistortedFile() 
            throws UnsupportedAudioFileException, IOException {
        System.out.println("Processing distorted test file...");
        
        
        File inputFile = new File("distorted_test.wav");
        if (!inputFile.exists()) {
            System.out.println("Distorted test file not found. Run DistortedWavGenerator first.");
            return;
        }
        
        
        AudioLoadResult loadResult = WavFileHandler.loadWavFile(inputFile);
        byte[] audioData = loadResult.getRawAudioData();
        AudioFormat format = loadResult.getFormat();
        
        
        AntiDistortionProcessor processor = new AntiDistortionProcessor();
        
        
        processor.setParameters(0.6f, 4.0f, 1.2f);
        
        
        byte[] processedAudio = processor.processAudio(audioData, format);
        
        
        File outputFile = new File("distorted_test_fixed.wav");
        WavFileHandler.saveWavFile(outputFile, processedAudio, format);
        
        System.out.println("Anti-distortion processing complete: " + outputFile.getAbsolutePath());
    }
    
    
    private static void processNoisyFile() 
            throws UnsupportedAudioFileException, IOException {
        System.out.println("Processing noisy test file...");
        
        
        File inputFile = new File("noisy_test.wav");
        if (!inputFile.exists()) {
            System.out.println("Noisy test file not found. Run NoisyWavGenerator first.");
            return;
        }
        
        
        AudioLoadResult loadResult = WavFileHandler.loadWavFile(inputFile);
        byte[] audioData = loadResult.getRawAudioData();
        AudioFormat format = loadResult.getFormat();
        
        
        NoiseReductionProcessor processor = new NoiseReductionProcessor();
        
        
        processor.setParameters(0.95f, 0.03f, 0.7f);
        
        
        byte[] processedAudio = processor.processAudio(audioData, format);
        
        
        File outputFile = new File("noisy_test_fixed.wav");
        WavFileHandler.saveWavFile(outputFile, processedAudio, format);
        
        System.out.println("Noise reduction processing complete: " + outputFile.getAbsolutePath());
    }
    
    
    private static void processCombinedEffects() 
            throws UnsupportedAudioFileException, IOException {
        System.out.println("Processing with combined effects...");
        
        
        File inputFile = new File("noisy_test.wav");
        if (!inputFile.exists()) {
            System.out.println("Noisy test file not found. Run NoisyWavGenerator first.");
            return;
        }
        
        
        AudioLoadResult loadResult = WavFileHandler.loadWavFile(inputFile);
        byte[] audioData = loadResult.getRawAudioData();
        AudioFormat format = loadResult.getFormat();
        
        
        NoiseReductionProcessor noiseProcessor = new NoiseReductionProcessor();
        AntiDistortionProcessor distortionProcessor = new AntiDistortionProcessor();
        AmplitudeModifier amplitudeModifier = AmplitudeModifier.createForFormat(format);
        
        
        noiseProcessor.setParameters(0.95f, 0.03f, 0.7f);
        distortionProcessor.setParameters(0.7f, 3.0f, 1.1f);
        
        
        byte[] processedAudio = noiseProcessor.processAudio(audioData, format);
        processedAudio = amplitudeModifier.modifyAmplitude(processedAudio, 1.5f);
        processedAudio = distortionProcessor.processAudio(processedAudio, format);
        
        
        File outputFile = new File("noisy_test_enhanced.wav");
        WavFileHandler.saveWavFile(outputFile, processedAudio, format);
        
        System.out.println("Combined processing complete: " + outputFile.getAbsolutePath());
    }
}