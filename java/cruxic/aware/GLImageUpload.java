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

/**
	Native functions for creating OpenGL textures from GLImage raster data.
 */
public class GLImageUpload
{
	/**Create one or more OpenGL texture objects from the given GLImage.
	 @returns an array of the OpenGL assigned texture Ids*/
	public native static int[] loadSlicedTexture(GLImage unsliced, int numSlices);

	/**Update the texture data associated with the currently bound texture object glBindTexture*/
	public native static void glTexImage2D(GLImage image);

	/**Update the texture data associated with the currently bound texture object glBindTexture*/
	public native static void glTexSubImage2D(int xOffset, int yOffset, GLImage image);
}
