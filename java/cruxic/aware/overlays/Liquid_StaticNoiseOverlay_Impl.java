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