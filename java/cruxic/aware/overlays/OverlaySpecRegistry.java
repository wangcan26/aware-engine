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
