
#Enable the use of luaL_register()
#Note: despite the fact that lua was compiled with LUA_COMPAT_ALL, luaconf.h does not hard-code it - you must ask for it explicitly
add_definitions(-DLUA_COMPAT_MODULE)

add_library(luagl STATIC
	luagl.c
	luagl_const.c
	luaglu.c
	luagl_util.c)
