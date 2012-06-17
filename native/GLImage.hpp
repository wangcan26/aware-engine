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
#ifndef GLImage_H
#define GLImage_H

#include <GL/gl.h>
#include "jni/GLImage_jni.h"
#include "misc/Rect4i.hpp"

class GLImage
{
public:
    GLsizei width;
    GLsizei height;

    GLenum format;
    GLint internalFormat;

    GLubyte *texels;
    int nBytes;

#ifdef GLIMAGE_SAFE
	GLubyte * getRowPtr(int y) const;
	GLubyte * getPixelPtr(int x, int y) const;
	GLubyte * getPixelPtrInRow(GLubyte * rowPtr, int x);
#else

	inline GLubyte * getRowPtr(int y) const
	{
		return &texels[y * width * internalFormat];
	}

	inline GLubyte * getPixelPtr(int x, int y) const
	{
		return &texels[((y * width) + x) * internalFormat];
	}

	inline GLubyte * getPixelPtrInRow(GLubyte * rowPtr, int x)
	{
		return &rowPtr[x * internalFormat];
	}
#endif

	bool isValidPixelPtr(GLubyte * ptr) const;

	GLImage();
	GLImage(int w, int h, int nComponents, bool fillBlack = true);
	virtual ~GLImage();

	void copyRect(const GLImage & source, const Rect4i & sourceRect);

	void allocateBlank(int width, int height, GLubyte initialGrayscaleColor);
};

/**Given a Java GLImage object get the C GLImage object.
@return NULL if dispose has already been called*/
GLImage * GLImage_getNativePtr(JNIEnv * env, jobject javaGLImageObject);


#endif
