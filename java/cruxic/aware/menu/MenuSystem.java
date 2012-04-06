/*
*	AwareEngine
*	Copyright (C) 2011  Adam Bennett <cruxicATgmailDOTcom>
*
*	This program is free software; you can redistribute it and/or
*	modify it under the terms of the GNU General Public License
*	as published by the Free Software Foundation; either version 2
*	of the License, or (at your option) any later version.
*
*	This program is distributed in the hope that it will be useful,
*	but WITHOUT ANY WARRANTY; without even the implied warranty of
*	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*	GNU General Public License for more details.
*
*	You should have received a copy of the GNU General Public License
*	along with this program; if not, write to the Free Software
*	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package cruxic.aware.menu;

import cruxic.jftgl_copy.*;
import cruxic.aware.tex_cache.TextureCache;
import cruxic.aware.ipo.*;
import cruxic.aware.*;
import cruxic.aware.misc.WorkingSet;
import cruxic.aware.overlays.Liquid_StaticNoiseOverlaySpec;
//import cruxic.aware.effects.WaterRipples_StaticNoise;
import cruxic.math.*;
import static cruxic.aware.MenuHandler.MenuAction.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
	The menu system presented to the user (save/load game, settings, etc)
 */
public class MenuSystem
{
	private static class MenuRect
	{
		public Rect4f rect;
		public Menu menu;
		public MenuRect(Rect4f rect, Menu menu)
		{
			this.rect = rect;
			this.menu = menu;
		}
	}


	private FTGLPixmapFont fontMenuItem;
	private FTGLPixmapFont fontMenuItemHover;
	private FTGLPixmapFont fontMenuTitle;
	private FTGLPixmapFont fontMenuTitle_shadow;

	private float LEFT = -1.0f;
	private float RIGHT = 1.0f;
	private float TOP = 1.0f;
	private float BOT = -1.0f;
	private float WIDTH = RIGHT - LEFT;
	private float HEIGHT = TOP - BOT;

	//private static final float BG_START_HUE = 2.35f;

	private boolean visible;
	private TextureCache texCache;

	private Mouse2DTracker mousePos;

	private IPOCurve bgHue;

	private SubMenu root;
	private ArrayList<MenuRect> menuLayout;
	private LinkedList<Menu> menuStack;

	private OpenGLContext glCtx;

	private PanoViewpoint menuViewpoint;

	private static final int FONT_NORMAL_POINT_SIZE = 30;
	private float fontScaleFactor;

	private MenuActionListener menuListener;

	Liquid_StaticNoiseOverlaySpec rippleSpec;


	public MenuSystem(Engine engine, OpenGLContext glCtx, TextureCache texCache, MenuActionListener menuListener)
	{
		this.menuListener = menuListener;
		this.texCache = texCache;
		this.glCtx = glCtx;
		visible = false;

		//preload handler classes to prevent click lag
		menuListener.menuActivated(new LeafMenu(MNull, null));

		rippleSpec = new Liquid_StaticNoiseOverlaySpec("ripples",
		  "res/data/ripple_noise1.png", "res/menu/background-mask.png");
		rippleSpec.noiseIntensity = 0.85f;

		//dummy Viewpoint necessary for overlayprocessor call
		menuViewpoint = new PanoViewpoint("menu");
		menuViewpoint.imageIds.add("res/menu/background.png");
		menuViewpoint.overlays.add(rippleSpec);

		//MUST happen before you call createFonts
		fontScaleFactor = glCtx.height / 600f	* FONT_NORMAL_POINT_SIZE;	//no scaling at 800x600

		createFonts();

		mousePos = new Mouse2DTracker(new Rect4f(LEFT, TOP, WIDTH, HEIGHT), glCtx);

		//bgHue = new RepeatingIPO(new LinearIPO(0.0f, 6.0f, 60.0f, engine.newTimeSource()));

		//Build the menu
		root = new SubMenu(null, "Aware Engine");
		root.addMenu(new LeafMenu(MPlay, "Play"));
		SubMenu settings = root.addMenu(new SubMenu(null, "Settings"));
		{
			settings.addMenu(new ToggleMenu(MToggle_show_fps, "Show FPS", "renderer.show_fps"));
			settings.addMenu(new ToggleMenu(MToggle_show_geom, "Show Geometry", "renderer.show_geom"));
			settings.addMenu(new ToggleMenu(MToggle_show_hotspots, "Show Hotspots", "renderer.show_hotspots"));

			//settings.addMenu(new ToggleMenu(MToggleMusic, "Music"));
			//settings.addMenu(new ToggleMenu(MToggleFullscreen, "Full Screen"));
			//SubMenu numbers = settings.addMenu(new SubMenu(null, "Numbers"));
			//for (int i = 0; i < 10; i++)
			//	numbers.addMenu(new LeafMenu(null, "Number "));
		}
		root.addMenu(new LeafMenu(MQuit, "Quit"));
		//for (int i = 0; i < 10; i++)
		//	root.addMenu(new LeafMenu("More " + i, null));

		menuLayout = new ArrayList<MenuRect>(32);
		menuStack = new LinkedList<Menu>();
		pushMenu(root);
	}

