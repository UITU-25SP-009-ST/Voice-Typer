package org.example;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

    public class AdminManager {

        public static boolean ensureAdmin() {
            if (isAdmin()) {
                return true; // We are already God-mode
            } else {
                // We are a peasant. Time to revolt.
                System.out.println("⚠️ Not Admin. Relaunching with permission request...");
                relaunchAsAdmin();
                System.exit(0); // Die, so the Admin version can live
                return false;
            }
        }

        // 1. Check if we currently have Admin privileges
        private static boolean isAdmin() {
            try {
                // "reg query" only works if you have Admin rights to read this specific protected key
                ProcessBuilder pb = new ProcessBuilder("reg", "query", "HKU\\S-1-5-19");
                Process process = pb.start();
                process.waitFor();
                return (process.exitValue() == 0);
            } catch (Exception e) {
                return false;
            }
        }

        // 2. Relaunch the current Jar with 'RunAs' (Admin)
        // REPLACE the old relaunchAsAdmin() with this:
        private static void relaunchAsAdmin() {
            try {
                String jarPath = new File(AdminManager.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()).getAbsolutePath();

                // Debug: Print where we think the jar is
                System.out.println("Path: " + jarPath);

                // Build a command that opens a NEW CMD window and keeps it open (/k)
                List<String> command = new ArrayList<>();
                command.add("powershell.exe");
                command.add("Start-Process");
                command.add("cmd.exe");
                // /k = Keep window open, /c = Run java command
                // We use triple quotes \" to escape quotes inside PowerShell arguments
                command.add("-ArgumentList");
                command.add("'/k java -jar \"" + jarPath + "\"'");
                command.add("-Verb");
                command.add("RunAs");

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }