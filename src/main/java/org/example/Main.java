package org.example;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;

public class Main implements NativeKeyListener {

    private static AudioRecorder recorder;
    private static Recognizer voskRecognizer;

    public static void main(String[] args) {
        if (!AdminManager.ensureAdmin()) return;

        try {
            System.out.println("🚀 Starting VoiceTyper...");
            LibVosk.setLogLevel(LogLevel.WARNINGS);

            // 1. SETUP AUDIO (Ask Mic what it supports FIRST)
            recorder = new AudioRecorder();
            float mySampleRate = recorder.detectSupportedSampleRate();

            // 2. SETUP VOSK (Tell it the specific speed we found)
            String jarPath = new File(Main.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getParent();
            String modelPath = jarPath + File.separator + "model";

            Model model = new Model(modelPath);

            // IMPORTANT: We pass the DETECTED sample rate to Vosk
            voskRecognizer = new Recognizer(model, mySampleRate);

            // 3. SETUP KEYBOARD
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new Main());

            System.out.println("✅ READY! (Mic set to " + (int)mySampleRate + "Hz)");
            System.out.println("👉 Hold [F9] to talk.");

            Thread.sleep(Long.MAX_VALUE);

        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_F9) {
            recorder.startRecording();
            System.out.print("\r🎤 Listening...");
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (e.getKeyCode() == NativeKeyEvent.VC_F9) {
            recorder.stopRecording();

            byte[] voiceData = recorder.getAudioData();
            if (voiceData.length > 0) {
                // Process audio
                String jsonResult;
                if (voskRecognizer.acceptWaveForm(voiceData, voiceData.length)) {
                    jsonResult = voskRecognizer.getResult();
                } else {
                    jsonResult = voskRecognizer.getFinalResult();
                }

                String cleanText = cleanVoskOutput(jsonResult);
                if (!cleanText.isEmpty()) {
                    System.out.println("\nTyping: " + cleanText);
                    typeString(cleanText);
                } else {
                    System.out.println("\n(No text detected)");
                }
            }
            voskRecognizer.reset();
        }
    }

    private static String cleanVoskOutput(String json) {
        if (json.contains(":")) {
            int start = json.indexOf(":") + 1;
            int end = json.lastIndexOf("}");
            return json.substring(start, end).replace("\"", "").trim();
        }
        return "";
    }

    public static void typeString(String text) {
        try {
            Robot robot = new Robot();
            for (char c : text.toCharArray()) {
                if (c == ' ') {
                    robot.keyPress(KeyEvent.VK_SPACE);
                    robot.keyRelease(KeyEvent.VK_SPACE);
                } else {
                    int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
                    if (keyCode != KeyEvent.VK_UNDEFINED) {
                        robot.keyPress(keyCode);
                        robot.keyRelease(keyCode);
                    }
                }
            }
            robot.keyPress(KeyEvent.VK_SPACE);
            robot.keyRelease(KeyEvent.VK_SPACE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}