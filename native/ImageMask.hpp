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
#ifndef ImageMask_H
#define ImageMask_H

#include "misc/Rect4i.hpp"
#include "misc/ArrayList.hpp"
#include "GLImage.hpp"

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

