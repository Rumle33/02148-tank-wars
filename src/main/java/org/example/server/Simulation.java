package org.example.server;

import org.example.Maps.Wall;
import java.util.ArrayList;
import java.util.List;

import org.example.util.Vector2f;
import org.jspace.Space;
import java.util.HashMap;
import java.util.Map;

public class Simulation {
    private final Space gameSpace;
    private final Map<String, Tank> playerTanks = new HashMap<>();
	
	public static final int UPS = 100;
	public static final int MILLI_WAIT = 1000 / UPS;
	
    private final List<Wall> mapWalls;
	private List<GameObject> dynamicObjects = new ArrayList<>();
	private List<GameObject> dynamicBuffer = new ArrayList<>();

	public static final boolean DEBUG = false;

    public Simulation(Space gameSpace, List<String> players) {
        this.gameSpace = gameSpace;
        this.mapWalls = new org.example.Maps.Map().getWalls();
        initializeTanks(players);
    }

    private void initializeTanks(List<String> players) {
        int i = 0;
        for (String player : players) {
            Tank tank = new Tank(player, 0);
            // Unique starting positions
			tank.setX(175 + (i * 400)).setY(300);
            playerTanks.put(player, tank);
            this.dynamicObjects.add(tank);
            i++;
            System.out.println("Initialized tank for " + player + " at x=" + tank.getX() + ", y=" + tank.getY());
        }
    }

    public List<Wall> getMapWalls() {
    	return mapWalls;
	}

    public List<Tank> getTanks() {
        return new ArrayList<>(playerTanks.values());
    }

	public void showMessage(String message) {
		System.out.println("[MESSAGE]> " + message); // Placeholder for actual UI messaging
	}

	public void debugPrint(String message) {
		if (Simulation.DEBUG) {
			System.out.println("[DEBUG]> " + message);
		}
	}

    public void handleMapRequests() {
        try {
            while (true) {
                Object[] request = gameSpace.get(new org.jspace.ActualField("REQUEST_MAP"), new org.jspace.FormalField(String.class));
                String requestingPlayer = (String) request[1];
                System.out.println("Sending map to: " + requestingPlayer);
                sendMap();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sendMap() {
        try {
            StringBuilder mapState = new StringBuilder();
            for (Wall wall : mapWalls) {
                mapState.append(wall.getStartX()).append(" ")
                        .append(wall.getStartY()).append(" ")
                        .append(wall.getEndX()).append(" ")
                        .append(wall.getEndY()).append("\n");
            }
            gameSpace.put("MAP", mapState.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}

    public void run() {
		new Thread(this::handleMapRequests).start();

		long lastTime = System.currentTimeMillis();
		
		int updates = 0;

		long lastSecond = lastTime;

		try {
			while (true) {
				long currentTime = System.currentTimeMillis();

				if (currentTime - lastTime > MILLI_WAIT) {
					System.out.println("[WARNING] server unable to keep up, " + (currentTime - lastTime) + " ms behind");
				}

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
				
				for (GameObject object : this.dynamicObjects) {
					if (object.update(this, delta)) {
						// dynamic objects return true to continue living
						dynamicBuffer.add(object);
					}
				}

				// swap buffers
				{
					List<GameObject> temp = dynamicBuffer;
					dynamicBuffer = dynamicObjects;
					dynamicObjects = temp;
				}

				// do collisions
				
				// brute force approximate (AABB based) collision check
				for (int i = 0; i < this.dynamicBuffer.size(); i++) {
					GameObject o0 = this.dynamicBuffer.get(i);
					for (int j = i + 1; j < this.dynamicBuffer.size(); j++) {
						GameObject o1 = this.dynamicBuffer.get(j);
						if (AABBCollision.test(o0, o1)) {
							if (PhysicsCollision.test(o0.getPhysicsComponent(), o1.getPhysicsComponent())) {
								o0.collide(this, o1);
								o1.collide(this, o0);
							}
						}
					}
				}

				for (GameObject o0 : this.dynamicBuffer) {
					for (Wall wall : this.mapWalls) {
						if (AABBCollision.test(o0, wall)) {
							if (PhysicsCollision.test(
								o0.getPhysicsComponent(), 
								(float) wall.getStartX(), 
								(float) wall.getStartY(), 
								(float) wall.getEndX(), 
								(float) wall.getEndY())
							) {
								o0.collide(this, wall);
							}
						}
					}
				}

				// do phsics update
				for (GameObject object: this.dynamicBuffer) {
					object.getPhysicsComponent().update();
				}

				broadcastGameState();
				
				// end of update
				// ===============================				
				
				lastTime = currentTime;
				
				
				updates++;
				if (updates % UPS == 0) {
					// System.out.println("Time delta goal diff: " + ((float)(lastTime - lastSecond - 1000) / (float)UPS) + " ms");
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
							Vector2f v = new Vector2f(Tank.TANK_HEIGHT * 0.5f + Projectile.PROJECTILE_RADIUS, 0)
								.rotate(tank.getRotation())
								.add(tank.getX(), tank.getY());
                            Projectile projectile = new Projectile(v.x, v.y, tank.getRotation(), tank);
							this.dynamicObjects.add(projectile);
                            System.out.println("Projectile fired by " + playerName + " at x=" + tank.getX() + ", y=" + tank.getY());
                        }
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void broadcastGameState() {
        try {
            StringBuilder state = new StringBuilder();
            for (GameObject object : dynamicObjects) {
                object.serialize(state);
            }
            gameSpace.put("STATE", state.toString());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
