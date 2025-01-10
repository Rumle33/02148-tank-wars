package org.example.server;

import java.util.ArrayList;
import java.util.List;

public class Simulation {

	public static final int UPS = 50;
	public static final int MILLI_WAIT = 1000 / UPS;

	private List<GameObject> dynamicObjects = new ArrayList<>();
	private List<GameObject> dynamicBuffer = new ArrayList<>();

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
				float delta = ((float)deltaTime * Simulation.UPS) / 1000.0f;

				// do update

				dynamicBuffer.clear();
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

				// end of update
				
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

}