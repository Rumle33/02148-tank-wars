package org.example.server;

import org.example.util.AABB;

public interface GameObject extends AABB {
	
	public boolean update(Simulation simulation, float delta);

	public void serialize(StringBuilder buffer);

	public void collide(GameObject object);

}
