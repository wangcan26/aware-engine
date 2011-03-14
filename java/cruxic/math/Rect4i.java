package cruxic.math;

/**

 */
public class Rect4i
{
	public int x1;
	public int y1;
	public int x2;
	public int y2;

	public Rect4i(int x1, int y1, int x2, int y2)
	{
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	public Rect4i()
	{
		//leave all zero
	}

	public Rect4i(Rect4i r)
	{
		x1 = r.x1;
		y1 = r.y1;
		x2 = r.x2;
		y2 = r.y2;
	}

	public final int width()
	{
		int w = x2 - x1;

		//abs value
		if (w < 0)
			w = -w;

		return w;
	}

	public final int height()
	{
		int h = y2 - y1;

		//abs value
		if (h < 0)
			h = -h;

		return h;
	}

	public void debugPrint()
	{
		System.out.printf("[%dx%d\t%d,%d %d,%d]\n", width(), height(), x1, y1, x2, y2);
	}

	public boolean contains(Rect4i rect)
	{
		return rect.x1 >= x1
			&& rect.y1 >= y1
			&& rect.x2 <= x2
			&& rect.y2 <= y2;
	}

	public final boolean contains(int x, int y)
	{
		return x >= x1
			&& x < x2
			&& y >= y1
			&& y < y2;
	}

	/**Return true if this rectangle overlaps the given*/
	public boolean overlaps(Rect4i r)
	{
		//Overlaps if any corner in 'r' is contained in this rect
		return contains(r.x1, r.y1)  //left,bottom
			|| contains(r.x2 - 1, r.y2 - 1)  //right,top (remember x2,y2 are exclusive)
			|| contains(r.x1, r.y2 - 1)  //left,top
			|| contains(r.x2 - 1, r.y1);  //right,bottom
	}


	public void round(Rect4i maxRect, int unit)
	{
		//Increase width as necessary
		int remainder = unit - (width() % unit);
		if (remainder != 0)
		{
			int dist1 = x1 - maxRect.x1;
			int dist2 = maxRect.x2 - x2;

			//do we have enough room to expand?
			if (remainder <= (dist1 + dist2))
			{
				dist1 = Math.min(dist1, remainder);
				x1 -= dist1;
				remainder -= dist1;

				dist2 = Math.min(dist2, remainder);
				x2 += dist2;
			}
		}

		//Increase height as necessary
		remainder = unit - (height() % unit);
		if (remainder != 0)
		{
			int dist1 = y1 - maxRect.y1;
			int dist2 = maxRect.y2 - y2;

			//do we have enough room to expand?
			if (remainder <= (dist1 + dist2))
			{
				dist1 = Math.min(dist1, remainder);
				y1 -= dist1;
				remainder -= dist1;

				dist2 = Math.min(dist2, remainder);
				y2 += dist2;
			}
		}
	}

}
