package affichage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

public class WaveformCanvas extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private float[] samples;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean isDragging = false;
    
    private List<SelectionListener> selectionListeners = new ArrayList<>();
    
    public interface SelectionListener {
        void onSelectionChanged(int startSample, int endSample);
    }
    
    public WaveformCanvas() {
        setPreferredSize(new Dimension(800, 200));
        setBackground(Color.BLACK);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (samples != null && samples.length > 0) {
                    selectionStart = sampleIndexFromX(e.getX());
                    selectionEnd = selectionStart;
                    isDragging = true;
                    repaint();
                    notifySelectionChanged();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDragging) {
                    selectionEnd = sampleIndexFromX(e.getX());
                    isDragging = false;
                    repaint();
                    notifySelectionChanged();
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging && samples != null && samples.length > 0) {
                    selectionEnd = sampleIndexFromX(e.getX());
                    repaint();
                    notifySelectionChanged();
                }
            }
        });
    }
    
    public void addSelectionListener(SelectionListener listener) {
        selectionListeners.add(listener);
    }
    
    public void removeSelectionListener(SelectionListener listener) {
        selectionListeners.remove(listener);
    }
    
    private void notifySelectionChanged() {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        
        for (SelectionListener listener : selectionListeners) {
            listener.onSelectionChanged(start, end);
        }
    }
    
    public void setSamples(float[] samples) {
        this.samples = samples;
        selectionStart = -1;
        selectionEnd = -1;
        repaint();
    }
    
    public int[] getSelection() {
        if (selectionStart < 0 || selectionEnd < 0 || samples == null) {
            return null;
        }
        
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        
        return new int[] { start, end };
    }
    
    public void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
        repaint();
    }
    
    private int sampleIndexFromX(int x) {
        if (samples == null || samples.length == 0) {
            return -1;
        }
        
        double ratio = (double) x / getWidth();
        return (int) (ratio * samples.length);
    }
    
    private int xFromSampleIndex(int sampleIndex) {
        if (samples == null || samples.length == 0) {
            return -1;
        }
        
        double ratio = (double) sampleIndex / samples.length;
        return (int) (ratio * getWidth());
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (samples == null || samples.length == 0) {
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;
        
        if (selectionStart >= 0 && selectionEnd >= 0) {
            int x1 = xFromSampleIndex(Math.min(selectionStart, selectionEnd));
            int x2 = xFromSampleIndex(Math.max(selectionStart, selectionEnd));
            
            g2d.fillRect(x1, 0, x2 - x1, height);
            
            g2d.setColor(new Color(0, 100, 200));
            g2d.drawLine(x1, 0, x1, height);
            g2d.drawLine(x2, 0, x2, height);
        }
        
        g2d.setColor(Color.GREEN);
        
        float samplesPerPixel = (float) samples.length / width;
        
        for (int x = 0; x < width; x++) {
            int sampleIndex = (int) (x * samplesPerPixel);
            int nextSampleIndex = (int) ((x + 1) * samplesPerPixel);
            
            float min = 0;
            float max = 0;
            
            for (int i = sampleIndex; i < nextSampleIndex && i < samples.length; i++) {
                if (samples[i] < min) min = samples[i];
                if (samples[i] > max) max = samples[i];
            }
            
            if (min == 0 && max == 0 && sampleIndex < samples.length) {
                min = max = samples[sampleIndex];
            }
            
            int y1 = centerY + (int) (min * centerY);
            int y2 = centerY + (int) (max * centerY);
            
            g2d.drawLine(x, y1, x, y2);
        }
    }
}