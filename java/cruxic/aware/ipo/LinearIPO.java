package cruxic.aware.ipo;


public class LinearIPO implements IPOCurve
{
	final float firstValue;
	final float lastValue;
	private final float durationSeconds;
	private final IPOTimeSource timeSource;
	private boolean complete;

	public LinearIPO(float firstValue, float lastValue, float durationSeconds, IPOTimeSource timeSource)
	{
		assert(durationSeconds > 0.0f);  //unecessary restriction?
		this.firstValue = firstValue;
		this.lastValue = lastValue;
		this.durationSeconds = durationSeconds;
		this.timeSource = timeSource;
		reset();
	}

	public float currentValue()
	{
		float elapsed = timeSource.elapsedSec();

		//if we have reached the end or are beyond just keep returning the same value
		if (elapsed >= durationSeconds)
		{
			//we are complete as soon as the last value is returned.
			//Otherwise the last tick of the game engine before it completes
			//will usually be a bit shy of the last value.  This is problematic for
			//things like the camera auto-pan effect
			complete = true;
			return lastValue;
		}
		else
			return firstValue + ((lastValue - firstValue) * (elapsed / durationSeconds));
	}

	public boolean isComplete()
	{
		//return timeSource.elapsedSec() >= durationSeconds;
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
			currentValue(), firstValue, lastValue, durationSeconds, wasComplete ? 1 : 0);
		complete = wasComplete;
	}

}
