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

import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.*;
import java.util.*;

import cruxic.math.*;
import cruxic.aware.overlays.OverlaySpecRegistry;

/**
	Holds the definition and state of one game world.
 */
public class GameWorld
{
	public Viewpoint active;

	private List<Viewpoint> viewpoints;

	public GameWorld()
	{
		viewpoints = new ArrayList<Viewpoint>(128);
	}

	public void addViewpoint(Viewpoint vp)
	{
		if (active == null)
			active = vp;

		viewpoints.add(vp);
	}

	public Viewpoint getActiveViewpoint()
	{
		return active;		
	}

	private static JSONObject parseJSONFile(File f)
		throws IOExceptionRt, RuntimeException
	{
		try
		{
			FileReader fr = new FileReader(f);
			try
			{
				JSONParser parser = new JSONParser();
				return (JSONObject)parser.parse(fr);
			}
			catch (ParseException e)
			{
				throw new RuntimeException(e);
			}
			finally
			{
				fr.close();
			}
		}
		catch (IOException ioe)
		{
			throw new IOExceptionRt(ioe);
		}
	}

	private static Vec3f parseVec3fFromJSON(Object listOfPoints)
	{
		List<Double> ar = (List<Double>)listOfPoints;
		if (ar != null)
		{
			float x = (float)ar.get(0).doubleValue();
			float y = (float)ar.get(1).doubleValue();
			float z = (float)ar.get(2).doubleValue();

			//optimization: 0,0,0 is a common point and we can save the allocation
			if (x == 0.0f && y == 0.0f && z == 0f)
				return Vec3f.ORIGIN;
			else
				return new Vec3f(x, y, z);
		}
		else
			return null;
	}

	private static String getRelativeBaseDir(File someFile)
	{
		try
		{
			String cwd = new File(".").getCanonicalPath();
			String gameBaseDir = someFile.getAbsoluteFile().getParent();
			String relativeBaseDir;
			if (gameBaseDir.startsWith(cwd))
				relativeBaseDir = gameBaseDir.substring(cwd.length() + 1);
			else
				relativeBaseDir = gameBaseDir;

			//System.out.printf("cwd=%s base=%s rel=%s\n", cwd, gameBaseDir, relativeBaseDir);

			return relativeBaseDir;
		}
		catch (IOException ioe)
		{
			throw new IOExceptionRt(ioe);
		}
	}

	public static GameWorld loadSpec(File specFile)
		throws IOExceptionRt
	{
		JSONObject root = parseJSONFile(specFile);

		String baseDir = getRelativeBaseDir(specFile);

		ResourceManager.FileResolver fileResolver = new ResourceManager.FileResolver(baseDir);

		GameWorld gw = new GameWorld();

		OverlaySpecRegistry overlaySpecRegistry = new OverlaySpecRegistry();

		IdentityHashMap<PanoHotspot, String> hotspotTargets = new IdentityHashMap<PanoHotspot, String>(128);
		Map<String, Viewpoint> viewpointsById = new HashMap<String, Viewpoint>(128);

		List<JSONObject> jviewpoints = (List<JSONObject>)root.get("viewpoints");
		for (JSONObject jvp: jviewpoints)
		{
			PanoViewpoint vp;

			boolean cubic = jvp.containsKey("type") && jvp.get("type").equals("cubic");
			if (cubic)
			{
				vp = new CubicViewpoint((String)jvp.get("id"));
			}
			else
			{
				vp = new EquirectViewpoint((String)jvp.get("id"));
			}

			vp.location = parseVec3fFromJSON(jvp.get("loc"));
			vp.implicitBackLink = (Boolean)jvp.get("implicitBackLink");
			//vp.cubic = jvp.containsKey("type") && jvp.get("type").equals("cubic");

			//Images
			for (String imgId: (List<String>)jvp.get("images"))
			{
				vp.imageIds.add(fileResolver.resolveImageId(imgId));
			}

			//Hotspots
			List<JSONObject> jhss = (List<JSONObject>)jvp.get("hotspots");
			for (JSONObject jhs: jhss)
			{
				PanoHotspot hs = new PanoHotspot((String)jhs.get("id"));

				String targetViewpoint = (String)jhs.get("targetViewpoint");
				if (targetViewpoint != null)
					hotspotTargets.put(hs, targetViewpoint);

				List<List<Double>> jpolygon = (List<List<Double>>)jhs.get("polygon");
				for (List<Double> point: jpolygon)
				{
					double yaw = point.get(0);
					double pitch = point.get(1);

					hs.polygon.add(new SphereCoord3f((float)yaw, (float)pitch, 1.0f));
				}

				vp.hotspots.add(hs);
			}

			//Overlay specs (optional)
			List<JSONObject> joverlays = (List<JSONObject>)jvp.get("overlays");
			if (joverlays != null)
			{
				for (JSONObject joverlay: joverlays)
					vp.overlays.add(overlaySpecRegistry.loadFromJSON(joverlay, fileResolver));
			}

			gw.addViewpoint(vp);
			viewpointsById.put(vp.id, vp);
		}

		//Connect up hotspot target references
		for (Map.Entry<PanoHotspot, String> e: hotspotTargets.entrySet())
		{
			Viewpoint target = viewpointsById.get(e.getValue());
			if (target == null)
				System.out.printf("Unable to hotspot target: \"%s\"\n", e.getValue());
			else
				e.getKey().targetViewpoint = target;
		}

		return gw;
	}

	public void saveSpec(File specFile)
	{

	}

	public void loadState(File stateFile)
	{


	}

	public void saveState(File stateFile)
	{

	}
}
