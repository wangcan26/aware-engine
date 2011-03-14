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
