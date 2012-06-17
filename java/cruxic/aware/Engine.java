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
import cruxic.math.SphereCoord3f;

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
	public Renderer renderer;
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

	public final Map<String, Object> params;

	private final File game_data_dir;

	public Engine(OpenGLContext glCtx, File game_data_dir)
	{
		assert instance == null;
		instance = this;
		stop = false;
		this.game_data_dir = game_data_dir;

		params = new HashMap<String, Object>();
		params.put("renderer.show_fps", Boolean.FALSE);
		params.put("renderer.show_geom", Boolean.FALSE);
		params.put("renderer.show_hotspots", Boolean.FALSE);
		params.put("devel.cycle_viewpoints", Boolean.TRUE);

		frameTimer = new EngineTimer();
		nextFPSValue = 0;
		fpsValues = new float[8];

		cursorFadeOut = new DelayedLinearIPO(1.0f, 0.0f, 0.5f, 0.5f, newTimeSource());

		this.glCtx = glCtx;

		cameraInput = new CameraInput(glCtx.height);

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
						else
							menu.setVisible(true);
					}
					break;
				}
				case Keyboard.KEY_PAUSE:  //a last resort way to exit during development
				{
					stop = true;
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


		if (menu.isVisible())
		{
			menu.drawMenu();
			if (mouseUp)
				menu.onMouseClick();
		}
		else
		{
			//check for mouse and keyboard movement
			if (cameraInput.checkForInput())
			{
				//show the pointer again since the mouse moved
				cursorFadeOut.reset();
			}

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
			if ((Boolean)params.get("devel.cycle_viewpoints"))
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

		//edit-mode: add a point to active hotspot
//		var edSpot = avp.ed_getCurrentHotSpot();
//		if (edSpot != null)
//		{
//			edSpot.polygon.add(engine.cameraInput.getLookRay());
//			return;
//		}

		//edit-mode: delete a viewpoint
//		switch (engine.console.state)
//		{
//			case ConsoleState.SELECT_HOTSPOT_TO_DELETE:
//			{
//				int hsIdx = avp.findActiveHotspot(engine.cameraInput.getLookRay());
//				if (hsIdx != -1)
//				{
//					avp.hotSpots.remove_at(hsIdx);
//					engine.console.popState();
//				}
//				return;
//			}
//			case ConsoleState.SELECT_HOTSPOT_TO_LINK:
//			{
//				int hsIdx = avp.findActiveHotspot(engine.cameraInput.getLookRay());
//				if (hsIdx != -1)
//				{
//					engine.console.popState();
//					engine.console.pushSelectionPrompt("Select viewpoint", getLinkableViewpoints(avp.hotSpots[hsIdx]), link_viewpoint_by_name, avp.hotSpots[hsIdx]);
//				}
//				return;
//			}
//			//no default case
//		}

		//Get clicked hotspot
		Viewpoint clickedVP = getHotspotTarget(cameraInput.getLookRay());
		System.out.printf("Clicked %s\n", clickedVP);

		//no hotspot clicked
		if (clickedVP == null)
			return;

		previousViewpoint = avp;
		avp = clickedVP;
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
		if ((Boolean)params.get("devel.cycle_viewpoints"))
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

	
}
