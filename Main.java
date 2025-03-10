import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import traitement.TreatAudio;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            TreatAudio controller = new TreatAudio();
            controller.initialize();
        });
    }
}