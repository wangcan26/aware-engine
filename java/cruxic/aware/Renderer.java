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

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glRasterPos2f;
import static org.lwjgl.util.glu.GLU.*;
import cruxic.aware.ipo.*;
import cruxic.math.*;

import java.awt.*;

import cruxic.jftgl_copy.FTGLPixmapFont;

/**

 */
public class Renderer
{
	public enum TransitionState
	{
		NONE,
		SHOW_WAIT_CURSOR,
		LOAD_NEW_TEXTURE,
		TEXTURE_LOADED,
		FADING
	}

	private Engine engine;

	private TransitionState transitionState;
	//used for the fade effect from old viewpoint to the new one
	IPOCurve viewpointFadeTransitionAlpha;
	private int sphereResolution;

	//private TrueTypeFont ttf;
	FTGLPixmapFont defaultFont;

	public Renderer(Engine engine)
	{
		this.engine = engine;

		sphereResolution = 128;
		transitionState = TransitionState.NONE;
		viewpointFadeTransitionAlpha = new LinearIPO(0.0f, 1.0f, 0.5f, engine.newTimeSource());


		glClearColor(0, 0, 0, 0);  //clear to black
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_LIGHTING);
		glEnable(GL_CULL_FACE);  //faster drawing (polygons must be drawn CCW so their back-faces are culled)
		glEnable(GL_BLEND);  //enable transparency effects