	private void createFonts()
	{
		final String face = "sans-serif";

		Font fnt = new Font(face, Font.PLAIN, relFontSize(2f));
		fontMenuTitle = new FTGLPixmapFont(fnt);
		fontMenuTitle.rgbaColor = new float[] {0.176471f, 0.364706f, 0.847059f, 1f};

		fnt = new Font(face, Font.PLAIN, relFontSize(2f));
		fontMenuTitle_shadow = new FTGLPixmapFont(fnt);
		fontMenuTitle_shadow.rgbaColor = new float[] {0, 0, 0, 1f};


		fnt = new Font(face, Font.PLAIN, relFontSize(1f));
		fontMenuItem = new FTGLPixmapFont(fnt);
		fontMenuItem.rgbaColor = new float[] {0.0352941f, 0.0745098f, 0.176471f, 1f};

		fontMenuItemHover = new FTGLPixmapFont(fnt);
		fontMenuItemHover.rgbaColor = new float[] {0.207843f, 0.431373f, 1f, 1f};

	}

	private int relFontSize(float percent)
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

	public void drawMenu()
	{
		mousePos.checkForInput();

		//glColor3f(0f, 0f, 0f);
		//setup 2D projection
		glMatrixMode(GL_PROJECTION);
		glPushMatrix();
		{
			glLoadIdentity();
			//left,right  bottom,top  near,far
			glOrtho(LEFT, RIGHT, BOT, TOP, 10.0f, -10.0f);

	//		float consoleFontPixelScale = 1.0f / (float)engine.windowHeight;
	//		float fontHeight = consoleFont.LineHeight() * consoleFontPixelScale;
	//		float avgCharWidth = fontHeight / 2.0f;

			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();

			glEnable(GL_TEXTURE_2D);

			drawBackground();

			drawMenuBorder(menuLayout);

			drawMenuLayout(menuLayout);

			drawMousePointer(mousePos.x, mousePos.y, "hand");

		}
		glMatrixMode(GL_PROJECTION);
		glPopMatrix();
	}


	private void drawMousePointer(float x, float y, String imageId)
	{
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glColor4f(1f, 1f, 1f, 1f);
		glEnable(GL_TEXTURE_2D);
		int[] texId = texCache.getTexture(imageId);
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

		draw2DBox(x + left, y + top, width, height, true, true);

		glBlendFunc(GL_ONE, GL_ZERO);   //equivalent to disable blending
		glDisable(GL_TEXTURE_2D);

		//glColor3f(1.0f, 0f, 0f);
		//glPointSize(4f);
		//glBegin(GL_POINTS);
		//glVertex2f(0f, 0f);
		//glEnd();
	}

	private void pushMenu(Menu menu)
	{
		menuStack.addLast(menu);
		layoutMenu(menu);
	}

