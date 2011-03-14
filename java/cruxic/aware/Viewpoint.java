package cruxic.aware;

import cruxic.math.SphereCoord3f;

import java.util.List;

/**

 */
public interface Viewpoint
{
	/**unique id of this viewpoint.*/
	public String getId();

	/**Get the current image*/
	public String getImage();

	/**search all hotspots  in this viewpoint for the one which
	is pierced by the given ray.
	@return -1 if none found*/
	public PanoHotspot findActiveHotspot(SphereCoord3f lookRay);

	/**Get all overlays that this viewpoint uses.  They are
	 sorted by layer index (the order in which they should be drawn).*/
	public List<OverlaySpec> getOverlays();
}