		//helpful?
		glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);

		Font fnt = new Font("sans-serif", Font.PLAIN, 16);
		defaultFont = new FTGLPixmapFont(fnt);
		defaultFont.rgbaColor = new float[] {1f, 1f, 1f, 1f};

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(65, (float)engine.glCtx.width/(float)engine.glCtx.height, 0.1f, 10.0f);
	}

	public void renderGame()
	{
		glClear(GL_COLOR_BUFFER_BIT);

		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

		//make Z axis point upwards (consistent with Blender)
		glRotatef(-90, 1, 0, 0);

		engine.cameraInput.apply_glRotatef();

		//Draw the textured sphere

		glEnable(GL_TEXTURE_2D);
		glColor4f(1.0f, 1.0f, 1.0f, 1.0f);  //must be white or texture is discolored
		glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		glBlendFunc(GL_ONE, GL_ZERO);   //equivalent to disable blending

		//are we currently doing a transition from one viewpoint to another?
		Viewpoint prevVP = engine.previousViewpoint;
		Viewpoint activeVP = engine.gameWorld.getActiveViewpoint();
		if (prevVP == null)
			prevVP = activeVP;  //just fade from active to active on the first frame. TODO: fade from black instead

		if (transitionState == TransitionState.SHOW_WAIT_CURSOR)
		{
			//Draw the previous viewpoint one last time so that we can
			//show the loading indicator in render2D_UI()
			drawViewpoint(prevVP);
		}
		else if (transitionState == TransitionState.LOAD_NEW_TEXTURE
			|| transitionState == TransitionState.TEXTURE_LOADED
			|| transitionState == TransitionState.FADING)
		{
			//Keep the alpha at zero until the texture has been fully loaded into VRAM.
			//This is important on slow machines.  Otherwise the transition might be half over
			//by the time we finish rendering the first transition frame
			if (transitionState.ordinal() < TransitionState.FADING.ordinal())
				viewpointFadeTransitionAlpha.reset();

			//draw old viewpoint
			drawViewpoint(prevVP);

			//fade in the new  (see http://jerome.jouvie.free.fr/OpenGl/Tutorials/Tutorial9.php#MixPicture)
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			glColor4f(1.0f, 1.0f, 1.0f, viewpointFadeTransitionAlpha.currentValue());
			drawViewpoint(activeVP);
		}
		//TransitionState.NONE
		else
		{
			//transition is done - OverlayProcessor can dispose previous viewpoint resources
			engine.overlayProcessor.retain_only(activeVP);

			drawViewpoint(activeVP);
		}

		//drawQuad(0.5f);

		//draw the sphere wireframe (for debuggging)
		if ((Boolean)engine.params.get("renderer.show_geom"))
		{
			glDisable(GL_TEXTURE_2D);
			glColor3f(0.5f, 0.5f, 0.5f);
			glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
			drawViewpoint(activeVP);
		}

		//draw the hotspots
		if ((Boolean)engine.params.get("renderer.show_hotspots")
			|| engine.dev.new_hotspot != null)
		{
			SphereCoord3f lookRay = engine.cameraInput.getLookRay();

			glDisable(GL_TEXTURE_2D);

			for (PanoHotspot hotspot: activeVP.hotspots())
				drawHotspot(hotspot, lookRay);

			if (engine.dev.new_hotspot != null)
				drawHotspot(engine.dev.new_hotspot, lookRay);

			//restore default line width
			glLineWidth(1.0f);
		}

		render_HUD();

		switch (transitionState)
		{
			case SHOW_WAIT_CURSOR:
				transitionState = TransitionState.LOAD_NEW_TEXTURE;
				break;
			case LOAD_NEW_TEXTURE:
				transitionState = TransitionState.TEXTURE_LOADED;
				break;
			case TEXTURE_LOADED:
				transitionState = TransitionState.FADING;
				break;
			case FADING:
			{
				if (viewpointFadeTransitionAlpha.isComplete())
					transitionState = TransitionState.NONE;
				break;
			}
		}
		

		/*glDisable(GL_TEXTURE_2D);
		glColor3f(0.5f, 0.5f, 0.5f);
		glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

		glEnable(GL_TEXTURE_2D);
		glColor3f(1.0f, 1.0f, 1.0f);
		glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		drawUVSphere(64, texIds);

		glDisable(GL_TEXTURE_2D);
		glColor3f(1, 0, 0);
		glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		glBegin(GL_TRIANGLES);
		glVertex3f(0, 0.5f, 0);
		glVertex3f(0.05f, 0.5f, 0.05f);
		glVertex3f(-0.05f, 0.5f, 0.05f);
		glEnd();*/
	}

	private void drawHotspot(PanoHotspot hotspot, SphereCoord3f lookRay)
	{
		//Setup color and style of hotspot

		glPointSize(4);
		glLineWidth(1.0f);

		if (hotspot == engine.dev.new_hotspot)
			glColor3f(1, 0, 0);
		else if (hotspot.isRayInside(lookRay))
		{
			//make it look fat
			glPointSize(6);
			glLineWidth(3.0f);
			glColor3f(0, 1, 0);
		}
		else
		{
			glColor3f(1, 1, 0);
		}

		//Draw twice, once for the lines and again for the dots
		for (int i = 0; i < 2; i++)
		{
			glBegin(i == 0 ? GL_LINE_STRIP : GL_POINTS);

			//have at least a line?
			if (hotspot.polygon.size() >= 2 || i == 1)
			{
				for (SphereCoord3f sc: hotspot.polygon)
				{
					//sc.toPoint().debugPrint();
					sc.toPoint().glVertex();
				}
			}

			//close the polygon unless it's being edited
			if (hotspot != engine.dev.new_hotspot && hotspot.polygon.size() >= 3)
				hotspot.polygon.get(0).toPoint().glVertex();

			glEnd();
		}
	}

	/**Render the "heads up display" (2D elements overlayed upon the game scene*/
	private void render_HUD()
	{
		HUDContext hc = engine.hudCtx;

		hc.pushContext();
		{
			//Draw FPS rate
			if ((Boolean)engine.params.get("renderer.show_fps"))
			{
				glRasterPos2f(-0.98f, 0.95f);
				defaultFont.render(engine.getFramesPerSecond() + " FPS");
			}

			//Draw the console text
			if (engine.dev.console_text.length() > 0)
			{
				glDisable(GL_TEXTURE_2D);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				glColor4f(0.5f, 0.5f, 0.5f, 0.25f);
				draw2DBox(-1.0f, 1.0f, 2.0f, 0.08f, true, false);
				glBlendFunc(GL_ONE, GL_ZERO);   //equivalent to disable blending

				glColor3f(1.0f, 1.0f, 1.0f);
				glRasterPos2f(-0.98f, 0.95f);
				defaultFont.render(engine.dev.console_text.toString());
			}

			//Draw viewpoint selector
			if (engine.dev.viewpoint_selector != null)
			{
				engine.dev.viewpoint_selector.draw(hc);
			}

			if (engine.useHUDMouse())
			{
				hc.drawMousePointer(hc.getMousePos(), "hand", 1.0f);
			}
			else
			{
				//Draw mouse at the center of the screen (movement rotates the camera instead)
				hc.drawMousePointer(Vec2f.ORIGIN, getCurrentPointerIcon(), engine.cursorFadeOut.currentValue());
			}
		}
		hc.popContext();
	}

	private String getCurrentPointerIcon()
	{
		if (transitionState == TransitionState.SHOW_WAIT_CURSOR)
			return "hand-clicked";
		else
		{
			SphereCoord3f lookRay = engine.cameraInput.getLookRay();
			boolean overLink = engine.getHotspotTarget(lookRay) != null;
			if (overLink)
				return "hand";
			else
				return "hand-no-action";
		}
	}

	public void startViewpointTransition()
	{
		transitionState = TransitionState.SHOW_WAIT_CURSOR;

		//if the cursor is hidden force it to show again
		engine.cursorFadeOut.reset();
	}

	/**
		@param center if true the image will be centered on left,top coordinates
	*/
	private void draw2DBox(float left, float top, float width, float height, boolean filled,
		boolean center)
	{
		if (center)
		{
			left -= width / 2.0f;
			top += height / 2.0f;
		}

		glPolygonMode(GL_FRONT_AND_BACK, filled ? GL_FILL : GL_LINE);

		glBegin(GL_QUADS);
		glTexCoord2f(1f, 1f);
		glVertex2f(left + width, top);
		glTexCoord2f(0f, 1f);
		glVertex2f(left, top);
		glTexCoord2f(0f, 0f);
		glVertex2f(left, top - height);
		glTexCoord2f(1f, 0f);
		glVertex2f(left + width, top - height);
		glEnd();
	}

	private void drawViewpoint(Viewpoint vp)
	{
		if (vp instanceof EquirectViewpoint)
		{
			EquirectViewpoint evp = (EquirectViewpoint)vp;
			if (!evp.getOverlays().isEmpty())
			{
//				HERE: finalize all this mess:
//			  		3) get viewpoint cleanup working
//			  		4) use overlayprocessor for menu effect
//				    5) Add test overlay specs from game file
// 					6) make Overlayprocess guess elapsed time intelligently

//				if (op == null)
//				{
//					op = new OverlayProcessor();
//
//					Liquid_StaticNoiseOverlaySpec spec = new Liquid_StaticNoiseOverlaySpec(
//						"water", "res/data/ripple_noise1.png", vp.getImage().replace(".png", " - mask.png"));
//
//					//Test_SolidOverlaySpec spec = new Test_SolidOverlaySpec(new Rect4i(1000, 1000, 1500, 1500));
//					//Test_TransparentOverlaySpec spec2 = new Test_TransparentOverlaySpec(new Rect4i(1000, 1000, 1500, 1500));
//					//Test_TransparentOverlaySpec spec2 = new Test_TransparentOverlaySpec(new Rect4i(400,300, 800,900));
//
//					ArrayList<OverlaySpec> specs = new ArrayList<OverlaySpec>(1);
//					specs.add(spec);
//					//specs.add(spec2);
//					op.load(specs, engine.texCache.getGLImage(vp.getImage()));
//
//
//				}

				int texId = engine.texCache.getTexture(evp.getImage())[0];

				glBindTexture(GL_TEXTURE_2D, texId);

				engine.overlayProcessor.upload(vp);

				drawUVSphere(sphereResolution, engine.texCache.getTexture(evp.getImage()));
			}
			else
			{
				drawUVSphere(sphereResolution, engine.texCache.getTexture(evp.getImage()));
			}
		}
		else if (vp instanceof CubicViewpoint)
		{
			CubicViewpoint cvp = (CubicViewpoint)vp;

			//Get the texure IDs
			int[] tex_fblrtb = new int[6];
			int i = 0;
			for (String imgId: cvp.imageIds)
				tex_fblrtb[i++] = engine.texCache.getTexture(imgId)[0];

			drawEnvCube(tex_fblrtb);
		}
		else
			throw new UnsupportedOperationException();
	}

	/**Used by drawEnvCube*/
	private static final float[][] CUBE_VERTS =
	{
		{1.0f, 1.0f, -1.0f},   //1
		{1.0f, -1.0f, -1.0f},  //2
		{-1.0f, -1.0f, -1.0f}, //3
		{-1.0f, 1.0f, -1.0f},  //4
		{1.0f, 1.0f, 1.0f},    //5
		{1.0f, -1.0f, 1.0f},   //6
		{-1.0f, -1.0f, 1.0f},  //7
		{-1.0f, 1.0f, 1.0f}    //8
	};

	/**Used by drawEnvCube.
	 the verticies of each face are orderd CCW starting at the lower-left corner of the quad
	 */
	private static final int[][] CUBE_FACES =
	{
		{4, 1, 5, 8},  //Front (+Y)
		{2, 3, 7, 6},  //Back  (-Y)
		{3, 4, 8, 7},  //Left  (-X)
		{1, 2, 6, 5},  //Right (+X)
		{8, 5, 6, 7},  //Top   (+Z)
		{3, 2, 1, 4}   //Bot   (-Z)
	};

	/**Used by drawEnvCube*/
	private static final float[][] CUBE_TEX_COORDS =
	{
		{0.0f, 0.0f},
		{1.0f, 0.0f},
		{1.0f, 1.0f},
		{0.0f, 1.0f},
	};

	void drawEnvCube(int[] tex_fblrtb)
	{
		for (int fi = 0; fi < 6; fi++)
		{
			glBindTexture(GL_TEXTURE_2D, tex_fblrtb[fi]);

			glBegin(GL_QUADS);
			for (int vi = 0; vi < 4; vi++)
			{
				float[] v = CUBE_TEX_COORDS[vi];
				glTexCoord2f(v[0], v[1]);

				v = CUBE_VERTS[CUBE_FACES[fi][vi] - 1];  //indicies are 1 based
				glVertex3f(v[0], v[1], v[2]);

				//glTexCoord2fv(TEX_COORDS[TEX_FACES[fi][vi] - 1]);  //indicies are 1 based
				//glVertex3fv(VERTS[FACES[fi][vi] - 1]);	//indicies are 1 based
			}
			glEnd();
		}
	}