	/**pop one submenu.
	 @return false if already on the root menu*/
	public boolean popMenu()
	{
		//never pop the root!
		if (menuStack.size() > 1)
		{
			menuStack.removeLast();

			layoutMenu(menuStack.getLast());
			return true;
		}
		else
			return false;
	}

	private void layoutMenu(Menu menu)
	{
		menuLayout.clear();

		Rect4f headingBox = getTextRect(menu.getText(), fontMenuTitle);
		headingBox = headingBox.centeredOn(Vec2f.ORIGIN).newY(HEIGHT * 0.25f);
		menuLayout.add(new MenuRect(headingBox, menu));

		float yPos = headingBox.bottom() - headingBox.height * 0.25f;

		//Sub menu entries
		for (Menu sub: menu.getSubmenu())
		{
			Rect4f rect = getTextRect(sub.getText(), fontMenuItem);
			rect = rect.centeredOn(Vec2f.ORIGIN).newY(yPos);
			menuLayout.add(new MenuRect(rect, sub));

			yPos = rect.bottom() - (rect.height * 0.25f);
		}
	}

	private MenuRect getRectUnderMouse(List<MenuRect> layout)
	{
		Vec2f pos = mousePos.getPos();
		for (MenuRect mr: layout)
		{
			if (mr.rect.contains(pos))
				return mr;
		}

		return null;
	}

	private Rect4f getMenuBoundingRect(List<MenuRect> layout)
	{
		float left = Float.MAX_VALUE;
		float right = Float.MIN_VALUE;
		float top = Float.MIN_VALUE;
		float bottom = Float.MAX_VALUE;

		//Find maximum bounding box
		for (MenuRect mr: layout)
		{
			if (mr.rect.left() < left)
				left = mr.rect.left();

			if (mr.rect.right() > right)
				right = mr.rect.right();

			if (mr.rect.bottom() < bottom)
				bottom = mr.rect.bottom();

			if (mr.rect.top() > top)
				top = mr.rect.top();
		}

		return new Rect4f(left, top, right - left, top - bottom);
	}

	private void drawMenuBorder(List<MenuRect> layout)
	{
		//border width and height
		final float BW = 0.035f;
		final float BH = BW * glCtx.getViewportAspectRatio();

		final float BW2 = BW * 2.0f;
		final float BH2 = BH * 2.0f;

		//Get bounding rect of entire menu text
		Rect4f bounds = getMenuBoundingRect(layout);

		//enlarge sligtly
		bounds = bounds.grow(0.1f);

		//enable standard alpha blending
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		//Draw background rect
		glDisable(GL_TEXTURE_2D);
		glColor4f(1f, 1f, 1f, 0.4f);
		drawRect(bounds.grow(-(BW / 2.0f)), true);

		glEnable(GL_TEXTURE_2D);
		glColor4f(1f, 1f, 1f, 1.0f);

		//Top & Bottom
		glBindTexture(GL_TEXTURE_2D, texCache.getTexture("res/menu/border-horz.png")[0]);
		draw2DBox(bounds.pos.x + BW, bounds.pos.y, bounds.width - BW2, BH, true, false);
		draw2DBox(bounds.pos.x + BW, bounds.bottom() + BH, bounds.width - BW2, BH, true, false);

		//Left & Right
		glBindTexture(GL_TEXTURE_2D, texCache.getTexture("res/menu/border-vert.png")[0]);
		draw2DBox(bounds.pos.x, bounds.pos.y - BH, BW, bounds.height - BH2, true, false);
		draw2DBox(bounds.right() - BW, bounds.pos.y - BH, BW, bounds.height - BH2, true, false);

		//4 corners
		glBindTexture(GL_TEXTURE_2D, texCache.getTexture("res/menu/corner.png")[0]);
		//top-left
		draw2DBox(bounds.pos.x, bounds.pos.y, BW, BH, true, false);
		//top-right
		draw2DBox(bounds.pos.x + bounds.width - BW, bounds.pos.y, BW, BH, true, false);
		//bot-left
		draw2DBox(bounds.pos.x, bounds.bottom() + BH, BW, BH, true, false);
		//bot-right
		draw2DBox(bounds.pos.x + bounds.width - BW, bounds.bottom() + BH, BW, BH, true, false);

		glBlendFunc(GL_ONE, GL_ZERO);   //equivalent to disable blending

	}

