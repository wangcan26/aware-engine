cmake_minimum_required (VERSION 2.8)
project (aware-engine)

#Generate C++ header and footer from the lzzFile before building the specified target.
function(lzz_dependency target lzzFile)
	add_custom_target(${lzzFile} lzz -hd -sd ${PROJECT_SOURCE_DIR}/${lzzFile})
	add_dependencies(${target} ${lzzFile})
endfunction(lzz_dependency)

#SET(DepsDir "${PROJECT_SOURCE_DIR}/Dependencies/${CRUXIC_CROSS_SYSTEM}")

#include_directories(BEFORE "${DepsDir}/ffmpeg/include")
#link_directories("${DepsDir}/ffmpeg/lib")

FIND_PACKAGE(OpenGL REQUIRED)
FIND_PACKAGE(PNG 1.2 REQUIRED)


#Windows
if (CRUXIC_CROSS_SYSTEM STREQUAL "Win32")
		
	set(AWARE_FFMPEG_INCLUDE_DIRS )
	set(AWARE_FFMPEG_LIBRARIES 
		avformat
		avcodec
		swscale
		avutil)
		
	set(SDL_LIBRARY	SDL.dll)  #really libSDL.dll.a
	set(LUA_LIBRARIES lua) #really liblua.a
	
	#Link libgcc and libstdc++ statically to avoid dependency on these DLLs:
	#  libgcc_s_sjlj-1.dll and libstdc++-6.dll
	#GCC manual says static libgcc is fine as long as you are not trying to throw C++ exceptions out of a DLL
	set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -static-libgcc -static-libstdc++")  #
		
#Linux
else()
	FIND_PACKAGE(SDL REQUIRED)


	set(OptAwareEngineDir /opt/AwareEngine)
	if(NOT IS_DIRECTORY ${OptAwareEngineDir})
		message(SEND_ERROR "${OptAwareEngineDir} missing.  Please install dependencies (like ffmpeg) there first.")
	endif()

	set(AWARE_FFMPEG_INCLUDE_DIRS ${OptAwareEngineDir}/include)
	SET(AWARE_FFMPEG_LIBRARIES 
		${OptAwareEngineDir}/lib/libavformat.so 
		${OptAwareEngineDir}/lib/libavcodec.so 
		${OptAwareEngineDir}/lib/libswscale.so 
		${OptAwareEngineDir}/lib/libavutil.so)
		
	
	set(LUA_LIBRARIES /usr/local/lib/liblua.a)
	set(LUA_INCLUDE_DIR /usr/local/include)
	
endif()

set(CMAKE_C_FLAGS "-Wall")
set(CMAKE_CXX_FLAGS "-Wall")

#
# Compile Lua OpenGL bindings into a static library
#
add_subdirectory(luagl)

include_directories(BEFORE ${LUA_INCLUDE_DIR} ${AWARE_FFMPEG_INCLUDE_DIRS} ${PNG_INCLUDE_DIRS} ${OPENGL_INCLUDE_DIR} )

#The library
add_executable(aware-engine main.cpp ae_native.cpp ae_native_wrap.cpp)

lzz_dependency(aware-engine ae_native.lzz)

add_custom_command(OUTPUT ${PROJECT_SOURCE_DIR}/ae_native_wrap.cpp
                   COMMAND swig -Wall "-c++" -lua -o ${PROJECT_SOURCE_DIR}/ae_native_wrap.cpp ${PROJECT_SOURCE_DIR}/ae_native.h
				   DEPENDS ${PROJECT_SOURCE_DIR}/ae_native.h
				   COMMENT "Generating SWIG bindings")


target_link_libraries(aware-engine luagl ${LUA_LIBRARIES} ${SDL_LIBRARY} ${PNG_LIBRARIES} ${AWARE_FFMPEG_LIBRARIES} ${OPENGL_LIBRARIES})



