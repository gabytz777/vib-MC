package net.vibmc.scoreboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Scoreboard {
    private final String name;
    private final Map<String, Objective> objectives;
    private final Map<String, Team> teams;

    public Scoreboard(String name) {
        this.name = name;
        this.objectives = new ConcurrentHashMap<>();
        this.teams = new ConcurrentHashMap<>();
    }

    public Objective registerObjective(String name, String criteria) {
        Objective obj = new Objective(name, criteria);
        objectives.put(name, obj);
        return obj;
    }

    public Objective getObjective(String name) {
        return objectives.get(name);
    }

    public void removeObjective(String name) {
        objectives.remove(name);
    }

    public Team createTeam(String name) {
        Team team = new Team(name);
        teams.put(name, team);
        return team;
    }

    public Team getTeam(String name) {
        return teams.get(name);
    }

    public void removeTeam(String name) {
        teams.remove(name);
    }

    public String getName() { return name; }
    public Collection<Objective> getObjectives() { return objectives.values(); }
    public Collection<Team> getTeams() { return teams.values(); }

    public static class Objective {
        private final String name;
        private final String criteria;
        private String displayName;
        private int displaySlot;

        public Objective(String name, String criteria) {
            this.name = name;
            this.criteria = criteria;
            this.displayName = name;
            this.displaySlot = -1;
        }

        public String getName() { return name; }
        public String getCriteria() { return criteria; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String name) { this.displayName = name; }
        public int getDisplaySlot() { return displaySlot; }
        public void setDisplaySlot(int slot) { this.displaySlot = slot; }
    }
}
