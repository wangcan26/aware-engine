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

import org.lwjgl.input.Mouse;
import cruxic.math.*;

/**
	Convert LWJGL Mouse position data into 2D coordinates.
 */
public class Mouse2DTracker
{
	public float x;
	public float y;
	private Rect4f viewport;
	private OpenGLContext glCtx;

	public Mouse2DTracker(Rect4f viewport, OpenGLContext glCtx)
	{
		this.viewport = viewport;
		this.glCtx = glCtx;
	}

	public void checkForInput()
	{
		final float sensX = viewport.width / glCtx.width;
		final float sensY = viewport.height / glCtx.height;

		if (Mouse.isGrabbed())
		{
			//must use deltas if mouse has been grabbed
			int dx = Mouse.getDX();
			int dy = Mouse.getDY();
			x += sensX * dx;
			y += sensY * dy;
		}
		else
		{
			//use absolute coordintes
			x = sensX * (Mouse.getX() - glCtx.width / 2f);
			y = sensY * (Mouse.getY() - glCtx.height / 2f);
		}


		//Keep pointer inside viewport
		if (x < viewport.pos.x)
			x = viewport.pos.x;
		else if (x > viewport.right())
			x = viewport.right();

		if (y > viewport.pos.y)
			y = viewport.pos.y;
		else if (y < viewport.bottom())
			y = viewport.bottom();
	}

	public Vec2f getPos()
	{
		return new Vec2f(x, y);
	}
}
