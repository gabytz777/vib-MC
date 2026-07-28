package net.vibmc.scheduler;

import net.vibmc.server.VibMC;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class VibMCScheduler {
    private final List<ScheduledTask> pendingTasks;
    private final AtomicInteger taskIdCounter;
    private int currentTick;

    public VibMCScheduler() {
        this.pendingTasks = new CopyOnWriteArrayList<>();
        this.taskIdCounter = new AtomicInteger(0);
        this.currentTick = 0;
    }

    public void start() {
        currentTick = 0;
    }

    public void stop() {
        pendingTasks.clear();
    }

    public void tick() {
        currentTick++;
        List<ScheduledTask> toRun = new ArrayList<>();
        for (ScheduledTask task : pendingTasks) {
            if (task.isCancelled()) {
                continue;
            }
            if (task.getNextExecution() <= currentTick) {
                toRun.add(task);
            }
        }
        for (ScheduledTask task : toRun) {
            if (task.isCancelled()) continue;
            try {
                task.getRunnable().run();
            } catch (Exception e) {
                VibMC.getInstance().getLogger().severe("Error executing task %d: %s", task.getTaskId(), e);
            }
            if (task.isRepeating()) {
                task.setNextExecution(currentTick + task.getDelay());
            } else {
                pendingTasks.remove(task);
            }
        }
    }

    public int scheduleSync(Runnable task, int delayTicks) {
        int id = taskIdCounter.incrementAndGet();
        pendingTasks.add(new ScheduledTask(id, task, currentTick + delayTicks, 0, false));
        return id;
    }

    public int scheduleSyncRepeating(Runnable task, int delayTicks, int periodTicks) {
        int id = taskIdCounter.incrementAndGet();
        pendingTasks.add(new ScheduledTask(id, task, currentTick + delayTicks, periodTicks, true));
        return id;
    }

    public boolean cancelTask(int taskId) {
        for (ScheduledTask task : pendingTasks) {
            if (task.getTaskId() == taskId && !task.isCancelled()) {
                task.cancel();
                return true;
            }
        }
        return false;
    }

    public int getCurrentTick() {
        return currentTick;
    }

    private static class ScheduledTask {
        private final int taskId;
        private final Runnable runnable;
        private volatile int nextExecution;
        private final int delay;
        private final boolean repeating;
        private volatile boolean cancelled;

        ScheduledTask(int taskId, Runnable runnable, int nextExecution, int delay, boolean repeating) {
            this.taskId = taskId;
            this.runnable = runnable;
            this.nextExecution = nextExecution;
            this.delay = delay;
            this.repeating = repeating;
            this.cancelled = false;
        }

        public int getTaskId() { return taskId; }
        public Runnable getRunnable() { return runnable; }
        public int getNextExecution() { return nextExecution; }
        public int getDelay() { return delay; }
        public boolean isRepeating() { return repeating; }
        public boolean isCancelled() { return cancelled; }

        public void setNextExecution(int nextExecution) { this.nextExecution = nextExecution; }
        public void cancel() { this.cancelled = true; }
    }
}
