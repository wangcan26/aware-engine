#include "cruxic_webm.h"

#define CWBM_VERSION "0.5"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>


/*
	ffmpeg includes

	You'll need to define `__STDC_CONSTANT_MACROS` when compiling a C++ program which links to ffmpeg
*/
#ifdef __cplusplus
extern "C"  /*ffmpeg doesn't do this for you like a good library should.  Without it you'll get a bunch of 'undefined reference' errors when linking a C++ program against ffmpeg*/
{
#endif
	#include <libavcodec/avcodec.h>
	#include <libavformat/avformat.h>
	#include "libswscale/swscale.h"
#ifdef __cplusplus
}
#endif



struct _cwbm_ctx
{
	AVFormatContext * ic;
	AVCodecContext * pCodecCtx;
	AVFrame * pFrame;
	int pixel_buf_size;
	int videx_stream_idx;


	/**Which raw frame in the video (starting at 1).
	Note that a raw frame could decode to multiple final frames.*/
	unsigned int rawFrameNumber;

	/**True if the file was successfully opened but the caller has not yet
	asked for the first frame.*/
	cwbm_bool freshlyOpened;

    /**Frame timestamp in nanoseconds*/
    uint64_t timestamp;

    unsigned int pixel_format;  /*one of CWBM_RGB or CWBM_BGRA etc*/

	/*for YUV->RGB conversion.  (From ffmpeg's highly optimized libswscale)*/
	struct SwsContext * yuv2rgb_ctx;
};

const char * cwbm_get_library_version()
{
	return CWBM_VERSION;
}

CWBM_EXPORT cwbm_ctx * new_cwbm_ctx()
{
	cwbm_ctx * ctx = (cwbm_ctx*)malloc(sizeof(struct _cwbm_ctx));
	memset(ctx, 0, sizeof(struct _cwbm_ctx));

	return ctx;
}

static void cwbm_ctx_close(cwbm_ctx * ctx)
{
	if (ctx)
	{
		/*
		if (ctx->decode_ctx.name != NULL)
			vpx_codec_destroy(&ctx->decode_ctx);

		if (ctx->ne_ctx)
			nestegg_destroy(ctx->ne_ctx);

		if (ctx->fin)
			fclose(ctx->fin);
		*/

		/*NULL out everything*/
		//memset(ctx, 0, sizeof(struct _cwbm_ctx));
	}
}

CWBM_EXPORT void delete_cwbm_ctx(cwbm_ctx * ctx)
{
	if (ctx)
	{
		cwbm_ctx_close(ctx);
		free(ctx);
	}
}

CWBM_EXPORT const char * cwbm_ctx_last_error_str(cwbm_ctx * ctx)
{
	return "";
}

/*static unsigned int cwbm_ctx_get_bytes_per_pixel(cwbm_ctx * ctx)
{
	switch (ctx->pixel_format)
	{
		case CWBM_RGB:
			return 3;
		case CWBM_BGRA:
			return 4;
		default:
			return 3;
	}
}*/

