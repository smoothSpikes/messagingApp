package assign.project;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Pure model class responsible for socket communication.
 * No JavaFX types are referenced here to keep a clean separation of concerns.
 */
public class ChatClient {

    private final String host;
    private final int port;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private String username = "";
    private boolean readOnly = false;
    private volatile boolean running = false;

    private Consumer<String> onMessage = s -> {};
    private Consumer<Boolean> onConnectionState = b -> {};
    private Consumer<String> onError = s -> {};

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setOnMessage(Consumer<String> onMessage) {
        this.onMessage = onMessage != null ? onMessage : s -> {};
    }

    public void setOnConnectionState(Consumer<Boolean> onConnectionState) {
        this.onConnectionState = onConnectionState != null ? onConnectionState : b -> {};
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError != null ? onError : s -> {};
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isConnected() {
        return running;
    }

    public String getUsername() {
        return username;
    }

    public void connect(String username) {
        if (running) {
            return;
        }

        try {
            socket = new Socket(host, port);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            if (username == null || username.trim().isEmpty()) {
                this.readOnly = true;
                this.username = "";
            } else {
                this.readOnly = false;
                this.username = username.trim();
            }

            dos.writeUTF(this.username);
            dos.flush();

            running = true;
            onConnectionState.accept(true);

            Thread receiver = new Thread(() -> {
                try {
                    while (running) {
                        String msg = dis.readUTF();
                        onMessage.accept(msg);
                    }
                } catch (IOException e) {
                    if (running) {
                        onError.accept("Connection lost: " + e.getMessage());
                    }
                } finally {
                    running = false;
                    onConnectionState.accept(false);
                    closeQuietly();
                }
            }, "ChatClient-Receiver");
            receiver.setDaemon(true);
            receiver.start();

        } catch (IOException e) {
            onError.accept("Cannot connect to server: " + e.getMessage());
            closeQuietly();
        }
    }

    public void send(String msg) {
        if (!running || dos == null) {
            return;
        }

        try {
            if (readOnly) {
                return;
            }

            if (msg.equalsIgnoreCase("bye") || msg.equalsIgnoreCase("end")) {
                dos.writeUTF(msg);
                dos.flush();
                disconnect();
                return;
            }

            dos.writeUTF(msg);
            dos.flush();
        } catch (IOException e) {
            onError.accept("Error sending message: " + e.getMessage());
        }
    }

    public void sendCommand(String cmd) {
        if (!running || dos == null) {
            return;
        }
        try {
            dos.writeUTF(cmd);
            dos.flush();
        } catch (IOException e) {
            onError.accept("Error sending command: " + e.getMessage());
        }
    }

    public void disconnect() {
        running = false;
        closeQuietly();
        onConnectionState.accept(false);
    }

    private void closeQuietly() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
    }
}

