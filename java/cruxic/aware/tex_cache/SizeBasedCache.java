package cruxic.aware.tex_cache;

import cruxic.aware.misc.WorkingSet;

import java.util.*;

/**
	A limited size cache of items.  If the cache gets full
 then low priority and infrequently used items are removed from
 the cache.

 BEWARE: this class is not thread safe.
 */
public class SizeBasedCache<K, V>
{
	private static class CacheEntry<K,V>
		implements Comparable<CacheEntry<K,V>>
	{
		final K id;
		final V item;
		final int size;
		int priority;

		/**when this item was last used.  Higher numbers mean more recently.
		 The unit of time does not correspond to any true concept of time.*/
		int lastUsedTime;

		CacheEntry(K itemId, V item, int size, int priority, int clockValue)
		{
			this.id = itemId;
			this.item = item;
			this.size = size;
			this.priority = priority;
			this.lastUsedTime = clockValue;
		}

		//Sort lowest priority and least recently used items first
		public int compareTo(CacheEntry<K,V> e)
		{
			//note: I avoid subtraction trick in case of numeric overflow
			if (priority > e.priority)
				return 1;
			else if (priority < e.priority)
				return -1;
			else //equal priority
			{
				if (lastUsedTime > e.lastUsedTime)
					return 1;
				else if (lastUsedTime < e.lastUsedTime)
					return -1;
				else
					return 0;  //same priority and usage time
			}
		}
	}

	private static final boolean DEBUG = false;
	String debugName = "SizeBasedCache";

	private final long maxCacheSize;

	private int clockValue;
	private final ResourceDestructor<V> resourceDestructor;

	private final HashMap<K, CacheEntry<K,V>> loaded;

	private final ArrayList<CacheEntry<K,V>> tempEntryList;

	public SizeBasedCache(long maxCacheSize, ResourceDestructor<V> resourceDestructor)
	{
		this.maxCacheSize = maxCacheSize;
		this.resourceDestructor = resourceDestructor;
		loaded = new HashMap<K, CacheEntry<K,V>>(128);

		tempEntryList = new ArrayList<CacheEntry<K,V>>(128);

		clockValue = 0;
	}

	/**Get a "time" value that can be used to track which items were
	 more recently used than others. Higher numbers mean more recent.
	 The unit of time does not correspond to any true concept of time.*/
	private final int getTime()
	{
		int time = clockValue++;

		//correct rollover
		if (time < 0)
		{
			time = 0;
			clockValue = 1;
			//ToDo: all clock values must be adjusted upon rollover because 1 < Integer.MAX_VALUE.  (Use an object to represent clock values?)
		}

		return time;
	}

	/**Try get an item from the cache.  If the cache does not have this
	 item, return null.  Each successful call marks the item as "recently used"
	 thus making it less likely to be removed from the cache later.*/
	public V get(K itemId)
	{
		CacheEntry<K,V> entry = loaded.get(itemId);
		if (entry != null)
		{
			entry.lastUsedTime = getTime();
			return entry.item;
		}
		else
			return null;
	}

	public boolean contains(K itemId)
	{
		return loaded.containsKey(itemId);
	}

	/**Change the priority of all items in this cache.
	 All items will be assigned the priority in the given WorkingSet.
	 Items not in the given set will be assigned the lowest possible priority*/
	public void updatePriority(WorkingSet<K> keepers)
	{
		for (Map.Entry<K, CacheEntry<K,V>> e: loaded.entrySet())
		{
			e.getValue().priority = keepers.getPriority(e.getKey());  //getPriority returns Integer.MIN_VALUE if item not found
		}
	}

	/**Get the current size of the cache*/
	public long getCacheSize()
	{
		long sum = 0;
		for (CacheEntry<K,V> entry: loaded.values())
			sum += entry.size;
		return sum;
	}

