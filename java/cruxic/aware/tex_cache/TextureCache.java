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

 */
public class TextureCache
{
	private static final boolean DEBUG = false;

	private ResourceManager resMgr;

	public GLTextureCache vram;
	private RamTextureCache ram;

	private ArrayList<WorkingSet<String>> workingSetStack;

	public TextureCache(ResourceManager resMgr)
	{
		this.resMgr = resMgr;
		workingSetStack = new ArrayList<WorkingSet<String>>(4);
		vram = new GLTextureCache();
		ram = new RamTextureCache(resMgr);
	}

	public void changeWorkingSet(WorkingSet<String> newSet)
	{
		//Images already cached in VRAM should have lower priority in th RAM cache.
		//At the same time we should update their LRU stamp in the VRAM cache to
		//encourage them to stay in VRAM.

		List<String> newList = newSet.getPrioritizedList();
		Collections.reverse(newList);  //sort low to high so that LRU stamp is greatest on higher priority items

		for (String imageId: newList)
		{
			//getTexIds also updates the LRU stamp
			if (vram.getTexIds(imageId) != null)
				newSet.adjustPriority(imageId, -1);
		}
		
		ram.changeWorkingSet(newSet);
	}

	public void pushWorkingSet(WorkingSet<String> newSet)
	{
		//save the current one onto the stack
		workingSetStack.add(ram.copyWorkingSet());

		//switch to new one
		changeWorkingSet(newSet);
	}

	public void popWorkingSet()
	{
		int stackSize = workingSetStack.size();
		if (stackSize > 0)
		{
			WorkingSet<String> oldSet = workingSetStack.get(stackSize - 1);
			changeWorkingSet(oldSet);
		}
		else
		{
			//switch to empty set
			changeWorkingSet(new WorkingSet<String>());
		}


	}

	/**Get the OpenGL texture object ids associated with the given image-id
	as fast as possible.  If this texture has been requested recently this call
	will be instantaneous because the texture is already loaded in the video card.*/
	public int[] getTexture(String imageId)
	{
		//already in VRAM?
		int[] tex = vram.getTexIds(imageId);
		if (tex != null)
			return tex;
		else
		{
			if (DEBUG) System.out.printf("TextureCache: not in VRAM: %s\n", imageId);

			//else get from RAM (will be loaded from disk if not in ram)
			GLImage ramImg = getGLImage(imageId);

			//load into VRAM
			tex = vram.load(imageId, ramImg, false);

			return tex;
		}
	}

	/**Get the specified image data if it's in the current working-set
	 otherwise fallback to the Null image*/
	public GLImage getGLImage(String imageId)
	{
		GLImage img = ram.getGLImage(imageId);
		if (img != null)
			return img;
		else
		{
			//ToDo: in the future merely add it to the working-set and let RamTextureCache manage it's memory
			System.err.println("warning: " + imageId + " was not in RamTextureCache working set; memory will not be disposed in a timely manner.");
			return resMgr.readGLImage(imageId);
		}
	}

	public void shutdown()
	{
		vram.clear();
		ram.shutdown();
	}
}
