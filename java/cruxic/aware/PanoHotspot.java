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
package cruxic.aware;

import cruxic.math.*;

import java.util.*;

public class PanoHotspot
{
	public final String id;

	/**a polygon shape specifying an area that can be clicked or hovered to activate
	something in the game*/
	public ArrayList<SphereCoord3f> polygon;

	public PanoViewpoint targetViewpoint;

	public PanoHotspot(String id)
	{
		this.id = id;
		polygon = new ArrayList<SphereCoord3f>(8);
		targetViewpoint = null;
	}

	public boolean isRayInside(SphereCoord3f ray)
	{
		//create the ray plane
		Vec3f rayPlaneNormal = ray.toPoint().normalized();

		//convert spherical coordinates to cartesian
		ArrayList<Vec3f> poly3D = new ArrayList<Vec3f>(polygon.size());
		for (SphereCoord3f sc: polygon)
			poly3D.add(sc.toPoint());

		//Remove points which are behind the ray/camera plane.
		//This is necessary for two reasons:
		//	1) We dont want to allow clicking hotspots you cannot event see!
		//  2) For hotspots that wrap around more than 180deg the polygon will
		//		be malformed when we flatten it onto the rayplane below
		poly3D = CrxMath.clipPolygon(poly3D, rayPlaneNormal, Vec3f.ORIGIN);

		//have a complete polygon?
		if (poly3D.size() >= 3)
		{
			//Project the polygon onto the ray plane (make all points coplanar)
			for (int i = 0; i < poly3D.size(); i++)
				poly3D.set(i, CrxMath.projectPointOntoPlane(poly3D.get(i), rayPlaneNormal, Vec3f.ORIGIN));

			//Rotate coplanar polygon onto the XY plane (origin is rotation pivot)

			float planeAngleFromUp = (float)Math.acos(Vec3f.UP.dot(rayPlaneNormal));  //see Vec3f.angle()

			//if the angle is nearly 0 or 180 then don't bother trying to rotate (RotationMatrix will throw exception)
			float TINY_ANGLE = 0.004363f;  //0.25 degrees
			if (planeAngleFromUp > TINY_ANGLE && planeAngleFromUp < (float)Math.PI - TINY_ANGLE)
			{
				Vec3f rayPlaneLocalYAxis = Vec3f.UP.cross(rayPlaneNormal);
				RotationMatrix rm = new RotationMatrix(rayPlaneLocalYAxis, Vec3f.ORIGIN, (float)Math.PI - planeAngleFromUp);
				for (int i = 0; i < poly3D.size(); i++)
					poly3D.set(i, rm.rotatePoint(poly3D.get(i)));
			}
			//else ray plane is effectively XY plane

			//now all Z values are zero!
			//ArrayList<Vec2f> poly2D = new ArrayList<Vec2f>(poly3D.size());
			//for (Vec3f pt: poly3D)
			//	poly2D.add(new Vec2f(pt.x, pt.y));

			//TODO: what if there are two polygons, one directly in front of me
			//and one behind.  This algorithm would consider them both clicked!

			return CrxMath.isPointInsidePolygon(Vec2f.ORIGIN, poly3D);
		}
		else
			return false;
	}
}