/* $Id: FTFont.java,v 1.10 2005/07/27 23:14:32 joda Exp $ */
package cruxic.jftgl_copy;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Iterator;

import cruxic.jftgl_copy.FTBBox;
import cruxic.jftgl_copy.FTGlyphContainer;
import cruxic.jftgl_copy.FTGlyph;


/**
 * FTFont is the public interface for the FTGL library. Specific font classes are derived from this
 * class. It uses the helper classes FTFace and FTSize to access the Freetype library. This class is
 * abstract and deriving classes must implement the protected <code>MakeGlyph</code> function to
 * create glyphs of the appropriate type. It is good practice after using these functions to test
 * the error code returned. <code>FT_Error Error()</code>
 *
 * @see FTGlyphContainer
 * @see FTGlyph
 */
public abstract class FTFont
{

	/** Default charset, with which the cache is initialised. */
	public static final char[] DEFAULTCHAR = { 'a', 'A', 'b', 'B', 'c', 'C', 'd', 'D', 'e', 'E', 'f', 'F',
												  'g', 'G', 'h', 'H', 'i', 'I', 'j', 'J', 'k', 'K', 'l', 'L',
												  'm', 'M', 'n', 'N', 'o', 'O', 'p', 'P', 'q', 'Q', 'r', 'R',
												  's', 'S', 't', 'T', 'u', 'U', 'v', 'V', 'x', 'X', 'y', 'Y',
												  'z', 'Z', '.', ':', ',', ';', '-', '+', '!', '?', '/', '\\',
												  '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '_'};

	/**
	 * This variable holds the standard FontRenderContext.
	 * Initialized with antialiasing and usesFranctionedMetrics
	 **/
	public static final FontRenderContext STANDARDCONTEXT = new FontRenderContext(null, true, true);

	/** AffineTransform to convert the java renderingspace to the opengl space. */
	private static final AffineTransform jToGL = new AffineTransform(1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f);

	private FontRenderContext fontrendercontext;

	/** An object that holds a list of glyphs. */
	protected FTGlyphContainer glyphCache = new FTGlyphContainer();

	/** The font, the FTFont is constructed from. */
	protected Font font;

	private int fontFlags = Font.LAYOUT_LEFT_TO_RIGHT;

	/** Current error code. Zero means no error. */
	protected int err; // TODO was FT_ERROR

	private float ascender  = 0.0f;
	private float descender = 0.0f;

	public static final float[] GREY = {0.5f, 0.5f, 0.5f, 1f};

	public float[] rgbaColor = GREY;


	/**
	 * Open and read a font file. Creates the font with the standard FontRenderContext.
	 * @param fontname font file name.
	 */
	public FTFont(final String fontname)
	{
		this(Font.decode(fontname));
	}

	/**
	 * Creates a new font from the given font with the standard FontRenderContext.
	 * @param font The font from which to construct this FTFont.
	 */
	public FTFont(final Font font)
	{
		this(font, STANDARDCONTEXT);
	}

	/**
	 * Reads the font from the given font name and renders the font to the given fontrendercontext.
	 * @param fontname The fontname.
	 * @param context The FontRenderContext to render with.
	 */
	public FTFont(final String fontname, final FontRenderContext context)
	{
		this(Font.decode(fontname), context);
	}

	/**
	 * Creates the FTFont from the given {@link Font}.
	 * @param font
	 * @param context
	 */
	public FTFont(final Font font, final FontRenderContext context)
	{
		this.font = font.deriveFont(FTFont.jToGL);
		this.fontrendercontext = context;
		this.updateAscenderDescender();
	}

	public void init()
	{
		Iterator i = this.glyphCache.getGlyphs();
		while(i.hasNext())
		{
			((FTGlyph) i.next()).createDisplayList(rgbaColor);
		}
	}


	/**
	 * Destructor.
	 */
	public void dispose()
	{
		this.glyphCache.clear();
	}




	/**
	 * Clears the cache of this font. If this font already has an gl and glu context,
	 * and the precache flag is set, the most recently used characters are cached in the
	 * cleared cache.
	 * @param precache An flag that indicates, wether the most recently used characters are cached
	 * after the cleareans or not.
	 */
	public void clearCache(boolean precache)
	{
		this.glyphCache.clear();
		if (precache)
			this.precache();
	}

	/**
	 * Prechaches the most recently used chars
	 */
	private void precache()
	{
//		for (int i = 0; i < DEFAULTCHAR.length; i++)
//			this.checkGlyph(DEFAULTCHAR[i]);
	}

	/**
	 * Sets the flags for the font.
	 * @param fontFlags The flags of the font e.g. {@link Font#LAYOUT_LEFT_TO_RIGHT}.
	 * @see Font
	 */
	public void setFontFlags(int fontFlags)
	{
		this.fontFlags = fontFlags;
	}

	/**
	 * Returns the flags of the font.
	 * @return The flags of the font.
	 */
	public int getFontFlags()
	{
		return this.fontFlags;
	}

	/**
	 * Set the char size for the current face.
	 * @param size the face size in points (1/72 inch)
	 * @return <code>true</code> if size was set correctly
	 */
	public boolean faceSize(final float size)
	{
		return this.faceSize(size, this.fontrendercontext);
	}

	/**
	 * Set the char size for the current face.
	 * @param size the face size in points (1/72 inch)
	 * @param context The rendercontext to render the font with.
	 * @return <code>true</code> if size was set correctly
	 */
	public boolean faceSize(final float size, final FontRenderContext context)
	{
		this.font = this.font.deriveFont(size);
		this.fontrendercontext = context;

		this.clearCache(true);
		this.updateAscenderDescender();
		return true;
	}

	/**
	 * Get the current face size in points.
	 * @return face size
	 */
	public final int faceSize()
	{
		return this.font.getSize();
	}

	/**
	 * Get the global ascender height for the face.
	 * @return Ascender height
	 */
	public final float ascender() // TODO
	{
		assert this.ascender>=0:"Evil Developer.";
		return this.ascender;
	}

	/**
	 * Gets the global descender height for the face.
	 * @return Descender height
	 */
	public final float descender() // TODO
	{
		assert this.descender<=0: Float.toString(this.descender);
		return this.descender;
	}

	/**added by cruxic: returns ascender() + -descender()*/
	public final float getMaxHeight()
	{
		return ascender - descender;  //descender is negative
	}

	private void updateAscenderDescender()
	{
		this.ascender  = (float) this.font.getMaxCharBounds(this.fontrendercontext).getMinY();
		this.descender = (float) this.font.getMaxCharBounds(this.fontrendercontext).getMaxY();

		//hack(cruxic): the above used to work flawlessly but I just upgraded my OS and suddenly
		//  ascender and descender are negated!?
		if (ascender < 0.0f)
			ascender = -ascender;
		if (descender > 0.0f)
			descender = -descender;
	}

	/**
	 * Returns the BoundingBox for the given string.
	 * @param string The string to get the bounding box for.
	 * @return The BoundingBox for the given string.
	 */
	public FTBBox getBBox(String string)
	{
		GlyphVector vec = this.font.layoutGlyphVector(this.fontrendercontext, string.toCharArray(), 0, string.length(), this.fontFlags);
		return new FTBBox(vec.getOutline());
	}

	/**
	 * Get the advance width for a string.
	 *
	 * @param string a char string
	 * @return advance width
	 */
	public float advance(final String string)
	{
		/**** first suggestion */
		GlyphVector vec  = this.font.layoutGlyphVector(this.fontrendercontext, string.toCharArray(), 0, string.length(), this.fontFlags);
		return (float) Math.abs(vec.getGlyphPosition(0).getX() - vec.getGlyphPosition(vec.getNumGlyphs()).getX());
		/**** second suggestion */
//		return new TextLayout(string, this.font, this.fontrendercontext).getAdvance(); //TODO maybe getVisibleAdvance()
	}

	/**
	 * Render a string of characters.
	 * @param string String to be output.
	 */
	public void render(final String string)
	{
		final char[] c = string.toCharArray();
		GlyphVector vec = this.font.layoutGlyphVector(this.fontrendercontext, c, 0, string.length(), this.fontFlags);

		for (int i = 0; i < vec.getNumGlyphs(); i++)
		{
			Point2D p = vec.getGlyphPosition(i);
			FTGlyph glyph = this.checkGlyph(vec.getGlyphCode(i), vec.getGlyphOutline(i, (float) -p.getX(), (float) p.getY()));
			//assert FTBBox.renderBBox(new Vector3f((float)p.getX(), (float)p.getY(), 0), glyph.getBBox());
			glyph.render((float)p.getX(), (float)p.getY(), 0.0f);
		}
	}

	/**
	 * Queries the Font for errors.
	 * @return The current error code.
	 */
	public int error() // TODO was FT_ERROR
	{
		return this.err;
	}

	/**
	 * Construct a glyph of the correct type. Clients must overide the function and return their
	 * specialised FTGlyph.
	 * @param ftGlyph The glyph represented by a shape.
	 * @param advance The advance of the glyph.
	 * @return An FT****Glyph or <code>null</code> on failure.
	 */
	protected abstract FTGlyph makeGlyph(Shape ftGlyph, float advance);

	/**
	 * Check that the glyph at <code>chr</code> exist. If not load it.
	 *
	 * @param glyphCode the glyphcode to render.
	 * @param outline the outline of the glyph to render.
	 */
	private final FTGlyph checkGlyph(final int glyphCode, final Shape outline)
	{
		FTGlyph glyph = this.glyphCache.glyph(glyphCode);
		if (glyph == null)
		{
			glyph = this.makeGlyph(outline, 0.0f);
			glyph.createDisplayList(rgbaColor);
			this.glyphCache.add(glyph, glyphCode);
			FTBBox box = glyph.getBBox();
			if (this.ascender < box.upperY)  this.ascender  = box.upperY;
			if (this.descender > box.lowerY) this.descender = box.lowerY;
		}
		return glyph;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return "[" + this.getClass() + "\n" + this.font.toString() + "\n#glyphs: " + this.glyphCache.size() + "]";
	}

}