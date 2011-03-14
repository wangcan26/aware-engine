package cruxic.aware.tex_cache;

/**

 */
public interface ResourceDestructor<I>
{
	public void freeResource(I resource);
}
