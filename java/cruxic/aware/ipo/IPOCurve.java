package cruxic.aware.ipo;

/**An InterPOlation curve.  A sequence of numbers that is calculated based
starting and ending "key frames" and time.*/
public interface IPOCurve
{
	public float currentValue();
	public boolean isComplete();
	public void reset();
	public void debugPrint();
}