package cruxic.aware;

import java.util.List;

/**
	Specifies all the parameters needed to initialize one or
 	more Overlay objects.
 */
public interface OverlaySpec
{
	/**the user given identifier for this set of overlays*/
	public String getName();

	/**Create one or more Overlay objects given the parameters
	 in this spec.  REMEMBER to call dispose() on the Overlay objects
	 when you are through with them.
	 */
	public List<Overlay> loadOverlays(Engine engine);

	/**Get all image resources this Overlay spec will require*/
	public List<String> getImageResources();
}
