package cruxic.aware.overlays;

import cruxic.aware.*;
import cruxic.math.Rect4i;

/**

 */
public class Test_TransparentOverlay implements Overlay
{
	private final Test_TransparentOverlaySpec ospec;
	private final Rect4i rect;
	private final GLImage pattern;

	Test_TransparentOverlay(Test_TransparentOverlaySpec ospec, Rect4i rect)
	{
		this.ospec = ospec;
		this.rect = rect;

		int checkSize = Math.max(rect.width(), rect.height());
		pattern = GLImage.createCheckerBoard(checkSize, 10, 0, 0x444444);
	}

	public OverlaySpec getSpec()
	{
		return ospec;
	}

	public void update(float elapsedSeconds, GLImage image, int offsetX, int offsetY)
	{
		image.combine(pattern, offsetX, offsetY, offsetX + rect.width(), offsetY + rect.height());
	}

	public void dispose()
	{
		pattern.dispose();
	}

	public boolean isOpaque()
	{
		return false;
	}

	public Rect4i getRect()
	{
		return rect;
	}
}