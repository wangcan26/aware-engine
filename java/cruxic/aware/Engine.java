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
package cruxic.aware;

import org.lwjgl.opengl.Display;
import org.lwjgl.input.*;
import cruxic.aware.ipo.*;
import cruxic.aware.tex_cache.TextureCache;
import cruxic.aware.misc.WorkingSet;
import cruxic.aware.menu.MenuSystem;
import cruxic.math.*;
import cruxic.aware.Develop;

import java.util.*;
import java.io.File;

/**
	Orchestrates all subsystems within the game-engine to create the game experience.
 */
public class Engine
{
	/**the one and only running engine instance (singleton)*/
	public static Engine instance;

	public MenuSystem menu;
	public Develop dev;
	public Renderer renderer;
	public HUDContext hudCtx;
	public CameraInput cameraInput;
	public OpenGLContext glCtx;
	public TextureCache texCache;
	public ResourceManager resMgr;

	public GameWorld gameWorld;
	Viewpoint previousViewpoint;

	public OverlayProcessor overlayProcessor;

	IPOCurve cursorFadeOut;

	private EngineTimer frameTimer;
	private float[] fpsValues;
	private int nextFPSValue;

	boolean stop;

	public final Params params;

	private final File game_data_dir;

	public Engine(OpenGLContext glCtx, File game_data_dir)
	{
		assert instance == null;
		instance = this;
		stop = false;
		this.game_data_dir = game_data_dir;

		this.dev = new Develop();

		File prefsDir = new File(System.getProperty("user.home"), "Aware-Engine");
		prefsDir.mkdirs();
		if (prefsDir.isDirectory())
		{
			params = new Params(new File(prefsDir, "engine.properties"));
		}
		else
		{
			System.err.println("Unable to create dir: " + prefsDir.getPath());
			params = new Params();
		}


		frameTimer = new EngineTimer();
		nextFPSValue = 0;
		fpsValues = new float[8];

		cursorFadeOut = new DelayedLinearIPO(1.0f, 0.0f, 0.5f, 0.5f, newTimeSource());

		this.glCtx = glCtx;

		cameraInput = new CameraInput(glCtx.height);

		hudCtx = new HUDContext(glCtx);

		overlayProcessor = new OverlayProcessor();

		resMgr = new ResourceManager();

		texCache = new TextureCache(resMgr);
		preloadBuiltinTextures();		

		menu = new MenuSystem(this, glCtx, texCache, new MenuHandler(this));
		renderer = new Renderer(this);

		//load an empty game world
		GameWorld empty = new GameWorld();
		PanoViewpoint dummy = new EquirectViewpoint("dummy");
		dummy.imageIds.add(ResourceManager.ID_IMAGE_NULL);
		empty.addViewpoint(dummy);
		//empty.addViewpoint(new PanoViewpoint("bc/ent - ladder base - hall lights off.png"));
		loadGameWorld(empty);
	}

	private void preloadBuiltinTextures()
	{
		texCache.vram.load(ResourceManager.ID_IMAGE_NULL, resMgr.readGLImage(ResourceManager.ID_IMAGE_NULL), true);

		texCache.vram.load("hand", resMgr.readGLImage("res/pointers/hand.png"), true);
		texCache.vram.load("hand-no-action", resMgr.readGLImage("res/pointers/hand-no-action.png"), true);
		texCache.vram.load("hand-clicked", resMgr.readGLImage("res/pointers/hand-clicked.png"), true);
		texCache.vram.load("link", resMgr.readGLImage("res/pointers/link.png"), true);
	}

	/**cleanup all resources held by the engine in preparation for process exit*/
	void destroy()
	{
		try
		{
			overlayProcessor.shutdown();
			texCache.shutdown();
			System.out.println("Engine shutdown complete");
		}
		finally
		{
			instance = null;
		}
	}

