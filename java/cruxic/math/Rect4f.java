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

/**

 */
public class Rect4f
{
	public final Vec2f pos;
	public final float width;
	public final float height;

	public Rect4f(float x, float y, float width, float height)
	{
		this.pos = new Vec2f(x, y);
		this.width = width;
		this.height = height;
	}

	public Rect4f(Vec2f pos, float width, float height)
	{
		this.pos = pos;
		this.width = width;
		this.height = height;
	}


	public Rect4f centeredOn(Vec2f point)
	{
		return new Rect4f(
			point.x - (width / 2.0f),
			point.y - (height / 2.0f),
			width, height);				
	}

	public float left()
	{
		return pos.x;
	}

	public float right()
	{
		return pos.x + width;
	}

	public float top()
	{
		return pos.y;		
	}

	public float bottom()
	{
		return pos.y - height;
	}

	public Rect4f newY(float newY)
	{
		return new Rect4f(pos.x, newY, width, height);
	}

	/**expand (or shink) the rectangle from its center point by the given amount.
	 Use negative values to shrink.*/
	public Rect4f grow(float amount)
	{
		return new Rect4f(pos.x - amount, pos.y + amount,
			width + (amount * 2.0f), height + (amount * 2.0f));
	}

	public boolean contains(Vec2f point)
	{
		return point.x >= pos.x	&& point.x <= pos.x + width
		    && point.y <= pos.y	&& point.y >= pos.y - height;
	}
}
