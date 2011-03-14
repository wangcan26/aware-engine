package cruxic.math;

import org.lwjgl.opengl.GL11;

/**2D vector or point*/
public class Vec2f
{
	public final float x;
	public final float y;

	/**0,0*/
	public static final Vec2f ORIGIN = new Vec2f(0.0f, 0.0f);

	public Vec2f(float x, float y)
	{
		this.x = x;
		this.y = y;
	}

	public void debugPrint()
	{
		System.out.printf("[%g, %g]\n", x, y);
	}

	public boolean equals(Vec2f v)
	{
		return x == v.x && y == v.y;
	}

	public void glVertex()
	{
		GL11.glVertex2f(x, y);
	}
}
