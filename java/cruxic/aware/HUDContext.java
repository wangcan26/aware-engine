package cruxic.aware;

import cruxic.math.*;
import cruxic.jftgl_copy.*;
import static org.lwjgl.opengl.GL11.*;

import java.awt.*;

/**
	Manages an overlay of 2D content such as a HUD (heads up display) or
 	menu.
 */
public class HUDContext
{
	private Mouse2DTracker mousePos;

	public final Rect4f viewport;

	private static final int FONT_NORMAL_POINT_SIZE = 30;
	private float fontScaleFactor;

	private OpenGLContext glCtx;


	public HUDContext(OpenGLContext glCtx)
	{
		this.glCtx = glCtx;
		this.viewport = new Rect4f(-1.0f, 1.0f, 2.0f, 2.0f);
		this.mousePos = new Mouse2DTracker(viewport, glCtx);

		//MUST happen before you call createFonts
		fontScaleFactor = glCtx.height / 600f	* FONT_NORMAL_POINT_SIZE;	//no scaling at 800x600
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

		//switch back to model view
		glMatrixMode(GL_MODELVIEW);
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

	public void draw2DBox_fullscreen(boolean filled)
	{
		draw2DBox(viewport.left(), viewport.top(), viewport.width, viewport.height, filled, false);
	}

	public void drawRect(Rect4f rect, boolean filled)
	{
		draw2DBox(rect.pos.x, rect.pos.y, rect.width, rect.height, filled, false);
	}

	public int relFontSize(float percent)
	{
		return (int)(percent * fontScaleFactor);
	}

	private static float computeFontPointsPerPixel(OpenGLContext glCtx)
	{
		final int TEST_SIZE = 100;
		FTGLPixmapFont ftgl = new FTGLPixmapFont(new Font(Font.DIALOG, Font.PLAIN, TEST_SIZE));
		FTBBox bounds = ftgl.getBBox("M");
		ftgl.clearCache(false);
		return TEST_SIZE / bounds.getHeight();
	}

	public Vec2f getMousePos()
	{
		return mousePos.getPos();
	}

	public Rect4f getTextRect(String text, FTGLPixmapFont font)
	{
		//get the bounding box in pixels
		FTBBox bb = font.getBBox(text);

		final float WIDTH_HACK = 2f;  //correct for inaccurate font width metrics in jFTGL

		return new Rect4f(Vec2f.ORIGIN,
			((bb.getWidth() + WIDTH_HACK) / glCtx.width) * viewport.width,  //convert pixel width to viewport width
			(font.getMaxHeight() / glCtx.height) * viewport.height);  //convert pixel height to viewport height
	}

	public void setTextRectRasterPos(Rect4f rect, FTGLPixmapFont font)
	{
		//note: FTFont.descender() is always negative
		float descender = (-font.descender()  / glCtx.height) * viewport.height;

		glRasterPos2f(rect.pos.x, (rect.pos.y - rect.height) + descender);
	}

	public float[] getTextRectRasterPos(Rect4f rect, FTGLPixmapFont font)
	{
		//note: FTFont.descender() is always negative
		float descender = (-font.descender()  / glCtx.height) * viewport.height;

		return new float[]{rect.pos.x, (rect.pos.y - rect.height) + descender};
	}


	private float pixelWidthToRealWidth(float pixelWidth)
	{
		return (pixelWidth / glCtx.width) * viewport.width;
	}

	private void drawCenteredText(float yPos, String text, FTGLPixmapFont font)
	{
		//get bounding box of the text (in screen pixels)
		FTBBox bb = font.getBBox(text);

		float xPos = pixelWidthToRealWidth(bb.getWidth()) / -2f;

		glRasterPos2f(xPos, yPos);
		font.render(text);

	}

	public void drawMousePointer(Vec2f mousePos, String imageId, float alpha)
	{
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glColor4f(1f, 1f, 1f, alpha);
		glEnable(GL_TEXTURE_2D);
		int[] texId = Engine.instance.texCache.getTexture(imageId);
		glBindTexture(GL_TEXTURE_2D, texId[0]);

		//the image coordinate where the tip of the pointer really is
		float IMG_SZ = 32f;
		float tipX = 10f / IMG_SZ;
		float tipY = 2f / IMG_SZ;

		float aspect = glCtx.getViewportAspectRatio();

		float width = 0.05f;
		float height = width * aspect;
		float left = width * 0.5f - tipX * width;
		float top = tipY * height - height * 0.5f;

		draw2DBox(mousePos.x + left, mousePos.y + top, width, height, true, true);

		glBlendFunc(GL_ONE, GL_ZERO);   //equivalent to disable blending
		glDisable(GL_TEXTURE_2D);

		//glColor3f(1.0f, 0f, 0f);
		//glPointSize(4f);
		//glBegin(GL_POINTS);
		//glVertex2f(0f, 0f);
		//glEnd();
	}



}