	/**Switch to a new game world*/
	public void loadGameWorld(GameWorld newGameWorld)
	{
		this.gameWorld = newGameWorld;
		previousViewpoint = null;

		//setup the initial working texture set
		WorkingSet<String> ws = new WorkingSet<String>();
		gameWorld.active.addImagesToWorkingSet(ws, WorkingSet.PRI_HIGH);

		for (Viewpoint vp: getSurroundingViewpoints(gameWorld.active))
			vp.addImagesToWorkingSet(ws, WorkingSet.PRI_NORM);
		texCache.changeWorkingSet(ws);
	}

	private void checkKeyboardInput()
	{
		while (Keyboard.next())
		{
			int keyCode = Keyboard.getEventKey();
			boolean released = !Keyboard.getEventKeyState();
			switch (keyCode)
			{
				case Keyboard.KEY_G:
				{
					//Toggle mouse-grab when G key is released
					if (released)
					{
						Mouse.setGrabbed(!Mouse.isGrabbed());
					}

					break;
				}
				case Keyboard.KEY_ESCAPE:
				{
					//Toggle the menu
					if (released)
					{
						if (menu.isVisible())
						{
							//if there are no submenus to pop then hide the menu
							if (!menu.popMenu())
							{
								menu.setVisible(false);
								resumeGame();
							}
						}
						else if (dev.viewpoint_selector != null)
						{
							dev.viewpoint_selector.cancel();
						}
						else if (dev.new_hotspot != null)
						{
							dev.new_hotspot = null;
							dev.console_text.setLength(0);
						}
						else if (dev.delete_next_hotspot)
						{
							dev.delete_next_hotspot = false;
							dev.console_text.setLength(0);
						}
						else if (dev.link_next_hotspot)
						{
							dev.link_next_hotspot = false;
							dev.console_text.setLength(0);
						}
						else
							menu.setVisible(true);
					}
					break;
				}
				case Keyboard.KEY_PAUSE:  //a last resort way to exit during development
				{
					if (develop())
						stop = true;
					break;
				}
				case Keyboard.KEY_GRAVE:  //'~'
				{
					//Show/Hide the "Develop" menu
					if (released && develop())
					{
						if (!menu.within_submenu(MenuHandler.MenuAction.Mdevelop))
							menu.jumpTo(MenuHandler.MenuAction.Mdevelop);

						if (menu.isVisible())
						{
							resumeGame();
							menu.setVisible(false);
						}
						else
							menu.setVisible(true);
					}
					break;
				}
				case Keyboard.KEY_RETURN:
				{
					if (develop()
						&& dev.new_hotspot != null
						&& dev.new_hotspot.polygon.size() > 2)
					{
						gameWorld.getActiveViewpoint().hotspots().add(dev.new_hotspot);
						dev.new_hotspot = null;
						dev.console_text.setLength(0);

						gameWorld.write_viewpoint_properties(gameWorld.getActiveViewpoint());
					}

					break;
				}
				case Keyboard.KEY_A:
				{
					if (develop())
						menu.simulateMenuClick(MenuHandler.MenuAction.Mhotspot_add);

					break;
				}
				case Keyboard.KEY_DELETE:
				{
					if (develop())
						menu.simulateMenuClick(MenuHandler.MenuAction.Mhotspot_delete);
					break;
				}
				case Keyboard.KEY_J:
				{
					if (develop())
						menu.simulateMenuClick(MenuHandler.MenuAction.MJump2Viewpoint);
					break;
				}
			}
		}
	}

	public int getFramesPerSecond()
	{
		//average the array
		float avg = 0.0f;
		for (float f: fpsValues)
			avg += f;

		return Math.round(avg / fpsValues.length);
	}

	private void recordFPSValue(float fps)
	{
		nextFPSValue++;
		if (nextFPSValue >= fpsValues.length)
			nextFPSValue = 0;

		fpsValues[nextFPSValue] = fps;
	}

	int tickn = 0;