	/**Attempt to store an item in the cache.  The item will not be cached
	 if the cache is full and it has a lower priority than all other items.
	 If the item has an equal or higher priority it will replace one or more currently
	 cached items.

	 @return false if the cache is full of higher priority items.*/
	public boolean store(K itemId, V item, int itemSize, int priority)
	{
		//Compute current cache size and also
		//find which items could be evicted if necessary
		long curCacheSize = 0;
		long evictableSize = 0;
		tempEntryList.clear();
		for (CacheEntry<K,V> entry: loaded.values())
		{
			curCacheSize += entry.size;
			if (entry.priority <= priority)
			{
				evictableSize += entry.size;
				tempEntryList.add(entry);
			}
		}

		//Cache is too full?
		if (curCacheSize + itemSize > maxCacheSize)
		{
			//Could we make room?
			if ((curCacheSize - evictableSize) + itemSize <= maxCacheSize)
			{
				//Sort evictable items by lowest priority and least recently used
				Collections.sort(tempEntryList);

				//Remove items until there's room
				int idx = 0;
				while (curCacheSize + itemSize > maxCacheSize)
				{
					//release cache reference
					CacheEntry<K,V> victim = tempEntryList.get(idx++);
					boolean removed = loaded.remove(victim.id) != null;
					assert removed;

					//Explicitly free it.  We can't just wait for the garbage collector
					//because that sometimes takes ages.
					resourceDestructor.freeResource(victim.item);

					curCacheSize -= victim.size;

					if (DEBUG) System.out.printf("%s: evicited %s (pri=%d) for %s (pri=%d)\n", debugName, victim.id, victim.priority, itemId, priority);
				}
			}
			else
			{
				if (DEBUG) System.out.printf("%s: no room for %s (pri=%d; size=%dMiB; cached=%dMiB)\n", debugName, itemId, priority, itemSize / (1024 * 1024), curCacheSize / (1024 * 1024));

				return false;  //full
			}
		}

		//important: only 'loaded' should retain references to CacheEntry objects.
		tempEntryList.clear();

		//We have room!  Store the item
		loaded.put(itemId, new CacheEntry<K,V>(itemId, item, itemSize, priority, getTime()));
		if (DEBUG) System.out.printf("%s: added %s (size=%dMiB)\n", debugName, itemId, itemSize / (1024 * 1024));
		return true;
	}

	/**Determine ahead of time if store() will succeed*/
	public boolean canStore(int itemSize, int priority)
	{
		//Compute current cache size and also
		//find which items could be evicted if necessary
		long curCacheSize = 0;
		long evictableSize = 0;
		for (CacheEntry<K,V> entry: loaded.values())
		{
			curCacheSize += entry.size;
			if (entry.priority <= priority)
				evictableSize += entry.size;
		}

		//Could we make room?
		return (curCacheSize - evictableSize) + itemSize <= maxCacheSize;
	}

	/**free all resources in the cache and empty it out*/
	public void clear()
	{
		for (CacheEntry<K,V> entry: loaded.values())
		{
			//if (DEBUG) System.out.println("%s: clearing %d", debugName);
			resourceDestructor.freeResource(entry.item);
		}

		loaded.clear();
		tempEntryList.clear();  //just in case
		clockValue = 0;
	}

	public static void unit_test()
	{

		TestDestructor destructor = new TestDestructor();
		SizeBasedCache<Integer, String> cache = new SizeBasedCache<Integer, String>(10, destructor);

		assert(cache.getCacheSize() == 0);

		//Add 3 items
		assert(cache.canStore(3, -1));
		assert(cache.store(1, "One", 3, -1));
		assert(cache.canStore(2, 22));
		assert(cache.store(2, "Two", 2, 22));
		assert(cache.canStore(4, -3));
		assert(cache.store(3, "Three", 4, -3));
		assert(cache.contains(1));
		assert(cache.get(1).equals("One"));
		assert(cache.contains(2));
		assert(cache.get(2).equals("Two"));
		assert(cache.contains(3));
		assert(cache.get(3).equals("Three"));
		assert(cache.getCacheSize() == 9);

		//Too low priority
		assert( ! cache.canStore(2, -4));
		assert( ! cache.store(4, "Four", 2, -4));

		//Too big despite higher priority
		assert( ! cache.canStore(6, -3));
		assert( ! cache.store(4, "Four", 6, -3));
		assert( ! cache.canStore(6, -2));
		assert( ! cache.store(4, "Four", 6, -2));
		assert(!cache.contains(4));

		//Just right
		assert(destructor.numFreedResources == 0);
		assert(cache.canStore(5, -3));
		assert(cache.store(4, "Four", 5, -3));
		assert(cache.contains(4));
		assert(destructor.numFreedResources == 1);
		assert( ! cache.contains(3));

		//Cache is completely full
		assert(cache.getCacheSize() == 10);
		assert( ! cache.canStore(1, -99));
		assert( ! cache.store(5, "Five", 1, -99));

		//Cache now contains:
		//	ID	Size	Pri
		//	 1	   3	 -1
		//	 2	   2	 22
		//	 4	   5	 -3

		//Replace two
		destructor.numFreedResources = 0;
		assert(cache.canStore(8, -1));
		assert(cache.store(6, "six", 8, -1));
		assert(destructor.numFreedResources == 2);
		assert(cache.getCacheSize() == 10);
		assert(cache.contains(6));
		assert(!cache.contains(1) && !cache.contains(4));

		//Cache not large enough despite high priority
		assert(!cache.canStore(11, 100000));
		assert(!cache.store(7, "seven", 11, 100000));
		assert(cache.contains(6) && cache.contains(2));


		System.out.println("SizeBasedCache PASSED");
	}
}

class TestDestructor
	implements ResourceDestructor<String>
{
	public int numFreedResources = 0;
	public void freeResource(String resource)
	{
		numFreedResources++;
	}
};