	private void drawMenuLayout(List<MenuRect> layout)
	{
		glDisable(GL_TEXTURE_2D);

		//get the item under the mouse
		MenuRect hover = getRectUnderMouse(layout);

		boolean isHeading = true;
		for (MenuRect mr: layout)
		{
			//glColor3f(1f, 1f, 1f);
			//drawRect(mr.rect, false);

			FTGLPixmapFont font;
			if (isHeading)
			{
				font = fontMenuTitle;
				isHeading = false;

				//Draw head font shadow first
				float[] xy = getTextRectRasterPos(mr.rect, font);
				float offset = (-font.descender() / glCtx.height) * 0.35f;  //offset the shadow relative to the size of the font
				glRasterPos2f(xy[0] + offset, xy[1] - offset);
				fontMenuTitle_shadow.render(mr.menu.getText());
			}
			else if (hover == mr)
				font = fontMenuItemHover;
			else
				font = fontMenuItem;

			setTextRectRasterPos(mr.rect, font);
			font.render(mr.menu.getText());
		}
	}

	private void setTextRectRasterPos(Rect4f rect, FTGLPixmapFont font)
	{
		//note: FTFont.descender() is always negative
		float descender = (-font.descender()  / glCtx.height) * HEIGHT;

		glRasterPos2f(rect.pos.x, (rect.pos.y - rect.height) + descender);
	}

	private float[] getTextRectRasterPos(Rect4f rect, FTGLPixmapFont font)
	{
		//note: FTFont.descender() is always negative
		float descender = (-font.descender()  / glCtx.height) * HEIGHT;

		return new float[]{rect.pos.x, (rect.pos.y - rect.height) + descender};
	}

	private Rect4f getTextRect(String text, FTGLPixmapFont font)
	{
		//get the bounding box in pixels
		FTBBox bb = font.getBBox(text);

		final float WIDTH_HACK = 2f;  //correct for inaccurate font width metrics in jFTGL

		return new Rect4f(Vec2f.ORIGIN,
			((bb.getWidth() + WIDTH_HACK) / glCtx.width) * WIDTH,  //convert pixel width to viewport width
			(font.getMaxHeight() / glCtx.height) * HEIGHT);  //convert pixel height to viewport height
	}

	private float pixelWidthToRealWidth(float pixelWidth)
	{
		return (pixelWidth / glCtx.width) * WIDTH;
	}

	private void drawCenteredText(float yPos, String text, FTGLPixmapFont font)
	{
		//get bounding box of the text (in screen pixels)
		FTBBox bb = font.getBBox(text);

		float xPos = pixelWidthToRealWidth(bb.getWidth()) / -2f;

		glRasterPos2f(xPos, yPos);
		font.render(text);

	}

	private void drawBackground()
	{
		//hueToRGB(bgHue.currentValue());
		glColor3f(1.0f, 1.0f, 1.0f);
		glEnable(GL_TEXTURE_2D);

		glBindTexture(GL_TEXTURE_2D, texCache.getTexture(menuViewpoint.getImage())[0]);

		//Engine.instance.overlayProcessor.upload(menuViewpoint);

		draw2DBox(LEFT, TOP, WIDTH, HEIGHT, true, false);
	}


