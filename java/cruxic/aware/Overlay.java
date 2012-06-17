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
package cruxic.aware;

import cruxic.math.Rect4i;

/**
	A rectangular image which is layed on top of the base viewpoint image.
 Some overlays are static - their image data does not change.  However,
 many overlays are animated image data.
 */
public interface Overlay
{
	/**Get the parameters which were used to create this Overlay.*/
	public OverlaySpec getSpec();

	/**Compute the overlay data and copy it into given 'image'. (see param notes below)

	 @param elapsedSeconds number of seconds that have elapsed since the previous frame
	   of animation.  Note: when rendering asynchronously this value is actually a predictive
	   estimate.

	 @param image a pixel buffer into which the overlay copies it's pixels.  This
	   buffer has been initialized to the correct background image so overlay only
	   needs to update the pixels which it cares about.  This buffer is often larger
	   than the actual overlay (see offsetX, offsetY).

	 @param offsetX the distance from the overlay x1 to the image-buffer x1
	 @param offsetY the distance from the overlay y1 to the image-buffer y1
	 */
	public void update(float elapsedSeconds, GLImage image, int offsetX, int offsetY);

	/**Free all native resources associated with this overlay immediately.
	 This is always preferable to waiting for garbage collection.*/
	public void dispose();

	/**Get the layer at which this overlay should be drawn.
	 Lower numbers are drawn first.*/
	//public int getLayerIndex();

	/**true if this ovelay completely overwrites all image data directly under it.*/
	public boolean isOpaque();

	/**Get the coordinates of this overlay upon the larger base-image.*/
	public Rect4i getRect();
}