/*
	void drawEnvCube()
	{
		final float SZ = 1.0f;

		final float[][] VERTS =
		{
			{SZ, SZ, -SZ},
			{SZ, -SZ, -SZ},
			{-SZ, -SZ, -SZ},
			{-SZ, SZ, -SZ},
			{SZ, SZ, SZ},
			{SZ, -SZ, SZ},
			{-SZ, -SZ, SZ},
			{-SZ, SZ, SZ}
		};

		final int[][] FACES =
		{
			{1, 4, 3, 2},  //Bot
			{5, 6, 7, 8},  //Top
			{1, 2, 6, 5},  //Right
			{2, 3, 7, 6},  //Back
			{3, 4, 8, 7},  //Left
			{4, 1, 5, 8}   //Front
		};

		final float[][] TEX_COORDS =
		{
			//image bottom row
			{0.0f, 0.0f},
			{1/3.0f, 0.0f},
			{2/3.0f, 0.0f},
			{1.0f, 0.0f},
			//image middle row
			{0.0f, 0.5f},
			{1/3.0f, 0.5f},
			{2/3.0f, 0.5f},
			{1.0f, 0.5f},
			//image top row
			{0.0f, 1.0f},
			{1/3.0f, 1.0f},
			{2/3.0f, 1.0f},
			{1.0f, 1.0f},
		};

		//http://en.wikibooks.org/wiki/Blender_3D:_Noob_to_Pro/Build_a_skybox
		final int[][] TEX_FACES =
		{
			{6, 5, 1, 2},  //Bot
			{3, 7, 6, 2},  //Top
			{7, 8, 12, 11},  //Right
			{6, 7, 11, 10},  //Back
			{5, 6, 10, 9},	//Left
			{3, 4, 8, 7}	 //Front
		};

		glBegin(GL_QUADS);
		for (int fi = 0; fi < 6; fi++)
		{
			for (int vi = 0; vi < 4; vi++)
			{
				float[] v = TEX_COORDS[TEX_FACES[fi][vi] - 1];  //indicies are 1 based
				glTexCoord2f(v[0], v[1]);

				v = VERTS[FACES[fi][vi] - 1];  //indicies are 1 based
				glVertex3f(v[0], v[1], v[2]);

				//glTexCoord2fv(TEX_COORDS[TEX_FACES[fi][vi] - 1]);  //indicies are 1 based
				//glVertex3fv(VERTS[FACES[fi][vi] - 1]);	//indicies are 1 based
			}
		}
		glEnd();
	}
*/
	/**
		Draw a textured UV sphere (look at Blender "UVsphere" object if you don't know what that is).
		The sphere drawn is identical to gluSphere(myQuadric, 1.0, N, N) except that, if multiple textures
		are passed in, they will be mapped to the sphere as if they were one big texture.  This allows
		me to draw a textured sphere using huge textures (larger than GL_MAX_TEXTURE_SIZE).

		@param N the resolution of the sphere - number of segments/slices and rings/stacks.  Must be evenly
		divisible by the number of textures being used and also by 2.

		@param texIds the GL texture ids that, when put together left to right make up the larger texture.
	*/
	private static void drawUVSphere(int N, int[] texIds)
	{
		assert(N >= 4);  //cannot have less than a cube!
		assert(N % 2 == 0);  //must be divisible by 2
		assert(texIds.length > 0);

		int nTexIds = texIds.length;

		int SEGS_PER_TDIV = N / nTexIds;
		double uinc = 1.0 / (double)N * (double)nTexIds;
		double vinc = 2.0 / (double)N;	//v increments at twice the rate of u since we are only going round half of the circle (vertically)

		//Cache cos() and sin() values for efficiency
		double[] cosVals = new double[N + 1];  //length must be divisible by 2
		double[] sinVals = new double[N + 1];
		{
			double angleInc = Math.PI * 2.0 / (double)N;
			double angle = 0.0;
			for (int i = 0; i <= N; i++)
			{
				cosVals[i] = Math.cos(angle);
				sinVals[i] = Math.sin(angle);
				angle += angleInc;
			}
		}

		//Top and Bottom caps
		for (int tdiv = 0; tdiv < nTexIds; tdiv++)
		{
			glBindTexture(GL_TEXTURE_2D, texIds[tdiv]);

			glBegin(GL_TRIANGLES);
			double lastX = 0.0;
			double lastY = 0.0;
			double centerz = cosVals[0];
			double ringz = cosVals[1];
			double rad = sinVals[1];
			double u = 0.0;
			for (int seg = tdiv * SEGS_PER_TDIV; seg <= (tdiv + 1) * SEGS_PER_TDIV; seg++)
			{
				double x = cosVals[seg] * rad;
				double y = sinVals[seg] * rad;
				if (seg == tdiv * SEGS_PER_TDIV)
				{
					lastX = x;
					lastY = y;
				}
				else
				{
					glTexCoord2d(u, 1.0);
					glVertex3d(0.0, 0.0, centerz);
					glTexCoord2d(u + uinc, 1.0 - vinc);
					glVertex3d(x, y, ringz);
					glTexCoord2d(u, 1.0 - vinc);
					glVertex3d(lastX, lastY, ringz);
					lastX = x;
					lastY = y;

					u += uinc;
				}
			}
			glEnd();

			//middle (the bulk)
			double v = 1.0 - vinc;
			for (int ring = 1; ring < N / 2 - 1; ring++)
			{
				u = 0.0;
				glBegin(GL_QUADS);
				for (int seg = tdiv * SEGS_PER_TDIV; seg < (tdiv + 1) * SEGS_PER_TDIV; seg++)
				{
					rad = sinVals[ring];
					//top-left corner
					glTexCoord2d(u, v);
					glVertex3d(cosVals[seg] * rad, sinVals[seg] * rad, cosVals[ring]);
					//top-right corner
					glTexCoord2d(u + uinc, v);
					glVertex3d(cosVals[seg + 1] * rad, sinVals[seg + 1] * rad, cosVals[ring]);
					//bot-right corner
					rad = sinVals[ring + 1];
					glTexCoord2d(u + uinc, v - vinc);
					glVertex3d(cosVals[seg + 1] * rad, sinVals[seg + 1] * rad, cosVals[ring + 1]);
					//bot-left corner
					glTexCoord2d(u, v - vinc);
					glVertex3d(cosVals[seg] * rad, sinVals[seg] * rad, cosVals[ring + 1]);
					u += uinc;
				}
				glEnd();

				v -= vinc;
			}

			//bottom cap
			centerz = cosVals[N / 2];
			ringz = cosVals[N / 2 - 1];
			rad = sinVals[N / 2 - 1];
			glBegin(GL_TRIANGLES);
			u = 0.0;
			for (int seg = tdiv * SEGS_PER_TDIV; seg <= (tdiv + 1) * SEGS_PER_TDIV; seg++)
			{
				double x = cosVals[seg] * rad;
				double y = sinVals[seg] * rad;
				if (seg == tdiv * SEGS_PER_TDIV)
				{
					lastX = x;
					lastY = y;
				}
				else
				{
					glTexCoord2d(u, 0.0);
					glVertex3d(0.0, 0.0, centerz);
					glTexCoord2d(u, vinc);
					glVertex3d(lastX, lastY, ringz);
					glTexCoord2d(u + uinc, vinc);
					glVertex3d(x, y, ringz);
					lastX = x;
					lastY = y;

					u += uinc;
				}
			}
			glEnd();
		}
	}

}
