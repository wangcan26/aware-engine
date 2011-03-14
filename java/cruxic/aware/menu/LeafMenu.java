package cruxic.aware.menu;

import java.util.*;

/**
	A leaf node in the menu hierarchy (has no submenu).
 */
public class LeafMenu extends AbstractMenu
{
	public LeafMenu(Object id, String text)
	{
		super(id, text);
	}

	public List<Menu> getSubmenu()
	{
		return Collections.EMPTY_LIST;
	}

	public boolean hasSubmenu()
	{
		return false;
	}
}
