package cruxic.math;

import cruxic.math.CrxMath;
import static cruxic.math.CrxMath.nearly_eqf;
import static cruxic.math.CrxMath.nearly_eqv;

/**A point in 3D space stored using
 spherical coordinates (coordinate system using radius and two angles)*/
public class SphereCoord3f
{
	/**The horizontal angle (left to right).
	Measured in radians from the +X axis towards the -X axis (CCW).
	Range is [0, 2pi)*/
	public float yaw;

	/**the vertical angle (up and down)
	Measured in radians downward from the +Z axis
	(assuming a cartesian coordinate system where +Z points upwards).*/
	public float pitch;

	/**distance from the origin to the point*/
	public float radius;

	public SphereCoord3f(float yaw, float pitch, float radius)
	{
		this.yaw = yaw;
		this.pitch = pitch;
		this.radius = radius;
	}

	/**Convert from cartesian coordinates to spherical*/
	public static SphereCoord3f fromPoint(Vec3f v)
	{
		float yaw = 0.0f;
		float pitch = 0.0f;
		float radius = v.magnitude();
		if (radius != 0.0f)
		{
			//prevent NaN if x and y are zero
			if (v.x == 0.0f && v.y == 0.0f)
			{
				radius = Math.abs(v.z);  //slightly more exact than len
				if (v.z < 0.0f)
					pitch = CrxMath.M_PIf;
				//else pitch remains 0.0
				//yaw remains 0.0
			}
			else
			{
				//http://www.math.montana.edu/frankw/ccp/multiworld/multipleIVP/spherical/body.htm#converting
				float S = (float)Math.sqrt(v.x * v.x + v.y * v.y);
				yaw = (float)Math.asin(v.y / S);
				if (v.x < 0.0f)
					yaw = (CrxMath.M_PIf) - yaw;

				pitch = (float)Math.acos(v.z / radius);
			}
		}

		return new SphereCoord3f(yaw, pitch, radius);
	}

	/**convert to cartesian coordinates (inverse of fromPoint())*/
	public Vec3f toPoint()
	{
		float PSinPhi = radius * (float)Math.sin(pitch);
		float x = PSinPhi * (float)Math.cos(yaw);
		float y = PSinPhi * (float)Math.sin(yaw);
		float z = radius * (float)Math.cos(pitch);
		return new Vec3f(x, y, z);
	}

	/**return the difference between this point and the given point (this - s).*/
	public SphereCoord3f minus(SphereCoord3f s)
	{
		return new SphereCoord3f(yaw - s.yaw, pitch - s.pitch, radius - s.radius);
	}

	/**Ignore the radius and map this spherical coordinate onto a 2D rectangle
	where the lower left corner of the rectangle has coordinate 0,0 and the
	upper-right has coordinate 1,1.  Yaw of 0 is 0 and 2p is 1.
	Pitch of 0 is 1 and pi is 0

	(equirectangular)*/
	public Vec2f toEquirect()
	{
		//normalize yaw to [0, 2pi]
		float nYaw = CrxMath.normalize_angle(yaw);
		//normalize pitch to [0, pi]
		float nPitch = CrxMath.normalize_angle(pitch);
		if (nPitch > CrxMath.M_PIf)
			nPitch -= CrxMath.M_PIf;

		return new Vec2f(nYaw / CrxMath.TWOPIf, 1.0f - (nPitch / CrxMath.M_PIf));
	}

	public void debugPrint()
	{
		System.out.printf("[yaw=%g pitch=%g rad=%g]\n", yaw, pitch, radius);
	}

