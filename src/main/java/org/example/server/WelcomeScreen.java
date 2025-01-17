package org.example.server;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class WelcomeScreen extends Application {

    private Scene welcomeScene;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Welcome Screen");
        primaryStage.setScene(createWelcomeScene(primaryStage));
        primaryStage.show();

		primaryStage.getScene().getWindow().setOnCloseRequest(
			new EventHandler<WindowEvent>() {

				@Override
				public void handle(WindowEvent event) {
					System.exit(0);
				}
				
			}
		);
    }

    public Scene createWelcomeScene(Stage primaryStage) {
        VBox root = new VBox(20);
        root.setStyle("-fx-padding: 40; -fx-alignment: center; -fx-background-color: linear-gradient(to bottom, #2b5876, #4e4376);");

        Button hostButton = new Button("Host");
        Button joinButton = new Button("Join");

        hostButton.setPrefWidth(300);
        joinButton.setPrefWidth(300);

        String buttonStyle = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; " +
                "-fx-background-color: linear-gradient(to bottom, #3c8dbc, #357ca5); " +
                "-fx-border-radius: 8px; -fx-background-radius: 8px;";

        hostButton.setStyle(buttonStyle);
        joinButton.setStyle(buttonStyle);

        Label label = new Label("Tank Wars");
        label.setStyle("-fx-text-fill: white;");
        label.setFont(Font.font("Monospaced", 80));
        VBox.setMargin(label, new Insets(0, 0, 40, 0));

        root.getChildren().addAll(label, hostButton, joinButton);


        hostButton.setOnAction(e -> {
            ConfigurationScreen hostScene = new ConfigurationScreen(primaryStage, this,true);
            primaryStage.setScene(hostScene.getScene());
        });

        joinButton.setOnAction(e -> {
            ConfigurationScreen hostScene = new ConfigurationScreen(primaryStage, this, false);
            primaryStage.setScene(hostScene.getScene());
        });

        welcomeScene = new Scene(root, 800, 800);
        return welcomeScene;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

