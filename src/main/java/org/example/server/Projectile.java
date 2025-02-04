package org.example.server;

import org.example.Maps.Wall;
import org.example.util.Vector2f;

public class Projectile implements GameObject {
	
	public static final float PROJECTILE_RADIUS = 5.0f;
	
	public float x;
	public float y;
	public float rotation;
	public float velocity = 150.0f; // Adjust for visible speed
	public float ttl = 10.0f; // Time to live in seconds
	
	private PhysicsComponent physics;
    private final Tank shooter; // Reference to the shooter

	static {
		// This causes issue 

		// int points = 256;
		// Projectile.MESH = new float[points * 2];

		// for (int i = 0; i < points; i++) {
		// 	float angle = (float)i / (float)points * (float)Math.PI * 2.0f;
		// 	Projectile.MESH[i * 2 + 0] = PROJECTILE_RADIUS * (float) Math.cos(angle);
		// 	Projectile.MESH[i * 2 + 1] = PROJECTILE_RADIUS * (float) Math.sin(angle);
		// }
	}

	public static float MESH[] = {
		-PROJECTILE_RADIUS, -PROJECTILE_RADIUS,
		-PROJECTILE_RADIUS, PROJECTILE_RADIUS,
		PROJECTILE_RADIUS, PROJECTILE_RADIUS,
		PROJECTILE_RADIUS, -PROJECTILE_RADIUS
	};

	public Projectile(float x, float y, float rotation, Tank shooter) {
		this.x = x;
		this.y = y;
		this.rotation = rotation;

		this.physics = new PhysicsComponent(x, y, rotation, MESH);
		this.shooter = shooter;
	}

    @Override
    public boolean update(Simulation simulation, float delta) {
        ttl -= delta;
        if (ttl < 0) {
            return false; 
        }
		this.x = physics.x;
		this.y = physics.y;
		this.rotation = physics.r;

		Vector2f translate = new Vector2f(velocity, 0.0f).rotate(rotation);

		float dx = translate.x * delta;
		float dy = translate.y * delta; 

		this.physics.dx = dx;
		this.physics.dy = dy;

        return true; // Projectile remains active
    }

	@Override
	public void serialize(StringBuilder buffer) {
		buffer
			.append("Projectile").append(" ")
			.append(this.x).append(" ")
			.append(this.y).append(" ")
			.append(this.rotation).append(" ")
			.append(this.getAABBX()).append(" ")
			.append(this.getAABBY()).append(" ")
			.append(this.getAABBWidth()).append(" ")
			.append(this.getAABBHeight()).append(" ")
		;

		float[] mesh = this.physics.getTransformMesh();
		for (int i = 0; i < mesh.length; i++) {
			buffer.append(mesh[i]).append(" ");
		}

		buffer.append("\n");
	}

	@Override
	public float getAABBX() {
		return this.x + this.physics.dx - Projectile.PROJECTILE_RADIUS;
	}

	@Override
	public float getAABBY() {
		return this.y + this.physics.dy - Projectile.PROJECTILE_RADIUS;
	}

	@Override
	public float getAABBWidth() {
		return Projectile.PROJECTILE_RADIUS * 2;
	}

	@Override
	public float getAABBHeight() {
		return Projectile.PROJECTILE_RADIUS * 2;
	}

	public void collide(Simulation simulation, Object object) {

		if (object instanceof Tank) {
			simulation.debugPrint("Projectile hit tank!");
			this.ttl = -1;

			simulation.showMessage(shooter.name + " wins!");
            simulation.ReSeTSiM();
		}
		else if (object instanceof Projectile) {
			simulation.debugPrint("Projectile hit projectile!");
			this.ttl = -1;
		}
		else if (object instanceof Wall) {
			simulation.debugPrint("Projectil hit wall");

			Wall wall = (Wall)object;
			boolean isHorizontal = Math.abs(wall.getEndY() - wall.getStartY()) < Math.abs(wall.getEndX() - wall.getStartX());

			float wx0 = (float) Math.min(wall.getStartX(), wall.getEndX());
			float wx1 = (float) Math.max(wall.getStartX(), wall.getEndX());

			float wy0 = (float) Math.min(wall.getStartY(), wall.getEndY());
			float wy1 = (float) Math.max(wall.getStartY(), wall.getEndY());

			if (isHorizontal) {
				if (this.getAABBX() > wx1) {
					isHorizontal = false;
					this.physics.dx = 1;
				}
				else if (this.getAABBX() + this.getAABBWidth() < wx0) {
					isHorizontal = false;
					this.physics.dx = -1;
				}
			}
			else {
				if (this.getAABBY() > wy1) {
					isHorizontal = true;
					this.physics.dy = 1;
				}
				else if (this.getAABBY() + this.getAABBHeight() < wy0) {
					isHorizontal = true;
					this.physics.dy = -1;
				}
			}

			if (isHorizontal) {
				this.physics.dr = - 2 * rotation; // Reverse Y direction
			} else {
				this.physics.dr = (float)Math.PI - 2 * rotation; // Reverse X direction
			}
			this.velocity = (float)Math.min(this.velocity + 25, 750);
		}
		else {
			simulation.debugPrint("Projectile hit unknown!");
		}

	}

	@Override
	public PhysicsComponent getPhysicsComponent() {
		return this.physics;
	}
}
