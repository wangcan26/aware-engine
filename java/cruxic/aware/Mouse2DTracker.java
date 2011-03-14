package cruxic.aware;

import org.lwjgl.input.Mouse;
import cruxic.math.*;

/**
	Convert LWJGL Mouse position data into 2D coordinates.
 */
public class Mouse2DTracker
{
	public float x;
	public float y;
	private Rect4f viewport;
	private OpenGLContext glCtx;

	public Mouse2DTracker(Rect4f viewport, OpenGLContext glCtx)
	{
		this.viewport = viewport;
		this.glCtx = glCtx;
	}

	public void checkForInput()
	{
		final float sensX = viewport.width / glCtx.width;
		final float sensY = viewport.height / glCtx.height;

		if (Mouse.isGrabbed())
		{
			//must use deltas if mouse has been grabbed
			int dx = Mouse.getDX();
			int dy = Mouse.getDY();
			x += sensX * dx;
			y += sensY * dy;
		}
		else
		{
			//use absolute coordintes
			x = sensX * (Mouse.getX() - glCtx.width / 2f);
			y = sensY * (Mouse.getY() - glCtx.height / 2f);
		}


		//Keep pointer inside viewport
		if (x < viewport.pos.x)
			x = viewport.pos.x;
		else if (x > viewport.right())
			x = viewport.right();

		if (y > viewport.pos.y)
			y = viewport.pos.y;
		else if (y < viewport.bottom())
			y = viewport.bottom();
	}

	public Vec2f getPos()
	{
		return new Vec2f(x, y);
	}
}
