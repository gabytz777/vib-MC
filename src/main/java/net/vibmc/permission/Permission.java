package net.vibmc.permission;

public class Permission {
    private final String name;
    private final String description;
    private final String defaultValue;

    public Permission(String name, String description) {
        this(name, description, "op");
    }

    public Permission(String name, String description, String defaultValue) {
        this.name = name;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDefaultValue() { return defaultValue; }
}
