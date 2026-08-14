package net.vibmc.network;

import net.vibmc.network.handler.HandshakeHandler;
import net.vibmc.server.VibMC;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NetworkServer {
    private ServerSocketChannel serverChannel;
    private Selector selector;
    private final Map<SocketChannel, ClientConnection> connections;
    private volatile boolean running;
    private Thread networkThread;
    private final ByteBuffer readBuffer;

    public NetworkServer() {
        this.connections = new ConcurrentHashMap<>();
        this.readBuffer = ByteBuffer.allocateDirect(65536);
    }

    public void start(String address, int port) throws IOException {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(address, port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        running = true;

        networkThread = new Thread(this::networkLoop, "Network Thread");
        networkThread.setDaemon(true);
        networkThread.start();
    }

    private void networkLoop() {
        while (running) {
            try {
                int selected = selector.select(50);
                if (selected == 0) {
                    continue;
                }
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    if (!key.isValid()) continue;

                    try {
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        } else if (key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (IOException e) {
                        closeConnection(key);
                    }
                }
            } catch (ClosedSelectorException e) {
                break;
            } catch (IOException e) {
                VibMC.getInstance().getLogger().severe("Network error: %s", e.getMessage());
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel client = serverChannel.accept();
        VibMC.getInstance().getLogger().debug("Accepted client from %s", client.getRemoteAddress());
        client.configureBlocking(false);
        client.socket().setTcpNoDelay(true);
        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
        ClientConnection connection = new ClientConnection(client, clientKey);
        connection.setHandler(new HandshakeHandler());
        connections.put(client, connection);
        clientKey.attach(connection);
        connection.getHandler().onConnect(connection);
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientConnection connection = connections.get(channel);
        if (connection == null) return;

        readBuffer.clear();
        int bytesRead = channel.read(readBuffer);
        if (bytesRead == -1) {
            closeConnection(key);
            return;
        }
        if (bytesRead == 0) return;

        readBuffer.flip();
        byte[] data = new byte[bytesRead];
        readBuffer.get(data);
        VibMC.getInstance().getLogger().debug("Read %d bytes from %s", bytesRead, connection.getUsername() != null ? connection.getUsername() : "client");
        connection.feed(data);
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ClientConnection connection = connections.get(channel);
        if (connection == null) return;

        if (connection.flushWrites()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    private void closeConnection(SelectionKey key) {
        if (key == null) return;
        SocketChannel channel = (SocketChannel) key.channel();
        ClientConnection connection = connections.remove(channel);
        if (connection != null) {
            String reason = "Connection closed";
            if (connection.getHandler() != null) {
                connection.getHandler().onDisconnect(connection, reason);
            }
            connection.forceClose();
        } else {
            try {
                key.cancel();
                channel.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public void stop() {
        running = false;
        for (ClientConnection conn : new ArrayList<>(connections.values())) {
            if (conn.getHandler() != null) {
                conn.getHandler().onDisconnect(conn, "Server shutting down");
            }
            conn.forceClose();
        }
        connections.clear();
        try {
            if (selector != null) selector.close();
            if (serverChannel != null) serverChannel.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public void tick() {
    }

    public int getOnlineCount() {
        return connections.size();
    }

    public Collection<ClientConnection> getConnections() {
        return Collections.unmodifiableCollection(connections.values());
    }
}
