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
import cruxic.math.*;

/**
	A front-end to the native code behind Liquid_StaticNoiseOverlay.
 */
class Liquid_StaticNoiseOverlay_Impl
{
	/**pointer to native code.  0 means the object is disposed*/
	//KEEP: referenced by native code
	private long nativePtr;

	Liquid_StaticNoiseOverlaySpec ospec;
	ManualIPOTimeSource timeSource;
	IPOCurve noiseRotationAngle;

	final int numOverlays;

	Liquid_StaticNoiseOverlay_Impl(Liquid_StaticNoiseOverlaySpec ospec, GLImage noise, GLImage mask)
	{
		this.ospec = ospec;
		timeSource = new ManualIPOTimeSource();
		noiseRotationAngle = new RepeatingIPO(new LinearIPO(0.0f, CrxMath.M_2PIf, 220.0f * ospec.rippleSpeed, timeSource));

		numOverlays = init(noise, mask, ospec.refractionIndex, ospec.noiseIntensity);
		assert nativePtr != 0;
	}

	private native int init(GLImage noise, GLImage mask, float refactionIndex, float noiseIntensity);

	native void update(int overlayIndex, float angle, GLImage image, int offsetX, int offsetY);

	native void dispose();

	public native Rect4i getRect(int overlayIndex);

	public void finalize()
		throws Throwable
	{
		//free native ram if necessary
		if (nativePtr != 0)
			dispose();

		super.finalize();
	}
}