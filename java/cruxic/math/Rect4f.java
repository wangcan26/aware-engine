package cruxic.math;

/**

 */
public class Rect4f
{
	public final Vec2f pos;
	public final float width;
	public final float height;

	public Rect4f(float x, float y, float width, float height)
	{
		this.pos = new Vec2f(x, y);
		this.width = width;
		this.height = height;
	}

	public Rect4f(Vec2f pos, float width, float height)
	{
		this.pos = pos;
		this.width = width;
		this.height = height;
	}


	public Rect4f centeredOn(Vec2f point)
	{
		return new Rect4f(
			point.x - (width / 2.0f),
			point.y - (height / 2.0f),
			width, height);				
	}

	public float left()
	{
		return pos.x;
	}

	public float right()
	{
		return pos.x + width;
	}

	public float top()
	{
		return pos.y;		
	}

	public float bottom()
	{
		return pos.y - height;
	}

	public Rect4f newY(float newY)
	{
		return new Rect4f(pos.x, newY, width, height);
	}

	/**expand (or shink) the rectangle from its center point by the given amount.
	 Use negative values to shrink.*/
	public Rect4f grow(float amount)
	{
		return new Rect4f(pos.x - amount, pos.y + amount,
			width + (amount * 2.0f), height + (amount * 2.0f));
	}

	public boolean contains(Vec2f point)
	{
		return point.x >= pos.x	&& point.x <= pos.x + width
		    && point.y <= pos.y	&& point.y >= pos.y - height;
	}
}
