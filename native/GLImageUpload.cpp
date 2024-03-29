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
#include "jni/GLImageUpload_jni.h"

#include <string.h>
#include <stdlib.h>

#include "GLImage.hpp"
#include "aware_util.h"

//Make GL_CLAMP_TO_EDGE available on Windows (Microsoft OpenGL header is very old)
#ifdef WIN32
#define GL_CLAMP_TO_EDGE 0x812F
#endif

static GLuint * loadSlicedTexture(GLImage * unsliced, int numSlices)
{
	//generate some opengl texture objects
	GLuint * texIds = (GLuint*)malloc(sizeof(GLuint) * numSlices);
	glGenTextures((GLsizei)numSlices, texIds);

	if (numSlices > 1)
	{
		//ideally the image is evenly divisible by the number of slices so that we
		//land evenly on pixel boundaries
		int slice_width = unsliced->width / numSlices;
		if (slice_width * numSlices != unsliced->width)
			aw_setLastError_i(AW_ERR_TYPE_GENERAL, "Image width is not evenly divisible by %d, artifacts may appear.", numSlices);

		//allocate a temporary buffer to hold one slice of the image
		GLubyte * sliceBuf = (GLubyte*)malloc(slice_width * unsliced->height * unsliced->internalFormat);  //internalFormat is 1,2,3 or 4

		//load each slice...
		for (int i = 0; i < numSlices; i++)
		{
			//Copy a sub region of the unsliced image into sliceBuf
			int xOffset = slice_width * i * unsliced->internalFormat;
			GLubyte * rowPtr = unsliced->texels;
			GLubyte * subPtr = sliceBuf;
			//  for each row in the unsliced image...
			for (int row = 0; row < unsliced->height; row++)
			{
				memcpy(subPtr, rowPtr + xOffset, slice_width * unsliced->internalFormat);
				subPtr += slice_width * unsliced->internalFormat;
				rowPtr += unsliced->width * unsliced->internalFormat;
			}

			glBindTexture(GL_TEXTURE_2D, texIds[i]);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);  //GL_CLAMP_TO_EDGE instead of GL_CLAMP fixes the troublesome seams where two textures meet
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

			//upload texture data to OpenGL
			glTexImage2D(GL_TEXTURE_2D, 0, unsliced->internalFormat,
				(GLsizei)slice_width, unsliced->height, 0, unsliced->format,
				GL_UNSIGNED_BYTE, sliceBuf);
			//checkOpenGLError();
		}
	}
	else
	{
		glBindTexture(GL_TEXTURE_2D, texIds[0]);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);  //GL_CLAMP_TO_EDGE instead of GL_CLAMP fixes the troublesome seams where two textures meet
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

		//upload texture data to OpenGL
		glTexImage2D(GL_TEXTURE_2D, 0, unsliced->internalFormat,
			unsliced->width, unsliced->height, 0, unsliced->format,
			GL_UNSIGNED_BYTE, unsliced->texels);
		//checkOpenGLError();
	}

	return texIds;
}


/*
 * Class:     cruxic_aware_GLImageUpload
 * Method:    loadSlicedTexture
 * Signature: (Lcruxic/aware/GLImage;I)[I
 */
JNIEXPORT jintArray JNICALL Java_cruxic_aware_GLImageUpload_loadSlicedTexture
  (JNIEnv * env, jclass clazz, jobject glimg, jint numSlices)
{
	CHECK_NPE(glimg, NULL);
	GLImage * img = GLImage_getNativePtr(env, glimg);
	CHECK_NPE(img, NULL);

	//allocate java array
	jintArray jTexIds = env->NewIntArray(numSlices);
	CHECK_JNI_PTR(jTexIds, NULL);

	GLuint * texIds = loadSlicedTexture(img, numSlices);

	//Copy native array to java
	env->SetIntArrayRegion(jTexIds, 0, numSlices, (jint *)texIds);
	free(texIds);

	return jTexIds;
}

/*
 * Class:     cruxic_aware_GLImageUpload
 * Method:    glTexImage2D
 * Signature: (Lcruxic/aware/GLImage;)V
 */
JNIEXPORT void JNICALL Java_cruxic_aware_GLImageUpload_glTexImage2D
  (JNIEnv * env, jclass na, jobject glimg)
{
	CHECK_NPE(glimg,);
	GLImage * img = GLImage_getNativePtr(env, glimg);
	CHECK_NPE(img,);

	glTexImage2D(GL_TEXTURE_2D, 0, img->internalFormat,
		img->width, img->height, 0, img->format,
		GL_UNSIGNED_BYTE, img->texels);
}

/*
 * Class:     cruxic_aware_GLImageUpload
 * Method:    glTexSubImage2D
 * Signature: (IILcruxic/aware/GLImage;)V
 */
JNIEXPORT void JNICALL Java_cruxic_aware_GLImageUpload_glTexSubImage2D
  (JNIEnv * env, jclass na, jint xOffset, jint yOffset, jobject glimg)
{
	CHECK_NPE(glimg,);
	GLImage * img = GLImage_getNativePtr(env, glimg);
	CHECK_NPE(img,);

	glTexSubImage2D(GL_TEXTURE_2D, 0, xOffset, yOffset,
		img->width, img->height, img->format, GL_UNSIGNED_BYTE, img->texels);
}
