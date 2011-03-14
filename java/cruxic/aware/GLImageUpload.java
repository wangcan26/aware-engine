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
