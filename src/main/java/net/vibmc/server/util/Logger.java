package net.vibmc.server.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class Logger {
    private final java.util.logging.Logger impl;
    private final SimpleDateFormat dateFormat;

    public Logger() {
        this.impl = java.util.logging.Logger.getLogger("vibMC");
        this.impl.setUseParentHandlers(false);
        this.dateFormat = new SimpleDateFormat("HH:mm:ss");

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new Formatter() {
            @Override
            public String format(LogRecord record) {
                String timestamp = dateFormat.format(new Date(record.getMillis()));
                String level = record.getLevel().getName();
                String message = record.getMessage();
                if (record.getParameters() != null) {
                    message = String.format(message, record.getParameters());
                }
                String throwable = "";
                if (record.getThrown() != null) {
                    StringWriter sw = new StringWriter();
                    record.getThrown().printStackTrace(new PrintWriter(sw));
                    throwable = "\n" + sw.toString();
                }
                return String.format("[%s %s] %s%s%n", timestamp, level, message, throwable);
            }
        });
        handler.setLevel(Level.ALL);
        impl.addHandler(handler);
        impl.setLevel(Level.ALL);
    }

    public void info(String msg, Object... args) {
        Throwable thrown = extractThrowable(args);
        log(Level.INFO, msg, args, thrown);
    }

    public void warn(String msg, Object... args) {
        Throwable thrown = extractThrowable(args);
        log(Level.WARNING, msg, args, thrown);
    }

    public void severe(String msg, Object... args) {
        Throwable thrown = extractThrowable(args);
        log(Level.SEVERE, msg, args, thrown);
    }

    public void debug(String msg, Object... args) {
        Throwable thrown = extractThrowable(args);
        log(Level.FINE, msg, args, thrown);
    }

    public void trace(String msg, Object... args) {
        Throwable thrown = extractThrowable(args);
        log(Level.FINER, msg, args, thrown);
    }

    private void log(Level level, String msg, Object[] args, Throwable thrown) {
        LogRecord record = new LogRecord(level, msg);
        record.setParameters(args);
        record.setThrown(thrown);
        impl.log(record);
    }

    private Throwable extractThrowable(Object[] args) {
        if (args == null || args.length == 0) return null;
        Object last = args[args.length - 1];
        if (last instanceof Throwable t) {
            // Remove the throwable from args so it doesn't appear in format
            args[args.length - 1] = "";
            return t;
        }
        return null;
    }
}
