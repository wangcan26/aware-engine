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
package cruxic.aware.ipo;

/**A constant value that does not change with time*/
public class ConstantIPO implements IPOCurve
{
	private float constantValue;
	private boolean complete;

	public ConstantIPO(float constantValue)
	{
		this.constantValue = constantValue;
		complete = false;
	}

	public float currentValue()
	{
		return constantValue;
	}

	public boolean isComplete()
	{
		return complete;
	}

	public void setComplete(boolean complete)
	{
		this.complete = complete;
	}

	public void reset()
	{
		complete = false;
	}

	/**get an IPO curve that is already complete*/
	public static ConstantIPO getAlreadyCompleteIPO()
	{
		ConstantIPO ipo = new ConstantIPO(0.0f);
		ipo.setComplete(true);
		return ipo;
	}

	public void debugPrint()
	{
		System.out.println("ConstantIPO.debugPrint todo");
	}
}