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
#ifndef Rect4i_H
#define Rect4i_H

/**

*/
class Rect4i
{
public:
	//x1, y1 is lower-left corner of box
	int x1;
	int y1;

	//x2, y2 is upper-right corner of box
	int x2;
	int y2;

	Rect4i();
	//builtin copy ctor is fine - Rect4i(const Rect4i & toCopy);
	Rect4i(int newX1, int newY1, int newX2, int newY2);

	int area() const;
	void set(int newX1, int newY1, int newX2, int newY2);
	int width() const;
	int height() const;

	void debugPrint() const;

	void keep_inside(int & x, int & y);

	bool contains(const Rect4i & rect);

	#ifdef UNIT_TEST
	//static void test(UnitTest & tst);
	#endif
};

#endif	//Rect4i_H

