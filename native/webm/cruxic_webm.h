#ifndef cruxic_webm_H
#define cruxic_webm_H

#define CWBM_EXPORT
typedef int cwbm_bool;
#define cwbm_true 1
#define cwbm_false 0

#include <stddef.h>  /*for size_t*/

#ifdef __cplusplus
extern "C"
{
#endif

	enum
	{
		CWBM_RGB = 1,
		CWBM_BGRA = 2
	};

	/**Get the version of this cruxic-webm shared-library*/
	CWBM_EXPORT const char * cwbm_get_library_version();

	/**A context for decoding a webm video stream.
	This struct is opaque - all members are accessed via functions.

	A given context instance should never be accessed by more than one thread at a time.
	*/
	typedef struct _cwbm_ctx cwbm_ctx;

	/**Create a context for decoding a webm video stream.*/
	CWBM_EXPORT cwbm_ctx * new_cwbm_ctx();

	/**Close the decoding context an all it's associated resources.*/
	CWBM_EXPORT void delete_cwbm_ctx(cwbm_ctx * ctx);

	/**Get the last error that occurred while decoding the webm stream.
	Returns an empty string if no errors have occurred*/
	CWBM_EXPORT const char * cwbm_ctx_last_error_str(cwbm_ctx * ctx);

	/**Open (or reopen) a webm file.  If cwbm_true is returned the
	webm file was opened successfully and is ready to decode.

	@param output_pixel_format either CWBM_RGB or CWBM_BGRA
	*/
	CWBM_EXPORT cwbm_bool cwbm_ctx_open_webm_file(cwbm_ctx * ctx, const char * webm_file_name, unsigned int output_pixel_format);

	/**Read the next frame and decode it.  If cwbm_false is returned
	there was a fatal error decoding the webm stream.*/
	CWBM_EXPORT cwbm_bool cwbm_ctx_decode_next(cwbm_ctx * ctx);

	/**Get the needed size of the pixel buffer used in cwbm_ctx_convert_pixels().
	This size will not change during decoding.*/
	CWBM_EXPORT size_t cwbm_ctx_get_pixel_buffer_suggested_size(cwbm_ctx * ctx);

	/**Get the presentation time stamp of the last decoded frame, in nanoseconds*/
	CWBM_EXPORT unsigned long cwbm_ctx_get_tstamp(cwbm_ctx * ctx);

	/**Get the width and height (in pixels) of the last decoded frame.*/
	CWBM_EXPORT void cwbm_ctx_get_dimensions(cwbm_ctx * ctx, unsigned int * width_out, unsigned int * height_out);


	/**Convert the current frame from YUV to the pixel format specified in cwbm_ctx_open_webm_file().
	The given buffer must be AT LEAST the size returned by cwbm_ctx_get_pixel_buffer_suggested_size().
	*/
	CWBM_EXPORT cwbm_bool cwbm_ctx_convert_pixels(cwbm_ctx * ctx, void * buffer, size_t buf_max_size);


#ifdef __cplusplus
}
#endif




#endif	/*cruxic_webm_H*/

