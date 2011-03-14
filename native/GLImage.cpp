#include "GLImage.h"

#include <stdlib.h>
#include <string.h>
#include <ctype.h>  //tolower()

#include "aware_util.h"

#include "misc/libtarga.h"
#include <png.h>
#include <jpeglib.h>
#include <setjmp.h>  //for jpeg error recovery

#include "misc/Rect4i.hpp"

#define FQCN_GLImage "cruxic/aware/GLImage"

//allocated an empty GLImage
GLImage::GLImage()
{
    width = 0;
    height = 0;
    texels = NULL;
    nBytes = 0;
	format = 0;
	internalFormat = 0;
}

GLImage::GLImage(int w, int h, int nComponents, bool fillBlack)
{
    width = w;
    height = h;
	internalFormat = nComponents;
	switch (internalFormat)
	{
		case 1:
			format = GL_ALPHA;  //is GL_ALPHA right?
			break;
		case 3:
			format = GL_RGB;
			break;
		case 4:
			format = GL_RGBA;
			break;
		default:
			format = 0;  //error
	}

	nBytes = w * h * internalFormat;
    texels = (GLubyte *)malloc(nBytes);

	if (fillBlack)
		memset(texels, 0, nBytes);
}

//free the given GLImage and it's texel-data
GLImage::~GLImage()
{
	free(texels);  //use free() because texels was allocated with malloc
	texels = NULL;
}

void GLImage::allocateBlank(int w, int h, GLubyte initialGrayscaleColor)
{
	width = w;
	height = h;
	format = GL_RGB;
	internalFormat = 3;  //RGB
	nBytes = w * h * internalFormat;
	free(texels);
	texels = (GLubyte*)malloc(nBytes);

	memset(texels, initialGrayscaleColor, nBytes);
}

void GLImage::copyRect(const GLImage & source, const Rect4i & sourceRect)
{
	//this function only works if source and dest pixel format is the same
	if (source.internalFormat != internalFormat)
	{
		fprintf(stderr, "GLImage::copyRect: format mismatch\n");
		return;
	}

	//is target buffer large enough?
	int srcSize = sourceRect.area() * internalFormat;
	if (srcSize > nBytes)
	{
		fprintf(stderr, "GLImage::copyRect: destination too small\n");
		return;
	}

	int rectW = sourceRect.width();

	//Optimization: Just use a single memcpy if rect spans entire width of source image
	if (rectW == source.width)
	{
		memcpy(texels, source.getRowPtr(sourceRect.y1), srcSize);  //safe because I asserted above that width and height is the same (above)
	}
	else
	{
		GLubyte * destPos = texels;

		GLubyte * srcPos = source.getPixelPtr(sourceRect.x1, sourceRect.y1);
		const int srcStride = source.width * internalFormat;
		const int bytesPerRow = rectW * internalFormat;

		//for each row...
		for (int y = sourceRect.y1; y < sourceRect.y2; y++)
		{
			//copy from x1 to x2
			memcpy(destPos, srcPos, bytesPerRow);

			destPos += bytesPerRow;
			srcPos += srcStride;
		}
	}
}

bool GLImage::isValidPixelPtr(GLubyte * ptr) const
{
	bool valid = false;

	if (ptr == NULL)
		fprintf(stderr, "isValidPixelPtr: NULL pointer\n");
	else if (texels == NULL)
		fprintf(stderr, "isValidPixelPtr: texel pointer is NULL\n");
	else if (ptr < texels)
		fprintf(stderr, "isValidPixelPtr: pointer < texels\n");
	else if (ptr < texels)
		fprintf(stderr, "isValidPixelPtr: pointer (%p) < texels (%p)\n", ptr, texels);
	else if (ptr >= (texels + nBytes))
		fprintf(stderr, "isValidPixelPtr: pointer (%p) >= texels (%p)\n", ptr, texels);
	else if (((unsigned long)(ptr - texels)) % internalFormat != 0)
		fprintf(stderr, "isValidPixelPtr: pointer offset (%d) not multiple of internalFormat (%d)\n", (int)(ptr - texels), internalFormat);
	else
		valid = true;

	return valid;
}

#ifdef GLIMAGE_SAFE
GLubyte * GLImage::getRowPtr(int y) const
{
	if (texels == NULL)
		fprintf(stderr, "GLImage: getRowPtr: texels is null\n");
	else if (y < 0 || y >= height)
		fprintf(stderr, "GLImage: getRowPtr: y=%d is out of range [0,%d)\n", y, height);

	return &texels[y * width * internalFormat];
}

