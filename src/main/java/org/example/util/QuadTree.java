package org.example.util;

import java.util.ArrayList;
import java.util.List;

public class QuadTree<T extends AABB>  {
	
	private static final int MAX_DEPTH = 7;

	private int depth;
	private float x, y;
	private float halfWidth, halfHeight; 

	private boolean split = false;
	private QuadTree<T> ul = null, ur = null;
	private QuadTree<T> bl = null, br = null;

	private List<T> leaves = new ArrayList<>(0);

	public QuadTree(float x0, float y0, float x1, float y1) {
		this(
			(x0 + x1) * 0.5f, 
			(y0 + y1) * 0.5f, 
			(x1 - x0) * 0.5f, 
			(y1 - y0) * 0.5f, 
			0
		);
	}

	private QuadTree(float x, float y, float halfWidth, float halfHeight, int depth) {
		this.x = x;
		this.y = y;
		this.halfWidth = halfWidth;
		this.halfHeight = halfHeight;
		this.depth = depth;
	}

	public void insert(T item) {
		
		final float x0 = item.getAABBX();
		final float y0 = item.getAABBY();
		final float x1 = x0 + item.getAABBWidth();
		final float y1 = y0 + item.getAABBHeight();

		leaf: {
			// force leaf is reached max depth
			if (this.depth == MAX_DEPTH) {
				break leaf;
			}

			if (!this.split) {
				// not many items, don't bother splitting
				if (this.leaves.size() <= 4) {
					break leaf;
				}

				// split
				T i0 = this.leaves.get(0);
				T i1 = this.leaves.get(1);
				T i2 = this.leaves.get(2);
				T i3 = this.leaves.get(3);
				this.leaves.clear();

				this.split = true;

				float qw = this.halfWidth * 0.5f;
				float qh = this.halfHeight * 0.5f;

				this.ul = new QuadTree<>(this.x - qw, this.y + qh, qw, qh, this.depth + 1);
				this.ur = new QuadTree<>(this.x + qw, this.y + qh, qw, qh, this.depth + 1);
				this.bl = new QuadTree<>(this.x - qw, this.y - qh, qw, qh, this.depth + 1);
				this.br = new QuadTree<>(this.x + qw, this.y - qh, qw, qh, this.depth + 1);

				this.insert(i0);
				this.insert(i1);
				this.insert(i2);
				this.insert(i3);

				// continue to insert normally
			}

			if (x1 <= this.x) {
				if (y1 <= this.y) {
					// bottom left
					this.bl.insert(item);
				} 
				else if (y0 > this.y) {
					// upper left
					this.ul.insert(item);
				} 
				else { break leaf; }
			} 
			else if (x0 > this.x) {
				if (y1 <= this.y) {
					// bottom right
					this.br.insert(item);
				}
				else if (y0 > this.y) {
					// upper right
					this.ur.insert(item);
				}
				else { break leaf; }
			} 
			else { break leaf; }

			return;
		}

		this.leaves.add(item);
	}
}
