package cruxic.aware;

import static org.lwjgl.opengl.GL11.*;
import java.util.*;
import java.util.List;
import java.awt.*;

import cruxic.jftgl_copy.FTGLPixmapFont;
import cruxic.math.*;

/**

 */
public class ViewpointSelector
{
	public static interface SelectionListener
	{
		/**selectedVP will be null if canceled*/
		public void handleSelection(Viewpoint selectedVP);
	}

	private static class VPChoice implements Comparable<VPChoice>
	{
		String text;
		Viewpoint viewpoint;
		private boolean isActive;
		//ToDo: in the future I'll add a thumbnail image

		public VPChoice(Viewpoint vp, boolean isActive)
		{
			this.viewpoint = vp;
			this.text = vp.getId();
			this.isActive = isActive;
		}

		public int compareTo(VPChoice vpChoice)
		{
			return text.compareTo(vpChoice.text);
		}
	}

	private List<Viewpoint> sorted_viewpoints;

	private SelectionListener listener;
	private HUDContext hc;
	private Layout2D<VPChoice> layout;
	private List<VPChoice> ordered_choices;
	private FTGLPixmapFont choiceFont;
	private FTGLPixmapFont choiceFontHover;	

	public ViewpointSelector(SelectionListener listener, HUDContext hc, Collection<Viewpoint> choices, Viewpoint activeViewpoint)
	{
		this.hc = hc;
		this.listener = listener;
		layout = new Layout2D<VPChoice>();
		ordered_choices = new ArrayList<VPChoice>(choices.size());

		for (Viewpoint vp: choices)
		{
			ordered_choices.add(new VPChoice(vp, vp == activeViewpoint));
		}

		Collections.sort(ordered_choices);

		Font fnt = new Font("sans-serif", Font.PLAIN, hc.relFontSize(0.5f));
		choiceFont = new FTGLPixmapFont(fnt);
		choiceFont.rgbaColor = new float[] {0.0352941f, 0.0745098f, 0.176471f, 1f};

		choiceFontHover = new FTGLPixmapFont(fnt);
		choiceFontHover.rgbaColor = new float[] {0.207843f, 0.431373f, 1f, 1f};

		//
		// Layout
		//

		final float BORDER = 0.05f;
		float yPos = 1.0f - BORDER;
		float xPos = -1.0f + BORDER;

		float maxWidthInColumn = 0.0f;
		float textHeight = 0.0f;
		
		for (VPChoice choice: ordered_choices)
		{
			Rect4f rect = hc.getTextRect(choice.text, choiceFont);
			rect = rect.withPos(xPos, yPos);
			layout.addItem(choice, rect);

			if (rect.width > maxWidthInColumn)
				maxWidthInColumn = rect.width;

			if (textHeight == 0.0f)
				textHeight = rect.height * 0.25f;

			yPos = rect.bottom() - textHeight;

			//Time to move to next column?
			if ((yPos - textHeight) <= (-1.0f + BORDER))
			{
				yPos = 1.0f - BORDER;
				xPos += maxWidthInColumn + 0.02f;
			}
		}
	}

	public void draw(HUDContext hc)
	{
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glColor4f(0.5f, 0.5f, 0.5f, 0.75f);

		hc.draw2DBox_fullscreen(true);
		glBlendFunc(GL_ONE, GL_ZERO);   //equivalent to disable blending

		//get the item under the mouse
		VPChoice hover = layout.getItemAtPos(hc.getMousePos());

		for (VPChoice choice : layout)
		{
			Rect4f rect = layout.getItemRect(choice);
			//glColor3f(1f, 1f, 1f);
			//drawRect(mr.rect, false);

			FTGLPixmapFont font;
			if (hover == choice || choice.isActive)
				font = choiceFontHover;
			else
				font = choiceFont;

			hc.setTextRectRasterPos(rect, font);
			font.render(choice.text);
		}


	}

	public void onMouseClick()
	{
		VPChoice vpc = layout.getItemAtPos(hc.getMousePos());
		if (vpc != null)
			listener.handleSelection(vpc.viewpoint);
	}

	public void cancel()
	{
		listener.handleSelection(null);
	}
}
