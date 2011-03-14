package cruxic.aware.overlays;

import cruxic.aware.*;
import cruxic.aware.ipo.*;
import cruxic.math.Rect4i;

import java.util.*;

/**

 */
public class Test_TransparentOverlaySpec implements OverlaySpec
{
	private final Rect4i[] rects;

	public Test_TransparentOverlaySpec(Rect4i... rects)
	{
		this.rects = rects;
	}

	public String getName()
	{
		return "transparent-overlay-test";
	}

	public List<Overlay> loadOverlays(Engine engine)
	{
		ArrayList<Overlay> overlays = new ArrayList<Overlay>(rects.length);

		for (Rect4i rect: rects)
		{
			overlays.add(
				new Test_TransparentOverlay(this, rect));
		}

		return overlays;
	}

	public List<String> getImageResources()
	{
		return Collections.EMPTY_LIST;
	}
}