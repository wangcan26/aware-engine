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
package cruxic.math;

import java.util.*;

public class CrxMath
{
	//Multiplier to convert degrees to radians
	public static final double Deg2Rad = Math.PI / 180.0;
	public static final float Deg2Radf = (float)Deg2Rad;
	//Multiplier to convert radians to degrees
	public static final double Rad2Deg = 180.0 / Math.PI;
	public static final float Rad2Degf = (float)Rad2Deg;

	public static final double M_PI_2 = Math.PI / 2.0;

	public static final float M_2PIf = (float)(Math.PI * 2.0);
	public static final float M_PIf = (float)Math.PI;
	public static final float M_PI_2f = (float) M_PI_2;
	public static final float M_PI_4f = (float)(Math.PI / 4.0);

	//2 * PI as float
	public static final float TWOPIf = (float)(Math.PI * 2.0);


	public static boolean nearly_eqf(float a, float b)
	{
		//stdout.printf("nearly_eqf(%f=%f, %f=%f)\n", a, roundnf(a, 4), b, roundnf(b, 4));

		float diff = a - b;
		if (diff < 0.0f)
			diff = -diff;
		return diff < 0.0001f;

		//equal to 4 decimal places
		//return a == b || roundnf(a, 4) == roundnf(b, 4);
	}

	public static boolean nearly_eqv(Vec3f a, Vec3f b)
	{
		return nearly_eqf(a.x, b.x)
			|| nearly_eqf(a.y, b.y)
			|| nearly_eqf(a.z, b.z);
	}

	public static double roundd(double value)
	{
		return Math.floor(value + 0.5);
	}

	/**round a floating point number to N decimal places.
	For example, roundnf(1234.5678, 2) == 1234.57*/
	public static float roundnf(float value, int numPlaces)
	{
		//use double to prevent loss of precision
		double places = Math.pow((double)numPlaces, 10);
		return (float)(roundd((double)value * places) / places);
	}

	/**Given a rotation from startingAngle to endingAngle invert endingAngle as needed
	so that the rotation takes the shortest path around the circle.*/
	public static float shortest_rotation(float startingAngle, float endingAngle)
	{
		float s = normalize_angle(startingAngle);
		float e = normalize_angle(endingAngle);

		//compute the minimal angle between the two (the acute angle)
		float delta = Math.abs(e - s);
		if (delta > Math.PI)
			delta = TWOPIf - delta;

		float res;
		if (delta == 0.0f)
			res = startingAngle;
		else
		{
			//get a version of e that is guaranteed greater than s
			float greaterE = e < s ? (e + TWOPIf) : e;
			boolean cw = greaterE - delta != s;

			if (cw)
				res = startingAngle - delta;
			else
				res = startingAngle + delta;
		}

		//stdout.printf("sa=%g->%g ea=%g->%g delta=%g  res=%g\n", startingAngle*Rad2Degf, s*Rad2Degf, endingAngle*Rad2Degf, e*Rad2Degf, delta*Rad2Deg, res * Rad2Deg);
		return res;
	}

	/**Ensure the given angle (in radians) is a number in the range [0, 2pi).*/
	public static float normalize_angle(float a)
	{
		//Adapted from: http://www.java2s.com/Tutorial/Java/0120__Development/Normalizeanangleina2piwideintervalaroundacentervalue.htm
		if (a < 0.0f || a >= TWOPIf)
			return a - (TWOPIf * (float)Math.floor(a / TWOPIf));
		else
			return a;
	}

	private static ArrayList<Vec2f> makePoly(float... pairs)
	{
		ArrayList<Vec2f> list = new ArrayList<Vec2f>();
		for (int i = 0; i < pairs.length - 1; i += 2)
			list.add(new Vec2f(pairs[i], pairs[i + 1]));
		return list;
	}

