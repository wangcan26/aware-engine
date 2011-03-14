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

import java.util.List;

/**
	Specifies all the parameters needed to initialize one or
 	more Overlay objects.
 */
public interface OverlaySpec
{
	/**the user given identifier for this set of overlays*/
	public String getName();

	/**Create one or more Overlay objects given the parameters
	 in this spec.  REMEMBER to call dispose() on the Overlay objects
	 when you are through with them.
	 */
	public List<Overlay> loadOverlays(Engine engine);

	/**Get all image resources this Overlay spec will require*/
	public List<String> getImageResources();
}
