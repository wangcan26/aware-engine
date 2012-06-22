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
		return Engine.instance.params.getBool(paramName);
	}

	public String getText()
	{
		return text + (isOn() ? ": On" : ": Off");
	}

	public void toggle()
	{
		Engine.instance.params.update(paramName, !isOn());
	}
}