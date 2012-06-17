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

	
}
