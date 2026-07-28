package net.vibmc.scoreboard;

import net.vibmc.entity.PlayerEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BossBar {
    private final String id;
    private String title;
    private float progress;
    private int color;
    private int division;
    private final Set<PlayerEntity> players;

    public BossBar(String id, String title) {
        this.id = id;
        this.title = title;
        this.progress = 1.0f;
        this.color = 0;
        this.division = 0;
        this.players = ConcurrentHashMap.newKeySet();
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public float getProgress() { return progress; }
    public void setProgress(float progress) { this.progress = Math.max(0, Math.min(1, progress)); }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    public int getDivision() { return division; }
    public void setDivision(int division) { this.division = division; }

    public void addPlayer(PlayerEntity player) { players.add(player); }
    public void removePlayer(PlayerEntity player) { players.remove(player); }
    public Set<PlayerEntity> getPlayers() { return players; }

    public void removeAll() { players.clear(); }
}
