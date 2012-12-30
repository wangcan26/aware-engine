cmake_minimum_required (VERSION 2.8)
project (libaware)

#SET(DepsDir "${PROJECT_SOURCE_DIR}/Dependencies/${CRUXIC_CROSS_SYSTEM}")

#include_directories(BEFORE "${DepsDir}/ffmpeg/include")
#link_directories("${DepsDir}/ffmpeg/lib")

#JNI
if (CRUXIC_CROSS_SYSTEM STREQUAL "Win32")
	FIND_PACKAGE(JNI REQUIRED)
#Linux
else()
	#FindJNI is broken on Ubuntu 12.04
	if(IS_DIRECTORY $ENV{JDK_HOME}/include)
		set(JNI_INCLUDE_DIRS $ENV{JDK_HOME}/include $ENV{JDK_HOME}/include/linux)
	else()
		message(SEND_ERROR "Please set JDK_HOME environment variable")
	endif()
endif()



#FIND_PACKAGE(Java REQUIRED)
#FIND_PACKAGE(Java COMPONENTS Development REQUIRED)

FIND_PACKAGE(PNG 1.2 REQUIRED)
FIND_PACKAGE(OpenGL REQUIRED)

#
# Function to run javah command to produce JNI header files
#
set(generated_jni_headers)
function(gen_jni_header class_simpleName package_name)
	set(header_path ${CMAKE_CURRENT_SOURCE_DIR}/jni/${class_simpleName}_jni.h)

	add_custom_command(OUTPUT ${header_path}
		COMMAND javah -force -classpath ${PROJECT_SOURCE_DIR}/../aware.jar -o ${header_path} ${package_name}.${class_simpleName}
		DEPENDS ${PROJECT_SOURCE_DIR}/../aware.jar)

	set(generated_jni_headers ${generated_jni_headers} ${header_path} PARENT_SCOPE)
	
endfunction(gen_jni_header)


set(CMAKE_C_FLAGS "-Wall")
set(CMAKE_CXX_FLAGS "-Wall")

if (CRUXIC_CROSS_SYSTEM STREQUAL "Win32")
	SET(ffmpeg_libs avformat-54.dll avcodec-54.dll swscale-2.dll avutil-51.dll)
#Linux
else()
	#SET(ffmpeg_libs avformat avcodec swscale avutil)
	#SET(test_libs rt)  #needed for clock_gettime
endif()

#Generate JNI headers using the javah command
gen_jni_header(GLImage "cruxic.aware")
gen_jni_header(GLImageUpload "cruxic.aware")
gen_jni_header(Liquid_StaticNoiseOverlay_Impl "cruxic.aware.overlays")

set(SOURCES 
	misc/ArrayList.cpp
	misc/libtarga.c
	misc/noise1234.cpp
	misc/Rect4i.cpp
	aware_util.cpp
	GLImage.cpp
	GLImageUpload.cpp
	HeightMap.cpp
	ImageMask.cpp
	Liquid_StaticNoiseOverlay_Impl.cpp
	StaticNoiseLiquidRefactionRenderer.cpp
	jni/Liquid_StaticNoiseOverlay_Impl_jni.cpp
	${generated_jni_headers}
	)
	
include_directories(BEFORE ${JNI_INCLUDE_DIRS} ${PNG_INCLUDE_DIRS} ${OPENGL_INCLUDE_DIR})


#The library
add_library(aware SHARED ${SOURCES})
target_link_libraries(aware ${PNG_LIBRARIES} ${OPENGL_LIBRARIES})


