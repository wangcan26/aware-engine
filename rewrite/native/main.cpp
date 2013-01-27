#include <stdarg.h> 
#include <stdio.h> 
#include <errno.h>
#include <string.h>

//#include "GameWindow.h"
#include <lua.hpp>  //lua.hpp appears to be generated when you `make ... install` lua

//SWIG generated bindings loader
extern "C"
{
	extern int luaopen_ae_native(lua_State* L);
}

#include "luagl/luagl.h"
#include "luagl/luaglu.h"

#define LOG_FILE "aware-engine.log"
#define MAIN_SCRIPT "main.lua"

int main(int argc, char* argv[]) 
{
	//
	// Redirect stdout and stderr to a log file
	// 
	//TODO: doesn't work because stdout and stderr have different write pointers so the stomp on eachothers output
// 	FILE * stdoutLog = NULL;
// 	FILE * stderrLog = NULL;
// 	
// 	stdoutLog = freopen(LOG_FILE, "w+", stdout);
// 	if (!stdoutLog)
// 	{
// 		perror(LOG_FILE);
// 		return 1;
// 	}
// 	stderrLog = freopen(LOG_FILE, "w+", stderr);
// 	if (!stderrLog)
// 	{
// 		printf("Failed to redirect stderr to " LOG_FILE ": %s\n", strerror(errno));
// 		return 1;
// 	}

	
    // create new Lua state
    lua_State *L = luaL_newstate();
	
	//Load all standard libraries
	luaL_openlibs(L);
	
	//Load SWIG generated bindings for aware-engine
	luaopen_ae_native(L);
	
	luaopen_luagl(L);
	luaopen_luaglu(L);
	 
    // run the Lua script
    if (luaL_dofile(L, MAIN_SCRIPT) != 0)
	{
		const char * err = lua_tostring(L, -1);
		if (err)
			fprintf(stderr, "ERROR: %s\n", err);
		else
			fprintf(stderr, "Unknown ERROR executing %s\n", MAIN_SCRIPT);
	}
 
    // close the Lua state
    lua_close(L);	
	
} 