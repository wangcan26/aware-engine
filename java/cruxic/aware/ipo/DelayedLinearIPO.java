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

import javax.naming.OperationNotSupportedException;


/**Like LinearIPO except the first value is held for a period of time before the interpolation begins*/
public class DelayedLinearIPO implements IPOCurve
{
	private float firstValue;
	private float lastValue;
	private float ipoSeconds;
	private float holdSeconds;
	private IPOTimeSource timeSource;
	private boolean complete;

	/**
		@param firstValue the starting value
		@param lastValue the ending value
		@param holdDurationSeconds how long to remain on the firstValue
		@param ipoDurationSeconds after holdDurationSeconds has elapsed, how long until lastValue is reached
	*/
	public DelayedLinearIPO(float firstValue, float lastValue, float holdDurationSeconds, float ipoDurationSeconds, IPOTimeSource timeSource)
	{
		assert(ipoDurationSeconds > 0.0f);  //unecessary restriction?
		this.firstValue = firstValue;
		this.lastValue = lastValue;
		this.ipoSeconds = ipoDurationSeconds;
		this.holdSeconds = holdDurationSeconds;
		this.timeSource = timeSource;
		reset();
	}

	public float currentValue()
	{
		float elapsed = timeSource.elapsedSec();

		//if we have reached the end or are beyond just keep returning the same value
		if (elapsed >= holdSeconds + ipoSeconds)
		{
			//we are complete as soon as the last value is returned.
			//Otherwise the last tick of the game engine before it completes
			//will usually be a bit shy of the last value.  This is problematic for
			//things like the camera auto-pan effect
			complete = true;
			return lastValue;
		}
		else if (elapsed <= holdSeconds)
		{
			return firstValue;
		}
		else
			return firstValue + ((lastValue - firstValue) * ((elapsed - holdSeconds) / ipoSeconds));
	}

	public boolean isComplete()
	{
		//return timeSource.elapsedSec() >= ipoSeconds;
		return complete;
	}

	public void reset()
	{
		timeSource.mark();
		complete = false;
	}

	public void debugPrint()
	{
		boolean wasComplete = complete;  //because following call to currentValue() might change complete flag
		System.out.printf("[cur=%g, start=%g, stop=%g, seconds=%g, complete=%d]\n",
			currentValue(), firstValue, lastValue, ipoSeconds, wasComplete ? 1 : 0);
		complete = wasComplete;
	}
}