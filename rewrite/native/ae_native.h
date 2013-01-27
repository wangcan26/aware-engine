// ae_native.h
//

#ifndef LZZ_ae_native_h
#define LZZ_ae_native_h

	#ifdef SWIG
		%module ae_native
		%{
		#include "ae_native.h"
		%}
	#else
	
		#include <SDL/SDL.h>
		
	#endif
#define LZZ_INLINE inline
typedef char const * ae_error_str;
class GameWindow
{
private:
  SDL_Surface * screen;
public:
  GameWindow ();
  ~ GameWindow ();
  void testException () throw (ae_error_str);
  void open (char const * title, int width, int height, bool fullscreen) throw (ae_error_str);
  int getWidth ();
  int getHeight ();
  void close ();
  bool haveQuitEvent ();
  void waitForQuit ();
  void swapBuffers ();
};
#undef LZZ_INLINE
#endif
