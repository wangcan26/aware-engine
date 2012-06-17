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