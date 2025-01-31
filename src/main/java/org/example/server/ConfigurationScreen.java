package org.example.server;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.example.Tank.Tank;
import org.jspace.*;

import java.net.InetAddress;


public class ConfigurationScreen {

    private static final String STANDARD_IP_ADDRESS = "127.0.0.1";
    private static final int STANDARD_PORT_NUMBER = 31145;
    private Scene scene;

    public ConfigurationScreen(Stage primaryStage, WelcomeScreen welcomeScreen, boolean isHost) {
        VBox root = createRootLayout();

        Label titleLabel = createLabel("Game Configuration", "Arial", 30, true, "#ffffff");
        VBox.setMargin(titleLabel, new Insets(0, 0, 40, 0));

        Label playerNameLabel = createLabel("Player Name:", "Arial", 14, false, "#cfcfcf");
        TextField playerNameField = createTextField();
        playerNameField.setPromptText("Enter your player name");

        Label ipLabel = createLabel("IP Address:", "Arial", 14, false, "#cfcfcf");
        TextField ipField = createTextField();
        ipField.setPromptText(isHost ? "Enter your server IP address to host" : "Enter the server IP address to join");

        Label portLabel = createLabel("Port Number:", "Arial", 14, false, "#cfcfcf");
        TextField portField = createTextField();
        portField.setPromptText(isHost ? "Enter your server Port number to host" : "Enter the server´s Port number to join");

        Button hostOrJoinButton = createStyledButton(isHost ? "Host" : "Join", "linear-gradient(to bottom, #4e9af1, #357ca5)");
        hostOrJoinButton.setOnAction(e -> handleSubmission(primaryStage,playerNameField, ipField, portField, isHost));;

        Button backButton = createStyledButton("Main Menu", "linear-gradient(to bottom, #f45c43, #eb3349)");
        backButton.setOnAction(e -> primaryStage.setScene(welcomeScreen.createWelcomeScene(primaryStage)));

        root.getChildren().addAll(titleLabel, playerNameLabel, playerNameField, ipLabel, ipField, portLabel, portField, hostOrJoinButton, backButton);

        this.scene = new Scene(root, 800, 800);

		primaryStage.getScene().getWindow().setOnCloseRequest(
			new EventHandler<WindowEvent>() {

				@Override
				public void handle(WindowEvent event) {
					System.exit(0);
				}
				
			}
		);
    }

    private VBox createRootLayout() {
        VBox root = new VBox(20);
        root.setStyle("-fx-padding: 40; -fx-alignment: center; -fx-background-color: linear-gradient(to bottom, #1c1c3c, #4e4376);");
        return root;
    }

    private Label createLabel(String text, String font, int size, boolean bold, String color) {
        Label label = new Label(text);
        label.setFont(Font.font(font, bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        label.setStyle("-fx-text-fill: " + color + ";");
        return label;
    }

    private TextField createTextField() {
        TextField textField = new TextField();
        textField.setPrefWidth(100);
        textField.setStyle("-fx-background-color: #ffffff; -fx-border-color: #6b6b9d; -fx-border-radius: 5px; -fx-padding: 5px;");
        return textField;
    }

    private Button createStyledButton(String text, String backgroundColor) {
        Button button = new Button(text);
        button.setPrefWidth(200);
        String baseStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white; -fx-border-radius: 10px; -fx-background-radius: 10px;";
        button.setStyle(baseStyle + " -fx-background-color: " + backgroundColor + ";");
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + " -fx-background-color: #285d8b;"));
        button.setOnMouseExited(e -> button.setStyle(baseStyle + " -fx-background-color: " + backgroundColor + ";"));
        return button;
    }

