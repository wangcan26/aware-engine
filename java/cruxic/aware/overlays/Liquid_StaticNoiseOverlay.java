package cruxic.aware.overlays;

import cruxic.aware.*;
import cruxic.math.Rect4i;

/**

 */
public class Liquid_StaticNoiseOverlay implements Overlay
{
	private final Liquid_StaticNoiseOverlay_Impl impl;
	private final int overlayIndex;
	private final Rect4i rect;

	Liquid_StaticNoiseOverlay(int overlayIndex, Liquid_StaticNoiseOverlay_Impl impl)
	{
		this.overlayIndex = overlayIndex;
		this.impl = impl;

		//cache the rect
		rect = impl.getRect(overlayIndex);
	}

	public OverlaySpec getSpec()
	{
		return impl.ospec;
	}

	public void update(float elapsedSeconds, GLImage image, int offsetX, int offsetY)
	{
		//update the manual time source
		impl.timeSource.setCurrentTime(elapsedSeconds);

		impl.update(overlayIndex, impl.noiseRotationAngle.currentValue(), image, offsetX, offsetY);
	}

	public void dispose()
	{
		impl.dispose();
	}

	public boolean isOpaque()
	{
		//water only effects the masked area
		return false;
	}

	public Rect4i getRect()
	{
		return rect;
	}
}
