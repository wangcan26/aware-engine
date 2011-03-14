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
#include "Liquid_StaticNoiseOverlay_Impl_jni.h"

#include "../Liquid_StaticNoiseOverlay_Impl.hpp"
#include "../aware_util.h"

/*
 * Class:     cruxic_aware_overlays_Liquid_StaticNoiseOverlay_Impl
 * Method:    init
 * Signature: (Lcruxic/aware/GLImage;Lcruxic/aware/GLImage;FF)I
 */
JNIEXPORT jint JNICALL Java_cruxic_aware_overlays_Liquid_1StaticNoiseOverlay_1Impl_init
  (JNIEnv * env, jobject jthis, jobject _noise, jobject _mask, jfloat refactionIndex, jfloat noiseIntensity)
{
	aw_clearLastError();

	GLImage * noise = GLImage_getNativePtr(env, _noise);
	CHECK_NPE(noise,-1);

	GLImage * mask = GLImage_getNativePtr(env, _mask);
	CHECK_NPE(mask,-1);

	Liquid_StaticNoiseOverlay_Impl * impl = new Liquid_StaticNoiseOverlay_Impl(
		*noise, *mask, refactionIndex, noiseIntensity);

	//attach pointer to java object
	if (!aw_setNativePtr(env, jthis, impl))
		return -1;  //exception has been set

	return impl->masks.size();
}



/*
 * Class:     cruxic_aware_overlays_Liquid_StaticNoiseOverlay_Impl
 * Method:    update
 * Signature: (IFLcruxic/aware/GLImage;II)V
 */
JNIEXPORT void JNICALL Java_cruxic_aware_overlays_Liquid_1StaticNoiseOverlay_1Impl_update
  (JNIEnv * env, jobject jthis, jint overlayIndex, jfloat angle, jobject _image, jint offsetX, jint offsetY)
{
	Liquid_StaticNoiseOverlay_Impl * impl = (Liquid_StaticNoiseOverlay_Impl *)aw_getNativePtr(env, jthis);

	if (impl != NULL)
	{
		GLImage * image = GLImage_getNativePtr(env, _image);
		CHECK_NPE(image,);

		impl->update(overlayIndex, angle, *image, offsetX, offsetY);
	}
	//else object has been disposed
}

/*
 * Class:     cruxic_aware_overlays_Liquid_StaticNoiseOverlay_Impl
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_cruxic_aware_overlays_Liquid_1StaticNoiseOverlay_1Impl_dispose
  (JNIEnv * env, jobject jthis)
{
	Liquid_StaticNoiseOverlay_Impl * impl = (Liquid_StaticNoiseOverlay_Impl *)aw_getNativePtr(env, jthis);
	if (impl != NULL)
	{
		delete impl;
		aw_setNativePtr(env, jthis, NULL);
	}
}


/*
 * Class:     cruxic_aware_overlays_Liquid_StaticNoiseOverlay_Impl
 * Method:    getRect
 * Signature: (I)Lcruxic/math/Rect4i;
 */
JNIEXPORT jobject JNICALL Java_cruxic_aware_overlays_Liquid_1StaticNoiseOverlay_1Impl_getRect
  (JNIEnv * env, jobject jthis, jint overlayIndex)
{
	Liquid_StaticNoiseOverlay_Impl * impl = (Liquid_StaticNoiseOverlay_Impl *)aw_getNativePtr(env, jthis);
	if (impl != NULL)
	{
		if (overlayIndex >= 0 && overlayIndex <= impl->rects.size())
		{
			Rect4i * rect = impl->rects.items()[overlayIndex];

			// Construct a Java Rect4i

			jclass classRect4i = env->FindClass("cruxic/math/Rect4i");
			CHECK_JNI_PTR(classRect4i,NULL);
			jmethodID ctorRect4i = env->GetMethodID(classRect4i,"<init>","(IIII)V");
			CHECK_JNI_PTR(ctorRect4i,NULL);

			return env->NewObject(classRect4i, ctorRect4i, rect->x1, rect->y1, rect->x2, rect->y2);
		}
	}
	//else object has been disposed

	//error
	return NULL;
}