package assign.project;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Pure server-side model that manages the TCP server and connected clients.
 * No JavaFX types are used here.
 */
public class ServerModel {

    private final int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    private final Set<ClientHandlerThread> clients = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, String> userColors = Collections.synchronizedMap(new HashMap<>());

    private Consumer<String> onLog = s -> {};
    private Consumer<Set<String>> onUserListChanged = users -> {};

    private static final String[] COLOR_HEX = {
            "#f97316", "#22c55e", "#0ea5e9", "#a855f7", "#eab308",
            "#ef4444", "#14b8a6", "#6366f1"
    };

    public ServerModel(int port) {
        this.port = port;
    }

    public void setOnLog(Consumer<String> onLog) {
        this.onLog = onLog != null ? onLog : s -> {};
    }

    public void setOnUserListChanged(Consumer<Set<String>> onUserListChanged) {
        this.onUserListChanged = onUserListChanged != null ? onUserListChanged : users -> {};
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;

        Thread accepter = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                serverSocket = ss;
                log("Server Started on port " + port);
                log("Waiting for Client...");

                while (running) {
                    Socket socket = ss.accept();
                    log("Accepted connection from " + socket.getInetAddress());
                    ClientHandlerThread handler = new ClientHandlerThread(socket, this);
                    clients.add(handler);
                    handler.start();
                }
            } catch (IOException e) {
                if (running) {
                    log("Server error: " + e.getMessage());
                }
            } finally {
                running = false;
            }
        }, "Server-Accepter");
        accepter.setDaemon(true);
        accepter.start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        synchronized (clients) {
            for (ClientHandlerThread c : clients) {
                c.closeQuietly();
            }
            clients.clear();
        }
        notifyUserListChanged();
        log("Server Stopped.");
    }

    public void broadcast(String msg, ClientHandlerThread sender) {
        String name = sender != null ? sender.getUsername() : "Server";
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String formatted = "[" + time + "] " + name + ": " + msg;

        synchronized (clients) {
            for (ClientHandlerThread c : clients) {
                if (c != sender) {
                    c.sendMessage(formatted);
                }
            }
        }
        log("Broadcast: " + formatted);
    }

    public String getAllUsers() {
        StringBuilder users = new StringBuilder("Active users:\n");
        synchronized (clients) {
            for (ClientHandlerThread c : clients) {
                if (c.getUsername() != null && !c.getUsername().isBlank()) {
                    users.append(c.getUsername()).append("\n");
                }
            }
        }
        return users.toString();
    }

    public void registerClientUsername(ClientHandlerThread client, String username) {
        if (username != null && !username.isBlank()) {
            String color = COLOR_HEX[Math.abs(username.hashCode()) % COLOR_HEX.length];
            userColors.put(username, color);
            log("Welcome " + username + " (" + color + ")");
            broadcast(username + " joined the chat.", client);
            notifyUserListChanged();
        } else {
            log("Client connected in READ ONLY MODE.");
        }
    }

    public void removeClient(ClientHandlerThread client) {
        clients.remove(client);
        String username = client.getUsername();
        if (username != null && !username.isBlank()) {
            broadcast(username + " left the chat.", client);
        }
        notifyUserListChanged();
    }

    public Map<String, String> getUserColorsSnapshot() {
        synchronized (userColors) {
            return new HashMap<>(userColors);
        }
    }

    void log(String msg) {
        onLog.accept(msg);
    }

    private void notifyUserListChanged() {
        Set<String> users = new HashSet<>();
        synchronized (clients) {
            for (ClientHandlerThread c : clients) {
                if (c.getUsername() != null && !c.getUsername().isBlank()) {
                    users.add(c.getUsername());
                }
            }
        }
        onUserListChanged.accept(users);
    }
}

