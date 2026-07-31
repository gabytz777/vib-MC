package net.vibmc.server;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load("server.properties");
        Server server = new Server(config);
        server.start();
    }
}
