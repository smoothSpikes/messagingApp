package assign.project;

import java.io.*;
import java.net.Socket;

public class tcpClient {

    private static Socket socket;
    private static DataInputStream dis;
    private static DataOutputStream dos;
    private static String username = "";
    private static boolean readOnly = false;

    public static void main(String[] args) {

        try {

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            socket = new Socket("127.0.0.1", 6666);

            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());

            System.out.println("Connected to server.");

            System.out.print("Enter username (leave empty for READ ONLY mode): ");
            username = console.readLine();

            // determine mode
            if (username == null || username.trim().isEmpty()) {
                readOnly = true;
                username = "";   // important: still send something
                System.out.println("READ ONLY MODE activated. You cannot send messages.");
            }

            // ALWAYS send username to server
            dos.writeUTF(username);
            dos.flush();

            // thread that listens to server messages
            Thread receiver = new Thread(() -> {
                try {
                    while (true) {
                        String msg = dis.readUTF();
                        System.out.println("\n" + msg);
                        System.out.print("Enter message: ");
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });

            receiver.start();

            // sending loop
            while (true) {

                System.out.print("Enter message: ");
                String msg = console.readLine();

                if (readOnly) {
                    System.out.println("READ ONLY MODE - sending disabled.");
                    continue;
                }

                if (msg.equalsIgnoreCase("bye") || msg.equalsIgnoreCase("end")) {
                    dos.writeUTF(msg);
                    dos.flush();
                    break;
                }

                if (msg.equalsIgnoreCase("allUsers")) {
                    dos.writeUTF("allUsers");
                    dos.flush();
                    continue;
                }

                dos.writeUTF(msg);
                dos.flush();
            }

            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}