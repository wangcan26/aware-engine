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

import org.json.simple.JSONObject;
import cruxic.aware.*;

/**
	Used to instantiate overlay-spec objects when loading the game spec file.
 */
public class OverlaySpecRegistry
{
	public OverlaySpecRegistry()
	{

	}

	public OverlaySpec loadFromJSON(JSONObject node, ResourceManager.FileResolver fileResolver)
	  throws IOExceptionRt
	{
		String type = (String)node.get("type");
		String name = (String)node.get("name");

		if (name == null)
			throw new IOExceptionRt("overlay missing \"name\"");

		if (type == null)
			throw new IOExceptionRt("overlay missing \"type\"");

		if (type.equals("Liquid_StaticNoiseOverlaySpec"))
			return Liquid_StaticNoiseOverlaySpec.loadFromJSON(node, fileResolver);
		else
			throw new IOExceptionRt("Unknown type: \"" + type + "\" for overlay \"" + name + "\"");
	}
}
