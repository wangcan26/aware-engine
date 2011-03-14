#ifndef ImageMask_H
#define ImageMask_H

#include "misc/Rect4i.hpp"
#include "misc/ArrayList.hpp"
#include "GLImage.h"

/**

*/
class ImageMask
{
private:
	GLImage & img;

	inline GLubyte * getPixelPtr(int x, int y) const
	{
		return &img.texels[(y * img.width + x) * img.internalFormat];
	};

	/**reduce the size of given subrect so there are no black borders on any of the 4 sides.*/
	void shrinkFitRectToMask(Rect4i & subrect);
	int findSplit(Rect4i & subrect);
	void isolateMaskRegionsRecursive(Rect4i & subrect, ArrayList<Rect4i> & result);
public:
	ImageMask(GLImage & img);
	bool isRowBlack(int y, int xStart, int xEnd);
	bool isColumnBlack(int x, int yStart, int yEnd);
	int width() const;
	int height() const;
	GLImage * copySubrect(const Rect4i & rect) const;

	void isolateMaskRegions(ArrayList<Rect4i> & result);

	#ifdef UNIT_TEST
	//static void test(UnitTest & tst);
	#endif
};


#endif	//ImageMask_H

