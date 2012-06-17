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

import cruxic.aware.ipo.IPOTimeSource;

/**
	Measures elapsed time for various parts of the Engine.
 */
class EngineTimer
	implements IPOTimeSource
{
	private long beginNanos;
	private float markSeconds;

	/**start a new timer*/
	public EngineTimer()
	{
		beginNanos = System.nanoTime();
	}

	public void mark()
	{
		markSeconds = totalSec();
	}

	public float elapsedSec()
	{
		return totalSec() - markSeconds;
	}

	/**Equivalent to calling elapsedSec() followed by mark() but more efficient.*/
	public float elapsedSecAndMark()
	{
		float oldMark = markSeconds;
		markSeconds = totalSec();
		return markSeconds - oldMark;
	}

	/**return amount of time since this timer was constructed*/
	public float totalSec()
	{
		long elapsedMillis = (System.nanoTime() - beginNanos) / 1000000;
		return elapsedMillis / 1000.0f;
	}
}

