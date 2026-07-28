package net.vibmc.scoreboard;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Team {
    private final String name;
    private String displayName;
    private String prefix;
    private String suffix;
    private String color;
    private boolean allowFriendlyFire;
    private boolean seeInvisibleTeam;
    private final Set<String> members;

    public Team(String name) {
        this.name = name;
        this.displayName = name;
        this.prefix = "";
        this.suffix = "";
        this.color = "white";
        this.allowFriendlyFire = false;
        this.seeInvisibleTeam = true;
        this.members = ConcurrentHashMap.newKeySet();
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public boolean isAllowFriendlyFire() { return allowFriendlyFire; }
    public void setAllowFriendlyFire(boolean allow) { this.allowFriendlyFire = allow; }
    public boolean isSeeInvisibleTeam() { return seeInvisibleTeam; }
    public void setSeeInvisibleTeam(boolean see) { this.seeInvisibleTeam = see; }

    public void addMember(String playerName) { members.add(playerName); }
    public void removeMember(String playerName) { members.remove(playerName); }
    public boolean hasMember(String playerName) { return members.contains(playerName); }
    public Set<String> getMembers() { return members; }
}
