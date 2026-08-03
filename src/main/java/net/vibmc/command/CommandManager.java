package net.vibmc.command;

import net.vibmc.command.commands.*;
import net.vibmc.entity.PlayerEntity;
import net.vibmc.permission.PermissionManager;
import net.vibmc.server.VibMC;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CommandManager {
    private final Map<String, Command> commands;
    private Thread consoleThread;

    public CommandManager() {
        this.commands = new HashMap<>();
        registerDefaults();
    }

    private void registerDefaults() {
        register(new HelpCommand());
        register(new TeleportCommand());
        register(new GamemodeCommand());
        register(new TimeCommand());
        register(new WeatherCommand());
        register(new GiveCommand());
        register(new KillCommand());
        register(new SayCommand());
        register(new SeedCommand());
        register(new SaveAllCommand());
        register(new StopCommand());
        register(new ListCommand());
    }

    public void register(Command command) {
        commands.put(command.getName().toLowerCase(), command);
        VibMC.getInstance().getLogger().debug("Registered command: /%s", command.getName());
    }

    public void registerCommand(Command command) {
        register(command);
    }

    public boolean execute(CommandSender sender, String input) {
        if (input == null || input.trim().isEmpty()) return false;

        String[] parts = input.trim().split(" ");
        String commandName = parts[0].toLowerCase();
        if (commandName.startsWith("/")) {
            commandName = commandName.substring(1);
        }
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);

        Command command = commands.get(commandName);
        if (command == null) {
            sender.sendMessage("{\"text\":\"§cUnknown command. Use /help for a list of commands.\"}");
            return false;
        }

        if (command.getPermission() != null && !command.getPermission().isEmpty()) {
            if (sender.isPlayer()) {
                PlayerEntity player = sender.getPlayer();
                PermissionManager permManager = VibMC.getInstance().getPluginManager().getPermissionManager();
                if (!permManager.hasPermission(player, command.getPermission())) {
                    sender.sendMessage("{\"text\":\"§cYou don't have permission to use this command.\"}");
                    return false;
                }
            }
        }

        return command.execute(sender, args);
    }

    public void startConsole() {
        consoleThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (VibMC.getInstance().isRunning() && scanner.hasNextLine()) {
                    String input = scanner.nextLine();
                    if (input == null) break;
                    CommandSender console = new CommandSender("CONSOLE");
                    execute(console, input);
                }
            } catch (Exception e) {
                // Console input ended
            }
        }, "Console");
        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    public Command getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    public Map<String, Command> getCommands() {
        return commands;
    }
}
