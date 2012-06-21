package cruxic.aware;

import cruxic.math.*;

import java.util.*;

/**
	Holds a set of 2D rectangles which represent UI components that can be selected
 	by a 2D coordinate
 */
public class Layout2D<Item> implements Iterable<Item>
{
	private Map<Rect4f, Item> items;
	/**a reverse index of items*/
	private IdentityHashMap<Item, Rect4f> itemRects;

	/**cached bounding box*/
	private Rect4f _bounding_box;

	public Layout2D()
	{
		items = new HashMap<Rect4f, Item>(32);
		itemRects = new IdentityHashMap<Item, Rect4f>(32);
	}

	public void addItem(Item item, Rect4f rect)
	{
		items.put(rect, item);
		itemRects.put(item, rect);

		//cause bounding box to be recomputed
		_bounding_box = null;

	}

	public Rect4f getItemRect(Item item)
	{
		Rect4f rect = itemRects.get(item);
		if (rect != null)
			return rect;
		else
			throw new NoSuchElementException(item.toString());
	}
	
	/**Get the item which has a rectangle containing the given point.
	 @return null if none contain the point*/
	public Item getItemAtPos(Vec2f pos)
	{
		for (Rect4f rect: items.keySet())
		{
			if (rect.contains(pos))
				return items.get(rect);
		}

		return null;
	}

	/**Get the rectangle which contains all other rectangles*/
	public Rect4f getBoundingBox()
	{
		if (_bounding_box == null)
		{

			float left = Float.MAX_VALUE;
			float right = Float.MIN_VALUE;
			float top = Float.MIN_VALUE;
			float bottom = Float.MAX_VALUE;

			//Find maximum bounding box
			for (Rect4f rect: items.keySet())
			{
				if (rect.left() < left)
					left = rect.left();

				if (rect.right() > right)
					right = rect.right();

				if (rect.bottom() < bottom)
					bottom = rect.bottom();

				if (rect.top() > top)
					top = rect.top();
			}

			//avoid MAX_VALUE and MIN_VALUE
			if (items.isEmpty())
			{
				left = -0.01f;
				right = 0.01f;
				top = 0.01f;
				bottom = -0.01f;
			}

			_bounding_box = new Rect4f(left, top, right - left, top - bottom);
		}

		return _bounding_box;
	}

	public Iterator<Item> iterator()
	{
		final Iterator<Item> myItr = itemRects.keySet().iterator();

		return new Iterator<Item>()
		{

			public boolean hasNext()
			{
				return myItr.hasNext();
			}

			public Item next()
			{
				return myItr.next();
			}

			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	public void clear()
	{
		items.clear();
		itemRects.clear();
		_bounding_box = null;
	}
}
