package cruxic.aware.menu;

import java.util.*;

/**
	A leaf node in the menu hierarchy (has no submenu).
 */
public class SubMenu extends AbstractMenu
{
	private List<Menu> submenus;

	public SubMenu(Object id, String text)
	{
		super(id, text);
		submenus = new LinkedList<Menu>();
	}

	public List<Menu> getSubmenu()
	{
		return submenus;
	}

	public boolean hasSubmenu()
	{
		return !submenus.isEmpty();
	}

	public SubMenu addMenu(SubMenu subMenu)
	{
		submenus.add(subMenu);
		return subMenu;
	}

	public Menu addMenu(Menu menu)
	{
		submenus.add(menu);
		return menu;
	}
}