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
//import cruxic.aware.overlays.Liquid_StaticNoiseOverlaySpec;
import cruxic.math.*;
import static cruxic.aware.MenuHandler.MenuAction.*;

import java.awt.*;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;

/**
	The menu system presented to the user (save/load game, settings, etc)
 */
public class MenuSystem
{
	private FTGLPixmapFont fontMenuItem;
	private FTGLPixmapFont fontMenuItemHover;
	private FTGLPixmapFont fontMenuTitle;
	private FTGLPixmapFont fontMenuTitle_shadow;

	//private static final float BG_START_HUE = 2.35f;

	private boolean visible;
	private TextureCache texCache;

	private IPOCurve bgHue;

	private Engine engine;

	private SubMenu root;
	private Layout2D<Menu> layout;
	private LinkedList<Menu> menuStack;

	private OpenGLContext glCtx;

	private EquirectViewpoint menuViewpoint;


	private MenuActionListener menuListener;

	//Liquid_StaticNoiseOverlaySpec rippleSpec;


	public MenuSystem(Engine engine, OpenGLContext glCtx, TextureCache texCache, MenuActionListener menuListener)
	{
		this.menuListener = menuListener;
		this.texCache = texCache;
		this.glCtx = glCtx;
		visible = false;

		this.engine = engine;

		//preload handler classes to prevent click lag
		menuListener.menuActivated(new LeafMenu(MNull, null));

		//rippleSpec = new Liquid_StaticNoiseOverlaySpec("ripples",
		//  "res/data/ripple_noise1.png", "res/menu/background-mask.png");
		//rippleSpec.noiseIntensity = 0.85f;

		//dummy Viewpoint necessary for overlayprocessor call
		menuViewpoint = new EquirectViewpoint("menu");
		menuViewpoint.imageIds.add("res/menu/background.png");
		//menuViewpoint.overlays.add(rippleSpec);


		createFonts();

		//bgHue = new RepeatingIPO(new LinearIPO(0.0f, 6.0f, 60.0f, engine.newTimeSource()));

		//Build the menu
		root = new SubMenu(null, "Aware Engine");
		root.addMenu(new LeafMenu(MPlay, "Play"));
		SubMenu settings = root.addMenu(new SubMenu(null, "Settings"));
		{
			settings.addMenu(new ToggleMenu(MToggle_show_fps, "Show FPS", "renderer.show_fps"));
			//settings.addMenu(new ToggleMenu(MToggle_develop_mode, "Development Mode", "devel.enable"));

			//settings.addMenu(new ToggleMenu(MToggleMusic, "Music"));
			//settings.addMenu(new ToggleMenu(MToggleFullscreen, "Full Screen"));
			//SubMenu numbers = settings.addMenu(new SubMenu(null, "Numbers"));
			//for (int i = 0; i < 10; i++)
			//	numbers.addMenu(new LeafMenu(null, "Number "));
		}
		SubMenu develop = root.addMenu(new SubMenu(Mdevelop, "Develop [`]"));
		{
			develop.addMenu(new LeafMenu(MJump2Viewpoint, "Jump to viewpoint [J]"));

			SubMenu dev_hotspots = develop.addMenu(new SubMenu(null, "Hotspots"));
			{
				dev_hotspots.addMenu(new ToggleMenu(MToggle_hotspot_show_all, "Show Hotspots", "renderer.show_hotspots"));
				dev_hotspots.addMenu(new LeafMenu(Mhotspot_add, "Add [A]"));
				dev_hotspots.addMenu(new LeafMenu(Mhotspot_delete, "Delete [DEL]"));
				dev_hotspots.addMenu(new LeafMenu(Mhotspot_link, "Change Link"));
			}

			develop.addMenu(new ToggleMenu(MToggle_show_geom, "Show Geometry", "renderer.show_geom"));
			develop.addMenu(new ToggleMenu(MToggle_cycle_viewpoints, "Cycle Viewpoints", "devel.cycle_viewpoints"));


			//settings.addMenu(new ToggleMenu(MToggleMusic, "Music"));
			//settings.addMenu(new ToggleMenu(MToggleFullscreen, "Full Screen"));
			//SubMenu numbers = settings.addMenu(new SubMenu(null, "Numbers"));
			//for (int i = 0; i < 10; i++)
			//	numbers.addMenu(new LeafMenu(null, "Number "));
		}
		root.addMenu(new LeafMenu(MQuit, "Quit"));
		//for (int i = 0; i < 10; i++)
		//	root.addMenu(new LeafMenu("More " + i, null));

		layout = new Layout2D<Menu>();
		menuStack = new LinkedList<Menu>();
		pushMenu(root);
	}

