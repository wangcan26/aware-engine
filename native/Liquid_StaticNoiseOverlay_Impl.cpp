#include "Liquid_StaticNoiseOverlay_Impl.hpp"

#include <stdio.h>

#include "ImageMask.hpp"

#ifndef MIN
#define MIN(a, b) (a) < (b) ? (a) : (b)
#endif

/*Liquid_StaticNoiseOverlay_Impl::Liquid_StaticNoiseOverlay_Impl(const Liquid_StaticNoiseOverlay_Impl & toCopy)
{
	This is a constructor!  Initialize member data BEFORE calling the assignment operator.
	*this = copy;	//Just use assignment operator
}*/


/**Convert an image to a height map.  The image is treated as grey-scale.

@param border create a height map which is larger than the source image.  The source
	image will be centered in the heightmap which creates a kind of border.  This border
	is filled with by tiling the soure image.  If your source image was
	created to be tilable (it ought to be) then the heightmap will be seamless.  The whole
	point of this is so that you can call HeightMap.blur() without ruining the seamless
	nature of your heightmap.  (After bluring you should ignore the border region)
*/
HeightMap * imageToHeightMap_tilable(GLImage & img, int border)
{
	HeightMap * hmap = new HeightMap(img.width + border + border, img.height + border + border);

	for (int y = 0; y < hmap->H; y++)
	{
		for (int x = 0; x < hmap->W; x++)
		{
			int imgX = infinite_coord_to_finite(x - border, img.width);
			int imgY = infinite_coord_to_finite(y - border, img.height);

			//TODO: GLImage should be a class with an inline getPixelPtr(x,y) method.
			GLubyte * pixel = &img.texels[((imgY * img.width) + imgX) * img.internalFormat];

			hmap->set(x, y, pixel[0] / 255.0f);
		}
	}

	return hmap;
}


void roundMaskRect(Rect4i & r, const Rect4i & maxRect, int unit)
{
	//Increase width as necessary
	int remainder = unit - (r.width() % unit);
	if (remainder != 0)
	{
		int dist1 = r.x1 - maxRect.x1;
		int dist2 = maxRect.x2 - r.x2;

		//do we have enough room to expand?
		if (remainder <= (dist1 + dist2))
		{
			dist1 = MIN(dist1, remainder);
			r.x1 -= dist1;
			remainder -= dist1;

			dist2 = MIN(dist2, remainder);
			r.x2 += dist2;
		}
	}

	//Increase height as necessary
	remainder = unit - (r.height() % unit);
	if (remainder != 0)
	{
		int dist1 = r.y1 - maxRect.y1;
		int dist2 = maxRect.y2 - r.y2;

		//do we have enough room to expand?
		if (remainder <= (dist1 + dist2))
		{
			dist1 = MIN(dist1, remainder);
			r.y1 -= dist1;
			remainder -= dist1;

			dist2 = MIN(dist2, remainder);
			r.y2 += dist2;
		}
	}

}


Liquid_StaticNoiseOverlay_Impl::Liquid_StaticNoiseOverlay_Impl(GLImage & noise, GLImage & mask, float refactionIndex, float noiseIntensity)
{
	const int BORDER = 1;  //without this border the bluring below would ruin the seamless nature of the image

	HeightMap * noiseMap = imageToHeightMap_tilable(noise, BORDER);

	//images have only 255 steps - make the steps smoother
	noiseMap->blur(2);

	//reduce intensity of water effect
	noiseMap->scaleAll(noiseIntensity);

	renderer = new StaticNoiseLiquidRefactionRenderer(refactionIndex, *noiseMap, BORDER);

	//done with noiseMap
	delete noiseMap;

	//Split the mask into independent rectangles
	ImageMask im(mask);
	im.isolateMaskRegions(rects);

	Rect4i imgRect(0, 0, im.width(), im.height());

	int largestRectArea = 1;

	for (int i = 0; i < rects.size(); i++)
	{
		Rect4i & rect = *rects.items()[i];
		printf("Found rect ");
		rect.debugPrint();

        //UPDATE: this bugfix is now done in Java code
		//bug workaround: For some reason certain widths of rectangles
		//  causes strange gray looking textures after uploading with glTexSubImage2D().
		//  I can avoid the issue by making sure width is an even multiple of 8.  I picked
		//  8 out of the blue and it seems to work even though I don't fully understand the problem.
		//roundMaskRect(rect, imgRect, 8);

		//printf("Rounded to ");
		//rect.debugPrint();

		masks.add(im.copySubrect(rect));

		int area = rect.area();
		if (area > largestRectArea)
			largestRectArea = area;
	}

	fflush(stdout);

	//Allocate a temporary pixel buffer to use in the update() call.
	//It needs to be big enough to hold the largest MaskRect
	tempBuf.allocateBlank(1, largestRectArea, 0);
}

Liquid_StaticNoiseOverlay_Impl::~Liquid_StaticNoiseOverlay_Impl()
{
	delete renderer;
}

void Liquid_StaticNoiseOverlay_Impl::update(int overlayIndex, float angle,
	GLImage & image, int offsetX, int offsetY)
{
	//prevent segfault
	if (overlayIndex < 0 || overlayIndex >= masks.size())
	{
		fprintf(stderr, "Liquid_StaticNoiseOverlay_Impl::update: overlayIndex %d out of range\n", overlayIndex);
		return;
	}

	//printf("overlayIndex=%d\tangle=%g\timage=%dx%d\toffsX=%d\toffsY=%d\n", overlayIndex, angle, image.width, image.height, offsetX, offsetY);
	//fflush(stdout);

	GLImage & mask = *masks.items()[overlayIndex];


	//Copy 'image' to a temporary buffer
	//given image holds the background-image and it is also what we will be updating.  This is
	// problematic for the water effect because we need to read the unmodified background image as we
	// are generating the final buffer.  In short, we need to buffers: the background and the output.
	Rect4i imgRect(offsetX, offsetY, offsetX + mask.width, offsetY + mask.height);
	tempBuf.copyRect(image, imgRect);
	//update dimensions to match 'imageRect'
	tempBuf.width = imgRect.width();
	tempBuf.height = imgRect.height();

	//rotate the noise according to angle
	renderer->updateRotationOffsets(angle);

	//Render the water effect from tempBuf into image
	renderer->renderMaskRect(mask, tempBuf, image, imgRect);
}

#ifdef UNIT_TEST
/*
void Liquid_StaticNoiseOverlay_Impl::test(UnitTest & tst)
{
	tst.startTest("Liquid_StaticNoiseOverlay_Impl");
	tst.endTest();
}*/

#endif

