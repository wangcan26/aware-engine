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
#include "HeightMap.hpp"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>  //memset

//macro to ensure given float is on the range [0.0, 1.0]
#define clip_height_value(value) if (value > 1.0f) value = 1.0f; else if (value < 0.0f) value = 0.0f

HeightMap::HeightMap(int width, int height)
{
	W = width;
	H = height;

	values = new float[W * H];
	setAll(0.5f);
}

HeightMap::~HeightMap()
{
	delete[] values;
}

void HeightMap::setClipped(int x, int y, float value)
{
	clip_height_value(value);
	values[(y * W) + x] = value;
}

/**set all values in the heightmap*/
void HeightMap::setAll(float value)
{
	clip_height_value(value);
	int len = W * H;
	for (int i = 0; i < len; i++)
		values[i] = value;
}

/**multiply all values times the given scale.
scale MUST be on the range [0.0, 1.0]*/
void HeightMap::scaleAll(float scale)
{
	if (scale >= 0.0f && scale <= 1.0f)
	{
		int len = W * H;
		for (int i = 0; i < len; i++)
			values[i] *= scale;
	}
	//else bad input - make no change
}

/**Add the specified amount to each value.
If the shifted value is out of range it	will be clipped*/
void HeightMap::shiftAll(float amount)
{
	int len = W * H;
	for (int i = 0; i < len; i++)
	{
		float value = values[i] + amount;
		clip_height_value(value);
		values[i] = value;
	}
}

void HeightMap::setBlackPoint(float newBlackPoint)
{
	int len = W * H;
	for (int i = 0; i < len; i++)
	{
		values[i] = values[i] - (newBlackPoint * values[i]) + newBlackPoint;
	}
}

void HeightMap::setWhitePoint(float newWhitePoint)
{
	int len = W * H;
	for (int i = 0; i < len; i++)
	{
		values[i] = values[i] - (newWhitePoint * values[i]);
	}
}

/**Copy values from the source HeightMap into this HeightMap*/
void HeightMap::setRect(int posX, int posY, HeightMap * src)
{
	int stopX = posX + src->W;
	int stopY = posY + src->H;

	if (posX >= 0
		&& posY >= 0
		&& stopX <= W
		&& stopY <= H)
	{
		for (int y = posY; y < stopY; y++)
		{
			for (int x = posX; x < stopX; x++)
			{
				//TODO: this could probably be done more efficiently as a memcpy
				set(x, y, src->get(x - posX, y - posY));
			}
		}
	}
}


/**Add values from the source HeightMap into this HeightMap*/
void HeightMap::addRect(int posX, int posY, HeightMap * src)
{
	int stopX = posX + src->W;
	int stopY = posY + src->H;

	if (posX >= 0
		&& posY >= 0
		&& stopX <= W
		&& stopY <= H)
	{
		for (int y = posY; y < stopY; y++)
		{
			for (int x = posX; x < stopX; x++)
			{
				float newValue = get(x, y) + src->get(x - posX, y - posY);

				clip_height_value(newValue);

				//printf("addRect %g\n", newValue);

				set(x, y, newValue);
			}
		}
	}
}

void HeightMap::multiplyRect(int posX, int posY, HeightMap * src)
{
	int stopX = posX + src->W;
	int stopY = posY + src->H;

	if (posX >= 0
		&& posY >= 0
		&& stopX <= W
		&& stopY <= H)
	{
		for (int y = posY; y < stopY; y++)
		{
			for (int x = posX; x < stopX; x++)
			{
				float newValue = get(x, y) * src->get(x - posX, y - posY);

				//not necessary if src values between [0,1]
				//clip_height_value(newValue);

				set(x, y, newValue);
			}
		}
	}
}


void HeightMap::combineRect(int posX, int posY, HeightMap * src)
{
	int stopX = posX + src->W;
	int stopY = posY + src->H;

	if (posX >= 0
		&& posY >= 0
		&& stopX <= W
		&& stopY <= H)
	{
		for (int y = posY; y < stopY; y++)
		{
			for (int x = posX; x < stopX; x++)
			{
				float sv = src->get(x - posX, y - posY);
				float dv = get(x, y);
				setClipped(x, y, dv + (sv - 0.5f));
			}
		}
	}
}

void HeightMap::combine(HeightMap * src)
{
	int srcLen = src->W * src->H;
	int len = W * H;
	if (len > srcLen)
		len = srcLen;

	for (int i = 0; i < len; i++)
	{
		float sv = src->values[i];
		float newVal = values[i] + (sv - 0.5f);
		clip_height_value(newVal);
		values[i] = newVal;
	}
}


