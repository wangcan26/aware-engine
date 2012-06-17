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
#ifndef StaticNoiseLiquidRefactionRenderer_H
#define StaticNoiseLiquidRefactionRenderer_H

#include "HeightMap.hpp"
#include "misc/ArrayList.hpp"
#include "GLImage.hpp"

typedef struct _DispVal
{
	short displaceX;
	short displaceY;
} DispVal;



/**
    Renders a water refaction effect as light shines through a noise heighmap
    which is moved around over a background image.
*/
class StaticNoiseLiquidRefactionRenderer
{
private:
public:
private:
	DispVal * table;

	int offsetX;
	int offsetY;
public:
	int W;
	int H;

	StaticNoiseLiquidRefactionRenderer(double refractionIndex, HeightMap & hmap, int BORDER = 0);
	virtual ~StaticNoiseLiquidRefactionRenderer();

	void renderWater(GLImage & backgroundImage, GLImage & outputImage);
	//void renderWaterMasked(GLImage & backgroundImage, ArrayList<MaskRect> & masks);
	void renderMaskRect(GLImage & mask, const GLImage & background, GLImage & output, const Rect4i & outputRect);
	void renderNoiseMap(GLImage & outputImage);

	void updateRotationOffsets(double angle);

	#ifdef UNIT_TEST
	//static void test(UnitTest & tst);
	#endif
};

/**maps a coordinate, 'x', on an infinitely large image, to a coordinate
on a finite image of size 'dimension'*/
inline int infinite_coord_to_finite(int x, int dimension)
{
	x = x % dimension;
	if (x < 0)
		return dimension + x;
	else
		return x;
}

#endif	//StaticNoiseLiquidRefactionRenderer_H