	/**Render another frame of the game world.
	 Usually this is called in a loop.*/
	public void tick()
	{
		Display.update();
		tickn++;

		checkKeyboardInput();

		//Check for mouse click
		boolean mouseDown = false;
		boolean mouseUp = false;
		while (Mouse.next())
		{
			int button = Mouse.getEventButton();
			//Left click
			if (button == 0)
			{
				boolean released = !Mouse.getEventButtonState();
				if (released && !mouseUp)
					mouseUp = true;
				else if (!released && !mouseDown)
					mouseDown = true;
			}
			//System.out.printf("Mouse.getEventButton() %d\n", Mouse.getEventButton());
		}

		if (useHUDMouse())
			hudCtx.checkForMouseMovement();
		else
		{
			if (cameraInput.checkForInput())
			{
				//show the pointer again since the mouse moved
				cursorFadeOut.reset();
			}
		}


		if (menu.isVisible())
		{
			menu.drawMenu();
			if (mouseUp)
				menu.onMouseClick();
		}
		else
		{
			if (mouseDown)  //don't wait for button release so that game feels more responsive
				onMouseClick();

			renderer.renderGame();
		}

		//Sleep if we are rendering faster than the target framerate
		Display.sync(60);

		recordFPSValue(1.0f / frameTimer.elapsedSecAndMark());
	}

	public IPOTimeSource newTimeSource()
	{
		return new EngineTimer();
	}

	Viewpoint getHotspotTarget(SphereCoord3f lookRay)
	{
		Viewpoint avp = gameWorld.getActiveViewpoint();

		//Get clicked hotspot
		PanoHotspot clickedHS = avp.findActiveHotspot(lookRay);

		//no hotspot clicked
		if (clickedHS == null)
		{
			if (params.getBool("devel.cycle_viewpoints"))
				return gameWorld.getNextViewpointInCycle(avp);
			//return to previous viewpoint
			else if (avp.isImplicitBackLink() && previousViewpoint != null)
				return previousViewpoint;
		}
		else if (clickedHS.targetViewpoint != null)
		{
			return clickedHS.targetViewpoint;
		}
		//else hotspot not linked

		return null;
	}

	public void onMouseClick()
	{
		Viewpoint avp = gameWorld.getActiveViewpoint();

		if (develop())
		{
			PanoHotspot clickedHS = avp.findActiveHotspot(cameraInput.getLookRay());

			//Adding a new hotspot?
			if (dev.new_hotspot != null)
			{
				dev.new_hotspot.polygon.add(cameraInput.getLookRay());
				return;
			}
			else if (dev.delete_next_hotspot)
			{
				dev.delete_next_hotspot = false;
				dev.console_text.setLength(0);

				if (clickedHS != null)
				{
					Iterator<PanoHotspot> itr = avp.hotspots().iterator();
					while (itr.hasNext())
					{
						if (itr.next() == clickedHS)
						{
							itr.remove();
							gameWorld.write_viewpoint_properties(avp);
							break;
						}
					}
				}

				return;
			}
			else if (dev.viewpoint_selector != null)
			{
				dev.viewpoint_selector.onMouseClick();
				return;
			}
			else if (dev.link_next_hotspot
				|| (clickedHS != null && clickedHS.targetViewpoint == null && !avp.isImplicitBackLink()))  //If they clicked a hotspot without a target then show the viewpoint selector
			{
				dev.link_next_hotspot = false;
				dev.console_text.setLength(0);

				dev.hotspot_to_link = avp.findActiveHotspot(cameraInput.getLookRay());
				if (dev.hotspot_to_link != null)
				{
					ViewpointSelector.SelectionListener sl = new ViewpointSelector.SelectionListener()
					{
						public void handleSelection(Viewpoint selectedVP)
						{
							//not canceled?
							if (selectedVP != null)
							{
								dev.hotspot_to_link.targetViewpoint = selectedVP;

								gameWorld.write_viewpoint_properties(gameWorld.getActiveViewpoint());
							}
							dev.viewpoint_selector = null;
							dev.hotspot_to_link = null;
							dev.console_text.setLength(0);
						}
					};

					dev.viewpoint_selector = new ViewpointSelector(sl, hudCtx, gameWorld.viewpoints, avp);
				}

				return;
			}

		}

		//Get clicked hotspot
		Viewpoint clickedVP = getHotspotTarget(cameraInput.getLookRay());

		if (clickedVP != null)
		{
			//System.out.printf("Clicked %s\n", clickedVP);
			jump2Viewpoint(clickedVP);
		}
	}

