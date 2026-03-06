package assign.project;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class ServerApp extends Application {

    private ServerModel serverModel;

    private TextArea logArea;
    private ListView<String> userListView;
    private ObservableList<String> userItems;
    private Label statusLabel;
    private Circle statusCircle;
    private Button startStopButton;

    @Override
    public void start(Stage primaryStage) {
        int port = 6666;

        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/server.properties")) {
            if (in != null) {
                props.load(in);
                port = Integer.parseInt(props.getProperty("server.port", String.valueOf(port)));
            }
        } catch (IOException | NumberFormatException ignored) {
        }

        serverModel = new ServerModel(port);
        wireModelCallbacks();

        GridPane root = buildUI(port);

        Scene scene = new Scene(root, 700, 500);
        var css = getClass().getResource("/server.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }

        primaryStage.setTitle("TCP Chat Server");
        primaryStage.setScene(scene);
        primaryStage.show();

        updateServerState(false);
    }

    private void wireModelCallbacks() {
        serverModel.setOnLog(msg ->
                Platform.runLater(() -> {
                    logArea.appendText(msg + "\n");
                })
        );

        serverModel.setOnUserListChanged(this::updateUserList);
    }

    private GridPane buildUI(int port) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(8);
        grid.setVgap(8);
        grid.getStyleClass().add("root");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(65);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(35);
        grid.getColumnConstraints().addAll(col1, col2);

        HBox topBox = new HBox(10);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.getStyleClass().add("top-bar");

        Label title = new Label("Server Port: " + port);

        statusCircle = new Circle(6);
        statusLabel = new Label("Stopped");
        HBox statusBox = new HBox(5, statusCircle, statusLabel);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        startStopButton = new Button("Start");
        startStopButton.setOnAction(e -> onStartStopClicked());

        var spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBox.getChildren().addAll(title, spacer, statusBox, startStopButton);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("log-area");

        userItems = FXCollections.observableArrayList();
        userListView = new ListView<>(userItems);
        userListView.getStyleClass().add("user-list");
        userListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    Map<String, String> colors = serverModel.getUserColorsSnapshot();
                    String color = colors.getOrDefault(item, "#4b5563");
                    setStyle("-fx-background-color: " + color + "; -fx-text-fill: white;");
                }
            }
        });

        GridPane.setConstraints(topBox, 0, 0, 2, 1);
        GridPane.setConstraints(logArea, 0, 1);
        GridPane.setConstraints(userListView, 1, 1);

        GridPane.setVgrow(logArea, Priority.ALWAYS);
        GridPane.setVgrow(userListView, Priority.ALWAYS);

        grid.getChildren().addAll(topBox, logArea, userListView);

        return grid;
    }

    private void onStartStopClicked() {
        if (serverModel.isRunning()) {
            serverModel.stop();
            updateServerState(false);
        } else {
            serverModel.start();
            updateServerState(true);
        }
    }

    private void updateServerState(boolean running) {
        if (running) {
            statusLabel.setText("Running");
            statusCircle.setFill(Color.LIMEGREEN);
            startStopButton.setText("Stop");
        } else {
            statusLabel.setText("Stopped");
            statusCircle.setFill(Color.DARKRED);
            startStopButton.setText("Start");
        }
    }

    private void updateUserList(Set<String> users) {
        Platform.runLater(() -> {
            userItems.setAll(users);
        });
    }

    @Override
    public void stop() {
        if (serverModel != null && serverModel.isRunning()) {
            serverModel.stop();
        }
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}

