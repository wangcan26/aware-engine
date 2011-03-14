package cruxic.math;

import org.lwjgl.opengl.GL11;

/**A vector (or point) in 3D space*/
public class Vec3f
	extends Vec2f
	implements Comparable<Vec3f>
{
	/**0,0,0*/
	public static Vec3f ORIGIN = new Vec3f(0.0f, 0.0f, 0.0f);
	/**0,0,1*/
	public static Vec3f UP = new Vec3f(0f, 0f, 1f);

	public float z;

	public Vec3f(float x, float y, float z)
	{
		super(x, y);
		this.z = z;
	}

	public void glVertex()
	{
		GL11.glVertex3f(x, y, z);
	}


	public float distance(Vec3f v)
	{
		//pythagorean theorem
		float a = x - v.x;
		float b = y - v.y;
		float c = z - v.z;
		return (float)Math.sqrt(a * a + b * b + c * c);
	}

	/**the length of this vector (Distance from the origin to the head)*/
	public float magnitude()
	{
		//pythagorean theorem
		return (float)Math.sqrt(x * x + y * y + z * z);
	}

	/**return the difference between this vector and the given vector (this - v).*/
	public Vec3f minus(Vec3f v)
	{
		return new Vec3f(x - v.x, y - v.y, z - v.z);
	}

	/**return the addition between this vector and the given vector (this + v).*/
	public Vec3f plus(Vec3f v)
	{
		return new Vec3f(x + v.x, y + v.y, z + v.z);
	}

	/**Multiply all components of this vector times the given scalar*/
	public Vec3f mult(float scalar)
	{
		return new Vec3f(x * scalar, y * scalar, z * scalar);
	}

	/**Compute the dot product of this vector and the given vector.

	Interesting properties of the dot product:
		* A.B == 0 when angle between A and B is 90 degrees
		* A.B > 0 when angle < 90 degrees
		* A.B < 0 when angle > 90 degrees
		* A.B == cos(theta) [if A and B are normalized]
	*/
	public float dot(Vec3f v)
	{
		return (x * v.x) + (y * v.y) + (z * v.z);
	}

	/**Compute the cross product of this vector and the given vector.
	The cross product of two vectors is the perpendicular "normal" vector.*/
	public Vec3f cross(Vec3f v)
	{
		//A X B = (AyBz - AzBy, AzBx - AxBz, AxBy - AyBx)
		return new Vec3f(
			(y * v.z) - (z * v.y),  //AyBz - AzBy
			(z * v.x) - (x * v.z),  //AzBx - AxBz
			(x * v.y) - (y * v.x)); //AxBy - AyBx
	}

	public Vec3f normalized()
	{
		float mag = magnitude();
		return new Vec3f(x / mag, y / mag, z / mag);
	}

	public boolean isNormalized()
	{
		float mag = magnitude();
		return mag >= 0.999999f && mag <= 1.00001f;
	}

	/**Compute the minimum angle between this vector and another.
	angle will range from [0, PI]*/
	public float angle(Vec3f v)
	{
		Vec3f v1 = normalized();
		Vec3f v2 = v.normalized();
		return (float)Math.acos(v1.dot(v2));
	}

	public boolean equals(Vec3f v)
	{
		return x == v.x && y == v.y && z == v.z;
	}

	public int compareTo(Vec3f vec)
	{
		if (x > vec.x)
			return 1;
		else if (x < vec.x)
			return -1;
		else if (y > vec.y)
			return 1;
		else if (y < vec.y)
			return -1;
		else if (z > vec.z)
			return 1;
		else if (z < vec.z)
			return -1;
		else  //equal!
			return 0;
	}

	public void debugPrint()
	{
		System.out.printf("[%g, %g, %g]\n", x, y, z);
	}
}