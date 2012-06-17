/*
*	AwareEngine
*	Copyright (C) 2011  Adam Bennett <cruxicATgmailDOTcom>
*
*	This program is free software; you can redistribute it and/or
*	modify it under the terms of the GNU General Public License
*	as published by the Free Software Foundation; either version 2
*	of the License, or (at your option) any later version.
*
*	This program is distributed in the hope that it will be useful,
*	but WITHOUT ANY WARRANTY; without even the implied warranty of
*	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*	GNU General Public License for more details.
*
*	You should have received a copy of the GNU General Public License
*	along with this program; if not, write to the Free Software
*	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
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