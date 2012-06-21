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

import cruxic.aware.menu.*;

/**

 */
public class MenuHandler
	implements MenuActionListener
{
	private Engine engine;

	public enum MenuAction
	{
		MNull,
		MPlay,
		MQuit,
		MToggleMusic,
		MToggleFullscreen,
		MToggle_develop_mode,
		Mdevelop,
		MToggle_show_fps,
		MToggle_show_geom,
		MToggle_hotspot_show_all,
		MToggle_cycle_viewpoints,
		Mhotspot_add,
		Mhotspot_delete,
		Mhotspot_link,
		MJump2Viewpoint

	}

	public MenuHandler(Engine engine)
	{
		this.engine = engine;


	}

	public void menuActivated(Menu theMenu)
	{
		MenuAction ma = (MenuAction)theMenu.getId();

		if (theMenu instanceof ToggleMenu)
			((ToggleMenu)theMenu).toggle();

		if (ma != null)
		{
			//System.out.println("MenuClick: " + ma);

			switch (ma)
			{
				case MPlay:
					engine.menu.setVisible(false);
					engine.resumeGame();
					break;
				case MQuit:
					engine.stop = true;
					break;
				/*case MToggleFullscreen:
				{
					boolean on = ((ToggleMenu)theMenu).on;
					System.out.println("fullscreen = " + on);
					break;
				}
				case MToggleMusic:
				{
					boolean on = ((ToggleMenu)theMenu).on;
					System.out.println("music = " + on);
					break;
				}
				case MToggle_showFPS:
				{
					engine.renderer.showFPS = ((ToggleMenu)theMenu).on;
					break;
				}*/

				case Mhotspot_add:
				{
					String hsName = "hotspot_" + (engine.gameWorld.active.hotspots().size() + 1);
					engine.dev.new_hotspot = new PanoHotspot(hsName);
					engine.dev.console_text.setLength(0);
					engine.dev.console_text.append("Click to add hotspot points. [ENTER] to finish, [A] to start over");
					//turn on showing of hotspots
					engine.params.put("renderer.show_hotspots", Boolean.TRUE);


					engine.menu.setVisible(false);

					break;
				}
				case Mhotspot_delete:
				{
					if (engine.dev.new_hotspot != null)
					{
						engine.dev.new_hotspot = null;
						engine.dev.console_text.setLength(0);
					}
					else if (!engine.gameWorld.getActiveViewpoint().hotspots().isEmpty())
					{
						engine.dev.console_text.setLength(0);
						engine.dev.console_text.append("Click a hotspot to delete");
						engine.dev.delete_next_hotspot = true;
					}

					engine.params.put("renderer.show_hotspots", Boolean.TRUE);
					engine.menu.setVisible(false);

					break;
				}
				case Mhotspot_link:
				{
					if (engine.dev.new_hotspot == null)
					{
						engine.dev.link_next_hotspot = true;
						engine.dev.hotspot_to_link = null;
						engine.dev.console_text.setLength(0);
						engine.dev.console_text.append("Select a hotspot to link. (Click a non-hotspot to cancel)");

						engine.params.put("renderer.show_hotspots", Boolean.TRUE);
						engine.menu.setVisible(false);
					}
					break;
				}
				case MJump2Viewpoint:
				{
					engine.resumeGame();

					ViewpointSelector.SelectionListener sl = new ViewpointSelector.SelectionListener()
					{
						public void handleSelection(Viewpoint selectedVP)
						{
							if (selectedVP != null)
								engine.jump2Viewpoint(selectedVP);
							engine.dev.viewpoint_selector = null;
						}
					};

					engine.dev.viewpoint_selector = new ViewpointSelector(sl, engine.hudCtx, engine.gameWorld.viewpoints);
					engine.menu.setVisible(false);

					break;
				}
			}
		}

	}
}
