package net.vibmc.server.util;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    /**
     * Whether debug lines are printed at all.
     *
     * <p>Static because the logger is built before the config is read, and because a
     * console you cannot type into for the packet chatter is not a console.
     */
    private static volatile boolean debugEnabled;

    private final String name;
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss");

    public Logger(String name) {
        this.name = name;
    }

    /** Turns the debug channel on, from the {@code log-level} setting. */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void info(String format, Object... args) {
        log("INFO", System.out, format, args);
    }

    public void warn(String format, Object... args) {
        log("WARN", System.err, format, args);
    }

    public void severe(String format, Object... args) {
        log("ERROR", System.err, format, args);
    }

    public void debug(String format, Object... args) {
        if (!debugEnabled) {
            return;
        }
        log("DEBUG", System.out, format, args);
    }

    private void log(String level, PrintStream out, String format, Object... args) {
        String message;
        try {
            message = String.format(format, args);
        } catch (Exception e) {
            message = format;
        }
        out.println("[" + timestampFormat.format(new Date()) + "] [" + level + "] [" + name + "] " + message);
        if (args.length > 0 && args[args.length - 1] instanceof Throwable) {
            ((Throwable) args[args.length - 1]).printStackTrace(out);
        }
    }
}
