package cruxic.aware;

import cruxic.aware.tex_cache.TextureCache;

import java.util.*;
import java.io.File;

/**
	Manages resources within the game such as textures, sounds, and music.
 */
public class ResourceManager
{
	/**image-id of the fallback image texture*/
	public static final String ID_IMAGE_NULL = "NULL";

	public ResourceManager()
	{

	}

	public GLImage readGLImage(String imageId)
	{
		if (imageId == null || imageId.length() == 0 || imageId.equals(ID_IMAGE_NULL))
			return GLImage.createNullImage();
		{
			try
			{
				return GLImage.loadFromFile(imageId);
			}
			catch (IOExceptionRt ioe)
			{
				ioe.printStackTrace();
				
				//fallback to the null image
				return GLImage.createNullImage();
			}
		}
	}

	/**Esimate the uncompressed size of the specified image (in bytes).
	 This is used by RamTextureCache when deciding if it has room to preload the image
	 into memory.*/
	public int estimateImageSize(String imageId)
	{
		final int NULL_IMAGE_SIZE = 128 * 128 * 3;
		if (imageId == null || imageId.length() == 0 || imageId.equals(ID_IMAGE_NULL))
			return NULL_IMAGE_SIZE;
		{
			try
			{
				return GLImage.readImageSize(imageId);
				//System.out.printf("%s: %dMiB\n", imageId, size / (1024 * 1024));
			}
			catch (IOExceptionRt ioe)
			{
				ioe.printStackTrace();

				//fallback to the null image
				return NULL_IMAGE_SIZE;
			}
		}
	}

	public static class FileResolver
	{
		private String baseDir;
		private StringBuilder sb;

		public FileResolver(String baseDir)
		{
			this.baseDir = baseDir;
			sb = new StringBuilder(128);
		}

		/**when reading an image file name from the game spec file sometimes an exension.
		 */
		public String resolveImageId(String alias)
		{
			sb.setLength(0);

			//remove white space
			alias = alias.trim();

			//all files not in "res/" are relative to the basedir
			if (!alias.startsWith("res/"))
			{
				sb.append(baseDir);
				sb.append('/');
			}

			sb.append(alias);

			//if no extension then use the default
			if (!alias.endsWith(".png"))
				sb.append(".png");

			//TODO: Correct the slashes for the platform
			if (sb.indexOf("/") != -1 && !File.separator.equals("/"))
			{
				throw new UnsupportedOperationException("fix slashes");
			}

			return sb.toString();
		}
	}
}
