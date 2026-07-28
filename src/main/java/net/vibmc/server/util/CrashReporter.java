package net.vibmc.server.util;

import net.vibmc.server.VibMC;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrashReporter {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    public static void generateCrashReport(Throwable throwable, String context) {
        String timestamp = DATE_FORMAT.format(new Date());
        String fileName = "crash-" + timestamp + ".txt";

        File crashDir = new File("crash-reports");
        if (!crashDir.exists()) {
            crashDir.mkdirs();
        }

        File report = new File(crashDir, fileName);
        try (PrintWriter writer = new PrintWriter(new FileWriter(report))) {
            writer.println("---- vib-MC Crash Report ----");
            writer.println("Time: " + new Date());
            writer.println("Context: " + context);
            writer.println();
            writer.println("// Whoops! Something went wrong.");
            writer.println();
            writer.println("Stack trace:");
            throwable.printStackTrace(writer);
            writer.println();
            writer.println("System Information:");
            writer.println("  OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            writer.println("  Java: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
            writer.println("  Available processors: " + Runtime.getRuntime().availableProcessors());
            writer.println("  Max memory: " + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB");
            writer.println("  Total memory: " + Runtime.getRuntime().totalMemory() / 1024 / 1024 + " MB");
            writer.println("  Free memory: " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + " MB");
        } catch (Exception e) {
            System.err.println("Failed to write crash report: " + e.getMessage());
        }

        Logger logger = VibMC.getInstance().getLogger();
        logger.severe("Crash report saved to %s", report.getAbsolutePath());
    }
}
