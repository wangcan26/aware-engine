package cruxic.aware.overlays;

import cruxic.aware.*;
import cruxic.aware.ipo.*;
import cruxic.math.CrxMath;

import java.util.*;

import org.json.simple.JSONObject;

/**

 */
public class Liquid_StaticNoiseOverlaySpec implements OverlaySpec
{
	private final String name;
	public final String noiseImage;
	public final String maskImage;

	public float refractionIndex;

	/**scales strength of the ripples*/
	public float noiseIntensity;

	/**noise rotation rate scale factor*/
	public float rippleSpeed;

	public Liquid_StaticNoiseOverlaySpec(String name,
		String noiseImage,
		String maskImage)
	{
		this.name = name;
		this.noiseImage = noiseImage;
		this.maskImage = maskImage;

		this.noiseIntensity = 1.0f;
		this.rippleSpeed = 1.0f;
		this.refractionIndex = 2.0f;
	}

	public static Liquid_StaticNoiseOverlaySpec loadFromJSON(JSONObject node, ResourceManager.FileResolver fileResolver)
	  throws IOExceptionRt
	{
		String name = (String)node.get("name");
		String noiseImage = (String)node.get("noiseImage");
		String maskImage = (String)node.get("maskImage");

		assert name != null;
		assert noiseImage != null;
		assert maskImage != null;

		noiseImage = fileResolver.resolveImageId(noiseImage);
		maskImage = fileResolver.resolveImageId(maskImage);

		Liquid_StaticNoiseOverlaySpec spec = new Liquid_StaticNoiseOverlaySpec(name, noiseImage, maskImage);

		//Optional parameters
		Number noiseIntensity = (Number)node.get("noiseIntensity");
		Number rippleSpeed = (Number)node.get("rippleSpeed");
		Number refractionIndex = (Number)node.get("refractionIndex");

		if (noiseIntensity != null)
			spec.noiseIntensity = noiseIntensity.floatValue();
		if (rippleSpeed != null)
			spec.rippleSpeed = rippleSpeed.floatValue();
		if (refractionIndex != null)
			spec.refractionIndex = refractionIndex.floatValue();

		return spec;
	}

	public String getName()
	{
		return name;
	}

	public List<Overlay> loadOverlays(Engine engine)
	{
		GLImage noise = engine.texCache.getGLImage(noiseImage);
		GLImage mask = engine.texCache.getGLImage(maskImage);

		Liquid_StaticNoiseOverlay_Impl impl = new Liquid_StaticNoiseOverlay_Impl(this, noise, mask);

		List<Overlay> overlays = new ArrayList<Overlay>(impl.numOverlays);

		for (int overlayIndex = 0; overlayIndex < impl.numOverlays; overlayIndex++)
			overlays.add(new Liquid_StaticNoiseOverlay(overlayIndex, impl));

		return overlays;
	}

	public List<String> getImageResources()
	{
		ArrayList<String> images = new ArrayList<String>(2);
		images.add(maskImage);
		images.add(noiseImage);
		return images;
	}
}
