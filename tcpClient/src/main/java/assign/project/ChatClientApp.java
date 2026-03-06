package assign.project;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ChatClientApp extends Application {

    private ChatClient client;

    private TextArea chatArea;
    private TextField usernameField;
    private TextField messageField;
    private Button connectButton;
    private Button sendButton;
    private Button allUsersButton;
    private Label statusLabel;
    private Circle statusCircle;
    private Label modeLabel;

    @Override
    public void start(Stage primaryStage) {
        String host = "127.0.0.1";
        int port = 6666;

        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/client.properties")) {
            if (in != null) {
                props.load(in);
                host = props.getProperty("server.host", host);
                port = Integer.parseInt(props.getProperty("server.port", String.valueOf(port)));
            }
        } catch (IOException | NumberFormatException ignored) {
        }

        var params = getParameters().getRaw();
        if (params.size() >= 2) {
            host = params.get(0);
            try {
                port = Integer.parseInt(params.get(1));
            } catch (NumberFormatException ignored) {
            }
        }

        client = new ChatClient(host, port);
        wireClientCallbacks();

        GridPane root = buildUI();

        Scene scene = new Scene(root, 700, 500);
        var css = getClass().getResource("/client.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        primaryStage.setTitle("TCP Chat Client");
        primaryStage.setScene(scene);
        primaryStage.show();

        updateConnectionState(false);
    }

    private void wireClientCallbacks() {
        client.setOnMessage(msg ->
                Platform.runLater(() -> chatArea.appendText(msg + "\n"))
        );

        client.setOnConnectionState(connected ->
                Platform.runLater(() -> updateConnectionState(connected))
        );

        client.setOnError(err ->
                Platform.runLater(() -> {
                    chatArea.appendText("[ERROR] " + err + "\n");
                    showAlert("Error", err);
                })
        );
    }

    private GridPane buildUI() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(8);
        grid.setVgap(8);
        grid.getStyleClass().add("root");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(100);
        grid.getColumnConstraints().add(col1);

        HBox topBox = new HBox(10);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.getStyleClass().add("top-bar");

        Label userLabel = new Label("Username:");
        usernameField = new TextField();
        usernameField.setPromptText("Enter username (empty = read-only)");

        connectButton = new Button("Connect");
        connectButton.setOnAction(e -> onConnectClicked());

        statusCircle = new Circle(6);
        statusLabel = new Label("Offline");

        HBox statusBox = new HBox(5, statusCircle, statusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        modeLabel = new Label("Mode: -");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBox.getChildren().addAll(userLabel, usernameField, connectButton, spacer, modeLabel, statusBox);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.getStyleClass().add("chat-area");

        HBox bottomBox = new HBox(10);
        bottomBox.setAlignment(Pos.CENTER_LEFT);
        bottomBox.getStyleClass().add("bottom-bar");

        messageField = new TextField();
        messageField.setPromptText("Type a message, 'allUsers', or 'bye'/'end'");
        HBox.setHgrow(messageField, Priority.ALWAYS);

        sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendCurrentMessage());

        allUsersButton = new Button("All Users");
        allUsersButton.setOnAction(e -> sendAllUsersCommand());

        messageField.setOnAction(e -> sendCurrentMessage());

        bottomBox.getChildren().addAll(messageField, allUsersButton, sendButton);

        GridPane.setConstraints(topBox, 0, 0);
        GridPane.setConstraints(chatArea, 0, 1);
        GridPane.setConstraints(bottomBox, 0, 2);

        GridPane.setVgrow(chatArea, Priority.ALWAYS);

        grid.getChildren().addAll(topBox, chatArea, bottomBox);

        return grid;
    }

    private void onConnectClicked() {
        if (client.isConnected()) {
            client.disconnect();
            return;
        }

        chatArea.clear();
        String username = usernameField.getText();

        client.connect(username);

        if (username == null || username.trim().isEmpty()) {
            modeLabel.setText("Mode: READ ONLY");
        } else {
            modeLabel.setText("Mode: NORMAL (" + username.trim() + ")");
        }
    }

    private void sendCurrentMessage() {
        String msg = messageField.getText();
        if (msg == null || msg.isBlank()) {
            return;
        }

        if (!client.isConnected()) {
            showAlert("Not connected", "You must connect to the server first.");
            return;
        }

        if (client.isReadOnly()) {
            chatArea.appendText("[INFO] READ ONLY MODE - sending disabled.\n");
            messageField.clear();
            return;
        }

        if (msg.equalsIgnoreCase("allUsers")) {
            client.sendCommand("allUsers");
        } else {
            client.send(msg);
        }

        messageField.clear();
    }

    private void sendAllUsersCommand() {
        if (!client.isConnected()) {
            showAlert("Not connected", "You must connect to the server first.");
            return;
        }
        client.sendCommand("allUsers");
    }

    private void updateConnectionState(boolean connected) {
        if (connected) {
            statusLabel.setText("Online");
            statusCircle.setFill(Color.LIMEGREEN);
            connectButton.setText("Disconnect");
            messageField.setDisable(false);
            sendButton.setDisable(client.isReadOnly());
        } else {
            statusLabel.setText("Offline");
            statusCircle.setFill(Color.DARKRED);
            connectButton.setText("Connect");
            messageField.setDisable(true);
            sendButton.setDisable(true);
            modeLabel.setText("Mode: -");
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    @Override
    public void stop() {
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}