	/**
		Determine if the given point lies inside or outside an abitrary, closed, polygon shape.

		Adapted from Paul Bourke's "Solution 1 (2D)"
		http://local.wasp.uwa.edu.au/~pbourke/geometry/insidepoly/
	*/
	public static boolean isPointInsidePolygon(Vec2f p, List<? extends Vec2f> polygon)
	{
		assert(polygon.size() >= 3);
		int counter = 0;

		//get the first point
		Iterator<? extends Vec2f> itr = polygon.iterator();
		Vec2f first = itr.next();
		Vec2f p1 = first;

		//for each point ... (and back to first point)
		boolean done = false;
		while (!done)
		{
			Vec2f p2;
			if (itr.hasNext())
				p2 = itr.next();
			else
			{
				//we have reached the end - wrap around to close the polygon
				done = true;
				p2 = first;
			}

			if (p.y > Math.min(p1.y, p2.y))
			{
				if (p.y <= Math.max(p1.y, p2.y))
				{
					if (p.x <= Math.max(p1.x, p2.x))
					{
						if (p1.y != p2.y)
						{
							float xinters = (p.y - p1.y) * (p2.x - p1.x) / (p2.y - p1.y) + p1.x;
							if (p1.x == p2.x || p.x <= xinters)
								counter++;
						}
					}
				}
			}

			p1 = p2;
		}

		//odd number of crossings means inside
		return counter % 2 != 0;
	}

	/**
		Project 'point' onto the plane defined by 'planeNormal' and 'planePoint'.
		Returns the point on the plane which is closest to 'point'.  This is sometimes
		refered to as an "orthogonal projection" (I think).

		IMPORTANT: planeNormal MUST be normalized.
	*/
	public static Vec3f projectPointOntoPlane(Vec3f point, Vec3f planeNormal, Vec3f planePoint)
	{
		//plane normal must be normalized
		//assert(planeNormal.isNormalized());

		float distToClosest = planeNormal.dot(point.minus(planePoint));
		return point.minus(planeNormal.mult(distToClosest));
	}

	/**Determine if the given point lies on the specified plane.

		IMPORTANT: planeNormal MUST be normalized.

		@param tolerance a small positive number indicating how "close" the point
			must be to be considered "on" the plane.  Defaults to zero meaning it must
			be EXACTLY on (which is often problematic due to limited floating-point precision)
	*/
	public static boolean isPointOnPlane(Vec3f point, Vec3f planeNormal, Vec3f planePoint, float tolerance)
	{
		float minDist = planeNormal.dot(point.minus(planePoint));
		//Note minDist will often be negative.  If you want the true distance take the absolute value
		if (minDist < 0.0f)
			minDist = -minDist;

		return minDist <= tolerance;
	}


	/**
		Determine if the given point is behind or in front of a plane.

		@return true if behind, false if in front or exactly on the plane
	*/
	public static boolean is_point_behind_plane(Vec3f point, Vec3f planeNormal, Vec3f planePoint)
	{
		//create vector from plane-point to point
		Vec3f v = point.minus(planePoint);
		return planeNormal.dot(v) < 0.0f;
	}

	/**Find the point where a line intersects a plane.

		@param line_p1 a point on the line
		@param line_p2 another point on the line (not equal to line_p1)
		@param planeNormal the normal vector of the plane
		@param planePoint any point on the plane
		@param between_only (defaults to false) return null if the intersection point is not between the give points on the line

		@return the point where the line intersects the plane or null if the line is
			parallel to the plane or lies exactly on the plane.
	*/
	public static Vec3f line_plane_intersection(Vec3f line_p1, Vec3f line_p2, Vec3f planeNormal, Vec3f planePoint, boolean between_only)
	{
		//thanks to: http://local.wasp.uwa.edu.au/~pbourke/geometry/planeline/

		float numerator = planeNormal.dot(planePoint.minus(line_p1));
		Vec3f p2_minus_p1 = line_p2.minus(line_p1);
		float denominator = planeNormal.dot(p2_minus_p1);

		// "If the denominator is 0 then the normal to the plane is perpendicular to the line.
		//   Thus the line is either parallel to the plane and there are no solutions or
		//   the line is on the plane in which case there are an infinite number of solutions"
		if (denominator != 0.0f)
		{
			float u = numerator / denominator;

			// "If it is necessary to determine the intersection of the line segment
			// between P1 and P2 then just check that u is between 0 and 1."
			if (!between_only || (u >= 0.0f && u <= 1.0f))
			{
				//now that we know 'u' we can find the intersection point
				return line_p1.plus(p2_minus_p1.mult(u));
			}
		}

		return null;
	}