GLubyte * GLImage::getPixelPtr(int x, int y) const
{
	if (texels == NULL)
		fprintf(stderr, "GLImage: getPixelPtr: texels is null\n");
	else if (x < 0 || x >= width)
		fprintf(stderr, "GLImage: getPixelPtr: x=%d is out of range [0,%d)\n", x, width);
	else if (y < 0 || y >= height)
		fprintf(stderr, "GLImage: getPixelPtr: y=%d is out of range [0,%d)\n", y, height);

	return &texels[((y * width) + x) * internalFormat];
}

GLubyte * GLImage::getPixelPtrInRow(GLubyte * rowPtr, int x)
{
	if (texels == NULL)
		fprintf(stderr, "GLImage: getPixelPtrInRow: texels is null\n");
	else if (x < 0 || x >= width)
		fprintf(stderr, "GLImage: getPixelPtrInRow: x=%d is out of range [0,%d)\n", x, width);
	else if (!isValidPixelPtr(rowPtr))
		fprintf(stderr, "GLImage: getPixelPtrInRow: invalid pixel pointer %p\n", rowPtr);
	//ensure rowPtr is truly at start of row
	else
	{
		int offset = (int)(rowPtr - texels);
		if ((offset / internalFormat) % width != 0)
			fprintf(stderr, "GLImage: getPixelPtrInRow: row pointer (%p) not at start of row!\n", rowPtr);
	}

	return &rowPtr[x * internalFormat];
}

#endif


GLboolean getPNGtextureInfo(int color_type, GLImage * texinfo)
{
	switch (color_type)
	{
		case PNG_COLOR_TYPE_GRAY:
			texinfo->format = GL_LUMINANCE;
			texinfo->internalFormat = 1;
			break;

		case PNG_COLOR_TYPE_GRAY_ALPHA:
			texinfo->format = GL_LUMINANCE_ALPHA;
			texinfo->internalFormat = 2;
			break;

		case PNG_COLOR_TYPE_RGB:
			texinfo->format = GL_RGB;
			texinfo->internalFormat = 3;
			break;

		case PNG_COLOR_TYPE_RGB_ALPHA:
			texinfo->format = GL_RGBA;
			texinfo->internalFormat = 4;
			break;

		default:
			aw_setLastError_i(AW_ERR_TYPE_PNG, "Unsupported color type %d", color_type);
			texinfo->format = 0;
			texinfo->internalFormat = 0;
			return GL_FALSE;
	}

	//success!
	return GL_TRUE;
}

GLboolean readTGAFromFile(const char * filename, GLImage * tex)
{
    //use libtarga (http://www.cs.wisc.edu/graphics/Gallery/LibTarga/)
    tex->texels = (GLubyte *)tga_load(filename, &tex->width, &tex->height, TGA_TRUECOLOR_24);
    if (tex->texels == NULL)
    {
		aw_setLastError(AW_ERR_TYPE_IO, tga_error_string(tga_get_last_error()));
		return GL_FALSE;
    }

    tex->nBytes = tex->width * tex->height * 3;
    tex->format = GL_RGB;
    tex->internalFormat = 3;  //24bit

    return GL_TRUE;
}


