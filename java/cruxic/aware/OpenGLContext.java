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
