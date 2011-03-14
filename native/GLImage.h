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
