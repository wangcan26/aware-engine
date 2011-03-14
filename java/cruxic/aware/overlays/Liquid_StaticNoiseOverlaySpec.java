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
