package cruxic.aware;

import cruxic.math.*;

import java.util.*;

/**

 */
public class PanoViewpoint implements Viewpoint
{
	public final String id;

	public Vec3f location;
	public boolean implicitBackLink;

	public List<String> imageIds;

	public List<PanoHotspot> hotspots;

	public List<OverlaySpec> overlays;

	private String activeImage;

	public PanoViewpoint(String id)
	{
		this.id = id;
		imageIds = new ArrayList<String>(1);
		hotspots = new ArrayList<PanoHotspot>();
		overlays = new ArrayList<OverlaySpec>(1);
		location = Vec3f.ORIGIN;
	}

	public String getId()
	{
		return id;
	}

	/**Get the current image*/
	public String getImage()
	{
		//initialize activeImage if necessary
		if (activeImage == null && !imageIds.isEmpty())
			activeImage = imageIds.get(0);

		return activeImage;
	}

	public String toString()
	{
		return id;
	}

	/**search all hotspots  in this viewpoint for the one which
	is pierced by the given ray.
	@return null if none found*/
	public PanoHotspot findActiveHotspot(SphereCoord3f lookRay)
	{
		int idx = 0;
		for (PanoHotspot hs: hotspots)
		{
			if (hs.isRayInside(lookRay))
				return hs;
			else
				idx++;
		}

		return null;
	}

	public List<OverlaySpec> getOverlays()
	{
		return overlays;
	}
}