CWBM_EXPORT cwbm_bool cwbm_ctx_open_webm_file(cwbm_ctx * ctx, const char * webm_file_name, unsigned int output_pixel_format)
{


	/*Close previous file (if any)*/
	cwbm_ctx_close(ctx);

	ctx->pixel_format = output_pixel_format;

	av_register_all();


	// Open video file
	if (avformat_open_input(&ctx->ic, webm_file_name, NULL, NULL) == 0)
	{
		AVFormatContext * ic = ctx->ic;  //convenience

		if (avformat_find_stream_info(ic, NULL) >= 0)
		{
			ctx->videx_stream_idx = av_find_best_stream(ic, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
			if (ctx->videx_stream_idx >= 0)
			{
				printf("found video stream at index %d\n", ctx->videx_stream_idx);

				AVStream * vstream = ic->streams[ctx->videx_stream_idx];
				ctx->pCodecCtx = vstream->codec;

				// Find the decoder for the video stream
				AVCodec * pCodec = avcodec_find_decoder(ctx->pCodecCtx->codec_id);
				if (pCodec != NULL)
				{
					//Open codec
					AVDictionary * opts = NULL;
					av_dict_set(&opts, "threads", "auto", 0);
					if (avcodec_open2(ctx->pCodecCtx, pCodec, &opts) >= 0)
					{
						printf("opened codec\n");

						ctx->pFrame = avcodec_alloc_frame();
						if (ctx->pFrame)
						{
							// Determine required buffer size and allocate buffer
							ctx->pixel_buf_size = avpicture_get_size(PIX_FMT_BGRA, ctx->pCodecCtx->width, ctx->pCodecCtx->height);


							printf("Buffers allocated.  Video size: %dx%d\n", ctx->pCodecCtx->width, ctx->pCodecCtx->height);

							/*Decode the first frame so we know for sure things are working OK*/
							if (cwbm_ctx_decode_next(ctx))
							{
								/*causes the next call to cwbm_ctx_decode_next() to no-op and return the frame
								decoded above*/
								ctx->freshlyOpened = cwbm_true;

								return cwbm_true;
							}
						}
					}
				}
			}
		}
	}

	/*if code reaches this point the open failed*/
	cwbm_ctx_close(ctx);
	return cwbm_false;
}

static cwbm_bool cwbm_isOpen(cwbm_ctx * ctx)
{
	return ctx != NULL
		&& ctx->ic != NULL;
}

CWBM_EXPORT cwbm_bool cwbm_ctx_decode_next(cwbm_ctx * ctx)
{
	/**First frame was decoded when we opened the file*/
	if (cwbm_isOpen(ctx) && ctx->freshlyOpened)
	{
		ctx->freshlyOpened = cwbm_false;
		return cwbm_true;
	}

	int frameFinished = 0;
	AVPacket packet;
	memset(&packet, 0, sizeof(packet));  //for good measure


	while (!frameFinished && av_read_frame(ctx->ic, &packet) >= 0)
	{
		// Is this a packet from the video stream?
		if (packet.stream_index == ctx->videx_stream_idx)
		{
			// Decode video frame
			if (avcodec_decode_video2(ctx->pCodecCtx, ctx->pFrame, &frameFinished, &packet) <= 0)
			{
				av_free_packet(&packet);
				return cwbm_false;
			}

			if (frameFinished)
			{
				ctx->timestamp += 41666667;  //24 FPS (nanoseconds per frame)
			}

			/*
			if (frameFinished)
			{
				AVStream * vstream = ctx->ic->streams[ctx->videx_stream_idx];

				//double ticks_per_sec = vstream->time_base.num / (double)vstream->time_base.den;

				//printf("PTS: %d, %d/%d\n", packet.pts, vstream->time_base.num, vstream->time_base.den);
				//double pts_seconds = packet.pts * ticks_per_sec;
				//ctx->timestamp = (uint64_t)(pts_seconds * 1000000000.0);
				printf("PTS: %d (finished %d)\n", packet.pts, frameFinished);

				ctx->timestamp = (uint64_t)(packet.pts * 1000000);
				//ctx->timestamp *= 1
				//vstream->time_base->num

			}*/
		}

		// Free the packet that was allocated by av_read_frame
		av_free_packet(&packet);
	}

	return frameFinished ? cwbm_true : cwbm_false;
}

CWBM_EXPORT size_t cwbm_ctx_get_pixel_buffer_suggested_size(cwbm_ctx * ctx)
{
	return ctx->pixel_buf_size;
}

CWBM_EXPORT unsigned long cwbm_ctx_get_tstamp(cwbm_ctx * ctx)
{
	if (ctx)
		return ctx->timestamp;
	else
		return 0;
}

CWBM_EXPORT void cwbm_ctx_get_dimensions(cwbm_ctx * ctx, unsigned int * width_out, unsigned int * height_out)
{
	if (ctx)
	{
		*width_out = ctx->pCodecCtx->width;
		*height_out = ctx->pCodecCtx->height;
	}
	else
	{
		/*1 avoids divide by zero*/
		*width_out = 1;
		*height_out = 1;
	}
}

CWBM_EXPORT cwbm_bool cwbm_ctx_convert_pixels(cwbm_ctx * ctx, void * buffer, size_t buf_max_size)
{
	int w, h;
	enum PixelFormat dstFormat;
	uint8_t * dest_plane[1];
	int dest_stride;
	int bytes_per_px;
	const uint8_t * srcSlice[4];

	if (!ctx)
		return cwbm_false;

	if (buffer == NULL || buf_max_size < ctx->pixel_buf_size)
		return cwbm_false;

	w = ctx->pCodecCtx->width;
	h = ctx->pCodecCtx->height;


	if (ctx->pixel_format == CWBM_BGRA)
	{
		dstFormat = PIX_FMT_BGRA;
		bytes_per_px = 4;
	}
	else  //CWBM_RGB
	{
		dstFormat = PIX_FMT_RGB24;
		bytes_per_px = 3;
	}

	//Prepare to call ffmpeg's sws_scale() for YUV->RGB conversion
	ctx->yuv2rgb_ctx = sws_getCachedContext(ctx->yuv2rgb_ctx,
            w, h, PIX_FMT_YUV420P, w, h,
            dstFormat, SWS_FAST_BILINEAR, NULL, NULL, NULL);

	if (ctx->yuv2rgb_ctx == NULL)
	{
		fprintf(stderr, "sws_getCachedContext failed!\n");
		return cwbm_false;
	}

	//Call ffmpeg's sws_scale() for super fast YUV->RGB conversion
	dest_stride = w * bytes_per_px;
	dest_plane[0] = (uint8_t*)buffer;


	//this stupid copying of pointers is not necessary with g++, only gcc
	srcSlice[0] = ctx->pFrame->data[0];
	srcSlice[1] = ctx->pFrame->data[1];
	srcSlice[2] = ctx->pFrame->data[2];
	srcSlice[3] = ctx->pFrame->data[3];

	sws_scale(ctx->yuv2rgb_ctx, srcSlice, ctx->pFrame->linesize, 0, h, dest_plane, &dest_stride);

	return cwbm_true;
}