	public static void unit_test()
	{
		///Test fromPoint

		//zero
		Vec3f v = new Vec3f(0,0,0);
		SphereCoord3f s = SphereCoord3f.fromPoint(v);
		assert(s.radius == 0.0f);
		assert(s.yaw == 0.0f);
		assert(s.pitch == 0.0f);
		Vec3f p = s.toPoint();
		assert(p.equals(v));

		//+X
		v = new Vec3f(1.23f,0,0);
		s = SphereCoord3f.fromPoint(v);
		assert(s.radius == 1.23f);
		assert(s.yaw == 0.0f);
		assert(nearly_eqf(s.pitch, CrxMath.M_PI_2f));
		p = s.toPoint();
		assert(nearly_eqv(p, v));

		//-X
		v = new Vec3f(-1.23f,0,0);
		s = SphereCoord3f.fromPoint(v);
		assert(s.radius == 1.23f);
		assert(nearly_eqf(s.yaw, CrxMath.M_PIf));
		assert(nearly_eqf(s.pitch, CrxMath.M_PI_2f));
		p = s.toPoint();
		assert(nearly_eqv(p, v));

		//+Y
		v = new Vec3f(0,1.23f,0);
		s = SphereCoord3f.fromPoint(v);
		assert(s.radius == 1.23f);
		assert(nearly_eqf(s.yaw, CrxMath.M_PI_2f));
		assert(nearly_eqf(s.pitch, CrxMath.M_PI_2f));
		p = s.toPoint();
		assert(nearly_eqv(p, v));

		//-Y
		v = new Vec3f(0,-1.23f,0);
		s = SphereCoord3f.fromPoint(v);
		assert(s.radius == 1.23f);
		assert(nearly_eqf(s.yaw, -CrxMath.M_PI_2f));
		assert(nearly_eqf(s.pitch, CrxMath.M_PI_2f));
		p = s.toPoint();
		assert(nearly_eqv(p, v));

		//+Z
		v = new Vec3f(0,0,1.23f);
		s = SphereCoord3f.fromPoint(v);
		assert(s.radius == 1.23f);
		assert(nearly_eqf(s.yaw, 0.0f));
		assert(nearly_eqf(s.pitch, 0.0f));
		p = s.toPoint();
		assert(nearly_eqv(p, v));

		//-Z
		v = new Vec3f(0,0,-1.23f);
		s = SphereCoord3f.fromPoint(v);
		assert(s.radius == 1.23f);
		assert(nearly_eqf(s.yaw, 0.0f));
		assert(nearly_eqf(s.pitch, CrxMath.M_PIf));
		p = s.toPoint();
		assert(nearly_eqv(p, v));


		//a point in +X+Y+Z quadrant
		v = new Vec3f(3.0f, 3.0f, 3.0f);
		s = SphereCoord3f.fromPoint(v);
		assert(nearly_eqf(s.radius, 5.196152f));
		assert(nearly_eqf(s.yaw, CrxMath.M_PI_4f));
		assert(nearly_eqf(s.pitch, 0.95529587f));
		p = s.toPoint();
		assert(nearly_eqv(p, v));

		//a point in -X-Y-Z quadrant
		v = new Vec3f(-3.0f, -3.0f, -3.0f);
		s = SphereCoord3f.fromPoint(v);
		assert(nearly_eqf(s.radius, 5.196152f));
		assert(nearly_eqf(s.yaw, CrxMath.M_PIf + CrxMath.M_PI_4f));
		assert(nearly_eqf(s.pitch, CrxMath.M_PIf - 0.95529587f));
		p = s.toPoint();
		assert(nearly_eqv(p, v));

		//toEquirect
		{
			s = new SphereCoord3f(0.0f, 0.0f, 1.0f);
			Vec2f er = s.toEquirect();
			assert(nearly_eqf(er.x, 0.0f));
			assert(nearly_eqf(er.y, 1.0f));

			s = new SphereCoord3f(CrxMath.TWOPIf - 0.0001f, CrxMath.TWOPIf, 1.0f);
			er = s.toEquirect();
			assert(nearly_eqf(er.x, 1.0f));
			assert(nearly_eqf(er.y, 1.0f));

			s = new SphereCoord3f(CrxMath.TWOPIf, CrxMath.M_PIf, 1.0f);
			er = s.toEquirect();
			assert(nearly_eqf(er.x, 0.0f));
			assert(nearly_eqf(er.y, 0.0f));

			s = new SphereCoord3f(-3.0f * CrxMath.M_PIf, -CrxMath.M_PIf / 2.0f, 1.0f);
			er = s.toEquirect();
			assert(nearly_eqf(er.x, 0.5f));
			assert(nearly_eqf(er.y, 0.5f));
		}
	}
}
