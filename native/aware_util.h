#ifndef AWARE_UTIL_H
#define AWARE_UTIL_H

#ifdef __cplusplus
extern "C" {
#endif

#include <GL/gl.h>
#include <jni.h>

#define AW_ERR_TYPE_GENERAL "Error"
#define AW_ERR_TYPE_JNI "JNI Error"
#define AW_ERR_TYPE_GL "OpenGL Error"
#define AW_ERR_TYPE_IO "IO Error"
#define AW_ERR_TYPE_PNG "PNG Image Error"

void aw_setLastError(const char * errorType, const char * errorMessage);
void aw_setLastError_i(const char * errorType, const char * messageFormat, int value);
void aw_setLastError_s(const char * errorType, const char * messageFormat, const char * str);

/**behaves like the perror() function*/
void aw_setLastError_perror(const char * errorType, const char * optionalInfo);

void aw_clearLastError();
GLboolean aw_haveError();
const char * aw_getLastError();

int aw_throwJNIErr(JNIEnv * env, const char * file, int line);
int aw_throwJNINullPointerException(JNIEnv * env, const char * file, int line);
void aw_throwLastErrorAs_IOExceptionRt(JNIEnv * env, const char * fallbackError);

void * aw_getNativePtr(JNIEnv * env, jobject obj);
jboolean aw_setNativePtr(JNIEnv * env, jobject obj, void * newPointer);

#define CHECK_JNI_PTR(ptr, retval) if (!(ptr) && aw_throwJNIErr(env, __FILE__, __LINE__)) return retval
#define CHECK_NPE(ptr, retval) if (!(ptr) && aw_throwJNINullPointerException(env, __FILE__, __LINE__)) return retval



#ifdef __cplusplus
}
#endif


#endif