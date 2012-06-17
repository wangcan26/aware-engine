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

/* $Id: RotationMatrix.vala, adapted from Glenn Murray's RotationMatrix.java
 * By cruxic_at_gmail_dot_com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 */

/**
	A matrix for rotating points about an abitrary vector.
	Adapted from: //http://inside.mines.edu/~gmurray/ArbitraryAxisRotation/
 */
public class RotationMatrix
{
	//3x4 matrix
	private float m11;
	private float m12;
	private float m13;
	private float m14;
	private float m21;
	private float m22;
	private float m23;
	private float m24;
	private float m31;
	private float m32;
	private float m33;
	private float m34;

	/**
	 * Build a matrix for rotating points about an abitrary vector.
	 *
	 * @param vector the vector of rotation.
	 * @param vectorTailPoint where in 3D space the vector is located
	 * @param angle the angle of rotation in radians
	 */
	public RotationMatrix(Vec3f vector, Vec3f vectorTailPoint, double angle)
	{
		//even though Vec3f is float, compute the matrix with double to
		// enhance precision.  We can justify the penalty by how many points
		// we are going to multiply with it

		// Set some intermediate values.

		double cosT = Math.cos(angle);
		double sinT = Math.sin(angle);

		double a = vectorTailPoint.x;
		double b = vectorTailPoint.y;
		double c = vectorTailPoint.z;

		double u = vector.x;
		double v = vector.y;
		double w = vector.z;
		double u2 = u*u;
		double v2 = v*v;
		double w2 = w*w;
		double l2 = u2 + v2 + w2;
		double l =  Math.sqrt(l2);
		assert(l2 >= 0.000000001);  //RotationMatrix: direction vector too short?

		// Build the matrix entries element by element.
		m11 = (float)((u2 + (v2 + w2) * cosT)/l2);
		m12 = (float)((u*v * (1 - cosT) - w*l*sinT)/l2);
		m13 = (float)((u*w * (1 - cosT) + v*l*sinT)/l2);
		m14 = (float)((a*(v2 + w2) - u*(b*v + c*w)
				+ (u*(b*v + c*w) - a*(v2 + w2))*cosT + (b*w - c*v)*l*sinT)/l2);

		m21 = (float)((u*v * (1 - cosT) + w*l*sinT)/l2);
		m22 = (float)((v2 + (u2 + w2) * cosT)/l2);
		m23 = (float)((v*w * (1 - cosT) - u*l*sinT)/l2);
		m24 = (float)((b*(u2 + w2) - v*(a*u + c*w)
				+ (v*(a*u + c*w) - b*(u2 + w2))*cosT + (c*u - a*w)*l*sinT)/l2);

		m31 = (float)((u*w * (1 - cosT) - v*l*sinT)/l2);
		m32 = (float)((v*w * (1 - cosT) + u*l*sinT)/l2);
		m33 = (float)((w2 + (u2 + v2) * cosT)/l2);
		m34 = (float)((c*(u2 + v2) - w*(a*u + b*v)
				+ (w*(a*u + b*v) - c*(u2 + v2))*cosT + (a*v - b*u)*l*sinT)/l2);
	}

	/**
	* Rotate the point 'p' about the vector given in the constructor.
	*
	* @param p the point to rotate
	* @return a copy of p which has been rotated
	*/
	public Vec3f rotatePoint(Vec3f p)
	{
		return new Vec3f(
			//X
			m11*p.x + m12*p.y + m13*p.z + m14,
			//Y
			m21*p.x + m22*p.y + m23*p.z + m24,
			//Z
			m31*p.x + m32*p.y + m33*p.z + m34);
	}
}

