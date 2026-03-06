package assign.project;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandlerThread extends Thread {

    private final Socket socket;
    private final ServerModel serverModel;
    private DataInputStream dis;
    private DataOutputStream dos;
    private String username;

    public ClientHandlerThread(Socket socket, ServerModel serverModel) {
        this.socket = socket;
        this.serverModel = serverModel;
        try {
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            serverModel.log("Error creating client streams: " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String msg) {
        try {
            dos.writeUTF(msg);
            dos.flush();
        } catch (IOException e) {
            serverModel.log("Error sending to client: " + e.getMessage());
        }
    }

    public void closeQuietly() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Override
    public void run() {
        try {
            username = dis.readUTF();

            if (username == null || username.trim().isEmpty()) {
                sendMessage("READ ONLY MODE activated.");
            }

            serverModel.registerClientUsername(this, username);

            while (true) {
                String msg = dis.readUTF();

                if (msg.equalsIgnoreCase("bye") || msg.equalsIgnoreCase("end")) {
                    break;
                }

                if (msg.equalsIgnoreCase("allUsers")) {
                    sendMessage(serverModel.getAllUsers());
                    continue;
                }

                if (username == null || username.trim().isEmpty()) {
                    sendMessage("READ ONLY MODE - you cannot send messages.");
                    continue;
                }

                serverModel.broadcast(msg, this);
            }

        } catch (IOException e) {
            serverModel.log("Client disconnected.");
        } finally {
            serverModel.removeClient(this);
            closeQuietly();
        }
    }
}

