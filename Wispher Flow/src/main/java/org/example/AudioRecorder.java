package org.example;

import javax.sound.sampled.*;
import java.util.Arrays;

public class AudioRecorder implements Runnable {

    // --- CONFIGURATION ---
    // ADJUST THIS!
    // Lower (e.g., 200) = Sensitive (Hears breathing)
    // Higher (e.g., 1000) = Strict (Needs loud voice)
    private static final double SILENCE_THRESHOLD = 500.0;

    private TargetDataLine microphone;
    private volatile boolean isRecording = false;
    private Thread workerThread;

    public void startRecording() {
        if (isRecording) return;
        isRecording = true;
        workerThread = new Thread(this);
        workerThread.start();
    }

    public void stopRecording() {
        isRecording = false;
        // The loop will exit, and the 'finally' block will close the mic
    }

    public boolean isRecording() {
        return isRecording;
    }

    @Override
    public void run() {
        try {
            // 16kHz, 16-bit, Mono, Signed, Little Endian (False)
            AudioFormat format = new AudioFormat(16000f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("❌ Microphone not supported!");
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            System.out.println("🎙️ Mic ON. Noise Gate Threshold: " + SILENCE_THRESHOLD);

            byte[] buffer = new byte[2048]; // ~64ms buffer

            while (isRecording) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);

                if (bytesRead > 0) {
                    // --- PHASE 2 LOGIC: NOISE GATE ---
                    double volume = calculateRMS(buffer, bytesRead);

                    // Debugging: Uncomment this to see your mic volume numbers!
                    // System.out.println("Volume: " + volume);

                    if (volume > SILENCE_THRESHOLD) {
                        // "This is loud enough to be speech" -> Send to AI
                        byte[] dataToSend = Arrays.copyOf(buffer, bytesRead);
                        Main.audioQueue.put(dataToSend);
                    } else {
                        // "This is silence" -> Ignore it.
                        // The AI receives nothing, so it thinks "Silence" and waits.
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (microphone != null) {
                microphone.stop();
                microphone.close();
                System.out.println("🛑 Mic OFF");
            }
        }
    }

    /**
     * Calculates the Root Mean Square (RMS) amplitude of the buffer.
     * This turns raw bytes into a "Loudness" number (0 to 32767).
     */
    private double calculateRMS(byte[] audioData, int length) {
        long sum = 0;
        int sampleCount = length / 2; // 16-bit audio = 2 bytes per sample

        for (int i = 0; i < length; i += 2) {
            // Combine 2 bytes into 1 short (Little Endian)
            // byte[i] is Low Byte, byte[i+1] is High Byte
            int low = audioData[i] & 0xFF;
            int high = audioData[i + 1] << 8;

            short sample = (short) (high | low);

            sum += sample * sample;
        }

        double mean = sum / (double) sampleCount;
        return Math.sqrt(mean);
    }
}