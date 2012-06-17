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
#include "Rect4i.hpp"

#include <stdio.h>

/*Rect4i::Rect4i(const Rect4i & toCopy)
{
	This is a constructor!  Initialize member data BEFORE calling the assignment operator.
	*this = copy;	//Just use assignment operator
}*/

void Rect4i::keep_inside(int & x, int & y)
{
	if (x < x1)
		x = x1;
	else if (x >= x2)
		x = x2 - 1;

	if (y < y1)
		y = y1;
	else if (y >= y2)
		y = y2 - 1;
}

Rect4i::Rect4i()
	: x1(0), y1(0), x2(0), y2(0)
{

}

Rect4i::Rect4i(int newX1, int newY1, int newX2, int newY2)
	: x1(newX1), y1(newY1), x2(newX2), y2(newY2)
{

}

int Rect4i::width() const
{
	int w = x2 - x1;

	//abs value
	if (w < 0)
		w = -w;

	return w;
}

int Rect4i::height() const
{
	int h = y2 - y1;

	//abs value
	if (h < 0)
		h = -h;

	return h;
}


int Rect4i::area() const
{
	return width() * height();
}

void Rect4i::set(int newX1, int newY1, int newX2, int newY2)
{
	x1 = newX1;
	y1 = newY1;
	x2 = newX2;
	y2 = newY2;
};

void Rect4i::debugPrint() const
{
	printf("[%dx%d\t%d,%d %d,%d]\n", width(), height(), x1, y1, x2, y2);
}

bool Rect4i::contains(const Rect4i & rect)
{
	return rect.x1 >= x1
		&& rect.y1 >= y1
		&& rect.x2 <= x2
		&& rect.y2 <= y2;
}

#ifdef UNIT_TEST
/*
void Rect4i::test(UnitTest & tst)
{
	tst.startTest("Rect4i");
	tst.endTest();
}*/

#endif

