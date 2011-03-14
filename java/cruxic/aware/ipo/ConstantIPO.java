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