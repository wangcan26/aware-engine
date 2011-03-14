#ifndef HeightMap_H
#define HeightMap_H


class HeightMap
{
private:
	/**the rows*/
	float * values;
public:
	/**width*/
	int W;

	/**height*/
	int H;

	HeightMap(int width, int height);
	virtual ~HeightMap();

	inline const float get(int x, int y)
	{
		return values[(y * W) + x];
	};

	inline void set(int x, int y, float value)
	{
		values[(y * W) + x] = value;
	};

	/**like set but clips the value to valid range [0.0, 1.0]*/
	void setClipped(int x, int y, float value);

	void setRect(int posX, int posY, HeightMap * src);
	void addRect(int posX, int posY, HeightMap * src);

	void multiplyRect(int posX, int posY, HeightMap * src);

	/**Add or subract values in 'src' into this HeightMap.
	If 'src' value is < 0.5, subtract, otherwise add.*/
	void combineRect(int posX, int posY, HeightMap * src);

	/**faster version of combineRect is both maps are same size*/
	void combine(HeightMap * src);

	/**multiply all values times the given scale.
	scale MUST be on the range [0.0, 1.0].  If an invalid scale is
	given no change will be made.*/
	void scaleAll(float scale);

	/**Add the specified amount to each value.
	If the shifted value is out of range it	will be clipped*/
	void shiftAll(float amount);

	/**let no values be < the specified "black point".  This has
	the effect squishing or compressing values towards white (1.0)*/
	void setBlackPoint(float newBlackPoint);

	/**let no values be > the specified "white point".  This has
	the effect squishing or compressing values towards black (0.0)*/
	void setWhitePoint(float newWhitePoint);

	//void copyResize(HeightMap * dest);

	/**set all values in the heightmap*/
	void setAll(float value);

	/**Compute the sum of the surrounding 8 pixels (not including the center)*/
	float sumSurrounding8(int x, int y);

	float sumSurrounding8Safe(int x, int y);

	void blurTo(HeightMap * result);
	void blur(int iterations);
};

#endif