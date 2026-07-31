package net.vibmc.command;

import net.vibmc.server.Server;

import java.util.LinkedHashMap;
import java.util.Map;

public class CommandManager {
    private final Server server;
    private final Map<String, Command> commands = new LinkedHashMap<>();

    public CommandManager(Server server) {
        this.server = server;
        register(new HelpCommand());
        register(new TpCommand());
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
        commands.put(command.name(), command);
    }

    public boolean dispatch(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String[] parts = trimmed.split("\\s+");
        Command command = commands.get(parts[0].toLowerCase());
        if (command == null) {
            return false;
        }
        command.execute(server, parts);
        return true;
    }

    public interface Command {
        String name();

        void execute(Server server, String[] args);
    }

    public static class HelpCommand implements Command {
        @Override
        public String name() {
            return "help";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println("Available commands: help, tp, gamemode, time, weather, give, kill, say, seed, save-all, stop, list");
        }
    }

    public static class TpCommand implements Command {
        @Override
        public String name() {
            return "tp";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println("Teleport command not fully implemented yet.");
        }
    }

    public static class GamemodeCommand implements Command {
        @Override
        public String name() {
            return "gamemode";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println("Gamemode command not fully implemented yet.");
        }
    }

    public static class TimeCommand implements Command {
        @Override
        public String name() {
            return "time";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println("Time command available.");
        }
    }

    public static class WeatherCommand implements Command {
        @Override
        public String name() {
            return "weather";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println("Weather hook ready.");
        }
    }

    public static class GiveCommand implements Command {
        @Override
        public String name() {
            return "give";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println("Give command available.");
        }
    }

    public static class KillCommand implements Command {
        @Override
        public String name() {
            return "kill";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println("Kill command available.");
        }
    }

    public static class SayCommand implements Command {
        @Override
        public String name() {
            return "say";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println(String.join(" ", args));
        }
    }

    public static class SeedCommand implements Command {
        @Override
        public String name() {
            return "seed";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println("World seed: " + server.world().seed());
        }
    }

    public static class SaveAllCommand implements Command {
        @Override
        public String name() {
            return "save-all";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println("Saved world state.");
        }
    }

    public static class StopCommand implements Command {
        @Override
        public String name() {
            return "stop";
        }

        @Override
        public void execute(Server server, String[] args) {
            server.stop();
        }
    }

    public static class ListCommand implements Command {
        @Override
        public String name() {
            return "list";
        }

        @Override
        public void execute(Server server, String[] args) {
            System.out.println("No players connected.");
        }
    }
}
