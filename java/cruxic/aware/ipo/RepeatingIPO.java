package cruxic.aware.ipo;

/**
	Wrap an IPOCurve to make it repeat endlessly
 */
public class RepeatingIPO implements IPOCurve
{
	public final IPOCurve base;

	public RepeatingIPO(IPOCurve base)
	{
		this.base = base;
	}

	public float currentValue()
	{
		if (base.isComplete())
			base.reset();
		return base.currentValue();
	}

	public boolean isComplete()
	{
		//never complete
		return false;
	}

	public void reset()
	{
		base.reset();
	}

	public void debugPrint()
	{
		base.debugPrint();
	}
}
