package affichage;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import traitement.TreatAudio;
import entite.AudioData;

public class AmplitudeModifierGUI extends JFrame implements AudioData.AudioDataListener {
    private static final long serialVersionUID = 1L;
    
    private final TreatAudio controller;
    
    private JSlider amplitudeSlider;
    private JLabel amplitudeLabel;
    private WaveformCanvas waveformCanvas;
    private JButton loadButton;
    private JButton saveButton;
    private JButton playButton;
    private JButton stopButton;
    private JLabel statusLabel;
    
    private JCheckBox antiDistortionCheckbox;
    private JSlider thresholdSlider;
    private JSlider ratioSlider;
    private JSlider makeupGainSlider;
    private JLabel thresholdLabel;
    private JLabel ratioLabel;
    private JLabel makeupGainLabel;
    
    private JCheckBox noiseReductionCheckbox;
    private JSlider noiseReductionAmountSlider;
    private JSlider noiseFloorSlider;
    private JSlider smoothingFactorSlider;
    private JLabel noiseReductionAmountLabel;
    private JLabel noiseFloorLabel;
    private JLabel smoothingFactorLabel;
    private JButton resetNoiseProfileButton;
    
    public AmplitudeModifierGUI(TreatAudio controller) {
        this.controller = controller;
        
        setTitle("WAV Amplitude Modifier");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        createMenuBar();
        createControlPanel();
        createWaveformPanel();
        createStatusBar();
        
        setControlsEnabled(false);
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.cleanup();
            }
        });
        
        setLocationRelativeTo(null);
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open WAV File");
        openItem.addActionListener(e -> controller.loadWavFile());
        
        JMenuItem saveItem = new JMenuItem("Save Modified WAV");
        saveItem.addActionListener(e -> controller.saveWavFile());
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            controller.cleanup();
            System.exit(0);
        });
        
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }
    
    private void createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel sliderPanel = new JPanel(new BorderLayout());
        sliderPanel.setBorder(BorderFactory.createTitledBorder("Amplitude Control"));
        
        amplitudeSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, 100);
        amplitudeSlider.setMajorTickSpacing(50);
        amplitudeSlider.setMinorTickSpacing(10);
        amplitudeSlider.setPaintTicks(true);
        amplitudeSlider.setPaintLabels(true);
        
        amplitudeLabel = new JLabel("Amplification: 1.0x", JLabel.CENTER);
        
        amplitudeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                float factor = amplitudeSlider.getValue() / 100.0f;
                controller.setAmplificationFactor(factor);
            }
        });
        
        sliderPanel.add(amplitudeSlider, BorderLayout.CENTER);
        sliderPanel.add(amplitudeLabel, BorderLayout.SOUTH);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        JPanel antiDistortionPanel = createAntiDistortionPanel();
        tabbedPane.addTab("Anti-Distortion", antiDistortionPanel);
        
        JPanel noiseReductionPanel = createNoiseReductionPanel();
        tabbedPane.addTab("Noise Reduction", noiseReductionPanel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        loadButton = new JButton("Load WAV");
        loadButton.addActionListener(e -> controller.loadWavFile());
        
        saveButton = new JButton("Save Modified WAV");
        saveButton.addActionListener(e -> controller.saveWavFile());
        
        playButton = new JButton("Play");
        playButton.addActionListener(e -> controller.playAudio());
        
        stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> controller.stopAudio());
        
        buttonPanel.add(loadButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(playButton);
        buttonPanel.add(stopButton);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(sliderPanel, BorderLayout.NORTH);
        topPanel.add(tabbedPane, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        controlPanel.add(topPanel, BorderLayout.CENTER);
        
        add(controlPanel, BorderLayout.NORTH);
    }
    
    private JPanel createAntiDistortionPanel() {
        JPanel antiDistortionPanel = new JPanel(new BorderLayout());
        antiDistortionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        antiDistortionCheckbox = new JCheckBox("Enable Anti-Distortion");
        antiDistortionCheckbox.addActionListener(e -> 
            controller.setAntiDistortionEnabled(antiDistortionCheckbox.isSelected()));
        
        JPanel antiDistortionControls = new JPanel(new GridLayout(4, 2, 5, 5));
        
        thresholdSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, 70);
        thresholdSlider.setMajorTickSpacing(25);
        thresholdSlider.setMinorTickSpacing(5);
        thresholdSlider.setPaintTicks(true);
        thresholdLabel = new JLabel("Threshold: 0.70", JLabel.LEFT);
        
        ratioSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 4);
        ratioSlider.setMajorTickSpacing(5);
        ratioSlider.setMinorTickSpacing(1);
        ratioSlider.setPaintTicks(true);
        ratioLabel = new JLabel("Ratio: 4.0:1", JLabel.LEFT);
        
        makeupGainSlider = new JSlider(JSlider.HORIZONTAL, 50, 200, 100);
        makeupGainSlider.setMajorTickSpacing(50);
        makeupGainSlider.setMinorTickSpacing(10);
        makeupGainSlider.setPaintTicks(true);
        makeupGainLabel = new JLabel("Makeup Gain: 1.0x", JLabel.LEFT);
        
        JCheckBox tanhCheckbox = new JCheckBox("Use Tanh Soft Clipper");
        tanhCheckbox.setSelected(true);
        tanhCheckbox.addActionListener(e -> 
            controller.setUseTanhSoftClipper(tanhCheckbox.isSelected()));
        
        JLabel tanhLabel = new JLabel("Saturation plus douce et musicale", JLabel.LEFT);
        
        ChangeListener antiDistortionChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!thresholdSlider.getValueIsAdjusting() && 
                    !ratioSlider.getValueIsAdjusting() && 
                    !makeupGainSlider.getValueIsAdjusting()) {
                    
                    float threshold = thresholdSlider.getValue() / 100.0f;
                    float ratio = ratioSlider.getValue();
                    float makeupGain = makeupGainSlider.getValue() / 100.0f;
                    
                    thresholdLabel.setText(String.format("Threshold: %.2f", threshold));
                    ratioLabel.setText(String.format("Ratio: %.1f:1", ratio));
                    makeupGainLabel.setText(String.format("Makeup Gain: %.1fx", makeupGain));
                    
                    controller.setAntiDistortionParameters(threshold, ratio, makeupGain);
                }
            }
        };
        
        thresholdSlider.addChangeListener(antiDistortionChangeListener);
        ratioSlider.addChangeListener(antiDistortionChangeListener);
        makeupGainSlider.addChangeListener(antiDistortionChangeListener);
        
        antiDistortionControls.add(thresholdLabel);
        antiDistortionControls.add(thresholdSlider);
        antiDistortionControls.add(ratioLabel);
        antiDistortionControls.add(ratioSlider);
        antiDistortionControls.add(makeupGainLabel);
        antiDistortionControls.add(makeupGainSlider);
        antiDistortionControls.add(tanhCheckbox);
        antiDistortionControls.add(tanhLabel);
        
        JPanel antiDistortionTop = new JPanel(new BorderLayout());
        antiDistortionTop.add(antiDistortionCheckbox, BorderLayout.NORTH);
        antiDistortionTop.add(antiDistortionControls, BorderLayout.CENTER);
        
        antiDistortionPanel.add(antiDistortionTop, BorderLayout.CENTER);
        
        return antiDistortionPanel;
    }
    
    private JPanel createNoiseReductionPanel() {
        JPanel noiseReductionPanel = new JPanel(new BorderLayout());
        noiseReductionPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        noiseReductionCheckbox = new JCheckBox("Enable Noise Reduction");
        noiseReductionCheckbox.addActionListener(e -> 
            controller.setNoiseReductionEnabled(noiseReductionCheckbox.isSelected()));
        
        JPanel noiseReductionControls = new JPanel(new GridLayout(3, 2, 5, 5));
        
        noiseReductionAmountSlider = new JSlider(JSlider.HORIZONTAL, 10, 100, 90);
        noiseReductionAmountSlider.setMajorTickSpacing(25);
        noiseReductionAmountSlider.setMinorTickSpacing(5);
        noiseReductionAmountSlider.setPaintTicks(true);
        noiseReductionAmountLabel = new JLabel("Reduction Amount: 0.90", JLabel.LEFT);
        
        noiseFloorSlider = new JSlider(JSlider.HORIZONTAL, 1, 20, 5);
        noiseFloorSlider.setMajorTickSpacing(5);
        noiseFloorSlider.setMinorTickSpacing(1);
        noiseFloorSlider.setPaintTicks(true);
        noiseFloorLabel = new JLabel("Noise Floor: 0.05", JLabel.LEFT);
        
        smoothingFactorSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 70);
        smoothingFactorSlider.setMajorTickSpacing(25);
        smoothingFactorSlider.setMinorTickSpacing(5);
        smoothingFactorSlider.setPaintTicks(true);
        smoothingFactorLabel = new JLabel("Smoothing: 0.70", JLabel.LEFT);
        
        ChangeListener noiseReductionChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!noiseReductionAmountSlider.getValueIsAdjusting() && 
                    !noiseFloorSlider.getValueIsAdjusting() && 
                    !smoothingFactorSlider.getValueIsAdjusting()) {
                    
                    float amount = noiseReductionAmountSlider.getValue() / 100.0f;
                    float floor = noiseFloorSlider.getValue() / 100.0f;
                    float smoothing = smoothingFactorSlider.getValue() / 100.0f;
                    
                    noiseReductionAmountLabel.setText(String.format("Reduction Amount: %.2f", amount));
                    noiseFloorLabel.setText(String.format("Noise Floor: %.2f", floor));
                    smoothingFactorLabel.setText(String.format("Smoothing: %.2f", smoothing));
                    
                    controller.setNoiseReductionParameters(amount, floor, smoothing);
                }
            }
        };
        
        noiseReductionAmountSlider.addChangeListener(noiseReductionChangeListener);
        noiseFloorSlider.addChangeListener(noiseReductionChangeListener);
        smoothingFactorSlider.addChangeListener(noiseReductionChangeListener);
        
        noiseReductionControls.add(noiseReductionAmountLabel);
        noiseReductionControls.add(noiseReductionAmountSlider);
        noiseReductionControls.add(noiseFloorLabel);
        noiseReductionControls.add(noiseFloorSlider);
        noiseReductionControls.add(smoothingFactorLabel);
        noiseReductionControls.add(smoothingFactorSlider);
        
        JPanel noiseProfilePanel = new JPanel(new GridLayout(1, 2, 5, 5));
        
        JButton learnNoiseButton = new JButton("Learn Noise from File");
        learnNoiseButton.addActionListener(e -> controller.learnNoiseProfile());
        
        resetNoiseProfileButton = new JButton("Reset Noise Profile");
        resetNoiseProfileButton.addActionListener(e -> controller.resetNoiseProfile());
        
        noiseProfilePanel.add(learnNoiseButton);
        noiseProfilePanel.add(resetNoiseProfileButton);
        
        JPanel selectionPanel = new JPanel(new BorderLayout());
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Learn from Selection"));
        
        JButton selectNoiseButton = new JButton("Learn Noise from Selection");
        selectNoiseButton.addActionListener(e -> controller.learnNoiseFromSelection());
        
        JLabel selectionLabel = new JLabel("Select a portion of pure noise in the waveform", JLabel.CENTER);
        
        selectionPanel.add(selectionLabel, BorderLayout.NORTH);
        selectionPanel.add(selectNoiseButton, BorderLayout.CENTER);
        
        JPanel noiseReductionTop = new JPanel(new BorderLayout());
        noiseReductionTop.add(noiseReductionCheckbox, BorderLayout.NORTH);
        noiseReductionTop.add(noiseReductionControls, BorderLayout.CENTER);
        
        JPanel noiseReductionBottom = new JPanel(new BorderLayout());
        noiseReductionBottom.add(noiseProfilePanel, BorderLayout.NORTH);
        noiseReductionBottom.add(selectionPanel, BorderLayout.CENTER);
        
        noiseReductionPanel.add(noiseReductionTop, BorderLayout.NORTH);
        noiseReductionPanel.add(noiseReductionBottom, BorderLayout.CENTER);
        
        return noiseReductionPanel;
    }
    
    private void createWaveformPanel() {
        waveformCanvas = new WaveformCanvas();
        
        JPanel waveformPanel = new JPanel(new BorderLayout());
        waveformPanel.setBorder(BorderFactory.createTitledBorder("Waveform"));
        waveformPanel.add(waveformCanvas, BorderLayout.CENTER);
        
        JScrollPane scrollPane = new JScrollPane(waveformPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private void createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
        
        statusLabel = new JLabel("Ready");
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    public void setControlsEnabled(boolean enabled) {
        amplitudeSlider.setEnabled(enabled);
        antiDistortionCheckbox.setEnabled(enabled);
        thresholdSlider.setEnabled(enabled);
        ratioSlider.setEnabled(enabled);
        makeupGainSlider.setEnabled(enabled);
        noiseReductionCheckbox.setEnabled(enabled);
        noiseReductionAmountSlider.setEnabled(enabled);
        noiseFloorSlider.setEnabled(enabled);
        smoothingFactorSlider.setEnabled(enabled);
        resetNoiseProfileButton.setEnabled(enabled);
        saveButton.setEnabled(enabled);
        playButton.setEnabled(enabled);
        stopButton.setEnabled(enabled);
    }
    
    public void setStatusMessage(String message) {
        statusLabel.setText(message);
    }
    
    public void updateWaveform(float[] samples) {
        waveformCanvas.setSamples(samples);
    }
    
    @Override
    public void onAudioDataChanged() {
        setControlsEnabled(true);
        
        antiDistortionCheckbox.setSelected(controller.isAntiDistortionEnabled());
        noiseReductionCheckbox.setSelected(controller.isNoiseReductionEnabled());
    }
    
    @Override
    public void onAmplificationChanged(float factor) {
        amplitudeLabel.setText(String.format("Amplification: %.2fx", factor));
    }
    
    @Override
    public void onAntiDistortionChanged(boolean enabled, float threshold, float ratio, float makeupGain) {
        antiDistortionCheckbox.setSelected(enabled);
        thresholdSlider.setValue(Math.round(threshold * 100));
        ratioSlider.setValue(Math.round(ratio));
        makeupGainSlider.setValue(Math.round(makeupGain * 100));
        
        thresholdLabel.setText(String.format("Threshold: %.2f", threshold));
        ratioLabel.setText(String.format("Ratio: %.1f:1", ratio));
        makeupGainLabel.setText(String.format("Makeup Gain: %.1fx", makeupGain));
    }
    
    @Override
    public void onNoiseReductionChanged(boolean enabled, float amount, float floor, float smoothing) {
        noiseReductionCheckbox.setSelected(enabled);
        noiseReductionAmountSlider.setValue(Math.round(amount * 100));
        noiseFloorSlider.setValue(Math.round(floor * 100));
        smoothingFactorSlider.setValue(Math.round(smoothing * 100));
        
        noiseReductionAmountLabel.setText(String.format("Reduction Amount: %.2f", amount));
        noiseFloorLabel.setText(String.format("Noise Floor: %.2f", floor));
        smoothingFactorLabel.setText(String.format("Smoothing: %.2f", smoothing));
    }
    
    public void setTitle(String title) {
        super.setTitle("WAV Amplitude Modifier - " + title);
    }

    public WaveformCanvas getWaveformCanvas() {
        return waveformCanvas;
    }
}