	/**Give a list of 3D points which form a closed polygon shape, clip the polygon
	with the given plane.  Some points are completely removed from the polygon
	and some are added such that the shape of the polygon on the front side
	of the plane is preserved.

		@return the clipped polygon.  Note: in some cases this may be fewer than
			3 points meaning the polygon has been completely clipped.
	*/
	public static ArrayList<Vec3f> clipPolygon(List<Vec3f> poly, Vec3f planeNormal, Vec3f planePoint)
	{
		assert(planeNormal.isNormalized());

		int nPoints = poly.size();
		ArrayList<Vec3f> newPoly = new ArrayList<Vec3f>(nPoints);

		//tolerance value to avoid creating extremely tiny polygon edges
		float TOL = 0.0001f;

		//must be a closed polygon
		if (nPoints >= 3)
		{
			//for each point...
			int pIdx = 0;
			for (Vec3f pt: poly)
			{
				//should point be clipped?
				if (is_point_behind_plane(pt, planeNormal, planePoint)
					&& !isPointOnPlane(pt, planeNormal, planePoint, TOL))
				{
					//Get the two adjacent points
					int ia = pIdx - 1;
					int ib = pIdx + 1;
					if (ia < 0)
						ia = nPoints - 1;
					if (ib >= nPoints)
						ib = 0;
					Vec3f pa = poly.get(ia);
					Vec3f pb = poly.get(ib);

					//determine if a or b is behind the plane
					boolean aBehindPlane = is_point_behind_plane(pa, planeNormal, planePoint);
					boolean bBehindPlane = is_point_behind_plane(pb, planeNormal, planePoint);

					//determine if a or b lies on the plane
					boolean aOnPlane = isPointOnPlane(pa, planeNormal, planePoint, TOL);
					boolean bOnPlane = isPointOnPlane(pb, planeNormal, planePoint, TOL);

					//Both points are on or behind the plane?
					if ((aOnPlane && bOnPlane) || (aBehindPlane && bBehindPlane))
					{
						//throw away pt. (don't add pt to newPoly)
					}
					//Both points in front of plane (but not on it)?
					else if (!aBehindPlane && !bBehindPlane && !aOnPlane && !bOnPlane)
					{
						//find where each edge crosses the plane
						Vec3f poiA = line_plane_intersection(pt, pa, planeNormal, planePoint, false);
						Vec3f poiB = line_plane_intersection(pt, pb, planeNormal, planePoint, false);
						assert(poiA != null && poiB != null);

						newPoly.add(poiA);
						newPoly.add(poiB);
					}
					//Either a or b, (exclusive), is in front of the plane (but not on it)
					else
					{
						//which one is in front?
						Vec3f pInFront = bBehindPlane ? pa : pb;

						//find point where edge crosses the plane
						Vec3f poi = line_plane_intersection(pt, pInFront, planeNormal, planePoint, false);
						assert(poi != null);

						newPoly.add(poi);
					}
				}
				//keep it
				else
					newPoly.add(pt);

				pIdx++;
			}
		}
		else
		{
			//just clip whatever points we have
			for (Vec3f pt: poly)
			{
				//keep point?
				if (!is_point_behind_plane(pt, planeNormal, planePoint)
					|| isPointOnPlane(pt, planeNormal, planePoint, TOL))
					newPoly.add(pt);
			}
		}

		return newPoly;
	}

