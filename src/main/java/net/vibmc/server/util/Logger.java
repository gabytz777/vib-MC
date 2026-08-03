package net.vibmc.server.util;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
    private final String name;
    private final SimpleDateFormat timestampFormat = new SimpleDateFormat("HH:mm:ss");

    public Logger(String name) {
        this.name = name;
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
