package cruxic.aware.menu;

import cruxic.aware.Engine;


/**
	A leaf node in the menu hierarchy (has no submenu).
 */
public class ToggleMenu extends LeafMenu
{
	private final String paramName;

	public ToggleMenu(Object id, String text, String paramName)
	{
		super(id, text);
		this.paramName = paramName;
	}

	public boolean isOn()
	{
		Object on = Engine.instance.params.get(paramName);
		return on != null && on instanceof Boolean && ((Boolean)on).booleanValue();		
	}

	public String getText()
	{
		return text + (isOn() ? ": On" : ": Off");
	}

	public void toggle()
	{
		Engine.instance.params.put(paramName, !isOn());
	}
}