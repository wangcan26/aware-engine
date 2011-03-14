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
#include "aware_util.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>

/**a string containing the last error that occurred.  Empty string means no error has
occurred since the last call to aw_clearLastError()*/
static char gLastError[1024];

void aw_setLastError(const char * errorType, const char * errorMessage)
{
	strcpy(gLastError, errorType);
	strcat(gLastError, ": ");

	//prevent overflow
	int avail = sizeof(gLastError) - strlen(gLastError) - 1;  //null-term
	int msgLen = strlen(errorMessage);
	if (msgLen > avail)
		msgLen = avail;
	strncat(gLastError, errorMessage, msgLen);

	//print error in devel mode
	#ifdef AW_DEBUG
	fprintf(stderr, "aw_setLastError: %s\n", gLastError);
	#endif
}

void aw_setLastError_i(const char * errorType, const char * messageFormat, int value)
{
	char * buf = (char*)malloc(strlen(messageFormat) + 32);
	sprintf(buf, messageFormat, value);
	aw_setLastError(errorType, buf);
	free(buf);
}

void aw_setLastError_s(const char * errorType, const char * messageFormat, const char * str)
{
	if (str == NULL)
		str = "<NULL>";

	char * buf = (char*)malloc(strlen(messageFormat) + strlen(str) + 8);
	sprintf(buf, messageFormat, str);
	aw_setLastError(errorType, buf);
	free(buf);
}

void aw_setLastError_perror(const char * errorType, const char * optionalInfo)
{
	if (optionalInfo == NULL)
		optionalInfo = "";

	const char * str = errno != 0 ? strerror(errno) : "Unknown Error";
	if (str == NULL)
		str = "Unknown Error";

	char * msg = (char*)malloc(strlen(str) + strlen(optionalInfo) + 10);
	strcpy(msg, str);
	if (strlen(optionalInfo) > 0)
	{
		strcat(msg, ": ");
		strcat(msg, optionalInfo);
	}

	aw_setLastError(errorType, msg);
	free(msg);
}

void aw_clearLastError()
{
	gLastError[0] = 0;
}

GLboolean aw_haveError()
{
	return strlen(gLastError) > 0;
}

const char * aw_getLastError()
{
	return gLastError;
}

void * aw_getNativePtr(JNIEnv * env, jobject obj)
{
	//prevent segfault
	CHECK_NPE(obj, NULL);

	jclass objClass = env->GetObjectClass(obj);
	CHECK_JNI_PTR(objClass, NULL);

	jfieldID obj_nativePtr_field = env->GetFieldID(objClass, "nativePtr", "J");
	CHECK_JNI_PTR(obj_nativePtr_field, NULL);

	jlong nativePtr = env->GetLongField(obj, obj_nativePtr_field);

	return (void *)nativePtr;
}

jboolean aw_setNativePtr(JNIEnv * env, jobject obj, void * newPointer)
{
	//prevent segfault
	CHECK_NPE(obj, JNI_FALSE);

	jclass objClass = env->GetObjectClass(obj);
	CHECK_JNI_PTR(objClass, JNI_FALSE);

	jfieldID obj_nativePtr_field = env->GetFieldID(objClass, "nativePtr", "J");
	CHECK_JNI_PTR(obj_nativePtr_field, JNI_FALSE);

	env->SetLongField(obj, obj_nativePtr_field, (jlong)newPointer);

	return !env->ExceptionCheck();
}

int aw_throwJNIErr(JNIEnv * env, const char * file, int line)
{
	//if a previous JNI function has already set an exception then throw it instead
	if ( ! env->ExceptionCheck())
	{
		//throw a new exception

		char * msg = (char*)malloc(strlen(file) + 64);
		sprintf(msg, "JNI Problem at %s:%d", file, line);

		jclass javaLangError = env->FindClass("java/lang/Error");
		env->ThrowNew(javaLangError, msg);

		free(msg);
	}

	//ALWAYS return 1 so the CHECK_JNI_PTR macro can function properly
	return 1;
}

int aw_throwJNINullPointerException(JNIEnv * env, const char * file, int line)
{
	//if a previous JNI function has already set an exception then throw it instead
	if ( ! env->ExceptionCheck())
	{
		//throw a new exception
		char * msg = (char*)malloc(strlen(file) + 64);
		sprintf(msg, "encountered null object at %s:%d", file, line);

		jclass classNPE = env->FindClass("java/lang/NullPointerException");
		env->ThrowNew(classNPE, msg);

		free(msg);
	}

	//ALWAYS return 1 so the CHECK_NPE macro can function properly
	return 1;
}

void aw_throwLastErrorAs_IOExceptionRt(JNIEnv * env, const char * fallbackError)
{
	//if a previous JNI function has already set an exception then throw it instead
	if ( ! env->ExceptionCheck())
	{
		if (!aw_haveError())
			aw_setLastError(AW_ERR_TYPE_IO, fallbackError);

		jclass classIOERt = env->FindClass("cruxic/aware/IOExceptionRt");
		CHECK_JNI_PTR(classIOERt, );
		env->ThrowNew(classIOERt, aw_getLastError());

		aw_clearLastError();
	}
}
