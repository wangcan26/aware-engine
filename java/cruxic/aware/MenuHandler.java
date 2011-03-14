package cruxic.aware;

import cruxic.aware.menu.*;

import java.io.File;

/**

 */
public class MenuHandler
	implements MenuActionListener
{
	private Engine engine;

	public enum MenuAction
	{
		MNull,
		MPlay,
		MQuit,
		MToggleMusic,
		MToggleFullscreen,
		MToggle_showFPS,
	}

	public MenuHandler(Engine engine)
	{
		this.engine = engine;


	}

	public void menuActivated(Menu theMenu)
	{
		MenuAction ma = (MenuAction)theMenu.getId();

		if (theMenu instanceof ToggleMenu)
			((ToggleMenu)theMenu).toggle();

		if (ma != null)
		{
			System.out.println("MenuClick: " + ma);

			switch (ma)
			{
				case MPlay:
					engine.menu.setVisible(false);
					engine.resumeGame();
					break;
				case MQuit:
					engine.stop = true;
					break;
				/*case MToggleFullscreen:
				{
					boolean on = ((ToggleMenu)theMenu).on;
					System.out.println("fullscreen = " + on);
					break;
				}
				case MToggleMusic:
				{
					boolean on = ((ToggleMenu)theMenu).on;
					System.out.println("music = " + on);
					break;
				}
				case MToggle_showFPS:
				{
					engine.renderer.showFPS = ((ToggleMenu)theMenu).on;
					break;
				}*/
			}
		}

	}
}