	private void createFonts()
	{
		final String face = "sans-serif";

		HUDContext hc = engine.hudCtx;

		Font fnt = new Font(face, Font.PLAIN, hc.relFontSize(2f));
		fontMenuTitle = new FTGLPixmapFont(fnt);
		fontMenuTitle.rgbaColor = new float[] {0.176471f, 0.364706f, 0.847059f, 1f};

		fnt = new Font(face, Font.PLAIN, hc.relFontSize(2f));
		fontMenuTitle_shadow = new FTGLPixmapFont(fnt);
		fontMenuTitle_shadow.rgbaColor = new float[] {0, 0, 0, 1f};


		fnt = new Font(face, Font.PLAIN, hc.relFontSize(1f));
		fontMenuItem = new FTGLPixmapFont(fnt);
		fontMenuItem.rgbaColor = new float[] {0.0352941f, 0.0745098f, 0.176471f, 1f};

		fontMenuItemHover = new FTGLPixmapFont(fnt);
		fontMenuItemHover.rgbaColor = new float[] {0.207843f, 0.431373f, 1f, 1f};

	}


	public void drawMenu()
	{
		HUDContext hc = engine.hudCtx;

		//glColor3f(0f, 0f, 0f);
		//setup 2D projection
		hc.pushContext();
		{
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();

			glEnable(GL_TEXTURE_2D);

			drawBackground();

			drawMenuBorder();

			drawMenuLayout();

			hc.drawMousePointer(hc.getMousePos(), "hand", 1.0f);

		}
		hc.popContext();
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

	private LinkedList<Menu> findMenu(Object menuId)
	{
		LinkedList<Menu> path = new LinkedList<Menu>();
		if (_findMenu(menuStack.getFirst(), menuId, path))
			return path;
		else
			return null;
	}

	private boolean _findMenu(Menu node, Object menuId, LinkedList<Menu> path)
	{
		path.add(node);

		Object nodeId = node.getId();
		if (nodeId != null && nodeId.equals(menuId))
			return true;
		else
		{
			for (Menu child: node.getSubmenu())
			{
				if (_findMenu(child, menuId, path))
					return true;
			}

			path.removeLast();

			//not found
			return false;
		}
	}

	public void jumpTo(Object menuId)
	{
		//Pop to the root
		while (menuStack.size() > 1)
			menuStack.removeLast();

		//Push until the specified menu is found
		LinkedList<Menu> path = findMenu(menuId);
		if (path != null)
		{
			//we are already on the root
			path.removeFirst();

			for (Menu parent: path)
				pushMenu(parent);
		}

		layoutMenu(menuStack.getLast());
	}

	/**Return true if currently at or below the specified menu*/
	public boolean within_submenu(Object menuId)
	{
		for (Menu m: menuStack)
		{
			Object mId = m.getId();
			if (mId != null && mId.equals(menuId))
				return true;
		}
		return false;
	}

	private void layoutMenu(Menu menu)
	{
		layout.clear();

		HUDContext hc = engine.hudCtx;

		Rect4f headingBox = hc.getTextRect(menu.getText(), fontMenuTitle);
		headingBox = headingBox.centeredOn(Vec2f.ORIGIN).newY(hc.viewport.height * 0.25f);
		layout.addItem(menu, headingBox);

		float yPos = headingBox.bottom() - headingBox.height * 0.25f;

		//Sub menu entries
		for (Menu sub: menu.getSubmenu())
		{
			Rect4f rect = hc.getTextRect(sub.getText(), fontMenuItem);
			rect = rect.centeredOn(Vec2f.ORIGIN).newY(yPos);
			layout.addItem(sub, rect);

			yPos = rect.bottom() - (rect.height * 0.25f);
		}
	}


	private void drawMenuBorder()
	{
		HUDContext hc = engine.hudCtx;

		//border width and height
		final float BW = 0.035f;
		final float BH = BW * glCtx.getViewportAspectRatio();

		final float BW2 = BW * 2.0f;
		final float BH2 = BH * 2.0f;

		//Get bounding rect of entire menu text
		Rect4f bounds = layout.getBoundingBox();

		//enlarge sligtly
		bounds = bounds.grow(0.1f);

		//enable standard alpha blending
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

		//Draw background rect
		glDisable(GL_TEXTURE_2D);
		glColor4f(1f, 1f, 1f, 0.4f);
		hc.drawRect(bounds.grow(-(BW / 2.0f)), true);

		glEnable(GL_TEXTURE_2D);
		glColor4f(1f, 1f, 1f, 1.0f);

		//Top & Bottom
		glBindTexture(GL_TEXTURE_2D, texCache.getTexture("res/menu/border-horz.png")[0]);
		hc.draw2DBox(bounds.pos.x + BW, bounds.pos.y, bounds.width - BW2, BH, true, false);
		hc.draw2DBox(bounds.pos.x + BW, bounds.bottom() + BH, bounds.width - BW2, BH, true, false);

		//Left & Right
		glBindTexture(GL_TEXTURE_2D, texCache.getTexture("res/menu/border-vert.png")[0]);
		hc.draw2DBox(bounds.pos.x, bounds.pos.y - BH, BW, bounds.height - BH2, true, false);
		hc.draw2DBox(bounds.right() - BW, bounds.pos.y - BH, BW, bounds.height - BH2, true, false);

		//4 corners
		glBindTexture(GL_TEXTURE_2D, texCache.getTexture("res/menu/corner.png")[0]);
		//top-left
		hc.draw2DBox(bounds.pos.x, bounds.pos.y, BW, BH, true, false);
		//top-right
		hc.draw2DBox(bounds.pos.x + bounds.width - BW, bounds.pos.y, BW, BH, true, false);
		//bot-left
		hc.draw2DBox(bounds.pos.x, bounds.bottom() + BH, BW, BH, true, false);
		//bot-right
		hc.draw2DBox(bounds.pos.x + bounds.width - BW, bounds.bottom() + BH, BW, BH, true, false);

		glBlendFunc(GL_ONE, GL_ZERO);   //equivalent to disable blending

	}

	private void drawMenuLayout()
	{
		glDisable(GL_TEXTURE_2D);

		HUDContext hc = engine.hudCtx;

		//get the item under the mouse
		Menu hover = layout.getItemAtPos(hc.getMousePos());

		for (Menu mnu : layout)
		{
			Rect4f mrect = layout.getItemRect(mnu);
			//glColor3f(1f, 1f, 1f);
			//drawRect(mr.rect, false);

			FTGLPixmapFont font;

			//Heading?
			if (mnu == menuStack.getLast())
			{
				font = fontMenuTitle;

				//Draw head font shadow first
				float[] xy = hc.getTextRectRasterPos(mrect, font);
				float offset = (-font.descender() / glCtx.height) * 0.35f;  //offset the shadow relative to the size of the font
				glRasterPos2f(xy[0] + offset, xy[1] - offset);
				fontMenuTitle_shadow.render(mnu.getText());
			}
			else if (hover == mnu)
				font = fontMenuItemHover;
			else
				font = fontMenuItem;

			hc.setTextRectRasterPos(mrect, font);
			font.render(mnu.getText());
		}
	}



	private void drawBackground()
	{
		//hueToRGB(bgHue.currentValue());
		glColor3f(1.0f, 1.0f, 1.0f);
		glEnable(GL_TEXTURE_2D);

		glBindTexture(GL_TEXTURE_2D, texCache.getTexture(menuViewpoint.getImage())[0]);

		//Engine.instance.overlayProcessor.upload(menuViewpoint);

		engine.hudCtx.draw2DBox_fullscreen(true);
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

	public void onMouseClick()
	{
		Menu mnu = layout.getItemAtPos(engine.hudCtx.getMousePos());
		if (mnu != null)
		{
			menuListener.menuActivated(mnu);

			//Show submenu (if any)
			if (mnu.hasSubmenu())
				pushMenu(mnu);
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
			//ws.add(rippleSpec.noiseImage, WorkingSet.PRI_HIGH + 1);
			//ws.add(rippleSpec.maskImage, WorkingSet.PRI_HIGH);
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

	public void simulateMenuClick(Object menuId)
	{
		LinkedList<Menu> path = findMenu(menuId);
		if (path != null)
		{
			menuListener.menuActivated(path.getLast());
		}

	}
}
