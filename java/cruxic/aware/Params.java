package cruxic.aware;

import java.util.*;
import java.io.*;

/**

 */
public class Params
{
	private final Map<String, Object> map;
	private final File propFile;

	public Params(File paramFile)
		throws IOExceptionRt
	{
		map = new HashMap<String, Object>(32);
		this.propFile = paramFile;
		if (propFile.isFile())
			loadFromFile(paramFile);
		else
			loadDefaults();
	}

	public Params()
	{
		map = new HashMap<String, Object>(32);
		loadDefaults();
		this.propFile = null;
	}

	public void loadFromFile(File paramFile)
		throws IOExceptionRt
	{
		Properties props = new Properties();

		try
		{
			FileInputStream fis = new FileInputStream(paramFile);
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

		//Only load the properties for which a default has been specified because that tells us
		//what the datatype is
		loadDefaults();
		for (Map.Entry<String, Object> e: map.entrySet())
		{
			String key = e.getKey();
			String prop_value = props.getProperty(key);
			if (prop_value != null)
			{
				Object default_value = e.getValue();

				try
				{
					if (default_value instanceof Boolean)
						map.put(key, prop_value.length() > 0 && (prop_value.equals("true") || prop_value.equals("1")));
					else if (default_value instanceof Integer)
						map.put(key, Integer.parseInt(prop_value.trim()));
					else if (default_value instanceof Long)
						map.put(key, Long.parseLong(prop_value.trim()));
					else if (default_value instanceof Double)
						map.put(key, Double.parseDouble(prop_value.trim()));
					else if (default_value instanceof String)
						map.put(key, prop_value);
					else  //default_value is null?
						System.out.println("Don't know how to handle data type for key: " + key);

				}
				catch (NumberFormatException nfe)
				{
					System.out.printf("Bad property integer value: %s=%s (default will be used instead)\n", key, prop_value);
				}
			}
		}
	}

	public void loadDefaults()
	{
		map.clear();
		map.put("renderer.show_fps", false);
		map.put("renderer.show_geom", false);
		map.put("renderer.show_hotspots", false);
		map.put("renderer.label_hotspots", false);
		map.put("devel.cycle_viewpoints", false);
		map.put("devel.enable", true);
	}

	public void update(String key, boolean value)
	{
		map.put(key, value);
		try
		{
			updateFile();
		}
		catch (IOExceptionRt ioe)
		{
			ioe.printStackTrace();
		}
	}

	public boolean getBool(String key)
	{
		Object val = map.get(key);
		return val instanceof Boolean && ((Boolean)val).booleanValue();
	}

	/**Write all changes back to the file this class was constructed with*/
	public void updateFile()
		throws IOExceptionRt
	{
		if (propFile != null)
		{
			Properties props = new Properties();
			for (Map.Entry<String, Object> e: map.entrySet())
			{
				if (e.getValue() != null)
					props.setProperty(e.getKey(), e.getValue().toString());
			}

			try
			{
				FileOutputStream fos = new FileOutputStream(propFile);
				try
				{
					props.store(fos, null);
				}
				finally
				{
					fos.close();
				}
			}
			catch (IOException ioe)
			{
				throw new IOExceptionRt(ioe);
			}
		}
	}


}
