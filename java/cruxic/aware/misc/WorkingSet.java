package cruxic.aware.misc;

import java.util.*;

public class WorkingSet<E>
{
	private static class EntrySorter<E> implements Comparator<Map.Entry<E, Integer>>
	{
		public final int compare(Map.Entry<E, Integer> e1, Map.Entry<E, Integer> e2)
		{
			int v1 = e1.getValue();
			int v2 = e2.getValue();

			//sort highest to lowest priority
			//note: I avoid subtraction trick in case of numeric overflow
			if (v2 > v1)
				return 1;
			else if (v2 < v1)
				return -1;
			else
				return 0;
		}
	}

	public static final int PRI_HIGH = 0xFFFF;
	public static final int PRI_NORM = 0;
	public static final int PRI_LOW = -0xFFFF;

	//ToDo: working sets should be immutable once locked.  This gives LoaderThread some assurance
	//that another thread won't be modifying it
	private boolean locked;

	private HashMap<E, Integer> items;
	private final EntrySorter<E> sortDESC;

	public WorkingSet()
	{
		items = new HashMap<E, Integer>(64);
		sortDESC = new EntrySorter<E>();
	}

	/**copy constructor*/
	public WorkingSet(WorkingSet<E> toCopy)
	{
		items = new HashMap<E, Integer>(toCopy.items);
		sortDESC = new EntrySorter<E>();
	}

	/**add the specified id to the working set iff it's not already
	in the working set or the specified priority is higher.*/
	public void add(E id, int priorityGroup)
	{
		//only actually add the item if it's not yet present or the specified priority is higher than the existing
		if (!items.containsKey(id) || items.get(id) < priorityGroup)
			items.put(id, priorityGroup);
	}

	/**Get the priority group of the given image.  If the image is not
	in this working set then the lowest priority is returned*/
	public int getPriority(E id)
	{
		if (items.containsKey(id))
			return items.get(id);
		else
			return Integer.MIN_VALUE;
	}

	/**Increase or decrease the specified item's priority by the given amount (+/-)*/
	public void adjustPriority(E id, int amount)
	{
		Integer pri = items.get(id);
		if (pri != null)
			items.put(id, pri + amount);
	}

	public int size()
	{
		return items.size();
	}

	public boolean contains(E id)
	{
		return items.containsKey(id);
	}

	public Set<E> getPriorityGroup(int priorityGroup)
	{
		HashSet<E> list = new HashSet<E>();
		for (E id: items.keySet())
		{
			if (items.get(id) == priorityGroup)
				list.add(id);
		}

		return list;
	}

	/**Get all items in this working set (unordered)*/
	public Set<E> getSet()
	{
		//make copy to prevent accidental modification
		return new HashSet<E>(items.keySet());
	}

	/**return all items in the working set sorted highest to lowest priority*/
	public List<E> getPrioritizedList()
	{
		//put all entries in the hashmap into a sortable list
		ArrayList entries = new ArrayList(items.entrySet());

		//sort highest to lowest priority
		Collections.sort(entries, sortDESC);

		//Return just the keys.
		int size = entries.size();
		for (int i = 0; i < size; i++)
		{
			Object mapEntry = entries.get(i);
			entries.set(i, ((Map.Entry<E, Integer>)mapEntry).getKey());
		}

		return entries;
	}

	/**make this working set become a copy of the given*/
	public void copy(WorkingSet ws)
	{
		items.clear();
		items.putAll(ws.items);
	}

	public void clear()
	{
		items.clear();
	}

	public static void selftest()
	{
		WorkingSet<String> ws1 = new WorkingSet<String>();
		ws1.add("b", 2);
		ws1.add("a", 3);
		ws1.add("c", 1);
		ws1.add("B", 2);
		ws1.add("A", 3);
		ws1.add("C", 1);

		Set<String> set1 = ws1.getSet();
		assert(set1.size() == 6);
		assert(set1.contains("a")
			&& set1.contains("A")
			&& set1.contains("b")
			&& set1.contains("B")
			&& set1.contains("c")
			&& set1.contains("C"));

		List<String> l1 = ws1.getPrioritizedList();
		assert(l1.size() == 6);

		String p3 = l1.get(0) + l1.get(1);
		String p2 = l1.get(2) + l1.get(3);
		String p1 = l1.get(4) + l1.get(5);
		assert(p3.equals("aA") || p3.equals("Aa"));
		assert(p2.equals("bB") || p2.equals("Bb"));
		assert(p1.equals("cC") || p1.equals("Cc"));

		WorkingSet<String> ws2 = new WorkingSet<String>();
		ws2.add("d", Integer.MAX_VALUE);
		ws2.add("f", Integer.MIN_VALUE);
		ws2.add("e", Integer.MAX_VALUE);

		ws1.copy(ws2);
		List<String> l2 = ws1.getPrioritizedList();
		assert(l2.size() == 3);
		String pmx = l2.get(0) + l2.get(1);
		String pmin = l2.get(2);
		assert(pmx.equals("de") || pmx.equals("ed"));
		assert(pmin.equals("f"));

		System.out.printf("WorkingSet.selftest PASSED\n");
	}


}
