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

