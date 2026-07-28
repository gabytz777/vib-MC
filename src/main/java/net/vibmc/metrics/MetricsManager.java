package net.vibmc.metrics;

import net.vibmc.server.VibMC;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class MetricsManager {
    private final List<Integer> tpsHistory;
    private int currentTps;
    private final long startTime;
    private final Map<String, Long> timings;

    public MetricsManager() {
        this.tpsHistory = new CopyOnWriteArrayList<>();
        this.currentTps = 20;
        this.startTime = System.currentTimeMillis();
        this.timings = new HashMap<>();
    }

    public void start() {
        VibMC.getInstance().getScheduler().scheduleSyncRepeating(() -> {
            if (tpsHistory.size() > 1200) {
                tpsHistory.remove(0);
            }
        }, 0, 1200);
    }

    public void recordTps(int tps) {
        this.currentTps = tps;
        tpsHistory.add(tps);
        if (tpsHistory.size() > 1200) {
            tpsHistory.remove(0);
        }
    }

    public int getCurrentTps() {
        return currentTps;
    }

    public double getAverageTps() {
        if (tpsHistory.isEmpty()) return 20.0;
        return tpsHistory.stream().mapToInt(Integer::intValue).average().orElse(20.0);
    }

    public double getMaxTps() {
        return tpsHistory.stream().mapToInt(Integer::intValue).max().orElse(20);
    }

    public double getMinTps() {
        return tpsHistory.stream().mapToInt(Integer::intValue).min().orElse(20);
    }

    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }

    public String getUptimeFormatted() {
        long uptime = getUptime() / 1000;
        long hours = uptime / 3600;
        long minutes = (uptime % 3600) / 60;
        long seconds = uptime % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    public void startTiming(String name) {
        timings.put(name, System.nanoTime());
    }

    public long endTiming(String name) {
        Long start = timings.remove(name);
        if (start == null) return 0;
        return System.nanoTime() - start;
    }

    public Map<String, Object> getServerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("tps", currentTps);
        stats.put("avg_tps", getAverageTps());
        stats.put("uptime", getUptimeFormatted());
        stats.put("players", VibMC.getInstance().getPlayerManager().getOnlineCount());
        stats.put("worlds", VibMC.getInstance().getWorldManager().getMainWorld().getName());

        Runtime rt = Runtime.getRuntime();
        stats.put("memory_used", (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024 + " MB");
        stats.put("memory_max", rt.maxMemory() / 1024 / 1024 + " MB");
        stats.put("processors", rt.availableProcessors());
        return stats;
    }
}