	public void jump2Viewpoint(Viewpoint newViewpoint)
	{
		Viewpoint avp = gameWorld.getActiveViewpoint();

		previousViewpoint = avp;
		avp = newViewpoint;
		gameWorld.active = avp;

		//setup the new working texture set
		WorkingSet<String> ws = new WorkingSet<String>();
		avp.addImagesToWorkingSet(ws, WorkingSet.PRI_HIGH);
		addOverlayImages(ws, avp, WorkingSet.PRI_HIGH - 1);

		//bugfix: I used to assign the previous viewpoint PRI_HIGH but I'm pretty much guaranteed that it's already cached in VRAM
		//The loop below will increase it's priority if it's still a surrounding viewpoint (likely)
		previousViewpoint.addImagesToWorkingSet(ws, WorkingSet.PRI_LOW);

		for (Viewpoint vp: getSurroundingViewpoints(avp))
			vp.addImagesToWorkingSet(ws, WorkingSet.PRI_NORM);

		//start loading the images (asynchronous)
		texCache.changeWorkingSet(ws);

		//start processing overlays (asynchronous)
		//overlayProcessor.retain_only(avp, previousViewpoint);  trying to process 2 viewpoints at once is too slow.  it makes the transition choppy
		overlayProcessor.retain_only(avp);
		overlayProcessor.load(avp);

		//panCameraToViewpoint();

		//start the transition
		renderer.startViewpointTransition();
	}

	/**add to the working set all images which are need by the given Viewpoint overlays.
	 @param priority the starting priority.  Each successive image will be given lesser priority by 1
	 */
	private void addOverlayImages(WorkingSet<String> ws, Viewpoint vp, int priority)
	{
		for (OverlaySpec ospec: vp.getOverlays())
		{
			for (String image: ospec.getImageResources())
			{
				//System.out.println("\toverlay image: " + image + "; pri=" + priority);
				ws.add(image, priority--);
			}
		}
	}


	private List<Viewpoint> getSurroundingViewpoints(Viewpoint vp)
	{
		//Looking at all the hotspots linked from here

		List<Viewpoint> list = new ArrayList<Viewpoint>(8);
		for (PanoHotspot hotspot: vp.hotspots())
		{
			if (hotspot.targetViewpoint != null)
				list.add(hotspot.targetViewpoint);
		}

		//Also include implicit back links
		if (vp == gameWorld.getActiveViewpoint()
			&& vp.isImplicitBackLink()
			&& previousViewpoint != null
			&& !list.contains(previousViewpoint))
		{
			list.add(previousViewpoint);
		}

		//In cycle mode, add the next viewpoint in the cycle
		if (params.getBool("devel.cycle_viewpoints"))
		{
			Viewpoint next = gameWorld.getNextViewpointInCycle(vp);
			if (!list.contains(next))
				list.add(next);
		}

		return list;
	}

	private boolean alreadyLoaded = false;

	public void resumeGame()
	{
		if (!alreadyLoaded)  //ToDo: ask the engine if it's already loaded instead of this flag
		{
			loadGameWorld(GameWorld.load_game_data(game_data_dir));
			alreadyLoaded = true;
		}
	}

	public boolean develop()
	{
		return params.getBool("devel.enable");
	}

	/**True means we are showing a 2D mouse position (eg menu or viewpoint selector)
	 False means the mouse movement moves the camera*/
	public boolean useHUDMouse()
	{
		return menu.isVisible()
			|| dev.viewpoint_selector != null;
	}

	
}
