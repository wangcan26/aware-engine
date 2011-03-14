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

/**
	An IPOTimeSource that must be updated manually by
 assigning the currentTime member.
 */
public class ManualIPOTimeSource implements IPOTimeSource
{
	private float markSeconds;
	private float currentTime;

	public ManualIPOTimeSource()
	{
		//leave all at 0.0
	}

	public float totalSec()
	{
		return currentTime;
	}

	public void mark()
	{
		markSeconds = currentTime;
	}

	public float elapsedSec()
	{
		return currentTime - markSeconds;
	}

	public void setCurrentTime(float seconds)
	{
		currentTime = seconds;

		//prevent negative elapsedSec()
		if (currentTime < markSeconds)
			markSeconds = currentTime;
	}
}
