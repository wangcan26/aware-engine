package cruxic.aware;

import cruxic.aware.misc.WorkingSet;

/**
	Equirectangular panorama.
 */
public class EquirectViewpoint extends PanoViewpoint
{
	private String activeImage;

	public EquirectViewpoint(String id)
	{
		super(id);
	}

	/**Get the current image*/
	public String getImage()
	{
		//initialize activeImage if necessary
		if (activeImage == null && !imageIds.isEmpty())
			activeImage = imageIds.get(0);

		return activeImage;
	}	

	public void addImagesToWorkingSet(WorkingSet<String> ws, int priorityGroup)
	{
		ws.add(getImage(), priorityGroup);
	}
}
