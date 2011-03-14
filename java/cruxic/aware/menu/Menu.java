package cruxic.aware.menu;

import java.util.List;

/**

 */
public interface Menu
{
	public Object getId();
	public String getText();
	public boolean hasSubmenu();
	public List<Menu> getSubmenu();
}
