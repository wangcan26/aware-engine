package cruxic.aware.tex_cache;

import cruxic.aware.*;


import static org.lwjgl.opengl.GL11.*;

/**

 */
public class GLTextureCache
	implements ResourceDestructor<int[]>
{
	private static final boolean DEBUG = false;

	private final SizeBasedCache<String, int[]> cache;

	int _maxGLTextureDimension;

	public GLTextureCache()
	{
		cache = new SizeBasedCache<String, int[]>(128 * 1024 * 1024, this);
		cache.debugName = "GL:SizeBasedCache";
		_maxGLTextureDimension = -1;  //lazy initialized
	}

	public int[] getTexIds(String imageId)
	{
		return cache.get(imageId);
	}

	public int[] load(String imageId, GLImage img, boolean pinned)
	{
		int nSlices = detectNecessarySlicing(img);
		if (DEBUG) System.out.printf("GLTextureCache: loading %s; %d slices; %dMiB\n", imageId, nSlices, img.nBytes / (1024 * 1024));

		//All images have same priority (except pinned entries which have maximum priority).
		//By using constant priority I'm essentially guaranteed that the store() call will succeed.
		int priority = pinned ? Integer.MAX_VALUE : 0;
		int[] texIds = GLImageUpload.loadSlicedTexture(img, nSlices);
		boolean added = cache.store(imageId, texIds, img.nBytes, priority);
		assert(added);

		return texIds;
	}

	//force slicing if image is too large
	private int detectNecessarySlicing(GLImage image)
	{
		int maxSize = getMaxGLTextureDim();
		int numTextureSlices = 1;
		if (image.width > maxSize)
		{
			numTextureSlices = (int)Math.ceil(image.width / (double)maxSize);
			if (DEBUG && numTextureSlices > 1) System.out.printf("GLTextureCache: Slicing image into %d textures\n", numTextureSlices);
		}

		return numTextureSlices;
	}

	private int getMaxGLTextureDim()
	{
		if (_maxGLTextureDimension == -1)
		{
			_maxGLTextureDimension = glGetInteger(GL_MAX_TEXTURE_SIZE);
			if (DEBUG) System.out.printf("GL_MAX_TEXTURE_SIZE=%d\n", _maxGLTextureDimension);
		}

		return _maxGLTextureDimension;
	}

	public void freeResource(int[] texIds)
	{
		//ToDo: use array version?
		for (int texId: texIds)
		{
			glDeleteTextures(texId);
			//System.out.printf("GLTextureCache: freeing texture ID=%d\n", texId);
		}
	}

	public void clear()
	{
		cache.clear();
	}
}