/**
	Load a PNG file into memory.	Mostly copied from this tutorial: http://tfcduke.developpez.com/tutoriel/format/png/
*/
GLboolean readPNGFromFile(const char * filename, GLImage * texinfo, int metaDataOnly)
{
	png_byte magic[8];
	png_structp png_ptr = NULL;
	png_infop info_ptr = NULL;
	int bit_depth = 0, color_type = 0;
	FILE *fp = NULL;
	png_bytep *row_pointers = NULL;
	int i;

	/* open image file */
	fp = fopen (filename, "rb");
	if (!fp)
	{
		aw_setLastError_perror(AW_ERR_TYPE_IO, filename);
		return GL_FALSE;
	}

	/* read magic number */
	fread(magic, 1, sizeof (magic), fp);

	/* check for valid magic number */
	if (!png_check_sig(magic, sizeof (magic)))
	{
		aw_setLastError_s(AW_ERR_TYPE_PNG, "\"%s\" is not a valid PNG image!", filename);
		fclose (fp);
		return GL_FALSE;
	}

	/* create a png read struct */
	png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
	if (!png_ptr)
	{
		fclose (fp);
		return GL_FALSE;
	}

	/* create a png info struct */
	info_ptr = png_create_info_struct(png_ptr);
	if (!info_ptr)
	{
		fclose (fp);
		png_destroy_read_struct(&png_ptr, NULL, NULL);
		return GL_FALSE;
	}



	/* initialize the setjmp for returning properly after a libpng error occured */
	if (setjmp(png_jmpbuf(png_ptr)))
	{
		fclose (fp);
		png_destroy_read_struct(&png_ptr, &info_ptr, NULL);

		if (row_pointers)
			free(row_pointers);

		return GL_FALSE;
	}

	/* setup libpng for using standard C fread() function with our FILE pointer */
	png_init_io(png_ptr, fp);

	/* tell libpng that we have already read the magic number */
	png_set_sig_bytes(png_ptr, sizeof (magic));

	/* read png info */
	png_read_info(png_ptr, info_ptr);

	/* get some usefull information from header */
	bit_depth = png_get_bit_depth (png_ptr, info_ptr);
	color_type = png_get_color_type (png_ptr, info_ptr);

	/* convert index color images to RGB images */
	if (color_type == PNG_COLOR_TYPE_PALETTE)
		png_set_palette_to_rgb(png_ptr);

	/* convert 1-2-4 bits grayscale images to 8 bits grayscale. */
	if (color_type == PNG_COLOR_TYPE_GRAY && bit_depth < 8)
		png_set_gray_1_2_4_to_8 (png_ptr);

	if (png_get_valid (png_ptr, info_ptr, PNG_INFO_tRNS))
		png_set_tRNS_to_alpha (png_ptr);

	if (bit_depth == 16)
		png_set_strip_16(png_ptr);
	else if (bit_depth < 8)
		png_set_packing(png_ptr);

	/* update info structure to apply transformations */
	png_read_update_info(png_ptr, info_ptr);

	/* retrieve updated information */
	png_uint_32 w;
	png_uint_32 h;
	png_get_IHDR(png_ptr, info_ptr, &w, &h,	&bit_depth, &color_type, NULL, NULL, NULL);
	texinfo->width = w;
	texinfo->height = h;

	/* get image format and components per pixel */
	if (!getPNGtextureInfo(color_type, texinfo))
	{
		fclose(fp);
		return GL_FALSE;
	}

	if (!metaDataOnly)
	{
		/* we can now allocate memory for storing pixel data */
		texinfo->nBytes = sizeof (GLubyte) * texinfo->width * texinfo->height * texinfo->internalFormat;
		texinfo->texels = (GLubyte*)malloc(texinfo->nBytes);

		/* setup a pointer array.  Each one points at the beginning of a row. */
		row_pointers = (png_bytep *)malloc(sizeof(png_bytep) * texinfo->height);

		for (i = 0; i < texinfo->height; ++i)
		{
			row_pointers[i] = (png_bytep)(texinfo->texels + ((texinfo->height - (i + 1)) * texinfo->width * texinfo->internalFormat));
		}

		/* read pixel data using row pointers */
		png_read_image(png_ptr, row_pointers);
		/* finish decompression and release memory */
		png_read_end(png_ptr, NULL);

		/* we don't need row pointers anymore */
		free(row_pointers);
	}

	png_destroy_read_struct(&png_ptr, &info_ptr, NULL);


	fclose(fp);
	return GL_TRUE;
}

typedef struct
{
    struct jpeg_error_mgr pub;
    jmp_buf setjmp_buffer;
} my_jpeg_error_mgr;

void my_jpeg_error_exit(j_common_ptr cinfo)
{
    my_jpeg_error_mgr * myerr = (my_jpeg_error_mgr *)cinfo->err;
    //print the error message
    (*cinfo->err->output_message)(cinfo);
    longjmp(myerr->setjmp_buffer, 1);
}

