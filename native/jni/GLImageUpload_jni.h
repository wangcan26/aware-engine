/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class cruxic_aware_GLImageUpload */

#ifndef _Included_cruxic_aware_GLImageUpload
#define _Included_cruxic_aware_GLImageUpload
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     cruxic_aware_GLImageUpload
 * Method:    loadSlicedTexture
 * Signature: (Lcruxic/aware/GLImage;I)[I
 */
JNIEXPORT jintArray JNICALL Java_cruxic_aware_GLImageUpload_loadSlicedTexture
  (JNIEnv *, jclass, jobject, jint);

/*
 * Class:     cruxic_aware_GLImageUpload
 * Method:    glTexImage2D
 * Signature: (Lcruxic/aware/GLImage;)V
 */
JNIEXPORT void JNICALL Java_cruxic_aware_GLImageUpload_glTexImage2D
  (JNIEnv *, jclass, jobject);

/*
 * Class:     cruxic_aware_GLImageUpload
 * Method:    glTexSubImage2D
 * Signature: (IILcruxic/aware/GLImage;)V
 */
JNIEXPORT void JNICALL Java_cruxic_aware_GLImageUpload_glTexSubImage2D
  (JNIEnv *, jclass, jint, jint, jobject);

#ifdef __cplusplus
}
#endif
#endif
