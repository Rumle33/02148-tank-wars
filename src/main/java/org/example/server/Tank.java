package org.example.server;

import org.example.util.MathUtil;
import org.example.util.Vector2f;

public class Tank implements GameObject {

	public float x;
	public float y;
	public float rotation;
	public float velocity;
	public float angularVelocity;

	// requested change
	public float acceleration;
	public float angularAcceleration;
	
	// properties
	public float maxVelocity;
	public float maxAngularVelocity;

	public void update(float delta) {

		this.velocity = MathUtil.clamp(
			this.velocity + this.acceleration * delta, 
			-this.maxVelocity, 
			this.maxVelocity
		);

		this.angularVelocity = MathUtil.clamp(
			this.velocity + this.acceleration * delta, 
			-this.maxAngularVelocity, 
			this.maxAngularVelocity
		);

		this.rotation = this.rotation + this.angularVelocity * delta;
		
		Vector2f translate = MathUtil.rotate2f(new Vector2f(x, y), this.rotation);
		this.x = this.x + translate.x * delta;
		this.y = this.y + translate.y * delta;
	}
}