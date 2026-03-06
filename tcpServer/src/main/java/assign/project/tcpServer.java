package assign.project;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Console entry point kept for compatibility.
 * Uses ServerModel to respect separation of concerns.
 */
public class tcpServer {

    public static void main(String[] args) {
        int port = 6666;

        Properties props = new Properties();
        try (InputStream in = tcpServer.class.getResourceAsStream("/server.properties")) {
            if (in != null) {
                props.load(in);
                port = Integer.parseInt(props.getProperty("server.port", String.valueOf(port)));
            }
        } catch (IOException | NumberFormatException ignored) {
        }

        ServerModel model = new ServerModel(port);
        model.setOnLog(System.out::println);

        Runtime.getRuntime().addShutdownHook(new Thread(model::stop));

        model.start();
    }
}
