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
	private static class VPChoice implements Comparable<VPChoice>
	{
		String text;
		Viewpoint viewpoint;
		//ToDo: in the future I'll add a thumbnail image

		public VPChoice(Viewpoint vp)
		{
			this.viewpoint = vp;
			this.text = vp.getId();
		}

		public int compareTo(VPChoice vpChoice)
		{
			return text.compareTo(vpChoice.text);
		}
	}

	private List<Viewpoint> sorted_viewpoints;

	private Layout2D<VPChoice> layout;
	private List<VPChoice> ordered_choices;
	private FTGLPixmapFont choiceFont;
	private FTGLPixmapFont choiceFontHover;

	public ViewpointSelector(HUDContext hc, Collection<Viewpoint> choices)
	{

		layout = new Layout2D<VPChoice>();
		ordered_choices = new ArrayList<VPChoice>(choices.size());

		for (Viewpoint vp: choices)
		{
			ordered_choices.add(new VPChoice(vp));
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

		float yPos = 0.85f;
		
		for (VPChoice choice: ordered_choices)
		{
			Rect4f rect = hc.getTextRect(choice.text, choiceFont);
			rect = rect.centeredOn(Vec2f.ORIGIN).newY(yPos);
			layout.addItem(choice, rect);

			yPos = rect.bottom() - (rect.height * 0.25f);
		}

		here: focus on getting out a release that works.  FEATURES LATER
		just do a dual column layout with no dynamic nature.  auto generate a bunch of choices
		first.  don't even bother sorting unused first.
	}

	public void draw(HUDContext hc)
	{
		glDisable(GL_TEXTURE_2D);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glColor4f(0.5f, 0.5f, 0.5f, 0.75f);

		hc.draw2DBox(-0.9f, 0.9f, 1.8f, 1.8f, true, false);
		glBlendFunc(GL_ONE, GL_ZERO);   //equivalent to disable blending

//		glColor3f(1.0f, 1.0f, 1.0f);
//		glRasterPos2f(-0.98f, 0.95f);
//		defaultFont.render(engine.dev.console_text.toString());

		//get the item under the mouse
		VPChoice hover = layout.getItemAtPos(hc.getMousePos());

		for (VPChoice choice : layout)
		{
			Rect4f rect = layout.getItemRect(choice);
			//glColor3f(1f, 1f, 1f);
			//drawRect(mr.rect, false);

			FTGLPixmapFont font;
			if (hover == choice)
				font = choiceFontHover;
			else
				font = choiceFont;

			hc.setTextRectRasterPos(rect, font);
			font.render(choice.text);
		}


	}

	public void onMouseClick()
	{

	}
}
