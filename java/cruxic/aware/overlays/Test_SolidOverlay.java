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
package cruxic.aware.overlays;

import cruxic.aware.*;
import cruxic.aware.ipo.*;
import cruxic.math.Rect4i;

/**

 */
public class Test_SolidOverlay implements Overlay
{
	private final Test_SolidOverlaySpec ospec;
	private final ManualIPOTimeSource timeSource;
	private final IPOCurve hueCurve;
	private final Rect4i rect;


	Test_SolidOverlay(Test_SolidOverlaySpec ospec,
		ManualIPOTimeSource timeSource,
		IPOCurve hueCurve,
		Rect4i rect)
	{
		this.ospec = ospec;
		this.timeSource = timeSource;
		this.hueCurve = hueCurve;
		this.rect = rect;
	}

	public OverlaySpec getSpec()
	{
		return ospec;
	}

	public void update(float elapsedSeconds, GLImage image, int offsetX, int offsetY)
	{
		//update the manual time source
		timeSource.setCurrentTime(elapsedSeconds);

		byte[] rgb = new byte[3];
		hueToRGB(hueCurve.currentValue(), rgb);

		image.fillRect(offsetX, offsetY, offsetX + rect.width(), offsetY + rect.height(),
			rgb[0], rgb[1], rgb[2]);

//		GLImage solid = GLImage.allocateBlank(rect.width(), rect.height(), (byte)128);
//		image.copyRect(solid, offsetX, offsetY, offsetX + rect.width(), offsetY + rect.height());
//		solid.dispose();

	}

	public void dispose()
	{
		//no native resources allocated by this
	}

	public boolean isOpaque()
	{
		return true;
	}

	public Rect4i getRect()
	{
		return rect;
	}

	/**Select between all possible colors hues.
	 @param hue a number on the range [0.0, 6.0)
	 */
	private static void hueToRGB(float hue, byte[] rgbResult)
	{
		//RGB from [0.0,1.0]
		float r, g, b;

		//sanitize input
		if (hue < 0.0f)
			hue = 0.0f;
		else if (hue >= 6.0f)
			hue = 6.0f - 0.00001f;

		int phase = (int)hue;  //faster than Math.floor
		float amount = hue - phase;

		switch (phase)
		{
			//0-1: red-yellow
			case 0:
				r = 1.0f;
				g = amount;
				b = 0.0f;
				break;
			//1-2: yellow-green
			case 1:
				r = 1.0f - amount;
				g = 1.0f;
				b = 0.0f;
				break;
			//2-3: green-cyan
			case 2:
				r = 0.0f;
				g = 1.0f;
				b = amount;
				break;
			//3-4: cyan-blue
			case 3:
				r = 0.0f;
				g = 1.0f - amount;
				b = 1.0f;
				break;
			//4-5: blue-purple
			case 4:
				r = amount;
				g = 0.0f;
				b = 1.0f;
				break;
			//5-6: purple-red
			default:
				r = 1.0f;
				g = 0.0f;
				b = 1.0f - amount;
				break;
		}

		rgbResult[0] = (byte)(r * 255.0f);
		rgbResult[1] = (byte)(g * 255.0f);
		rgbResult[2] = (byte)(b * 255.0f);
	}

}
