
#include "ArrayList.hpp"

#include <stdio.h>

void test_ArrayList(int argc, char ** argv)
{
	{
		ArrayList<int> a;

		for (int i = 0; i < 100; i++)
			a.add(new int(i * i));

		for (int i = 0; i < a.size(); i++)
			printf("%d: %d\n", i, *a.items()[i]);
	}

	printf("done\n");

}
