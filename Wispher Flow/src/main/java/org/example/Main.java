package org.example;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class Main implements NativeKeyListener {

    // --- GLOBAL SHARED MEMORY (The Queue) ---
    // This is the bridge between AudioRecorder and Vosk
    public static final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();

    // --- AI COMPONENT ---
    private static Model model;
    private static Recognizer recognizer;
    private static boolean isAppRunning = true;

    // --- RECORDER ---
    private static final AudioRecorder recorder = new AudioRecorder();

    public static void main(String[] args) {
        // 1. Silence Vosk logs to keep console clean
        LibVosk.setLogLevel(LogLevel.WARNINGS);

        // 2. Initialize JNativeHook (The Key Listener)
        try {
            GlobalScreen.registerNativeHook();
        } catch (Exception e) {
            System.err.println("Failed to register native hook!");
            e.printStackTrace();
            return;
        }

        // 3. WARM UP: Load Model Once at Startup
        // Replace this path with YOUR actual model folder path
//        String modelPath = "C:\\Users\\YourUser\\Downloads\\vosk-model-en-us-0.22";
        String modelPath = "./model";

        try {
            System.out.println("⏳ Loading AI Model... Please wait...");
            model = new Model(modelPath);
            recognizer = new Recognizer(model, 16000f);
            System.out.println("✅ Model Loaded! System Ready. Hold F9 to speak.");

            // 4. Start the AI Processor Thread (The Brain)
            // This runs in the background forever, waiting for data.
            Thread processorThread = new Thread(Main::processAudioQueue);
            processorThread.setDaemon(true); // Auto-closes when app closes
            processorThread.start();

            // 5. Connect the Key Listener
            GlobalScreen.addNativeKeyListener(new Main());

        } catch (Exception e) {
            System.err.println("❌ Error loading model: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // --- THE BRAIN: Consumes Audio from Queue ---
    private static void processAudioQueue() {
        while (isAppRunning) {
            try {
                // .take() BLOCKS here until data arrives.
                // It uses 0% CPU while waiting for you to press F9.
                byte[] data = audioQueue.take();

                // Feed data to Vosk
                if (recognizer.acceptWaveForm(data , data.length)) {
                    // Case A: Silence detected (Sentence complete)
                    // We can execute final typing logic here later
                    // String result = recognizer.getResult();
                    // System.out.println("FINAL: " + result);
                } else {
                    // Case B: Still speaking (Partial result)
                    // This is where Real-Time magic happens
                    String partialJson = recognizer.getPartialResult();

                    // Simple print to prove it works (We will add Typing logic in Phase 3)
                    // Only print if it's not empty text
                    if (partialJson.length() > 20) {
                        System.out.println(">> " + partialJson);
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // --- KEYBOARD CONTROLS ---
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_F9) {
            if (!recorder.isRecording()) {
                recorder.startRecording();
            }
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_F9) {
            if (recorder.isRecording()) {
                recorder.stopRecording();
                // Optional: Reset recognizer so next sentence starts fresh
                // recognizer.reset();
            }
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {}
}