	/**Select between all possible colors hues.
	 @param hue a number on the range [0.0, 6.0)
	 */
	private static void hueToRGB(float hue)
	{
		//RGB from [0.0,1.0]
		float r, g, b;

		//sanitize input
		if (hue < 0.0f)
			hue = 0.0f;
		else if (hue >= 6.0f)
			hue = 6.0f - 0.00001f;

		int phase = (int)hue;  //faster than Math.floor
		float amount = hue - phase;

		switch (phase)
		{
			//0-1: red-yellow
			case 0:
				r = 1.0f;
				g = amount;
				b = 0.0f;
				break;
			//1-2: yellow-green
			case 1:
				r = 1.0f - amount;
				g = 1.0f;
				b = 0.0f;
				break;
			//2-3: green-cyan
			case 2:
				r = 0.0f;
				g = 1.0f;
				b = amount;
				break;
			//3-4: cyan-blue
			case 3:
				r = 0.0f;
				g = 1.0f - amount;
				b = 1.0f;
				break;
			//4-5: blue-purple
			case 4:
				r = amount;
				g = 0.0f;
				b = 1.0f;
				break;
			//5-6: purple-red
			default:
				r = 1.0f;
				g = 0.0f;
				b = 1.0f - amount;
				break;
		}

		glColor3f(r, g, b);
	}

	//Convert HSV color space to RGB color space
	// H is given on [0, 6] or UNDEFINED. S and V are given on [0, 1].
	// RGB are each returned on [0, 1].
	private static void hsvTo_glColor3f(float h, float s, float v)
	{
			int i = (int)Math.floor((double)h);
			float f = h - i;
			if ((i&1) != 0)
				f = 1.0f - f; // if i is even
			float m = v * (1.0f - s);
			float n = v * (1.0f - s * f);
			switch (i)
			{
				case 6:
				case 0: glColor3f(v, n, m);break;
				case 1: glColor3f(n, v, m);break;
				case 2: glColor3f(m, v, n);break;
				case 3: glColor3f(m, n, v);break;
				case 4: glColor3f(n, m, v);break;
				case 5: glColor3f(v, m, n);break;
				default:  glColor3f(0.0f, 0.0f, 0.0f);break;  //should not happen
			}
	}

	private void drawCenteredQuad(float percentWidth, float percentHeight)
	{

	}

	private void drawRect(Rect4f rect, boolean filled)
	{
		draw2DBox(rect.pos.x, rect.pos.y, rect.width, rect.height, filled, false);
	}

	/**
		@param center if true the image will be centered on left,top coordinates
	*/
	private void draw2DBox(float left, float top, float width, float height, boolean filled,
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

	public void onMouseClick()
	{
		MenuRect rect = getRectUnderMouse(menuLayout);
		if (rect != null)
		{
			menuListener.menuActivated(rect.menu);

			//Show submenu (if any)
			if (rect.menu.hasSubmenu())
				pushMenu(rect.menu);
		}
	}

	public void setVisible(boolean visible)
	{
		boolean wasVisible = this.visible;
		this.visible = visible;

		Engine engine = Engine.instance;

		//Show?
		if (visible && !wasVisible)
		{
			//Center the mouse
			//mousePos.x = 0f;
			//mousePos.y = 0f;

			//Push working set with all textures we will need
			WorkingSet<String> ws = new WorkingSet<String>();
			ws.add(rippleSpec.noiseImage, WorkingSet.PRI_HIGH + 1);
			ws.add(rippleSpec.maskImage, WorkingSet.PRI_HIGH);
			ws.add("res/menu/background.png", WorkingSet.PRI_HIGH);
			ws.add("res/menu/border-horz.png", WorkingSet.PRI_NORM);
			ws.add("res/menu/border-vert.png", WorkingSet.PRI_NORM);
			ws.add("res/menu/corner.png", WorkingSet.PRI_NORM);

			engine.texCache.pushWorkingSet(ws);

			//engine.overlayProcessor.load(menuViewpoint);
		}
		//Hide?
		else if (!visible && wasVisible)
		{
			//engine.overlayProcessor.unload(menuViewpoint);
			engine.texCache.popWorkingSet();
		}
	}

	public boolean isVisible()
	{
		return visible;
	}
}
