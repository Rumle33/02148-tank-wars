package org.example.server;

public class Simulation {

	public static final int UPS = 50;

	public void run() {

		long lastTime = System.currentTimeMillis();

		while (true) {
			long currentTime = System.currentTimeMillis();

			while (currentTime - lastTime < 1000 / Server.UPS) {
				Thread.sleep(1);
			}

			long deltaTime = currentTime - lastTime;
			double delta = ((double)deltaTime * UPS) / 1000.0 

			// do update

			

			// end of update

			lastTime = currentTime;
		}


	}	

}