    private void handleSubmission(Stage primaryStage,TextField playerNameField, TextField ipField, TextField portField, boolean isHost) {
        int port = STANDARD_PORT_NUMBER;
        String address;
        boolean valid = true;

        if(playerNameField.getText().isEmpty()){
            playerNameField.setStyle("-fx-background-color: #ffffff; -fx-border-color: red; -fx-border-radius: 5px; -fx-padding: 5px;");
            playerNameField.clear();
            playerNameField.setPromptText("Please Enter a Name");
            valid = false;
        }

        try {
            if (portField.getText().isEmpty()) {
                port = STANDARD_PORT_NUMBER;
            } else {
                port = Integer.parseInt(portField.getText());
                if (port < 1024 || port > 49151) {
                    throw new NumberFormatException();
                }
            }
        } catch (NumberFormatException e) {
            portField.setStyle("-fx-background-color: #ffffff; -fx-border-color: red; -fx-border-radius: 5px; -fx-padding: 5px;");
            portField.clear();
            portField.setPromptText("Invalid Port Number");
            valid = false;
        }

        if (!isValidIPAddress(ipField.getText())) {
            ipField.setStyle("-fx-background-color: #ffffff; -fx-border-color: red; -fx-border-radius: 5px; -fx-padding: 5px;");
            ipField.clear();
            ipField.setPromptText("Invalid IP Address");
            valid = false;
        }


        if (valid) {
            //port = portField.getText().isEmpty() ? STANDARD_PORT_NUMBER : Integer.parseInt(portField.getText());
            address = ipField.getText().isEmpty() ? STANDARD_IP_ADDRESS : ipField.getText();

            if (isHost) {
                System.out.println("Proceeding with Player Name: " + playerNameField.getText() + ", Address: " + address + ", Port: " + port);
                intializeServer(address, port);
                //switchToLobbyScene(primaryStage, address, port, playerNameField.getText());


            } else {
                switchToLobbyScene(primaryStage, address, port, playerNameField.getText());

                System.out.println("Joining server at Address: " + address + ", Port: " + port);
            }

            if (isHost)  switchToLobbyScene(primaryStage, address, port, playerNameField.getText());



        }

    }

    private static void intializeServer(String address, int port) {
        String uri = "tcp://" + address + ":" + port + "/?keep";

        Space lobbySpace = new SequentialSpace();
        Space gameSpace = new SequentialSpace();

        TankServer server = new TankServer(lobbySpace, gameSpace);
        new Thread(server::start).start();

        SpaceRepository repository = new SpaceRepository();
        repository.add("lobby", lobbySpace);
        repository.add("game", gameSpace);
        repository.addGate(uri);

        LobbyServer lobbyServer = new LobbyServer(lobbySpace, gameSpace);

        try {
            lobbySpace.getAll(new ActualField("UPDATE"), new FormalField(String.class), new FormalField(Boolean.class));
            gameSpace.getAll(new ActualField("START_GAME"), new FormalField(String.class));
            // Remove any old chat messages
            lobbySpace.getAll(new ActualField("CHAT_MSG"), new FormalField(String.class), new FormalField(String.class));
            System.out.println("Cleaned up old tuples in the lobby space.");
        } catch (Exception e){
            e.printStackTrace();
        }
        new Thread(lobbyServer::broadcastUpdates).start();
        new Thread(lobbyServer::handleLobby).start();



    }

    public boolean isValidIPAddress(String ip) {
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }



    private void switchToLobbyScene(Stage primaryStage, String address, int port, String playerName) {
        try {
            // Create RemoteSpaces for the lobby & game
            RemoteSpace lobbySpace = new RemoteSpace("tcp://" + address + ":" + port + "/lobby?keep");
            RemoteSpace gameSpace  = new RemoteSpace("tcp://" + address + ":" + port + "/game?keep");

            // Create an instance of your new LobbyClient
            LobbyClient lobbyClient = new LobbyClient();
            lobbyClient.setLobbySpace(lobbySpace);
            lobbyClient.setGameSpace(gameSpace);

            // Build the scene
            Scene lobbyScene = lobbyClient.createLobbyScene(playerName);

            // Switch to the new scene
            primaryStage.setScene(lobbyScene);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Scene getScene() {
        return scene;
    }
}