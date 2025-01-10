package org.example.server;

public interface GameObject {
	
	public boolean update(Simulation simulation, float delta);

	public void serialize(StringBuilder buffer);

}
