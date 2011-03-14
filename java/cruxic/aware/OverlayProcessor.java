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

import cruxic.math.Rect4i;
import cruxic.aware.ipo.IPOTimeSource;

import java.util.*;

/**
	Provides asynchronous loading and rendering of viewpoint Overlays.
 For example, animated water effect overlays are too slow to render on
 the fly - instead this OverlayProcessor updates them asynchronously using
 one or more threads.
 */
public class OverlayProcessor
	implements Runnable
{
	private static final boolean DEBUG = false;

	private Thread thread;

	//Map of viewpoint-id -> Job
	private final Map<String, Job> jobs;

	final Object readyMutex;

	private boolean stop;

	private IPOTimeSource timer;

	private EngineTimer debugTimer;
	private EngineTimer debugTimer2;

	public OverlayProcessor()
	{
		timer = Engine.instance.newTimeSource();

		jobs = new HashMap<String, Job>(16);

		readyMutex = new Object();

		if (DEBUG)
		{
			debugTimer = new EngineTimer();
			debugTimer2 = new EngineTimer();
		}

		thread = new Thread(this);
		thread.start();

	}

	public void shutdown()
	{
		if (DEBUG) System.out.printf("OverlayProcessor: shutting down...\n");

		//wake up thread
		synchronized (jobs)
		{
			stop = true;
			jobs.notify();
		}

		//wake up render thread
		synchronized (readyMutex)
		{
			readyMutex.notify();
		}

		try
		{
			thread.join();
		}
		catch (InterruptedException e)
		{
			//ignore
		}

		if (DEBUG) System.out.printf("OverlayProcessor: stopped\n");
	}

	/**Start asynchronous loading of the overlays used by the given Viewpoint*/
	public void load(Viewpoint vp)
	{
		List<OverlaySpec> ospecs = vp.getOverlays();
		if (!ospecs.isEmpty())
		{
			if (DEBUG) System.out.printf("OverlayProcessor: load request: %s\n", vp.getId());

			Job job = new Job(ospecs, vp.getImage());
			synchronized (jobs)
			{
				//don't put duplicate jobs
				if (!jobs.containsKey(vp.getId()))
				{
					jobs.put(vp.getId(), job);

					//wake the processor thread
					jobs.notify();
				}
				else if (DEBUG) System.out.printf("OverlayProcessor: %s already loaded\n", vp.getId());
			}
		}
	}

	public void unload(Viewpoint vp)
	{
		boolean foundJob = false;

		synchronized (jobs)
		{
			Job job = jobs.get(vp.getId());
			if (job != null)
			{
				if (DEBUG) System.out.printf("OverlayProcessor: unload request: %s\n", vp.getId());

				//tell processor thread to unload it
				job.unloadRequested = true;
				job.consumed = true;  //in case processor is waiting on this job to be consumed
				//System.out.println("request unload of " + e.getValue());
				foundJob = true;
			}
		}

		//wake processor thread if it's sleeping on readyMutex
		if (foundJob)
		{
			synchronized (readyMutex)
			{
				readyMutex.notify();
			}
		}

	}

	/**Cease updating and dispose all Overlays NOT in the given Set*/
	public void retain_only(Viewpoint... keepers)
	{
//		System.out.println("keepers:");
//		for (Viewpoint vp: keepers)
//			System.out.println("\t" + vp.getId());
		
		boolean unloadedSome = false;

		synchronized (jobs)
		{
			//for each job...
			for (Map.Entry<String, Job> e: jobs.entrySet())
			{
				boolean isKeeper = false;

				//linear search is OK because keepers is usually only 2
				for (Viewpoint keeper: keepers)
				{
					if (keeper.getId().equals(e.getKey()))
					{
						isKeeper = true;
						break;
					}
				}

				if (!isKeeper)
				{
					if (DEBUG) System.out.printf("OverlayProcessor: unload request: %s\n", e.getKey());

					//tell processor thread to unload it
					Job job = e.getValue();
					job.unloadRequested = true;
					job.consumed = true;  //in case processor is waiting on this job to be consumed
					//System.out.println("request unload of " + e.getValue());
					unloadedSome = true;
				}
			}
		}

		//wake processor thread if it's sleeping on readyMutex
		if (unloadedSome)
		{
			synchronized (readyMutex)
			{
				readyMutex.notify();
			}
		}
	}

	/**Update the currently bound OpenGL texture with the processed overlays
	 of the specified Viewpoint.

	 After the upload the OverlayProcessor will begin rendering the next frame
	 This should only be called once per frame.
	 */
	public void upload(Viewpoint vp)
	{
		final int MAX_WAIT_MS = 500;

		//Get the job associated with given viewpoint
		Job job;
		synchronized (jobs)
		{
			job = jobs.get(vp.getId());
		}

		//bail if job is no longer active
		if (stop || job == null)
		{
			if (DEBUG) System.out.printf("OverlayProcessor: skipping upload of %s\n", vp.getId());
			return;
		}

		if (DEBUG)
			debugTimer.mark();

		//Wait for this job to become ready for consumption
		synchronized (readyMutex)
		{
			while (job.consumed && !stop)
			{
				try
				{
					readyMutex.wait(MAX_WAIT_MS);

					if (job.consumed && !stop)
					{
						System.err.println("OverlayProcessor: timed out");
						return;
					}
				}
				catch (InterruptedException ie)
				{
					//loop again
				}
			}
		}

		if (stop)
			return;

		if (DEBUG && debugTimer.elapsedSec() > 0.075f) System.out.printf("OverlayProcessor: slow service (%g seconds for %s)\n", debugTimer.elapsedSec(), vp.getId());

		//Consume!
		for (OverlayGroup grp: job.groups)
			GLImageUpload.glTexSubImage2D(grp.unionRect.x1, grp.unionRect.y1, grp.buf);

		//Tell processor thread that this job has been consumed
		synchronized (readyMutex)
		{
			job.consumed = true;
			readyMutex.notify();
		}
	}

	/**Group together all Overlays which overlap with another.*/
	private static List<OverlayGroup> findOverlappingOverlays(List<Overlay> overlays)
	{
		IdentityHashMap<Overlay, OverlayGroup> gm = new IdentityHashMap<Overlay, OverlayGroup>((int)(overlays.size() * 1.35f));

		//foreach Overlay
		for (int i = 0; i < overlays.size(); i++)
		{
			Overlay o1 = overlays.get(i);
			Rect4i o1Rect = o1.getRect();

			OverlayGroup grp = gm.get(o1);

			//foreach Overlay beyond current
			for (int j = i + 1; j < overlays.size(); j++)
			{
				Overlay o2 = overlays.get(j);

				//o2 is not yet grouped and it overlaps with o1?
				if (!gm.containsKey(o2) && o1Rect.overlaps(o2.getRect()))
				{
					if (grp == null)
					{
						grp = new OverlayGroup();
						grp.overlays.add(o1);
						gm.put(o1, grp);
					}

					grp.overlays.add(o2);
					gm.put(o2, grp);
				}
			}

			//o1 does not overlap with any others
			if (grp == null)
			{
				grp = new OverlayGroup();
				grp.overlays.add(o1);
				gm.put(o1, grp);
			}
		}

		List<OverlayGroup> groups = new ArrayList<OverlayGroup>();
		int grpNum = 0;
		int nOverlays = 0;
		for (OverlayGroup grp: gm.values())
		{
			//add only distinct groups
			if (!groups.contains(grp))
			{
				groups.add(grp);
				nOverlays += grp.overlays.size();

				//grp.unionRect = grp.computeUnionRect();
				//grp.buf = GLImage.allocateBlank(grp.unionRect.width(), grp.unionRect.height(), (byte)0);

				if (DEBUG) System.out.printf("OverlayProcessor: Group %d: %d overlays\n", grpNum++, grp.overlays.size());

				//System.out.print("\t");
				//grp.unionRect.debugPrint();
			}
		}

		assert nOverlays == overlays.size();

		return groups;
	}

	public void run()
	{
		if (DEBUG) System.out.printf("OverlayProcessor: started\n");

		ArrayList<Job> consumed = new ArrayList<Job>(8);
		ArrayList<Job> jobsCopy = new ArrayList<Job>(8);

		while (!stop)
		{
			//Check for new jobs to be loaded
			synchronized (jobs)
			{
				//Wait for at least one job to arrive
				while (jobs.isEmpty() && !stop)
				{
					try
					{
						if (DEBUG) System.out.printf("OverlayProcessor: waiting for jobs...\n");
						jobs.wait();
					}
					catch (InterruptedException ie)
					{
						//ignore
					}
				}

				//make a copy of 'jobs' list so I can read it outside of synchronized block
				jobsCopy.clear();
				jobsCopy.addAll(jobs.values());
			}

			if (stop)
				return;

			assert !jobsCopy.isEmpty();

			//Unload old jobs
			Iterator<Job> itr = jobsCopy.iterator();
			while (itr.hasNext())
			{
				Job job = itr.next();
				if (job.unloadRequested)
				{
					job.unload();
					itr.remove();

					synchronized (jobs)
					{
						jobs.values().remove(job);
					}
				}
			}

			//Load new jobs
			for (Job job: jobsCopy)
			{
				if (!job.loaded)
				{
					try
					{
						job.load();
					}
					catch (Exception e)
					{
						e.printStackTrace();

						//Ditch this job due to error
						synchronized (jobs)
						{
							jobs.values().remove(job);
						}
					}
				}
			}

			//Determine which jobs have been consumed and are
			//  ready for me to process the next frame
			consumed.clear();
			synchronized (readyMutex)
			{
				while (!stop && consumed.isEmpty())
				{
					consumed.clear();
					for (Job job: jobsCopy)
					{
						if (job.consumed)
							consumed.add(job);
					}

					//Wait for render thread to consume one
					// OR for jobs to change
					if (consumed.isEmpty())
					{
						try
						{
							readyMutex.wait();
						}
						catch (InterruptedException e)
						{
							//ignore - just loop again
						}
					}
				}
			}

			if (stop)
				return;

			assert !consumed.isEmpty();
			//Process consumed jobs
			for (Job job: consumed)
			{
				float elapsed = timer.elapsedSec();

				try
				{
					if (DEBUG)
						debugTimer2.mark();

					for (OverlayGroup grp: job.groups)
						grp.composite(elapsed, job.background);

					if (DEBUG && debugTimer2.elapsedSec() > 0.065f) System.out.printf("OverlayProcessor: slow compositing (%g seconds for %s)\n", debugTimer2.elapsedSec(), job.toString());
				}
				catch (Exception e)
				{
					e.printStackTrace();

					//Ditch this job due to error
					synchronized (jobs)
					{
						jobs.values().remove(job);
						job.unload();
					}
				}

				//Tell render thread that a job is ready to be consumed
				synchronized (readyMutex)
				{
					if (!job.unloadRequested)
						job.consumed = false;
					readyMutex.notify();
				}
			}
		}
	}

	private static class Job
	{
		List<OverlayGroup> groups;
		String backgroundImageId;
		GLImage background;
		List<OverlaySpec> specs;

		/**True after load() has been called*/
		boolean loaded;

		/**True after the render thread has "consumed" the processed overlay
		 so that the OverlayProcessor can start preparing the next frame of animation*/
		boolean consumed;

		/**true when an unload has been requested*/
		boolean unloadRequested;

		public Job(List<OverlaySpec> specs, String backgroundImageId)
		{
			groups = new ArrayList<OverlayGroup>(16);
			this.specs = specs;
			this.backgroundImageId = backgroundImageId;
			loaded = false;
			unloadRequested = false;
			consumed = true;  //process first frame immediately
		}

		void load()
		{
			if (DEBUG) System.out.printf("OverlayProcessor: loading %s\n", toString());
			//Load all Overlays as one big list
			List<Overlay> allOverlays = new ArrayList<Overlay>(16);
			for (OverlaySpec spec: specs)
				allOverlays.addAll(spec.loadOverlays(Engine.instance));

			//group them
			groups.addAll(findOverlappingOverlays(allOverlays));

			//Load Background image
			background = Engine.instance.texCache.getGLImage(backgroundImageId);

			loaded = true;
			if (DEBUG) System.out.printf("OverlayProcessor: loaded %s\n", toString());
		}

		void unload()
		{
			if (loaded)
			{
				for (OverlayGroup grp: groups)
				{
					for (Overlay ol: grp.overlays)
						ol.dispose();
				}
				if (DEBUG) System.out.printf("OverlayProcessor: disposed %s\n", toString());
			}
		}
	}

	/**Processing of each OverlayGroup can be parallelized*/
	private static class OverlayGroup
	{
		/**the rectangle surrounding all the overlays*/
		Rect4i unionRect;

		/**an image that will be updated each frame.  same dimensions as unionRect*/
		GLImage buf;
		
		List<Overlay> overlays;

		OverlayGroup()
		{
			overlays = new ArrayList<Overlay>(4);
		}

		void composite(float elapsedSec, GLImage background)
		{
			//first call?
			if (unionRect == null)
			{
				unionRect = computeUnionRect();

				//bug workaround: For some reason certain widths of rectangles
				//  causes strange gray looking textures after uploading with glTexSubImage2D().
				//  I can avoid the issue by making sure width is an even multiple of 8.  I picked
				//  8 out of the blue and it seems to work even though I don't fully understand the problem.
				Rect4i bgRect = new Rect4i(0, 0, background.width, background.height);
				unionRect.round(bgRect, 8);

				buf = GLImage.allocateBlank(unionRect.width(), unionRect.height(), (byte)0);
			}

			//Optimization: If there's only one Overlay and it overwrites everything
			//  then don't bother copying from background.  For example, a video overlay
			if (overlays.size() > 1 || !overlays.get(0).isOpaque())
			{
				//Must copy background
				buf.copyRect(background, unionRect.x1, unionRect.y1, unionRect.x2, unionRect.y2);
			}

			for (Overlay overlay: overlays)
			{
				//buf is often a larger rect than the overlay so we need
				// to compute the offset of this overlay within the buf rect.
				Rect4i rect = overlay.getRect();
				int offsetX = rect.x1 - unionRect.x1;
				int offsetY = rect.y1 - unionRect.y1;

				overlay.update(elapsedSec, buf, offsetX, offsetY);
			}
		}

		/**Find the bounding rectangle of all overlays and assign to unionRect*/
		public Rect4i computeUnionRect()
		{
			if (overlays.size() == 1)
				return new Rect4i(overlays.get(0).getRect());
			else
			{
				Rect4i ur = new Rect4i(Integer.MAX_VALUE, Integer.MAX_VALUE,
					Integer.MIN_VALUE, Integer.MIN_VALUE);

				for (Overlay ol: overlays)
				{
					Rect4i r = ol.getRect();

					//left
					if (r.x1 < ur.x1)
						ur.x1 = r.x1;
					//right
					if (r.x2 > ur.x2)
						ur.x2 = r.x2;
					//bottom
					if (r.y1 < ur.y1)
						ur.y1 = r.y1;
					//top
					if (r.y2 > ur.y2)
						ur.y2 = r.y2;
				}

				return ur;
			}
		}
	}
}
