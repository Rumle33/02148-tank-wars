package org.example.Tank;

import javafx.application.Application;
import org.example.Maps.Map;
import org.example.server.Simulation;
import org.example.server.WelcomeScreen;

public class Main {
    public static void main(String[] args) {
		// Thread simulation = new Thread(new Simulation()::run);
		// simulation.setDaemon(true);
		// simulation.start();

        Application.launch(WelcomeScreen.class, args);
    }
}