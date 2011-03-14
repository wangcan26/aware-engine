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

import org.lwjgl.opengl.*;
import org.lwjgl.input.Keyboard;

import java.io.File;

import cruxic.math.*;
import cruxic.aware.tex_cache.SizeBasedCache;
import cruxic.aware.misc.WorkingSet;

/**

 */
public class Main
{
	public static void main(String[] args)
	{
		try
		{
			if (System.getProperty("UNIT_TEST") != null)
			{
				//ensure assertions are enabled (java -ea)
				boolean assertionsEnabled = false;
				assert (assertionsEnabled == true);
				if (!assertionsEnabled)
					throw new AssertionError("please enable assertions (java -ea)");

				//GameWorld.loadSpec(new File("bc/game.aware"));
				CrxMath.test_misc();
				WorkingSet.selftest();
				SizeBasedCache.unit_test();
				return;
			}

			//make sure we have an argument
			if (args.length == 0)
			{
				System.err.println("Missing argument!");
				System.err.println("USAGE: <path/to/game.aware>");
				System.exit(1);
			}

			File gameSpecFile = new File(args[0]);

			//Expose UnsatisfiedLinkErrors immediately
			GLImage.allocateBlank(1, 1, (byte)0).dispose();

			//FrmMain frm = new FrmMain();

			//Create an OpenGL Window
			OpenGLContext glCtx = new OpenGLContext(800, 600);
			glCtx.fullscreen = System.getProperty("fullscreen") != null;
			glCtx.init();

			//setup the game engine
			Engine engine = new Engine(glCtx, gameSpecFile);

			//start on the menu
			engine.menu.setVisible(true);

			//Run!
			while (!Display.isCloseRequested()
				&& !Keyboard.isKeyDown(Keyboard.KEY_PAUSE)  //a last resort way to exit during development
				&& !engine.stop)
			{
				engine.tick();
			}

			//Cleanup
			engine.destroy();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (Engine.instance != null)
			{
				try
				{
					Engine.instance.destroy();
				}
				catch (Throwable t)
				{
					System.err.println("Engine cleanup error: " + t.toString());
				}
			}

			Display.destroy();
		}
	}
}

/*
class FrmMain extends JFrame
	implements ActionListener
{
	public boolean mode;

	public FrmMain()
	{
		super("HelloWorldSwing");
		final JButton label = new JButton("Hello World");
		label.addActionListener(this);
		getContentPane().add(label);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pack();
		setVisible(true);
	}

	public void actionPerformed(ActionEvent event)
	{
		mode = !mode;
	}
}
*/