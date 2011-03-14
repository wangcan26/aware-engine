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

