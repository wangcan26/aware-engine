package cruxic.aware.menu;

import java.util.*;

/**
	A leaf node in the menu hierarchy (has no submenu).
 */
public abstract class AbstractMenu implements Menu
{
	protected final Object id;
	protected String text;

	public AbstractMenu(Object id, String text)
	{
		this.id = id;
		this.text = text;
	}

	public final Object getId()
	{
		return id;
	}

	public String getText()
	{
		return text;
	}
}