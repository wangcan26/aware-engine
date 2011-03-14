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
