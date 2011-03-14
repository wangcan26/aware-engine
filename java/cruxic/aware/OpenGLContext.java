package cruxic.aware;

import org.lwjgl.opengl.*;
import org.lwjgl.LWJGLException;

/**

 */
public class OpenGLContext
{
	public int width;
	public int height;
	public boolean fullscreen;

	public OpenGLContext(int width, int height)
	{
		this.width = width;
		this.height = height;
		fullscreen = false;
	}

	public void init()
		throws LWJGLException
	{
		Display.setTitle("Aware Engine");

		if (fullscreen)
			Display.setFullscreen(true);
		else
			Display.setDisplayMode(new DisplayMode(width, height));

		Display.setVSyncEnabled(true);

		Display.create();

		//draw one frame
		Display.update();

		//actual display-mode may differ from that requested
		DisplayMode mode = Display.getDisplayMode();
		fullscreen = Display.isFullscreen();
		width = mode.getWidth();
		height = mode.getHeight();
	}

	public float getViewportAspectRatio()
	{
		return width / (float)height;
	}

}
