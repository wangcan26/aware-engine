package cruxic.aware.overlays;

import cruxic.aware.*;
import cruxic.aware.ipo.*;
import cruxic.math.Rect4i;

import java.util.*;

/**

 */
public class Test_SolidOverlaySpec implements OverlaySpec
{
	private final Rect4i[] rects;

	public Test_SolidOverlaySpec(Rect4i... rects)
	{
		this.rects = rects;
	}

	public String getName()
	{
		return "solid-overlay-test";
	}

	public List<Overlay> loadOverlays(Engine engine)
	{
		ArrayList<Overlay> overlays = new ArrayList<Overlay>(rects.length);

		ManualIPOTimeSource timeSource = new ManualIPOTimeSource();
		IPOCurve hueCurve = new RepeatingIPO(new LinearIPO(0.0f, 6.0f, 6.0f, timeSource));
		for (Rect4i rect: rects)
		{
			overlays.add(
				new Test_SolidOverlay(this, timeSource, hueCurve, rect));
		}

		return overlays;
	}

	public List<String> getImageResources()
	{
		return Collections.EMPTY_LIST;
	}
}
