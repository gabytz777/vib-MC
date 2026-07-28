package net.vibmc.server.util;

public class MathHelper {
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int floor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    public static int ceil(double value) {
        int i = (int) value;
        return value > i ? i + 1 : i;
    }

    public static float wrapDegrees(float angle) {
        angle %= 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }

    public static double square(double value) {
        return value * value;
    }

    public static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    public static float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }
}