GLboolean readJPEGFromFile(const char * filename, GLImage * tex)
{
    //Adapted from http://www.efkhoury.com/content/opengl-jpeg-texture-loader-using-ijg-libjpeg

    FILE * infile = fopen(filename, "rb");
    if (infile == NULL)
    {
		aw_setLastError_perror(AW_ERR_TYPE_IO, filename);
		return GL_FALSE;
    }

    struct jpeg_decompress_struct cinfo;
    my_jpeg_error_mgr jerr;
    cinfo.err = jpeg_std_error(&jerr.pub);
    jerr.pub.error_exit = my_jpeg_error_exit;

    if (setjmp(jerr.setjmp_buffer))
    {
		jpeg_destroy_decompress(&cinfo);
		fclose(infile);
		aw_setLastError_perror(AW_ERR_TYPE_IO, NULL);
		return GL_FALSE;
    }
    jpeg_create_decompress(&cinfo);

    jpeg_stdio_src(&cinfo, infile);

    jpeg_read_header(&cinfo, GL_TRUE);
    jpeg_start_decompress(&cinfo);

    int row_stride = cinfo.output_width * cinfo.output_components;

    JSAMPARRAY buffer = (*cinfo.mem->alloc_sarray)((j_common_ptr) &cinfo, JPOOL_IMAGE, row_stride, 1);

    tex->nBytes = cinfo.output_height * cinfo.output_width * cinfo.output_components;
    tex->texels = (GLubyte*)malloc(tex->nBytes);
    tex->format = GL_RGB;
    tex->internalFormat = cinfo.output_components;
    tex->width = cinfo.output_width;
    tex->height = cinfo.output_height;

    //read each line (top to bottom)...
    int pos = tex->nBytes;
    while (cinfo.output_scanline < cinfo.output_height)
    {
	//read one line
	jpeg_read_scanlines(&cinfo, buffer, 1);

	//memcpy(&tex->texels[(cinfo.output_scanline - read_now) * cinfo.output_width * cinfo.output_components], buffer[0], row_stride);

	//fill buffer buffer in reverse because glTexImage2D expects pixels from bottom to top
	pos -= row_stride;
	memcpy(&tex->texels[pos], buffer[0], row_stride);
    }

    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);
    fclose(infile);
    return GL_TRUE;
}


/**return GL_TRUE if 'fn' ends with the given suffix (case insensitive)*/
GLboolean strEndsWithCI(const char * fn, const char * suffix)
{
	int suflen = strlen(suffix);
	int fnlen = strlen(fn);
	if (fnlen < suflen)
		return GL_FALSE;
	for (int i = 0; i < suflen; i++)
	{
		char cf = tolower(fn[(fnlen - suflen) + i]);
		char cs = tolower(suffix[i]);
		if (cf != cs)
			return GL_FALSE;
	}

	return GL_TRUE;
}

GLImage * GLImage_loadFromFile(const char * fileName, int metaDataOnly)
{
	if (!strEndsWithCI(fileName, ".png"))
	{
		aw_setLastError_s(AW_ERR_TYPE_IO, "metaDataOnly not implemented: %s", fileName);
		return NULL;
	}

    GLImage * img = new GLImage();


    if (strEndsWithCI(fileName, ".png"))
    {
		if (readPNGFromFile(fileName, img, metaDataOnly))
			return img;
    }
    else if (strEndsWithCI(fileName, ".jpg") || strEndsWithCI(fileName, ".jpeg"))
    {
		if (readJPEGFromFile(fileName, img))
			return img;
    }
    else if (strEndsWithCI(fileName, ".tga"))
    {
		if (readTGAFromFile(fileName, img))
			return img;
    }
    else
    {
		aw_setLastError_s(AW_ERR_TYPE_IO, "Unsupported image type: %s", fileName);
    }

	//error
	return NULL;
}

GLImage * GLImage_getNativePtr(JNIEnv * env, jobject javaGLImageObject)
{
	return (GLImage *)aw_getNativePtr(env, javaGLImageObject);
}


/*
 * Class:     cruxic_aware_GLImage
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_cruxic_aware_GLImage_dispose
  (JNIEnv * env, jobject self)
{
	GLImage * img = GLImage_getNativePtr(env, self);
	if (img != NULL)
	{
		delete img;
		aw_setNativePtr(env, self, NULL);
	}
}

/*
 * Class:     cruxic_aware_GLImage
 * Method:    loadPNG
 * Signature: (Ljava/lang/String;)Lcruxic/aware/GLImage;
 */