/**scale this HeightMap up or down to fit the destination HeightMap and copy
over all the values.
void HeightMap::copyResize(HeightMap * dest)
{
	//Scale up?
	if (W < dest->W && H < dest->H)
	{
		//Nearest neighbor (works but poor visual quality)
		float xStepSize = dest->W / (float)W;
		float yStepSize = dest->H / (float)H;

		int xNumSteps = W;
		int yNumSteps = H;

		for (int yStep = 0; yStep < yNumSteps; yStep++)
		{
			for (int xStep = 0; xStep < xNumSteps; xStep++)
			{
				int val = values[yStep][xStep];

				for (int iy = 0; iy < yStepSize; iy++)
				{
					int destY = (int)(yStep * yStepSize + iy);

					for (int ix = 0; ix < xStepSize; ix++)
					{
						int destX = (int)(xStep * xStepSize + ix);
						dest->values[destY][destX] = val;
					}
				}
			}
		}
	}
	else
	{
		fprintf(stderr, "HeightMap: scale down not implemented\n");
	}
}*/

/**Compute the sum of the surrounding 8 pixels (not including the center)*/
float HeightMap::sumSurrounding8(int x, int y)
{
	//the 3 above
	int idx = ((y - 1) * W) + x;
	float sum = values[idx - 1] + values[idx] + values[idx + 1];

	//the 2 beside
	idx += W;
	sum += values[idx - 1] + values[idx + 1];

	//the 3 below
	idx += W;
	sum += values[idx - 1] + values[idx] + values[idx + 1];

	return sum;
}

void HeightMap::blurTo(HeightMap * result)
{
	//must be same size
	if (W == result->W &&
		H == result->H)
	{
		int stopX = result->W - 1;
		int stopY = result->H - 1;

		int baseIdx = 0;

		//Blur everything except the 1 pixel border
		for (int y = 1; y < stopY; y++)
		{
			for (int x = 1; x < stopX; x++)
			{
				//Compute average of surrounding pixels

				//row above
				int idx = baseIdx + x;
				float sum = values[idx - 1] + values[idx] + values[idx + 1];
				//same row
				idx += W;
				sum += values[idx - 1] + values[idx] + values[idx + 1];
				//row below
				idx += W;
				sum += values[idx - 1] + values[idx] + values[idx + 1];

				result->values[idx - W] = sum / 9.0f;
			}

			//next row
			baseIdx += W;
		}

		//Blur the border

		//Top
		baseIdx = 0;
		for (int x = 1; x < stopX; x++)
		{
			//top row
			int idx = baseIdx + x;
			float sum = values[idx - 1] + values[idx] + values[idx + 1];
			//row below
			idx += W;
			sum += values[idx - 1] + values[idx] + values[idx + 1];

			result->values[idx - W] = sum / 6.0f;
		}

		//Bottom
		baseIdx = (H - 2) * W;
		for (int x = 1; x < stopX; x++)
		{
			//row above
			int idx = baseIdx + x;
			float sum = values[idx - 1] + values[idx] + values[idx + 1];
			//bottom row
			idx += W;
			sum += values[idx - 1] + values[idx] + values[idx + 1];

			result->values[idx] = sum / 6.0f;
		}

		//Left
		int idx = 0;
		for (int y = 1; y < stopY; y++)
		{
			//row above
			float sum = values[idx] + values[idx + 1];
			//same row
			idx += W;
			sum += values[idx] + values[idx + 1];
			//row below
			idx += W;
			sum += values[idx] + values[idx + 1];

			idx -= W;
			result->values[idx] = sum / 6.0f;
		}

		//Right
		idx = W - 1;
		for (int y = 1; y < stopY; y++)
		{
			//row above
			float sum = values[idx - 1] + values[idx];
			//same row
			idx += W;
			sum += values[idx - 1] + values[idx];
			//row below
			idx += W;
			sum += values[idx - 1] + values[idx];

			idx -= W;
			result->values[idx] = sum / 6.0f;
		}

		//4 Corners:

		//Top-Left
		idx = 0;
		result->values[idx] = (values[idx] + values[idx + 1] + values[idx + W] + values[idx + W + 1]) / 4.0f;
		//Top-Right
		idx = W - 1;
		result->values[idx] = (values[idx] + values[idx - 1] + values[idx + W] + values[idx + W - 1]) / 4.0f;
		//Bottom-Left
		idx = (H - 1) * W;
		result->values[idx] = (values[idx] + values[idx + 1] + values[idx - W] + values[idx - W + 1]) / 4.0f;
		//Bottom-Right
		idx = (H - 1) * W + (W - 1);
		result->values[idx] = (values[idx] + values[idx - 1] + values[idx - W] + values[idx - W - 1]) / 4.0f;

	}
}

void HeightMap::blur(int iterations)
{
	//must have even number of iterations so that we land back on ourselves
	if (iterations < 2)
		iterations = 2;
	else if (iterations % 1)
		iterations++;

	//Allocate temporary HeightMap
	HeightMap * htmp = new HeightMap(W, H);

	for (int i = 0; i < iterations; i++)
	{
		if (i & 1)
			htmp->blurTo(this);
		else
			this->blurTo(htmp);
	}

	delete htmp;
}