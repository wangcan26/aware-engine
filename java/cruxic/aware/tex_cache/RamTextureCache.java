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
package cruxic.aware.tex_cache;

import cruxic.aware.*;
import cruxic.aware.misc.WorkingSet;
import java.util.*;

/**
	BEWARE this class is not thread safe.  Only one thread may
 	call public methods at a time.
 */
public class RamTextureCache
	implements TextureLoaderPoolSink, ResourceDestructor<GLImage>
{
	private static final boolean DEBUG = false;

	private final SizeBasedCache<String, GLImage> cache;
	private ImageLoaderPool loaderPool;

	private Set<String> urgentImageIds;

	private WorkingSet<String> curSet;

	public RamTextureCache(ResourceManager resMgr)
	{
		cache = new SizeBasedCache<String, GLImage>(256 * 1024 * 1024, this);
		cache.debugName = "RAM:SizeBasedCache";
		loaderPool = new ImageLoaderPool(1, this, resMgr);
		curSet = new WorkingSet<String>();
		urgentImageIds = new HashSet<String>();
	}

	public WorkingSet<String> copyWorkingSet()
	{
		synchronized (cache)
		{
			return new WorkingSet<String>(curSet);
		}
	}

	public void changeWorkingSet(WorkingSet<String> newSet)
	{
		//cancel any pending jobs so that we don't waste time loading images no longer needed
		loaderPool.clearJobs();

		if (DEBUG)
		{
			System.out.println("RamTextureCache: changeWorkingSet:");
			for (String imageId: newSet.getPrioritizedList())
				System.out.printf("\t%s (pri %d)\n", imageId, newSet.getPriority(imageId));
		}

		synchronized (cache)  //because of race with imageLoaded()
		{
			//adjust priorities of already cached items.
			//(No longer needed items get super low priority)
			cache.updatePriority(newSet);

			//switch working sets
			curSet = newSet;

			//Start loading those which we are missing
			for (String imageId: newSet.getPrioritizedList())
			{
				if ( ! cache.contains(imageId))
				{
					if (DEBUG) System.out.printf("RamTextureCache: not cached: %s\n", imageId);
					loaderPool.addJob(imageId);
				}
			}
		}
	}

	/**Get the GLImage associated with the specified image-id.  If the image
	is cached in memory this call will be very fast, otherwise it will be loaded
	from disk and saved to cache.

	 @return null if the specified image is not in the current working set
	*/
	public GLImage getGLImage(String imageId)
	{
		synchronized (cache)
		{
			GLImage img = cache.get(imageId);
			if (img == null && curSet.contains(imageId))
			{
				//elevate the urgency of this image
				if (DEBUG) System.out.printf("RamTextureCache: requesting immediate load of %s\n", imageId);
				urgentImageIds.add(imageId);
				loaderPool.makeTopPriority(imageId);

				long startTime = System.currentTimeMillis();

				//wait for image to arrive
				while (curSet.contains(imageId)
					&& !cache.contains(imageId))
				{
					try
					{
						cache.wait(10000);

						img = cache.get(imageId);

						//timed out?
						if (img == null && (System.currentTimeMillis() - startTime) >= 10000)
						{
							System.err.println("RamTextureCache: timed out waiting for " + imageId);
							break;
						}
					}
					catch (InterruptedException e)
					{
						//ignore
					}
				}

				urgentImageIds.remove(imageId);

				//use fallback image
				if (img == null)
					img = GLImage.createNullImage();
			}
			else if (DEBUG && img == null)
				System.out.printf("RamTextureCache: %s not in working set\n", imageId);

			return img;
		}
	}

	public boolean shouldLoad(String filePath, int estimatedImageSize)
	{
		synchronized (cache)  //because of race with changeWorkingSet() and getGLImage
		{
			//if somebody is waiting for it we have no choice
			if (urgentImageIds.contains(filePath) && curSet.contains(filePath))
				return true;
			else
			{
				int priority = curSet.getPriority(filePath);

				//lower priority by one - ImageLoaderPool should not bother loading an image
				//if it's just going to replace one of equal priority - there's no point
				if (priority > Integer.MIN_VALUE)
					priority--;

				return cache.canStore(estimatedImageSize, priority);
			}
		}
	}

	public void imageLoaded(String filePath, GLImage image)
	{
		synchronized (cache)  //because of race with changeWorkingSet() and getGLImage
		{
			int priority = curSet.getPriority(filePath);
			boolean isInCurSet = priority > Integer.MIN_VALUE;

			if (DEBUG) System.out.printf("RamTextureCache: loaded %s (pri=%d)\n", filePath, priority);

			//Store the loaded image in cache
			boolean full = !cache.store(filePath, image, image.nBytes, priority);

			//Stop loading images if the cache is full.
			//Second condition is important in case image was from previous
			// working set - we don't want to abort loading the new set
			if (full && isInCurSet)
			{
				if (DEBUG) System.out.println("RamTextureCache: cache is full, stop loading");
				loaderPool.clearJobs();
			}

			//wake up threads inside getGLImage()
			cache.notifyAll();
		}
	}

	public void freeResource(GLImage resource)
	{
		//System.out.printf("RamTextureCache: freeing %dMiB\n", resource.nBytes / (1024*1024));
		resource.dispose();
	}

	public void shutdown()
	{
		synchronized (cache)
		{
			cache.clear();
			curSet.clear();
			urgentImageIds.clear();

			//wake any sleeping threads
			cache.notifyAll();
		}

		loaderPool.shutdown();
	}
}