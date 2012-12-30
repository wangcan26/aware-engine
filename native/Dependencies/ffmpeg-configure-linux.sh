#!/bin/sh

INSTALL_DIR=/opt/AwareEngine

#configure to build only vp8 support and libswscale (and dependencies)
./configure \
--prefix=$INSTALL_DIR \
--disable-static --enable-shared \
--enable-runtime-cpudetect \
--disable-everything \
--disable-doc \
--disable-ffmpeg \
--disable-ffplay \
--disable-ffprobe \
--disable-ffserver \
--disable-avdevice \
--enable-avcodec \
--enable-avformat \
--disable-swresample \
--disable-postproc \
--disable-avfilter \
--enable-swscale \
--enable-protocol=file \
--enable-protocol=cache \
--enable-parser=vp8 \
--enable-decoder=vp8 \
--enable-demuxer=matroska

