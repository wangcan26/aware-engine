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

import cruxic.math.*;

import java.util.*;

/**

 */
public class PanoViewpoint implements Viewpoint
{
	public final String id;

	public Vec3f location;
	public boolean implicitBackLink;

	public List<String> imageIds;

	public List<PanoHotspot> hotspots;

	public List<OverlaySpec> overlays;

	private String activeImage;

	public PanoViewpoint(String id)
	{
		this.id = id;
		imageIds = new ArrayList<String>(1);
		hotspots = new ArrayList<PanoHotspot>();
		overlays = new ArrayList<OverlaySpec>(1);
		location = Vec3f.ORIGIN;
	}

	public String getId()
	{
		return id;
	}

	/**Get the current image*/
	public String getImage()
	{
		//initialize activeImage if necessary
		if (activeImage == null && !imageIds.isEmpty())
			activeImage = imageIds.get(0);

		return activeImage;
	}

	public String toString()
	{
		return id;
	}

	/**search all hotspots  in this viewpoint for the one which
	is pierced by the given ray.
	@return null if none found*/
	public PanoHotspot findActiveHotspot(SphereCoord3f lookRay)
	{
		int idx = 0;
		for (PanoHotspot hs: hotspots)
		{
			if (hs.isRayInside(lookRay))
				return hs;
			else
				idx++;
		}

		return null;
	}

	public List<OverlaySpec> getOverlays()
	{
		return overlays;
	}
}
