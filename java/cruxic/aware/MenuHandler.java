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

import java.io.File;

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
		MToggle_showFPS,
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
			System.out.println("MenuClick: " + ma);

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
			}
		}

	}
}
