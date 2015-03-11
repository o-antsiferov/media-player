#!/bin/bash
SYSROOT=$ANDROID_NDK/platforms/android-9/arch-arm/
TOOLCHAIN=$ANDROID_NDK/toolchains/arm-linux-androideabi-4.8/prebuilt/darwin-x86_64
function build_one
{
./configure \
\
--prefix=$PREFIX \
\
--enable-version3 \
--enable-nonfree \
\
--enable-shared \
--disable-static \
\
--disable-programs \
\
--disable-doc \
\
--disable-avdevice \
--disable-swresample \
--disable-avfilter \
\
--disable-everything \
--enable-decoder=mpeg4 \
--enable-decoder=mpeg4_vdpau \
--enable-decoder=h264 \
--enable-decoder=h264_vda \
--enable-decoder=h264_vdpau \
--enable-hwaccel=h264_vaapi \
--enable-hwaccel=h264_vda \
--enable-hwaccel=h264_vdpau \
--enable-demuxer=h264 \
--enable-demuxer=rtp \
--enable-parser=h264 \
--enable-parser=mpeg4video \
--enable-protocol=file \
\
--disable-symver \
--enable-memalign-hack \
--enable-asm \
--cross-prefix=$TOOLCHAIN/bin/arm-linux-androideabi- \
--target-os=linux \
--arch=arm \
--enable-cross-compile \
--sysroot=$SYSROOT \
--extra-cflags="-Os -fpic $ADDI_CFLAGS" \
--extra-ldflags="$ADDI_LDFLAGS" \
$ADDITIONAL_CONFIGURE_FLAG
make clean
make
make install
}
CPU=armv7
PREFIX=$(pwd)/android/$CPU
ADDI_CFLAGS="-mfloat-abi=softfp -mfpu=vfp -marm -march=armv7-a"

ADDI_LDFLAGS="-Wl,--fix-cortex-a8"

build_one
