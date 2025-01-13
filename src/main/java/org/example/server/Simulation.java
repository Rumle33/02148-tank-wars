package org.example.server;

import java.util.ArrayList;
import java.util.List;

import org.example.util.QuadTree;
import org.jspace.Space;
import java.util.HashMap;
import java.util.Map;


public class Simulation {
    private final Space gameSpace;
    private final Map<String, Tank> playerTanks = new HashMap<>();

	public static final int UPS = 50;
	public static final int MILLI_WAIT = 1000 / UPS;

	private List<GameObject> dynamicObjects = new ArrayList<>();
	private List<GameObject> dynamicBuffer = new ArrayList<>();

	public QuadTree<GameObject> qt = new QuadTree<>(0, 0, 600, 600);

    public Simulation(Space gameSpace, List<String> players) {
        this.gameSpace = gameSpace;
        initializeTanks(players);
    }

    private void initializeTanks(List<String> players) {
        int i = 0;
        for (String player : players) {
            Tank tank = new Tank(player,0);
            tank.x = 200 + (i * 400); // Unique starting positions
            tank.y = 300;
            playerTanks.put(player, tank);
			this.dynamicObjects.add(tank);
            i++;
            System.out.println("Initialized tank for " + player + " at x=" + tank.x + ", y=" + tank.y);
        }
    }

    public void run() {
		long lastTime = System.currentTimeMillis();
		
		int updates = 0;

		long lastSecond = lastTime;

		try {
			while (true) {
				long currentTime = System.currentTimeMillis();

				while (currentTime - lastTime < MILLI_WAIT) {
					Thread.sleep(Math.max(0, MILLI_WAIT - currentTime + lastTime - 1));
					currentTime = System.currentTimeMillis();
				}
					
				long deltaTime = currentTime - lastTime;
				float delta = ((float)deltaTime) / 1000.0f;

				dynamicBuffer.clear();
				// ===============================
				// begining of update

				processPlayerActions();

				qt.clear();
				
				for (GameObject object : this.dynamicObjects) {
					if (object.update(this, delta)) {
						// dynamic objects return true to continue living
						dynamicBuffer.add(object);
					}
					qt.insert(object);
				}

				// do physics

				for (GameObject object : this.dynamicObjects) {
					
				}

				// generate quadtree


				broadcastGameState();
				
				// end of update
				// ===============================				
				// swap buffers
				{
					List<GameObject> temp = dynamicBuffer;
					dynamicBuffer = dynamicObjects;
					dynamicObjects = temp;
				}
				
				lastTime = currentTime;
				
				
				updates++;
				if (updates % 50 == 0) {
					System.out.println("Time delta goal diff: " + ((float)(lastTime - lastSecond - 1000) / 50.0f) + " ms");
					lastSecond = lastTime;
				}
			}
		
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

    private void processPlayerActions() {
        try {
            Object[] action;
            while ((action = gameSpace.getp(
                    new org.jspace.ActualField("ACTION"),
                    new org.jspace.FormalField(String.class),
                    new org.jspace.FormalField(String.class),
                    new org.jspace.FormalField(Float.class)
            )) != null) {
                String playerName = (String) action[1];
                String command = (String) action[2];
                float value = (Float) action[3];

                Tank tank = playerTanks.get(playerName);
                if (tank != null) {
                    switch (command) {
                        case "MOVE" -> tank.acceleration = value * tank.maxVelocity;
                        case "ROTATE" -> tank.angularAcceleration = value * tank.maxAngularVelocity;
                        case "SHOOT" -> {
                            // Create a new projectile at the tank's current position and direction
                            Projectile projectile = new Projectile(tank.x, tank.y, tank.rotation);
                            // projectiles.add(projectile);
							this.dynamicObjects.add(projectile);
                            System.out.println("Projectile fired by " + playerName + " at x=" + tank.x + ", y=" + tank.y);
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void updateScoreOfKilledTank(Tank killer, Tank killed) {
        ScoringSystem.rewardKillerTank(killer);
        ScoringSystem.penalizeKilledTank(killed);
    }

    private void updateScoreOfWinnerTank(Tank winner){
        ScoringSystem.rewardWinnerTank(winner);
    }



    private void broadcastGameState() {
        try {
            StringBuilder state = new StringBuilder();
			for (GameObject object: this.dynamicObjects) {
				object.serialize(state);
			}
            gameSpace.put("STATE", state.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }	

}