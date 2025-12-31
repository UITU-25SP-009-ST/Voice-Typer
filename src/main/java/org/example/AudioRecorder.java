package org.example;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class AudioRecorder implements Runnable {

    // 1. SETTINGS
    private volatile boolean isRecording = false;
    private TargetDataLine microphone;
    private ByteArrayOutputStream audioBuffer;

    // We will determine this dynamically
    private static float sampleRate = 16000;

    // 2. THE THREAD LOOP
    @Override
    public void run() {
        try {
            // Use the sample rate we found works
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("❌ Mic refused settings in thread!");
                isRecording = false; // RESET FLAG
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            audioBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];

            while (isRecording) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    audioBuffer.write(buffer, 0, bytesRead);
                }
            }

            microphone.stop();
            microphone.close();

        } catch (Exception e) {
            System.err.println("❌ Recorder Crashed: " + e.getMessage());
            e.printStackTrace();
            isRecording = false; // IMPORTANT: Reset flag so we can try again
        }
    }

    // 3. NEGOTIATION (Find what the Mic supports)
    public float detectSupportedSampleRate() {
        float[] rates = { 16000f, 44100f, 48000f, 32000f, 8000f };

        for (float rate : rates) {
            try {
                AudioFormat format = new AudioFormat(rate, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

                if (AudioSystem.isLineSupported(info)) {
                    System.out.println("✅ Microphone accepted: " + rate + " Hz");
                    sampleRate = rate;
                    return rate;
                }
            } catch (Exception e) {
                // Try next rate
            }
        }
        System.err.println("❌ No supported microphone format found!");
        return 16000f; // Default fallback
    }

    // 4. CONTROL METHODS
    public void startRecording() {
        if (!isRecording) {
            isRecording = true;
            new Thread(this).start();
        }
    }

    public void stopRecording() {
        isRecording = false;
    }

    public byte[] getAudioData() {
        return (audioBuffer != null) ? audioBuffer.toByteArray() : new byte[0];
    }
}