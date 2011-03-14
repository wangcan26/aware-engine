package cruxic.aware;

/**
 	An image texture sitting in main-memory that can be quickly uploaded
 	to graphics card memory.
 
	Most of this class is implemented as native code for maximum speed
  because Aware-Engine works with huge textures (eg 4096x2048).
 */
public class GLImage
{
	//load native library
	static { System.loadLibrary("aware"); }

	public final int width;
	public final int height;
	/**how many bytes the raw image data occupies*/
	public final int nBytes;

	/**pointer to native GLImage object.  0 means the object is invalid*/
	//KEEP: referenced by native code
	private long nativePtr; 

	//KEEP: referenced by native code
	private GLImage(long nativePtr, int w, int h, int numBytes)
	{
		this.nativePtr = nativePtr;
		width = w;
		height = h;
		this.nBytes = numBytes;
	}

	/**Free native image-data immediately
	 (as opposed to waiting for garbage collection).*/
	public native void dispose();

	public void finalize()
		throws Throwable
	{
		//free native ram if necessary
		if (nativePtr != 0)
			dispose();

		super.finalize();
	}

	public static native GLImage loadFromFile(String fileName)
		throws IOExceptionRt;

	/**read the header of the image file to determine it's uncompressed size in bytes.*/
	public static native int readImageSize(String imageId)
		throws IOExceptionRt;

	public static native GLImage createFromRawBytes(int width, int height, byte[] raw)
		throws IOExceptionRt;

	public static native GLImage allocateBlank(int width, int height, byte initialGrayscaleColor)
		throws IOExceptionRt;

	/**Create a checker pattern.

	 @param size the width and height of the texture
	 @param density pixels per checker square
	 	 
	 */
	public static GLImage createCheckerBoard(int size, int density, int color1_rgb, int color2_rgb)
	{
		assert size >= 4;  //any smaller than 4 causes weird results on my card
		assert size > density;
		//assert size % density == 0;

		byte[] texels = new byte[size * size * 3];

		//make checkerboard
		int pos = 0;
		final int check_size = size / density;
		for (int r = 0; r < size; r++)
		{
			boolean evenRow = ((r / check_size) & 1) > 0;

			for (int c = 0; c < size; c++)
			{
				boolean black = ((c / check_size) & 1) > 0;
				if (!evenRow)
					black = !black;

				copyColorBytes(texels, pos, black ? color1_rgb : color2_rgb);

				pos += 3;
			}
		}

		return GLImage.createFromRawBytes(size, size, texels);
	}

	private static void copyColorBytes(byte[] destination, int offset, int rgbColor)
	{
		destination[offset]     = (byte)((rgbColor & 0xFF0000) >>> 16);  //red
		destination[offset + 1] = (byte)((rgbColor & 0x00FF00) >>> 8);  //green
		destination[offset + 2] = (byte)(rgbColor & 0x0000FF);          //blue
	}

	public static GLImage createSolid(int size, int rgbColor)
	{
		assert size >= 4;  //any smaller than 4 causes weird results on my card

		byte[] texels = new byte[size * size * 3];

		//set all pixels to greenish: #446921
		for (int i = 0; i < texels.length; i += 3)
		{
			copyColorBytes(texels, i, rgbColor);
		}

		return GLImage.createFromRawBytes(size, size, texels);
	}

	public static GLImage createNullImage()
	{
		return createCheckerBoard(128, 16, 0x999999, 0x666666);
		//return makeSolid(4, 0x446921);
	}

	public native void copyRect(GLImage src, int x1, int y1, int x2, int y2)
		throws IllegalArgumentException;

	public native void fillRect(int x1, int y1, int x2, int y2, byte r, byte g, byte b)
		throws IllegalArgumentException;

	public native void combine(GLImage img, int x1, int y1, int x2, int y2)
		throws IllegalArgumentException;
}
