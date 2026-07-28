package net.vibmc.scoreboard;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    private final Map<String, Scoreboard> scoreboards;

    public ScoreboardManager() {
        this.scoreboards = new ConcurrentHashMap<>();
    }

    public Scoreboard createScoreboard(String name) {
        Scoreboard sb = new Scoreboard(name);
        scoreboards.put(name, sb);
        return sb;
    }

    public Scoreboard getScoreboard(String name) {
        return scoreboards.get(name);
    }

    public void removeScoreboard(String name) {
        scoreboards.remove(name);
    }

    public Team createTeam(String name) {
        Scoreboard sb = getOrCreateMain();
        return sb.createTeam(name);
    }

    private Scoreboard getOrCreateMain() {
        return scoreboards.computeIfAbsent("main", Scoreboard::new);
    }
}
