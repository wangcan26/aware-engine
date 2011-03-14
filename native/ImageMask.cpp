#include "ImageMask.hpp"


/*ImageMask::ImageMask(const ImageMask & toCopy)
{
	This is a constructor!  Initialize member data BEFORE calling the assignment operator.
	*this = copy;	//Just use assignment operator
}*/


ImageMask::ImageMask(GLImage & anImage)
	: img(anImage)
{
    if (img.internalFormat != 1 && img.internalFormat != 4)
    {
        fprintf(stderr, "ImageMask: mask has %d channels!  (Expected 1 or 4)\n", img.internalFormat);
        if (img.internalFormat == 2)
            fprintf(stderr, "\t(You probably forgot to remove the alpha channel)\n");
    }
}

int ImageMask::width() const
{
	return img.width;
}

int ImageMask::height() const
{
	return img.height;
}


bool ImageMask::isRowBlack(int y, int xStart, int xEnd)
{
	//start on first alpha byte
	GLubyte * alpha = &getPixelPtr(xStart, y)[img.internalFormat - 1];  //internalFormat is 1 for grayscale, 4 for RGBA

	for (int x = xStart; x < xEnd; x++)
	{
		if (*alpha == 0)
			alpha += img.internalFormat;
		else
			return false;
	}

	//all black
	return true;
}

bool ImageMask::isColumnBlack(int x, int yStart, int yEnd)
{
	//start on first alpha byte
	GLubyte * alpha = &getPixelPtr(x, yStart)[img.internalFormat - 1];  //internalFormat is 1 for grayscale, 4 for RGBA

	int stride = img.width * img.internalFormat;

	for (int y = yStart; y < yEnd; y++)
	{
		if (*alpha == 0)
			alpha += stride;
		else
			return false;
	}

	//all black
	return true;
}

GLImage * ImageMask::copySubrect(const Rect4i & rect) const
{
	GLImage * copy = new GLImage(rect.width(), rect.height(), 1, false);

	//TODO: if both are 1 byte per pixel just do a single memcpy

	GLubyte * nextPixel = copy->texels;
	for (int y = rect.y1; y < rect.y2; y++)
	{
		for (int x = rect.x1; x < rect.x2; x++)
		{
			GLubyte alpha = getPixelPtr(x, y)[img.internalFormat - 1];  //internalFormat is 1 for grayscale, 4 for RGBA
			*nextPixel = alpha;
			nextPixel++;
		}
	}

	return copy;
}


/**reduce the size of given subrect so there are no black borders on any of the 4 sides.*/
void ImageMask::shrinkFitRectToMask(Rect4i & subrect)
{
	//Move bottom edge upward
	while (subrect.y1 < subrect.y2
		&& isRowBlack(subrect.y1, subrect.x1, subrect.x2))
	{
		subrect.y1++;
	}

	//Move top edge downward
	while (subrect.y2 > subrect.y1
		&& isRowBlack(subrect.y2 - 1, subrect.x1, subrect.x2))
	{
		subrect.y2--;
	}

	//Move left edge towards right
	while (subrect.x1 < subrect.x2
		&& isColumnBlack(subrect.x1, subrect.y1, subrect.y2))
	{
		subrect.x1++;
	}

	//Move right edge towards left
	while (subrect.x2 > subrect.x1
		&& isColumnBlack(subrect.x2 - 1, subrect.y1, subrect.y2))
	{
		subrect.x2--;
	}
}

int ImageMask::findSplit(Rect4i & subrect)
{
	//search for horizontal split first
	for (int y = subrect.y1 + 1; y < (subrect.y2 - 1); y++)
	{
		if (isRowBlack(y, subrect.x1, subrect.x2))
			return y;
	}

	//otherwise search for vertical split
	for (int x = subrect.x1 + 1; x < (subrect.x2 - 1); x++)
	{
		if (isColumnBlack(x, subrect.y1, subrect.y2))
			return -x;  //negative value signals a vertical split
	}

	//no split found
	return 0;
}

void ImageMask::isolateMaskRegionsRecursive(Rect4i & subrect, ArrayList<Rect4i> & result)
{
	//Don't even try to split rectangles smaller than this
	const int MIN_AREA = 32 * 32;

	//Avoid tiny rectangles unless they save us at least this many pixels
	const int SIGNIFICANT_AREA_REDUCTION = 64 * 64;

	//Don't bother splitting if it's guaranteed to create rectangles below threshold area
	if (subrect.area() > MIN_AREA * 2)
	{
		//try split the rect into 2 pieces
		int split = findSplit(subrect);

		//printf("split = %d\n", split);

		if (split != 0)  //zero means no split found
		{
			//Remember: split value is exclusive of the first rect
			Rect4i r1;
			Rect4i r2;

			//horizontal split
			if (split > 0)
			{
				//create the 2 halves (bottom and top)
				r1.set(subrect.x1, subrect.y1, subrect.x2, split);
				r2.set(subrect.x1, split, subrect.x2, subrect.y2);
			}
			//vertical split
			else
			{
				//make positive (sign merely indicated split orientation)
				split = -split;

				//create the 2 halves (left and right)
				r1.set(subrect.x1, subrect.y1, split, subrect.y2);
				r2.set(split, subrect.y1, subrect.x2, subrect.y2);
			}

			shrinkFitRectToMask(r1);
			shrinkFitRectToMask(r2);

			//printf("split = %d\tr1 area %d\tr2 area %d\n", split, r1.area(), r2.area());

			//Both are of good size?
			if (r1.area() > MIN_AREA && r2.area() > MIN_AREA)
			{
				isolateMaskRegionsRecursive(r1, result);
				isolateMaskRegionsRecursive(r2, result);
			}
			//One or both rects are small.  Only keep them if we significantly reduced area
			else if (subrect.area() - (r1.area() + r2.area()) > SIGNIFICANT_AREA_REDUCTION)
			{
				isolateMaskRegionsRecursive(r1, result);
				isolateMaskRegionsRecursive(r2, result);
			}
			//Not a helpful split
			else
				result.add(new Rect4i(subrect));
		}
		//no more splits possible, keep the subrect
		else
			result.add(new Rect4i(subrect));

	}
	else
		result.add(new Rect4i(subrect));
}


void ImageMask::isolateMaskRegions(ArrayList<Rect4i> & result)
{
	//empty the list
	result.clear();

	//make rect of entire image
	Rect4i all(0, 0, width(), height());

	//shrink to fit mask
	shrinkFitRectToMask(all);

	//The basic recursive algorithm is this:
	//  1) find a horizontal or vertical line ("split") where all pixels are black (off)
	//  2) With each of the two created rectangles:
	//  3) Shrink the rectangle so there's no black border
	//  4) make recursive call

	if (all.area() > 0)
	{
		//start recursive search
		isolateMaskRegionsRecursive(all, result);
	}
	else
		result.add(new Rect4i(all));

	//else mask has no white pixels and thus no regions
}

#ifdef UNIT_TEST
/*
void ImageMask::test(UnitTest & tst)
{
	tst.startTest("ImageMask");
	tst.endTest();
}*/

#endif

