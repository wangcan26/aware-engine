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

	private static String getRelativeBaseDir(File someDir)
	{
		try
		{
			String cwd = new File(".").getCanonicalPath();
			String gameBaseDir = someDir.getAbsolutePath();
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

	private static Properties readProperties(File pfile)
		throws IOExceptionRt
	{
		Properties props = new Properties();
		try
		{
			FileInputStream fis = new FileInputStream(pfile);
			try
			{
				props.load(fis);
			}
			finally
			{
				fis.close();
			}
		}
		catch (IOException ioe)
		{
			throw new IOExceptionRt(ioe);
		}

		return props;
	}

	private static boolean isPropTrue(String property_value)
	{
		return property_value != null
			&& (property_value.toLowerCase().equals("true") || property_value.equals("1"));
	}

	/**Sort imageIds in this order: front, back, left, right, top, bottom*/
	private static List<String> sortCubeImages_FBLRTB(Collection<String> imageIds)
		throws IllegalArgumentException
	{
		String[] six = new String[6];

		for (String imgId: imageIds)
		{
			int idx = getCubicFaceIndex(imgId);
			if (idx != -1)
			{
				if (six[idx] != null)
					throw new IllegalArgumentException("Duplicate cube face image: " + imgId);
				else
					six[idx] = imgId;
			}
		}

		List<String> result = new ArrayList<String>(6);
		for (String imgId: six)
		{
			if (imgId == null)
				throw new IllegalArgumentException("Missing one or more cube-face images.  Make sure you have image names starting with: front, back, left, right, top, and bottom");
			result.add(imgId);
		}

		return result;
	}

	private static int getCubicFaceIndex(String imgId)
	{
		int n;
		if (imgId.contains("front"))
			n = 0;
		else if (imgId.contains("back"))
			n = 1;
		else if (imgId.contains("left"))
			n = 2;
		else if (imgId.contains("right"))
			n = 3;
		else if (imgId.contains("top"))
			n = 4;
		else if (imgId.contains("bottom"))
			n = 5;
		else
			n = -1;

		return n;
	}


	public static GameWorld load_game_data(File game_data_dir)
		throws IOExceptionRt
	{
		//Read game.properties
		Properties game_props = readProperties(new File(game_data_dir, "game.properties"));


		String baseDir = getRelativeBaseDir(game_data_dir);

		ResourceManager.FileResolver fileResolver = new ResourceManager.FileResolver(baseDir);

		GameWorld gw = new GameWorld();

		IdentityHashMap<PanoHotspot, String> hotspotTargets = new IdentityHashMap<PanoHotspot, String>(128);
		Map<String, Viewpoint> viewpointsById = new HashMap<String, Viewpoint>(128);

		File[] vpDirs = game_data_dir.listFiles();

		for (File vpDir: vpDirs)
		{
			//Skip normal files
			if (!vpDir.isFile())
			{
				Properties vpProps;
				File vpPropFile = new File(vpDir, "viewpoint.properties");
				if (vpPropFile.isFile())
					vpProps = readProperties(vpPropFile);
				else
					vpProps = new Properties();

				String vpId = vpDir.getName();

				File[] vpFiles = vpDir.listFiles();
				Set<String> imageFiles = new HashSet<String>();
				String eqr_pano_imgId = null;
				for (File f: vpFiles)
				{
					String fname = f.getName();
					if (fname.endsWith(".png"))
					{
						String relpath = vpDir.getName() + File.separatorChar + fname;
						String imgId = fileResolver.resolveImageId(relpath);
						imageFiles.add(imgId);

						if (fname.equals("eqr_pano.png"))
							eqr_pano_imgId = imgId;

					}
				}

				boolean cubic = eqr_pano_imgId == null && imageFiles.size() > 1;

				PanoViewpoint vp;

				if (cubic)
				{
					vp = new CubicViewpoint(vpId);
				}
				else
				{
					vp = new EquirectViewpoint(vpId);
				}

				vp.implicitBackLink = isPropTrue(vpProps.getProperty("implicitBackLink"));

				//Images
				if (cubic)
					vp.imageIds = sortCubeImages_FBLRTB(imageFiles);
				else
					vp.imageIds.add(eqr_pano_imgId);  //assume the first

				//Hotspots
				for (int n = 1; n < 1000; n++)
				{
					String hsKey = "hotspot_" + n;
					if (vpProps.containsKey(hsKey))
					{
						String val = vpProps.getProperty(hsKey).trim();

						PanoHotspot hs = new PanoHotspot(hsKey);
						boolean parse_error = false;

						int cidx = val.indexOf('[');
						if (cidx > 0)
						{
							String target = val.substring(0, cidx).trim();

							if (!target.startsWith("../"))
								throw new IOExceptionRt(String.format("Error parsing %s for viewpoint \"%s\": target is missing \"../\"", hsKey, vpId));
							else
								target = target.substring(3);

							hotspotTargets.put(hs, target);
						}
						else if (cidx != 0 || (cidx + 3) > val.length()
							|| !val.endsWith("]"))
							parse_error = true;

						if (!parse_error)
						{
							String[] parts = val.substring(cidx + 1, val.length() - 1).split(",");

							int nDoubles = 0;
							double yaw = 0.0;
							double pitch = 0.0;

							for (String part: parts)
							{
								part = part.trim();
								if (part.length() == 0)
								{
									parse_error = true;
									break;
								}

								try
								{
									double d = Double.parseDouble(part);

									//even?
									if ((nDoubles & 1) == 0)
									{
										yaw = d;
									}
									else
									{
										pitch = d;
										hs.polygon.add(new SphereCoord3f((float)yaw, (float)pitch, 1.0f));
									}

									nDoubles++;
								}
								catch (NumberFormatException nfe)
								{
									parse_error = true;
									break;
								}
							}

							if (nDoubles < 2 || nDoubles % 2 != 0)
								parse_error = true;
						}

						if (!parse_error)
							vp.hotspots.add(hs);
						else
						{
							throw new IOExceptionRt(String.format("Error parsing %s for viewpoint \"%s\"", hsKey, vpId));
						}
					}
					else
						break;
				}

				gw.addViewpoint(vp);
				viewpointsById.put(vp.id, vp);

			}
		}

		//Connect up hotspot target references
		for (Map.Entry<PanoHotspot, String> e: hotspotTargets.entrySet())
		{
			Viewpoint target = viewpointsById.get(e.getValue());
			if (target == null)
				System.out.printf("Unable to find hotspot target: \"%s\"\n", e.getValue());
			else
				e.getKey().targetViewpoint = target;
		}

		//Set active viewpoint to starting_viewpoint
		String starting_viewpoint_id = game_props.getProperty("starting_viewpoint");
		if (starting_viewpoint_id != null)
		{
			boolean found = false;
			for (Viewpoint vp: gw.viewpoints)
			{
				if (vp.getId().equals(starting_viewpoint_id))
				{
					gw.active = vp;
					found = true;
					break;
				}
			}

			if (!found)
				System.err.printf("Unable to find starting_viewpoint \"%s\"\n", starting_viewpoint_id);
		}

		return gw;
	}

	public static void copyFile(File sourceFile, File destFile) throws IOException {
    if(!destFile.exists()) {
        destFile.createNewFile();
    }

    java.nio.channels.FileChannel source = null;
    java.nio.channels.FileChannel destination = null;

    try {
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        destination.transferFrom(source, 0, source.size());
    }
    finally {
        if(source != null) {
            source.close();
        }
        if(destination != null) {
            destination.close();
        }
    }
}

	public static void convertSpec(File specFile)
		throws IOExceptionRt
	{
		JSONObject root = parseJSONFile(specFile);

		File newDir = new File(specFile.getParentFile(), "converted");

		String baseDir = getRelativeBaseDir(specFile);

		List<JSONObject> jviewpoints = (List<JSONObject>)root.get("viewpoints");
		for (JSONObject jvp: jviewpoints)
		{
			Properties props = new Properties();

			File vpDir = new File(newDir, (String)jvp.get("id"));
			vpDir.mkdirs();

			System.out.println(vpDir);

			PanoViewpoint vp = new EquirectViewpoint((String)jvp.get("id"));

			if ((Boolean)jvp.get("implicitBackLink"))
				props.put("implicitBackLink", "true");

			//Images
			for (String imgId: (List<String>)jvp.get("images"))
			{
				File imageFile = new File(specFile.getParent(), imgId + ".png");
				if (!imageFile.exists())
					throw new RuntimeException(imageFile.getPath());

				File target = new File(vpDir, "eqr_pano.png");
				target.delete();

				try
				{
					copyFile(imageFile, target);
				}
				catch (IOException e)
				{
					throw new IOExceptionRt(e);
				}
			}

			//Hotspots
			List<JSONObject> jhss = (List<JSONObject>)jvp.get("hotspots");
			int n = 1;
			for (JSONObject jhs: jhss)
			{
				StringBuilder sb = new StringBuilder();

				String targetViewpoint = (String)jhs.get("targetViewpoint");
				sb.append("../");
				sb.append(targetViewpoint);
				sb.append(" [");


				List<List<Double>> jpolygon = (List<List<Double>>)jhs.get("polygon");
				boolean first = true;
				for (List<Double> point: jpolygon)
				{
					double yaw = point.get(0);
					double pitch = point.get(1);

					if (first)
						first = false;
					else
						sb.append(", ");

					sb.append(yaw);
					sb.append(',');
					sb.append(pitch);
				}
				sb.append(']');

				props.put("hotspot_" + n, sb.toString());

				n++;
			}



			File propFile = new File(vpDir, "viewpoint.properties");
			try
			{
				FileOutputStream fos = new FileOutputStream(propFile);
				props.store(fos, null);
				fos.close();
			}
			catch (IOException ioe)
			{
				throw new IOExceptionRt(ioe);
			}
		}
	}

	public void loadState(File stateFile)
	{


	}

	public void saveState(File stateFile)
	{

	}

	public Viewpoint getNextViewpointInCycle(Viewpoint currentVp)
	{
		Iterator<Viewpoint> itr = viewpoints.iterator();
		while (itr.hasNext())
		{
			if (itr.next() == currentVp)
			{
				if (itr.hasNext())
					return itr.next();
				else
					return viewpoints.get(0);
			}
		}

		//given viewpoint not found
		return null;
	}
}
