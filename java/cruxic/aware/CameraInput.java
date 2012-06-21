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

import org.lwjgl.input.*;

import static org.lwjgl.opengl.GL11.*;
import cruxic.math.*;


/**Use the mouse and/or keyboard to look around*/
public class CameraInput
{
	//mouse movement sensitivity in rotation degrees per pixel of movement
	private float sensitivity;
	//keyboard movement sensitivity in rotation degrees key press
	private float keyboardSensitivity;
	float yaw;
	float pitch;

	public CameraInput(int windowHeight)
	{
		//compute sensitivity relative to the users screen resolution.
		//Looking from the floor to the ceiling should be equivalent to moving the mouse from the bottom of the screen to the top
		sensitivity = (float)Math.PI / windowHeight;

		keyboardSensitivity = 2.5f * CrxMath.Deg2Radf;

		//look down +Y axis
		pitch = CrxMath.M_PI_2f;
		yaw = CrxMath.M_PI_2f;

		//Grab the mouse so we can move rotate infinitely.
		Mouse.setGrabbed(true);
	}

	public void scaleSensitivity(float scale)
	{
		sensitivity *= scale;
		keyboardSensitivity *= scale;
	}

	/**@returns true if the mouse moved*/
	public boolean checkForInput()
	{
		//get mouse deltas
		int dx = Mouse.getDX();
		int dy = -Mouse.getDY();

		yaw -= dx * sensitivity;
		pitch += dy * sensitivity;

		//Check for keyboard movement with the arrow keys
		/*if (glfwGetKey(Key.UP)) pitch -= keyboardSensitivity;
		if (glfwGetKey(Key.DOWN)) pitch += keyboardSensitivity;
		if (glfwGetKey(Key.RIGHT)) yaw -= keyboardSensitivity;
		if (glfwGetKey(Key.LEFT)) yaw += keyboardSensitivity;*/

		//constrain vertical looking (prevent backflips!)
		if (pitch < 0.0f)
			pitch = 0.0f;
		else if (pitch > Math.PI)
			pitch = (float)Math.PI;

		//keep the horizontal angle sane
		if (yaw > CrxMath.M_2PIf || yaw < -CrxMath.M_2PIf)
			yaw = 0.0f;

		return dx != 0 || dy != 0;
	}

	public void apply_glRotatef()
	{
		//note: the following assumes Z axis point upwards	(glRotatef(-90, 1, 0, 0))
		glRotatef(pitch * CrxMath.Rad2Degf - 90.0f, 1, 0, 0);
		glRotatef(-yaw * CrxMath.Rad2Degf + 90.0f, 0, 0, 1);
	}

	/**Get the angles of the sight ray in radians*/
	public SphereCoord3f getLookRay()
	{
		return new SphereCoord3f(yaw, pitch, 1.0f);
	}

	public void setLookRay(SphereCoord3f ray)
	{
		yaw = ray.yaw;
		pitch = ray.pitch;
	}
}
