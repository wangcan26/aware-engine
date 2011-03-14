#ifndef Liquid_StaticNoiseOverlay_Impl_H
#define Liquid_StaticNoiseOverlay_Impl_H

#include "StaticNoiseLiquidRefactionRenderer.hpp"
#include "GLImage.h"
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

