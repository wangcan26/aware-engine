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
#ifndef Liquid_StaticNoiseOverlay_Impl_H
#define Liquid_StaticNoiseOverlay_Impl_H

#include "StaticNoiseLiquidRefactionRenderer.hpp"
#include "GLImage.hpp"
#include "misc/ArrayList.hpp"

/**

*/
class Liquid_StaticNoiseOverlay_Impl
{
private:
	GLImage tempBuf;

	StaticNoiseLiquidRefactionRenderer * renderer;
public:
	ArrayList<GLImage> masks;
	ArrayList<Rect4i> rects;  //parallel to masks

	Liquid_StaticNoiseOverlay_Impl(GLImage & noise, GLImage & mask, float refactionIndex, float noiseIntensity);
	virtual ~Liquid_StaticNoiseOverlay_Impl(); //virtual destructor prevents insideous memory leak for derived classes

	void update(int overlayIndex, float angle, GLImage & image, int offsetX, int offsetY);

	#ifdef UNIT_TEST
	//static void test(UnitTest & tst);
	#endif
};

#endif	//Liquid_StaticNoiseOverlay_Impl_H

