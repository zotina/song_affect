package util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

public class AudioUtils {
    
    public static int read24BitSample(byte[] data, int offset, boolean isBigEndian) {
        int sample;
        if (isBigEndian) {
            sample = (data[offset] << 16) | ((data[offset + 1] & 0xFF) << 8) | (data[offset + 2] & 0xFF);
        } else {
            sample = (data[offset + 2] << 16) | ((data[offset + 1] & 0xFF) << 8) | (data[offset] & 0xFF);
        }
        
        
        if ((sample & 0x800000) != 0) {
            sample |= 0xFF000000;
        }
        
        return sample;
    }
    
    
   
    
    public static Clip createClip(byte[] audioData, AudioFormat format) throws LineUnavailableException, IOException {
        AudioInputStream audioInputStream = new AudioInputStream(
            new ByteArrayInputStream(audioData),
            format,
            audioData.length / format.getFrameSize()
        );
        
        Clip clip = AudioSystem.getClip();
        clip.open(audioInputStream);
        return clip;
    }
    
    
    public static String getAudioFormatDetails(AudioFormat format) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sample Rate: ").append(format.getSampleRate()).append(" Hz\n");
        sb.append("Sample Size: ").append(format.getSampleSizeInBits()).append(" bits\n");
        sb.append("Channels: ").append(format.getChannels()).append("\n");
        sb.append("Frame Size: ").append(format.getFrameSize()).append(" bytes\n");
        sb.append("Frame Rate: ").append(format.getFrameRate()).append(" frames/second\n");
        sb.append("Encoding: ").append(format.getEncoding()).append("\n");
        sb.append("Endianness: ").append(format.isBigEndian() ? "Big Endian" : "Little Endian");
        return sb.toString();
    }
}