	public static void test_misc()
	{
		float PIf = (float)Math.PI;

		//normalize_angle
		{
			assert(nearly_eqf(normalize_angle(0.0f), 0.0f));
			assert(nearly_eqf(normalize_angle(PIf), PIf));
			assert(nearly_eqf(normalize_angle(-PIf), PIf));
			assert(nearly_eqf(normalize_angle(TWOPIf), 0.0f));
			assert(nearly_eqf(normalize_angle(-TWOPIf), 0.0f));
			assert(nearly_eqf(normalize_angle(TWOPIf - 0.00001f), TWOPIf));
			assert(nearly_eqf(normalize_angle(PIf * 3), PIf));
			assert(nearly_eqf(normalize_angle(PIf * 5), PIf));
			assert(nearly_eqf(normalize_angle(PIf * 4), 0.0f));
			assert(nearly_eqf(normalize_angle(PIf * -3.0f), PIf));
		}

		///Test shortest_rotation()
		{
			//stdout.printf("%g\n", shortest_rotation(-45 * Deg2Radf, 180 * Deg2Radf));
			//float sr = shortest_rotation(-45 * Deg2Radf, 180 * Deg2Radf);
			//float csr = -180 * Deg2Radf;
			//stdout.printf("%f %f %d\n", sr, csr, (int)(nearly_eqf(sr, csr)));

			//CW
			boolean t = false;
			assert(nearly_eqf(shortest_rotation(-45 * Deg2Radf, 180 * Deg2Radf), -180 * Deg2Radf) ||t);
			assert(nearly_eqf(shortest_rotation(-45 * Deg2Radf, -180 * Deg2Radf), -180 * Deg2Radf) ||t);
			assert(nearly_eqf(shortest_rotation(315 * Deg2Radf, 180 * Deg2Radf), 180 * Deg2Radf) ||t);
			assert(nearly_eqf(shortest_rotation(315 * Deg2Radf, -180 * Deg2Radf), 180 * Deg2Radf) ||t);

			//CCW
			//stdout.printf("%g\n", shortest_rotation(-45 * Deg2Radf, 90 * Deg2Radf) * Rad2Degf);
			assert(nearly_eqf(shortest_rotation(-45 * Deg2Radf, 90 * Deg2Radf), 90 * Deg2Radf) ||t);
			assert(nearly_eqf(shortest_rotation(-45 * Deg2Radf, -270 * Deg2Radf), 90 * Deg2Radf) ||t);
			assert(nearly_eqf(shortest_rotation(315 * Deg2Radf, 90 * Deg2Radf), 450 * Deg2Radf) ||t);
			assert(nearly_eqf(shortest_rotation(315 * Deg2Radf, -270 * Deg2Radf), 450 * Deg2Radf) ||t);

			//no rotation necessary
			assert(nearly_eqf(shortest_rotation(0.0f, 360 * Deg2Radf), 0.0f) ||t);
			assert(nearly_eqf(shortest_rotation(-360 * Deg2Radf, -360 * Deg2Radf), -360.0f * Deg2Radf) ||t);

			//angles in excess of 360deg
			assert(nearly_eqf(shortest_rotation(450 * Deg2Radf, 540 * Deg2Radf), 540 * Deg2Radf) ||t);
			//assert(nearly_eqf(shortest_rotation(720 * Deg2Radf, -90 * Deg2Radf), 670 * Deg2Radf) ||t);

			//stdout.printf("sr=%g\n", shortest_rotation(0.659734f, 1.67541f) ||t);
			//assert(nearly_eqf(shortest_rotation(0.659734f, 1.67541f), 670 * Deg2Radf) ||t);

		}

		SphereCoord3f.unit_test();

		//isPointInsidePolygon
		{
			//a simple right triangle
			List<Vec2f> poly = makePoly(new float[] {
				-1f, 1f,
				1f, 1f,
				-1f, -1f});

			assert(isPointInsidePolygon(new Vec2f(-0.5f, 0.5f), poly));
			assert(isPointInsidePolygon(new Vec2f(-0.999f, 0.999f), poly));
			assert(isPointInsidePolygon(new Vec2f(0.999f, 0.999f), poly));
			assert(isPointInsidePolygon(new Vec2f(-0.999f, -0.8f), poly));  //too near tip

			assert( ! isPointInsidePolygon(new Vec2f(0.0001f, 0.0f), poly));  //barely out
			assert( ! isPointInsidePolygon(new Vec2f(-1.001f, 0.9f), poly));
			assert( ! isPointInsidePolygon(new Vec2f(0.8f, 1.001f), poly));
			assert( ! isPointInsidePolygon(new Vec2f(-2f, -2f), poly));

			//A more complex shape.
			//Wavefront OBJ file exported from Blender:
			//
			// # Blender3D v249 OBJ File:
			// # www.blender3d.org
			// v 0.753639 0.854902 0.000000
			// v -0.391019 -1.390969 0.000000
			// v -1.433415 1.013667 0.000000
			// v -0.339888 -0.556152 0.000000
			// v 1.675436 -1.319185 0.000000
			// v 0.190243 0.181499 0.000000
			// v 0.440547 -0.295757 0.000000
			// usemtl (null)
			// s off
			// f 2 4 3
			// f 4 5 3
			// f 6 3 5
			// f 5 7 6
			// f 7 1 6

			poly = makePoly(new float[] {
				-1.433415f, 1.013667f,
				0.190243f, 0.181499f,
				0.753639f, 0.854902f,
				0.440547f, -0.295757f,
				1.675436f, -1.319185f,
				-0.339888f, -0.556152f,
				-0.391019f, -1.390969f});

			//a bunch of points barely inside
			List<Vec2f> insidePoints = makePoly(new float[]{
				0.215430f, 0.169929f,
				1.614133f, -1.285861f,
				-0.404708f, -1.321097f,
				-1.390751f, 0.966695f,
				0.360489f, -0.787948f,
				0.732628f, 0.805750f,
				-0.524853f, 0.527375f,
				-0.889409f, -0.204444f,
				-0.334350f, -0.514227f,
				0.430110f, -0.325057f});

			//a bunch of points barely outside
			List<Vec2f> outsidePoints = makePoly(new float[]{
				0.172996f, 0.236612f,
				1.593747f, -1.328681f,
				-0.459267f, -1.339283f,
				-1.445309f, 0.936385f,
				0.360489f, -0.862166f,
				0.793249f, 0.890620f,
				-0.482419f, 0.618306f,
				-0.925781f, -0.265065f,
				-0.299524f, -0.641458f,
				0.483123f, -0.272044f});

			for (Vec2f pt: insidePoints)
			{
				assert(isPointInsidePolygon(pt, poly));
			}

			for (Vec2f pt: outsidePoints)
			{
				assert( ! isPointInsidePolygon(pt, poly));
			}
		}

		//projectPointOntoPlane
		{
			Vec3f pNorm = new Vec3f(0.692619f, -0.232257f, 0.682888f);
			Vec3f pPt = new Vec3f(-0.621807f, -1.371915f, 0.626013f);
			Vec3f pt = new Vec3f(0.3f, 0.227f, 0.804f);
			Vec3f pp = projectPointOntoPlane(pt, pNorm, pPt);
			//pp.debugPrint();
			assert(nearly_eqv(pp, new Vec3f(0.0308f, 0.317f, 0.538f)));
		}

		//rotatePointsToZ
		/*{
			//a triangle
			var v1 = new Vec3f(-0.462150f, 0.457594f, -0.346319f);
			var v2 = new Vec3f(-0.373835f, -0.681921f, -0.272214f);
			var v3 = new Vec3f(-0.065197f, 0.597101f, -0.013236f);
			var vn = new Vec3f(0.642788f, 0.000000f, -0.766044f);
			var mesh = new ArrayList<Vec3f>();
			mesh.add(v1);
			mesh.add(v2);
			mesh.add(v3);

			var res = rotatePointsToZ(vn, mesh);
			foreach (var pt in res)
				pt.debugPrint();

			// result
			// v 0.576637 0.457594 -0.031768
			// v 0.461351 -0.681921 -0.031768
			// v 0.058452 0.597101 -0.031768
		}*/

		//line_plane_intersection
		{
			//Test 1
			Vec3f lp1 = new Vec3f(0f, 0f, 3.456f);
			Vec3f lp2 = new Vec3f(0f, 0f, -5.678f);
			Vec3f pn = new Vec3f(0f, 0f, 123.456f);
			Vec3f pp = new Vec3f(0f, 0f, -8.910f);
			Vec3f poi = line_plane_intersection(lp1, lp2, pn, pp, false);
			assert(poi != null);
			assert(poi.equals(pp));
			poi = line_plane_intersection(lp1, lp2, pn, pp, true);
			assert(poi == null);

			//Test 2
			lp1 = new Vec3f(3.456f, 3.456f, 3.456f);
			lp2 = new Vec3f(-5.678f, -5.678f, -5.678f);
			pn = new Vec3f(0.576090f, -0.417125f, 0.702942f);
			pp = new Vec3f(-0.817386f, 0.566000f, 1.005746f);
			poi = line_plane_intersection(lp1, lp2, pn, pp, false);
			assert(poi != null);
			assert(nearly_eqv(poi, new Vec3f(0f, 0f, 0f)));
			Vec3f poi2 = line_plane_intersection(lp1, lp2, pn, pp, true);
			assert(poi2 != null);
			assert(poi2.equals(poi));

			//Test 3 (line is parallel to plane)
			lp1 = new Vec3f(0.817386f, -0.566000f, -1.005746f);
			lp2 = new Vec3f(-0.817386f, -1.153975f, -1.005746f);
			pn = new Vec3f(0f, 0f, 0.702942f);
			pp = new Vec3f(-0.817386f, 0.566000f, 345.678f);
			poi = line_plane_intersection(lp1, lp2, pn, pp, false);
			assert(poi == null);
		}


		System.out.printf("test_misc PASSED\n");
	}
}