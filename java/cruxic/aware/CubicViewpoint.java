package cruxic.aware;

import cruxic.aware.misc.WorkingSet;


/**

 */
public class CubicViewpoint extends PanoViewpoint
{
	public CubicViewpoint(String id)
	{
		super(id);
	}

	public void addImagesToWorkingSet(WorkingSet<String> ws, int priorityGroup)
	{
		for (String imgId: imageIds)
		{
			//System.out.println(imgId);
			ws.add(imgId, priorityGroup);
		}
	}

	/**Sort imageIds in this order: front, back, left, right, top, bottom*/
	void sortCubeImages_FBLRTB()
		throws IllegalArgumentException
	{
		if (imageIds.size() != 6)
			throw new IllegalArgumentException(String.format("Expected 6 images but found %d instead!", imageIds.size()));

		String[] six = new String[6];

		for (String imgId: imageIds)
		{
			System.out.println("Before: " + imgId);
			int idx = getFaceIndex(imgId);
			if (idx == -1)
				throw new IllegalArgumentException(String.format("Image name \"%s\" does not contain one of [front, back, left, right, top, bottom]", imgId));
			else if (six[idx] != null)
				throw new IllegalArgumentException("Duplicate cube face image");
			else
				six[idx] = imgId;
		}

		imageIds.clear();
		for (String imgId: six)
		{
			imageIds.add(imgId);
			System.out.println("After: " + imgId);
		}
	}

	private int getFaceIndex(String imgId)
	{
		int n;
		if (imgId.contains("front"))
			n = 0;
		else if (imgId.contains("back"))
			n = 1;
		else if (imgId.contains("left"))
			n = 2;
		else if (imgId.contains("right"))
			n = 3;
		else if (imgId.contains("top"))
			n = 4;
		else if (imgId.contains("bottom"))
			n = 5;
		else
			n = -1;

		return n;
	}
	
}
