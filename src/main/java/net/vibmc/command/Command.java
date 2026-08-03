package net.vibmc.command;

public abstract class Command {
    private final String name;
    private final String description;
    private final String usage;
    private final String permission;

    public Command(String name, String description, String usage, String permission) {
        this.name = name;
        this.description = description;
        this.usage = usage;
        this.permission = permission;
    }

    public abstract boolean execute(CommandSender sender, String[] args);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public String getPermission() {
        return permission;
    }
}
