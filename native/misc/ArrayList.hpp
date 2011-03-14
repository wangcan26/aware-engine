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
#ifndef ArrayList_H
#define ArrayList_H

#include <string.h>  //memset

template <class E>
class ArrayList
{
private:
	E ** array;
	int curSize;
	int capacity;

public:
	ArrayList(int initialCapacity = 16);
	virtual ~ArrayList();

	void add(E * item);

	void clear();

	inline E ** items() const
	{
		return array;
	}

	inline int size() const
	{
		return curSize;
	}
};

template <class E>
ArrayList<E>::ArrayList(int initialCapacity)
	: array(NULL), curSize(0), capacity(initialCapacity)
{
	//allocate the array
	array = new E*[capacity];
}

template <class E>
ArrayList<E>::~ArrayList()
{
	//free elements
	clear();

	delete[] array;
}


template <class E>
void ArrayList<E>::clear()
{
	//free elements
	for (int i = 0; i < curSize; i++)
		delete array[i];
	curSize = 0;
}

template <class E>
void ArrayList<E>::add(E * item)
{
	//need to grow?
	if (curSize >= capacity)
	{
		//allocate new array
		capacity = capacity * 2;  //double in size
		E ** array2 = new E*[capacity];

		//copy over old elements
		for (int i = 0; i < curSize; i++)
			array2[i] = array[i];

		//set remaining to NULL for good measure
		memset(&array2[curSize], 0, sizeof(E*) * (capacity - curSize));

		//free old array
		delete[] array;

		//switch to new
		array = array2;
	}

	//add the element
	array[curSize++] = item;
}

#endif	//ArrayList_H