JNIEXPORT jobject JNICALL Java_cruxic_aware_GLImage_loadFromFile
  (JNIEnv * env, jclass clazz, jstring filePath)
{
	aw_clearLastError();

	//Get ready to call GLImage constructor
	jclass cls_GLImage = env->FindClass(FQCN_GLImage);
	CHECK_JNI_PTR(cls_GLImage, NULL);
	jmethodID ctor_GLImage = env->GetMethodID(cls_GLImage,"<init>","(JIII)V");
	CHECK_JNI_PTR(ctor_GLImage, NULL);

	//prevent segfault
	CHECK_NPE(filePath, NULL);

	const char * cFilePath = env->GetStringUTFChars(filePath, NULL);
	CHECK_JNI_PTR(cFilePath, NULL);

	GLImage * img = GLImage_loadFromFile(cFilePath, 0);
	env->ReleaseStringUTFChars(filePath, cFilePath);

	if (img != NULL)
	{
		return env->NewObject(cls_GLImage, ctor_GLImage, (jlong)img, img->width, img->height, img->nBytes);
	}
	//error
	else
	{
		aw_throwLastErrorAs_IOExceptionRt(env, "GLImage_load");
		return NULL;
	}
}

/*
 * Class:     cruxic_aware_GLImage
 * Method:    readImageSize
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_cruxic_aware_GLImage_readImageSize
  (JNIEnv * env, jclass na, jstring filePath)
{
	aw_clearLastError();

	//prevent segfault
	CHECK_NPE(filePath, 0);

	const char * cFilePath = env->GetStringUTFChars(filePath, NULL);
	CHECK_JNI_PTR(cFilePath, 0);

	//read meta-data only
	GLImage * img = GLImage_loadFromFile(cFilePath, 1);
	env->ReleaseStringUTFChars(filePath, cFilePath);

	if (img != NULL)
	{
		jint size = img->width * img->height * img->internalFormat;
		delete img;
		return size;
	}
	//error
	else
	{
		aw_throwLastErrorAs_IOExceptionRt(env, "GLImage_readImageSize");
		return 0;
	}
}

JNIEXPORT jobject JNICALL Java_cruxic_aware_GLImage_createFromRawBytes
  (JNIEnv * env, jclass cls_GLImage, jint width, jint height, jbyteArray raw)
{
	aw_clearLastError();

	//Get ready to call GLImage constructor
	CHECK_JNI_PTR(cls_GLImage, NULL);
	jmethodID ctor_GLImage = env->GetMethodID(cls_GLImage,"<init>","(JIII)V");
	CHECK_JNI_PTR(ctor_GLImage, NULL);

	//prevent segfault
	CHECK_NPE(raw, NULL);

	GLImage * img = new GLImage();
	img->width = width;
	img->height = height;
	img->nBytes = env->GetArrayLength(raw);
    img->texels = (GLubyte *)malloc(img->nBytes);
	img->format = GL_RGB;
	img->internalFormat = 3;

	//copy from java to native
	env->GetByteArrayRegion(raw, 0, img->nBytes, (jbyte *)img->texels);

	return env->NewObject(cls_GLImage, ctor_GLImage, (jlong)img, img->width, img->height, img->nBytes);
}

/*
 * Class:     cruxic_aware_GLImage
 * Method:    allocateBlank
 * Signature: (IIB)Lcruxic/aware/GLImage;
 */
JNIEXPORT jobject JNICALL Java_cruxic_aware_GLImage_allocateBlank
  (JNIEnv * env, jclass cls_GLImage, jint width, jint height, jbyte initialColor)
{
	aw_clearLastError();

	//Get ready to call GLImage constructor
	CHECK_JNI_PTR(cls_GLImage, NULL);
	jmethodID ctor_GLImage = env->GetMethodID(cls_GLImage,"<init>","(JIII)V");
	CHECK_JNI_PTR(ctor_GLImage, NULL);

	GLImage * img = new GLImage();
	img->width = width;
	img->height = height;
	img->nBytes = 3 * width * height;;
    img->texels = (GLubyte *)malloc(img->nBytes);
	img->format = GL_RGB;
	img->internalFormat = 3;

	//set grayscale color
	memset(img->texels, initialColor, img->nBytes);


	return env->NewObject(cls_GLImage, ctor_GLImage, (jlong)img, img->width, img->height, img->nBytes);
}

/*
 * Class:     cruxic_aware_GLImage
 * Method:    copyRect
 * Signature: (Lcruxic/aware/GLImage;IIII)V
 */
