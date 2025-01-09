package org.example.server;

public class Simulation {

	public static final int UPS = 50;

	public void run() {

		long lastTime = System.currentTimeMillis();
		
		try {
			while (true) {
				long currentTime = System.currentTimeMillis();

				while (currentTime - lastTime < 1000 / Simulation.UPS) {
					Thread.sleep(1);
				}
					
				long deltaTime = currentTime - lastTime;
				double delta = ((double)deltaTime * Simulation.UPS) / 1000.0;

				// do update

				

				// end of update
				
				lastTime = currentTime;
			}
		
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}	

}