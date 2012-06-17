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
#include "StaticNoiseLiquidRefactionRenderer.hpp"

#include <math.h>  //cos/sin

StaticNoiseLiquidRefactionRenderer::StaticNoiseLiquidRefactionRenderer(double refractionIndex,
	HeightMap & hmap, int BORDER)
{
	//minimum scale for height difference values.
	//larger values make waves look bigger
	const float DIFF_SCALE = 500.0f;

	//added onto DIFF_SCALE depending on wave height.  The highest waves (1.0) will
	//add 100% of this value, the lowest waves (0.0) will add 0%
	const float DEPTH_SCALE = 500.0f;

	//Note: width & height of lookup table should not include the border
	W = hmap.W - BORDER - BORDER;
	H = hmap.H - BORDER - BORDER;

	offsetX = 0;
	offsetY = 0;

	table = new DispVal[W * H];
	memset(table, 0, sizeof(DispVal) * W * H);

	for (int y = 0; y < H; y++)
	{
		int nextY = y + 1;
		if (nextY == H)  //on the last row, tile back to the first row
			nextY = 0;

		for (int x = 0; x < W; x++)
		{
			int nextX = x + 1;
			if (nextX == W)  //on the last column, tile back to the first column
				nextX = 0;

			float valueXY = hmap.get(x + BORDER, y + BORDER);
			double scale = DIFF_SCALE + (DEPTH_SCALE * valueXY);
			double xDiff = (hmap.get(nextX + BORDER, y + BORDER) - valueXY) * scale;
			double yDiff = (hmap.get(x + BORDER, nextY + BORDER) - valueXY) * scale;

			double xAngle = atan(xDiff);
			double xRefraction = asin(sin(xAngle) / refractionIndex);
			int xDisplace = (int)(tan(xRefraction) * xDiff);

			double yAngle = atan(yDiff);
			double yRefraction = asin(sin(yAngle) / refractionIndex);
			int yDisplace = (int)(tan(yRefraction) * yDiff);

			DispVal * dispv = &table[(y * W ) + x];

			if (xDiff < 0)
				dispv->displaceX = -xDisplace;  // Current position is higher - Clockwise rotation
			else
				dispv->displaceX = xDisplace;  //Current position is lower - Counterclockwise rotation

			if (yDiff < 0)
				dispv->displaceY = -yDisplace;
			else
				dispv->displaceY = yDisplace;
		}
	}
}


StaticNoiseLiquidRefactionRenderer::~StaticNoiseLiquidRefactionRenderer()
{
	delete[] table;
}

inline int iclip(int i, int minVal, int maxVal)
{
	return i < minVal ? minVal : (i > maxVal ? maxVal : i);
}

void StaticNoiseLiquidRefactionRenderer::updateRotationOffsets(double angle)
{
	//Compute the maximum rotation radius we can use without having to repeat the noise texture
	//float radius = (int)((W - backgroundImage->width) / 2.0f) - 2;  //2 corrects for possible off-by-one issues
	//offsetX = radius - (cos(angle) * radius);
	//offsetY = radius + (sin(angle) * radius);

	float radius = W * 4;

	offsetX = cos(angle) * radius;
	offsetY = sin(angle) * radius;
}


void StaticNoiseLiquidRefactionRenderer::renderNoiseMap(GLImage & outputImage)
{
	//GLubyte * background = backgroundImage->texels;
	GLubyte * output = outputImage.texels;

	int imgW = outputImage.width;
	int imgH = outputImage.height;

	//int rowStride = imgW * 3;

// 	static int mi = 0;
// 	static int ma = 0;

	const int range = 10;


	for (int y = 0; y < imgH; y++)
	{
		int ly = infinite_coord_to_finite(y + offsetY, H);
		//int ly = y + offsetY;
		int lookupIndex = ly * W;

		for (int x = 0; x < imgW; x++)
		{
			int lx = infinite_coord_to_finite(x + offsetX, W);
			//int lx = x + offsetX;

			int idx = lookupIndex + lx;

			output[0] = 0;
			output[1] = ((table[idx].displaceX + range) / (float)(range + range)) * 255.0f;
			output[2] = ((table[idx].displaceY + range) / (float)(range + range)) * 255.0f;

			output += 3;

// 			if (table[idx].displaceX < mi)
// 				mi = table[idx].displaceX;
// 			if (table[idx].displaceY < mi)
// 				mi = table[idx].displaceY;
//
// 			if (table[idx].displaceX > ma)
// 				ma = table[idx].displaceX;
// 			if (table[idx].displaceY > ma)
// 				ma = table[idx].displaceY;
		}
	}

//	printf("%d %d\n", mi, ma);
}



