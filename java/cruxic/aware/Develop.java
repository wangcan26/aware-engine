package cruxic.aware;


/**

 */
public class Develop
{
	/**when non-null the user is adding a hotspot*/
	public PanoHotspot new_hotspot;

	public boolean delete_next_hotspot;
	
	public boolean link_next_hotspot;

	public PanoHotspot hotspot_to_link;

	public StringBuilder console_text;

	public Develop()
	{
		console_text = new StringBuilder(0);
	}

}
