package traitement;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import affichage.AmplitudeModifierGUI;
import affichage.WaveformCanvas;
import entite.AntiDistortionProcessor;
import entite.AudioData;
import entite.NoiseReductionProcessor;
import entite.WavFileHandler;
import util.AudioUtils;


public class TreatAudio implements AudioData.AudioDataListener, WaveformCanvas.SelectionListener {
    private final AudioData audioData;
    private AmplitudeModifierGUI gui;
    private File currentFile;
    private Clip audioClip;
    private AmplitudeModifier amplitudeModifier;
    private AntiDistortionProcessor antiDistortionProcessor;
    private NoiseReductionProcessor noiseReductionProcessor;
    private int[] currentSelection = null;
    
    public TreatAudio() {
        this.audioData = new AudioData();
        this.audioData.addListener(this);
        this.antiDistortionProcessor = new AntiDistortionProcessor();
        this.noiseReductionProcessor = new NoiseReductionProcessor();
    }
    
    public void initialize() {
        gui = new AmplitudeModifierGUI(this);
        audioData.addListener(gui);
        gui.getWaveformCanvas().addSelectionListener(this);
        gui.setVisible(true);
    }
    
    public void loadWavFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open WAV File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("WAV files", "wav"));
        
        int result = fileChooser.showOpenDialog(gui);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                
                closeAudioResources();
                
                currentFile = fileChooser.getSelectedFile();
                gui.setTitle(currentFile.getName());
                
                
                WavFileHandler.AudioLoadResult loadResult = WavFileHandler.loadWavFile(currentFile);
                
                
                amplitudeModifier = AmplitudeModifier.createForFormat(loadResult.getFormat());
                
                
                noiseReductionProcessor.resetNoiseProfile();
                
                
                audioData.setAudioData(
                    loadResult.getRawAudioData(), 
                    loadResult.getFormat(), 
                    loadResult.getNormalizedSamples()
                );
                
                
                gui.updateWaveform(loadResult.getNormalizedSamples());
                
                
                gui.setStatusMessage(AudioUtils.getAudioFormatDetails(loadResult.getFormat()));
                
                JOptionPane.showMessageDialog(gui, 
                    AudioUtils.getAudioFormatDetails(loadResult.getFormat()),
                    "File Loaded", 
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (UnsupportedAudioFileException | IOException e) {
                JOptionPane.showMessageDialog(gui, 
                    "Error loading file: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    public void learnNoiseProfile() {
        if (!audioData.hasAudioData()) {
            JOptionPane.showMessageDialog(gui, 
                "Please load an audio file first!", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Open Noise Sample WAV File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("WAV files", "wav"));
        
        int result = fileChooser.showOpenDialog(gui);
        if (result == JFileChooser.APPROVE_OPTION) {
            File noiseFile = fileChooser.getSelectedFile();
            
            boolean success = noiseReductionProcessor.learnNoiseProfile(noiseFile);
            
            if (success) {
                JOptionPane.showMessageDialog(gui, 
                    "Noise profile learned successfully from " + noiseFile.getName(), 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                
                
                audioData.setNoiseReductionEnabled(true);
                
                
                updateWaveform();
            } else {
                JOptionPane.showMessageDialog(gui, 
                    "Failed to learn noise profile from the selected file.", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public void learnNoiseFromSelection() {
        if (!audioData.hasAudioData()) {
            JOptionPane.showMessageDialog(gui, 
                "Please load an audio file first!", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (currentSelection == null || currentSelection[0] == currentSelection[1]) {
            JOptionPane.showMessageDialog(gui, 
                "Please select a portion of the waveform that contains only noise.", 
                "No Selection", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        
        noiseReductionProcessor.learnNoiseProfileFromSection(
            audioData.getRawAudioData(),
            audioData.getAudioFormat(),
            currentSelection[0],
            currentSelection[1]
        );
        
        JOptionPane.showMessageDialog(gui, 
            "Noise profile learned from the selected portion of the waveform.", 
            "Success", 
            JOptionPane.INFORMATION_MESSAGE);
        
        
        audioData.setNoiseReductionEnabled(true);
        
        
        updateWaveform();
    }
    
    @Override
    public void onSelectionChanged(int startSample, int endSample) {
        currentSelection = new int[] { startSample, endSample };
    }
    
    public void saveWavFile() {
        if (!audioData.hasAudioData()) {
            JOptionPane.showMessageDialog(gui, 
                "No audio file loaded!", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Modified WAV File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("WAV files", "wav"));
        
        if (currentFile != null) {
            String newName = "modified_" + currentFile.getName();
            fileChooser.setSelectedFile(new File(currentFile.getParentFile(), newName));
        }
        
        int result = fileChooser.showSaveDialog(gui);
        if (result == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            if (!outputFile.getName().toLowerCase().endsWith(".wav")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".wav");
            }
            
            try {
                
                byte[] processedAudio = processAudio();
                
                
                WavFileHandler.saveWavFile(outputFile, processedAudio, audioData.getAudioFormat());
                
                JOptionPane.showMessageDialog(gui, 
                    "File saved successfully!", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(gui, 
                    "Error saving file: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }
    
    public void playAudio() {
        if (!audioData.hasAudioData()) {
            return;
        }
        
        try {
            stopAudio();
            
            
            byte[] processedAudio = processAudio();
            
            
            audioClip = AudioUtils.createClip(processedAudio, audioData.getAudioFormat());
            audioClip.start();
            
        } catch (LineUnavailableException | IOException e) {
            JOptionPane.showMessageDialog(gui, 
                "Error playing audio: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    public void stopAudio() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            audioClip.close();
        }
    }
    
    public void setAmplificationFactor(float factor) {
        audioData.setAmplificationFactor(factor);
        
        if (audioData.hasAudioData()) {
            updateWaveform();
        }
    }
    
    public void setAntiDistortionEnabled(boolean enabled) {
        audioData.setAntiDistortionEnabled(enabled);
        
        if (audioData.hasAudioData()) {
            updateWaveform();
        }
    }
    
    public void setAntiDistortionParameters(float threshold, float ratio, float makeupGain) {
        audioData.setAntiDistortionParameters(threshold, ratio, makeupGain);
        antiDistortionProcessor.setParameters(threshold, ratio, makeupGain);
        
        if (audioData.hasAudioData()) {
            updateWaveform();
        }
    }
    
    public void setUseTanhSoftClipper(boolean useTanh) {
        audioData.setUseTanhSoftClipper(useTanh);
        antiDistortionProcessor.setUseTanhSoftClipper(useTanh);
        
        if (audioData.hasAudioData()) {
            updateWaveform();
        }
    }
    
    public void setNoiseReductionEnabled(boolean enabled) {
        audioData.setNoiseReductionEnabled(enabled);
        
        if (audioData.hasAudioData()) {
            updateWaveform();
        }
    }
    
    public void setNoiseReductionParameters(float amount, float floor, float smoothing) {
        audioData.setNoiseReductionParameters(amount, floor, smoothing);
        noiseReductionProcessor.setParameters(amount, floor, smoothing);
        
        if (audioData.hasAudioData()) {
            updateWaveform();
        }
    }
    
    public void resetNoiseProfile() {
        if (audioData.hasAudioData()) {
            noiseReductionProcessor.resetNoiseProfile();
            updateWaveform();
            JOptionPane.showMessageDialog(gui, 
                "Noise profile has been reset. The noise profile will be re-estimated on the next processing.", 
                "Noise Profile Reset", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private byte[] processAudio() {
        
        byte[] processedAudio = audioData.getRawAudioData();
        
        
        processedAudio = amplitudeModifier.modifyAmplitude(
            processedAudio, 
            audioData.getAmplificationFactor()
        );
        
        
        if (audioData.isNoiseReductionEnabled()) {
            processedAudio = noiseReductionProcessor.processAudio(
                processedAudio, 
                audioData.getAudioFormat()
            );
        }
        
        
        if (audioData.isAntiDistortionEnabled()) {
            processedAudio = antiDistortionProcessor.processAudio(
                processedAudio, 
                audioData.getAudioFormat()
            );
        }
        
        return processedAudio;
    }
    
    private void updateWaveform() {
        if (!audioData.hasAudioData()) {
            return;
        }
        
        try {
            
            byte[] processedAudio = processAudio();
            
            
            float[] modifiedSamples = WavFileHandler.convertToNormalizedSamples(
                processedAudio, 
                audioData.getAudioFormat()
            );
            
            
            gui.updateWaveform(modifiedSamples);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void cleanup() {
        closeAudioResources();
    }
    
    private void closeAudioResources() {
        stopAudio();
    }
    
    @Override
    public void onAudioDataChanged() {
        
        
    }
    
    @Override
    public void onAmplificationChanged(float factor) {
        
        
    }
    
    @Override
    public void onAntiDistortionChanged(boolean enabled, float threshold, float ratio, float makeupGain) {
        
        antiDistortionProcessor.setParameters(threshold, ratio, makeupGain);
    }
    
    @Override
    public void onNoiseReductionChanged(boolean enabled, float amount, float floor, float smoothing) {
        
        noiseReductionProcessor.setParameters(amount, floor, smoothing);
    }
    
    
    public boolean isAntiDistortionEnabled() {
        return audioData.isAntiDistortionEnabled();
    }
    
    public boolean isNoiseReductionEnabled() {
        return audioData.isNoiseReductionEnabled();
    }
    
    public boolean getUseTanhSoftClipper() {
        return audioData.getUseTanhSoftClipper();
    }
}