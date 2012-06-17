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

import java.util.*;

/**
	A thread pool which loads texture files from disk.
 */
class ImageLoaderPool
	implements Runnable
{
	private static final boolean DEBUG = false;

	private boolean stop;
	private Thread theThread;
	private final LinkedList<String> jobs;
	private final TextureLoaderPoolSink sink;
	private final ResourceManager resMgr;

	public ImageLoaderPool(int numThreads, TextureLoaderPoolSink sink, ResourceManager resMgr)
	{
		assert(numThreads == 1);
		this.sink = sink;
		this.resMgr = resMgr;
		jobs = new LinkedList<String>();
		theThread = new Thread(this);
		theThread.start();
	}

	public void shutdown()
	{
		stop = true;
		//wake loader thread
		addJob("");

		try
		{
			theThread.join();
		}
		catch (InterruptedException e)
		{
			//ignore
		}
	}

	public void clearJobs()
	{
		synchronized (jobs)
		{
			jobs.clear();
		}
	}

	/**Enqueue a request to load the specified image.
	 If the request has already been made this call does nothing*/
	public void addJob(String filePath)
	{
		synchronized (jobs)
		{
			if (!jobs.contains(filePath))
			{
				jobs.add(filePath);

				//wake thread
				jobs.notify();
			}
		}
	}

	/**Request to load the specified image as soon as possible.
	 You must have called addJob previously.*/
	public void makeTopPriority(String filePath)
	{
		synchronized (jobs)
		{
			//Put at head of the queue ONLY if it's already in the queue.
			if (jobs.remove(filePath))
			{
				//bugfix: previously I would unconditionally re-add the job to the head of the
				//	queue.  This exposed a race condition which caused the same image to be loaded
				//	twice because addJob() was called and then shortly later this function.
				jobs.addFirst(filePath);
				//wake thread
				jobs.notify();
			}
		}
	}

	public void run()
	{
		stop = false;
		while (!stop)
		{
			//Wait for a job
			String nextJob;
			synchronized (jobs)
			{
				while (!stop && jobs.isEmpty())
				{
					try
					{
						jobs.wait();
					}
					catch (InterruptedException e)
					{
						//ignore
					}
				}

				if (stop)
					break;

				nextJob = jobs.poll();
			}

			//Process the job
			try
			{
				if (sink.shouldLoad(nextJob, resMgr.estimateImageSize(nextJob)))
				{
					if (DEBUG) System.out.printf("ImageLoaderPool: loading %s\n", nextJob);
					sink.imageLoaded(nextJob, resMgr.readGLImage(nextJob));
				}
				else if (DEBUG)
					System.out.printf("ImageLoaderPool: skipping %s\n", nextJob);
			}
			catch (Exception e)
			{
				e.printStackTrace();

				//use fallback image
				sink.imageLoaded(nextJob, GLImage.createNullImage());
			}
		}
	}
}

interface TextureLoaderPoolSink
{
	public boolean shouldLoad(String filePath, int estimatedImageSize);
	public void imageLoaded(String filePath, GLImage image);
}