void StaticNoiseLiquidRefactionRenderer::renderWater(GLImage & backgroundImage, GLImage & outputImage)
{
	//prevent segfault
	if (backgroundImage.width != outputImage.width
		|| backgroundImage.height != outputImage.height)
	{
		fprintf(stderr, "StaticNoiseLiquidRefactionRenderer::renderWater: precondition failed!\n");
		return;
	}

	GLubyte * background = backgroundImage.texels;
	GLubyte * output = outputImage.texels;

	int imgW = backgroundImage.width;
	int imgH = backgroundImage.height;

	int rowStride = imgW * 3;

	for (int y = 0; y < imgH; y++)
	{
		int ly = infinite_coord_to_finite(y + offsetY, H);
		//int ly = y + offsetY;
		int lookupIndex = ly * W;

		for (int x = 0; x < imgW; x++)
		{
			int lx = infinite_coord_to_finite(x + offsetX, W);
			//int lx = x + offsetX;

			int idx = lookupIndex + lx;

			int bgX = x + table[idx].displaceX;
			int bgY = y + table[idx].displaceY;

			//Keep values in range
			bgX = iclip(bgX, 0, imgW - 1);
			bgY = iclip(bgY, 0, imgH - 1);

			int bgOffset = (bgY * rowStride) + (bgX * 3);

			memcpy(output, &background[bgOffset], 3);  //compiles to CPU instruction
			output += 3;
		}
	}
}

void StaticNoiseLiquidRefactionRenderer::renderMaskRect(GLImage & mask, const GLImage & background,
	GLImage & output, const Rect4i & outputRect)
{
	//mask rect must match outputRect
	if (mask.width != outputRect.width() || mask.height != outputRect.height())
	{
		fprintf(stderr, "StaticNoiseLiquidRefactionRenderer::renderMaskRect: mask rect does not match outputRect dimesions\n");
		return;
	}

	//background must be same dimension as mask
	if (mask.width != background.width || mask.height != background.height)
	{
		fprintf(stderr, "StaticNoiseLiquidRefactionRenderer::renderMaskRect: mask rect does not match background dimesions\n");
		return;
	}

	//pixel formats must match
	if (background.internalFormat != output.internalFormat)
	{
		fprintf(stderr, "StaticNoiseLiquidRefactionRenderer::renderMaskRect: pixel format mismatch\n");
		return;
	}

	//for each row in mask...
	for (int y = 0; y < mask.height; y++)
	{
		int ly = infinite_coord_to_finite(y + offsetY, H);
		int lookupIndex = ly * W;

		GLubyte * outputPtr = output.getPixelPtr(outputRect.x1,  outputRect.y1 + y);
		GLubyte * maskPtr = mask.getRowPtr(y);

		//for each pixel in mask row...
		for (int x = 0; x < mask.width; x++)
		{
			int bgX = x;
			int bgY = y;

			GLubyte maskValue = *maskPtr;  //same as mask.getPixelPtr(x, y)
			maskPtr++;

			if (maskValue > 0)
			{
				int lx = infinite_coord_to_finite(x + offsetX, W);
				int idx = lookupIndex + lx;

				bgX = x + table[idx].displaceX;
				bgY = y + table[idx].displaceY;

				//Make sure displacement is still inside of image AND
				// never refract light to a pixel that is not water because it looks weird.
				if (bgX < 0 || bgX >= mask.width
					|| bgY < 0 || bgY >= mask.height
					|| *mask.getPixelPtr(bgX, bgY) == 0)
				{
					//fallback to no refaction
					bgX = x;
					bgY = y;
				}
			}
			//else leave pixel alone

			//copy pixel from background[bgX,bgY] to mask
			memcpy(outputPtr, background.getPixelPtr(bgX, bgY), background.internalFormat);

			outputPtr += background.internalFormat;
		}
	}
}



#ifdef UNIT_TEST
/*
void StaticNoiseLiquidRefactionRenderer::test(UnitTest & tst)
{
	tst.startTest("StaticNoiseLiquidRefactionRenderer");
	tst.endTest();
}*/

#endif

