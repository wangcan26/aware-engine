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
package cruxic.math;

import org.lwjgl.opengl.GL11;

/**2D vector or point*/
public class Vec2f
{
	public final float x;
	public final float y;

	/**0,0*/
	public static final Vec2f ORIGIN = new Vec2f(0.0f, 0.0f);

	public Vec2f(float x, float y)
	{
		this.x = x;
		this.y = y;
	}

	public void debugPrint()
	{
		System.out.printf("[%g, %g]\n", x, y);
	}

	public boolean equals(Vec2f v)
	{
		return x == v.x && y == v.y;
	}

	public void glVertex()
	{
		GL11.glVertex2f(x, y);
	}
}