JNIEXPORT void JNICALL Java_cruxic_aware_GLImage_copyRect
  (JNIEnv * env, jobject jthis, jobject _srcImg, jint x1, jint y1, jint x2, jint y2)
{
	aw_clearLastError();

	GLImage * thisImg = GLImage_getNativePtr(env, jthis);
	CHECK_NPE(thisImg,);

	GLImage * srcImg = GLImage_getNativePtr(env, _srcImg);
	CHECK_NPE(srcImg,);

	Rect4i srcRect(x1, y1, x2, y2);
	const int rectW = srcRect.width();
	const int rectH = srcRect.height();

	//Throw IllegalArgumentException if size of requested rect does not match this GLImage
	if (rectW != thisImg->width
		|| rectH != thisImg->height)
	{
		env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "destination GLImage width or height does not match that of the specified rectangle");
		return;
	}

	//Throw IllegalArgumentException if source-image is smaller than specified rectangle
	if (rectW > srcImg->width
		|| rectH > srcImg->height)
	{
		env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "specified rectangle is larger than source image");
		return;
	}

	//Throw IllegalArgumentException if pixel formats don't match
	if (thisImg->internalFormat != srcImg->internalFormat)
	{
		env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "pixel format mismatch");
		return;
	}

	thisImg->copyRect(*srcImg, srcRect);
}

/*
 * Class:     cruxic_aware_GLImage
 * Method:    fillRect
 * Signature: (IIIIBBB)V
 */
JNIEXPORT void JNICALL Java_cruxic_aware_GLImage_fillRect
  (JNIEnv * env, jobject jthis, jint x1, jint y1, jint x2, jint y2, jbyte r, jbyte g, jbyte b)
{
	aw_clearLastError();

	GLImage * thisImg = GLImage_getNativePtr(env, jthis);
	CHECK_NPE(thisImg,);

	Rect4i rect(x1, y1, x2, y2);
	Rect4i imgRect(0, 0, thisImg->width, thisImg->height);

	if (!imgRect.contains(rect))
	{
		env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Specified rect outside of image!");
		return;
	}

	if (thisImg->internalFormat < 3)
	{
		env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Image is not RGB");
		return;
	}


	GLubyte R = r & 0xFF;
	GLubyte G = g & 0xFF;
	GLubyte B = b & 0xFF;

	for (int y = y1; y < y2; y++)
	{
		GLubyte * rowPtr = thisImg->getRowPtr(y);

		for (int x = x1; x < x2; x++)
		{
			GLubyte * pixel = thisImg->getPixelPtrInRow(rowPtr, x);

			pixel[0] = R;
			pixel[1] = G;
			pixel[2] = B;
		}
	}
}

/*
 * Class:     cruxic_aware_GLImage
 * Method:    combine
 * Signature: (Lcruxic/aware/GLImage;IIII)V
 */
JNIEXPORT void JNICALL Java_cruxic_aware_GLImage_combine
  (JNIEnv * env, jobject jthis, jobject _source, jint x1, jint y1, jint x2, jint y2)
{
	aw_clearLastError();

	GLImage * dest = GLImage_getNativePtr(env, jthis);
	CHECK_NPE(dest,);

	GLImage * source = GLImage_getNativePtr(env, _source);
	CHECK_NPE(source,);

	//Rect4i rect(x1, y1, x2, y2);
	//Rect4i imgRect(0, 0, dest->width, dest->height);

/*	if (!imgRect.contains(rect))
	{
		env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "Specified rect outside of image!");
		return;
	}

	//Throw IllegalArgumentException if size of requested rect does not match this GLImage
	if (rectW != thisImg->width
		|| rectH != thisImg->height)
	{
		env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "destination GLImage width or height does not match that of the specified rectangle");
		return;
	}

	//Throw IllegalArgumentException if source-image is smaller than specified rectangle
	if (rectW > srcImg->width
		|| rectH > srcImg->height)
	{
		env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "specified rectangle is larger than source image");
		return;
	}

	//Throw IllegalArgumentException if pixel formats don't match
	if (thisImg->internalFormat != srcImg->internalFormat)
	{
		env->ThrowNew(env->FindClass("java/lang/IllegalArgumentException"), "pixel format mismatch");
		return;
	}
*/

	//TODO: throw IllegalArgumentException to prevent segfault below

	for (int y = y1; y < y2; y++)
	{
		GLubyte * destRowPtr = dest->getRowPtr(y);
		GLubyte * srcRowPtr = source->getRowPtr(y - y1);

		for (int x = x1; x < x2; x++)
		{
			GLubyte * destPixel = dest->getPixelPtrInRow(destRowPtr, x);
			GLubyte * srcPixel = source->getPixelPtrInRow(srcRowPtr, x - x1);

			destPixel[0] |= srcPixel[0];
			destPixel[1] |= srcPixel[1];
			destPixel[2] |= srcPixel[2];
		}
	}
}
