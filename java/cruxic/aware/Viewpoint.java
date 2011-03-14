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
