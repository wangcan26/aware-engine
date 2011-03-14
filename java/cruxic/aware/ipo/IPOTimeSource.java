package cruxic.aware.ipo;

/**

 */
public interface IPOTimeSource
{
	public float totalSec();
	public void mark();
	public float elapsedSec();
}
