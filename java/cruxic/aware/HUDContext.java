package cruxic.aware;

import cruxic.math.Rect4f;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_LINE;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL11.glEnd;

/**
	Manages an overlay of 2D content such as a HUD (heads up display) or
 	menu.
 */
public class HUDContext
{
	private Mouse2DTracker mousePos;

	private Rect4f viewport;

	public HUDContext(OpenGLContext glctx)
	{
		this.viewport = new Rect4f(-1.0f, 1.0f, 2.0f, 2.0f);
		this.mousePos = new Mouse2DTracker(viewport, glctx);
	}

	public void checkForMouseMovement()
	{
		mousePos.checkForInput();
	}

	public void pushContext()
	{
		glMatrixMode(GL_PROJECTION);
		glPushMatrix();

		glLoadIdentity();
		//left,right  bottom,top  near,far
		glOrtho(viewport.left(), viewport.right(), viewport.bottom(), viewport.top(), 10.0f, -10.0f);

		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
	}

	public void popContext()
	{
		glMatrixMode(GL_PROJECTION);
		glPopMatrix();				
	}

	/**
		@param center if true the image will be centered on left,top coordinates
	*/
	public void draw2DBox(float left, float top, float width, float height, boolean filled,
		boolean center)
	{
		if (center)
		{
			left -= width / 2.0f;
			top += height / 2.0f;
		}

		glPolygonMode(GL_FRONT_AND_BACK, filled ? GL_FILL : GL_LINE);

		glBegin(GL_QUADS);
		glTexCoord2f(1f, 1f);
		glVertex2f(left + width, top);
		glTexCoord2f(0f, 1f);
		glVertex2f(left, top);
		glTexCoord2f(0f, 0f);
		glVertex2f(left, top - height);
		glTexCoord2f(1f, 0f);
		glVertex2f(left + width, top - height);
		glEnd();
	}

}
