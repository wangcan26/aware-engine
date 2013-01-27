// ae_native.cpp
//

#include "ae_native.h"

#include <string.h>

#define AE_MAX_ERR_CHARS 255
char aeLastError[AE_MAX_ERR_CHARS + 1];

#define aeThrowSDLError() _aeThrowError(SDL_GetError(), __LINE__)

#define aeThrowError(msg) _aeThrowError((msg), __LINE__)
static void _aeThrowError(const char * err, int codeLine) throw(ae_error_str)
{
	if (!err || err[0] == 0)
		err = "Unknown Error";
		
	size_t len = strlen(err);
	if (len > AE_MAX_ERR_CHARS)
		len = AE_MAX_ERR_CHARS;
		
	strncpy(aeLastError, err, len);
	aeLastError[len] = 0;

	//Append the C code line number if we have room
	char temp[48];
	sprintf(temp, " (C code line %d)", codeLine);
	if (len + strlen(temp) < AE_MAX_ERR_CHARS)
		strcat(aeLastError, temp);
		
	throw(aeLastError);
}


#define LZZ_INLINE inline
GameWindow::GameWindow ()
        {
		screen = NULL;
	}
GameWindow::~ GameWindow ()
        {
		close();
	}
void GameWindow::testException () throw (ae_error_str)
        {
		aeThrowError("This is a test exception");
	}
void GameWindow::open (char const * title, int width, int height, bool fullscreen) throw (ae_error_str)
        {
		close();
		
		if (SDL_Init(SDL_INIT_VIDEO) < 0)
			aeThrowSDLError();
		
		Uint32 flags = SDL_OPENGL;
		if (fullscreen)
			flags |= SDL_FULLSCREEN;
	
		screen = SDL_SetVideoMode(width, height, 32, flags);
		if (!screen)
			aeThrowSDLError();
		
		SDL_WM_SetCaption(title, "icon?");
	}
int GameWindow::getWidth ()
        {
		return screen ? screen->w : 0;
	}
int GameWindow::getHeight ()
        {
		return screen ? screen->h : 0;
	}
void GameWindow::close ()
        {
		if (screen)
		{
			screen = NULL;
			SDL_Quit();
		}	
	}
bool GameWindow::haveQuitEvent ()
        {
		SDL_Event event;
		memset(&event, 0, sizeof(SDL_Event));
		if (SDL_PollEvent(&event))
		{
			switch (event.type)
			{
				case SDL_QUIT:
					return true;
				case SDL_KEYDOWN:
				{
					switch (event.key.keysym.sym)
					{
						case SDLK_ESCAPE:
							return true;
						default:
							break;
					}
				
					break;
				}
				default:
					break;
			}
		}
		
		//keep running
		return false;
	}
void GameWindow::waitForQuit ()
        {
		SDL_Event event;
		memset(&event, 0, sizeof(SDL_Event));
	
		while (SDL_WaitEvent(&event))
		{
			switch (event.type)
			{
				case SDL_QUIT:
					return;
				case SDL_KEYDOWN:
				{
					switch (event.key.keysym.sym)
					{
						case SDLK_ESCAPE:
							return;
						default:
							break;
					}
				
					break;
				}
				default:
					break;
			}
		}	
	}
void GameWindow::swapBuffers ()
        {
		SDL_GL_SwapBuffers();
	}
#undef LZZ_INLINE
