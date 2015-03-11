#include <android/bitmap.h>

#include <unistd.h>
#include <pthread.h>
#include <semaphore.h>

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <libavutil/avutil.h>
#include <libavutil/mathematics.h>

#define LOG_LEVEL 0
/*for android logs*/
#include <android/log.h>
#define LOG_TAG "libdecoder"
#define LOGI(level, ...) if (level <= LOG_LEVEL) {__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);}
#define LOGE(level, ...) if (level <= LOG_LEVEL) {__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);}

#define WRAP_PACKAGE(a) Java_bo_pic_android_media_content_animation_AnimationDecoder_##a
#define WRAP_PACKAGE_STRING(a) "Java_bo_pic_android_media_content_animation_AnimationDecoder_"#a
#define PROFILING_ON (LOG_LEVEL > 5)

typedef enum {
    ERROR_OPEN_FILE = -1000,
    ERROR_FIND_VIDEOSTREAM,
    ERROR_FIND_VIDEODECODER,
    ERROR_OPEN_VIDEODECODER,
    ERROR_NO_STREAM_INFO,
    ERROR_BAD_BITMAP,
    ERROR_WRONG_BITMAP_FORMAT,
    ERROR_LOCK_BITMAP_PIXELS,
    ERROR_BITMAP_FILL,
    ERROR_NOT_ENOUGH_MEMORY,
} DecoderError;

typedef struct thandle {
    AVFormatContext* formatContext;
    AVCodecContext* codecContext;
    AVStream* videoStream;
    int videoStreamIndex;
    AVFrame* frameYUV;
    AVFrame* frameRGB;
} DecoderHandle;

void make_exception(JNIEnv *env, DecoderError errorCode)
{
    char* buffer = 0;
    switch (errorCode) {
      case ERROR_OPEN_FILE:
        buffer = "DECODER_ERROR_OPEN_FILE";
        break;
      case ERROR_FIND_VIDEOSTREAM:
        buffer = "DECODER_ERROR_FIND_VIDEOSTREAM";
        break;
      case ERROR_OPEN_VIDEODECODER:
        buffer = "DECODER_ERROR_OPEN_VIDEODECODER";
        break;
      case ERROR_NO_STREAM_INFO:
        buffer = "ERROR_NO_STREAM_INFO";
        break;
      case ERROR_BAD_BITMAP:
        buffer = "DECODER_ERROR_BAD_BITMAP";
        break;
    case ERROR_WRONG_BITMAP_FORMAT:
        buffer = "DECODER_ERROR_WRONG_BITMAP_FORMAT";
        break;
    case ERROR_LOCK_BITMAP_PIXELS:
        buffer = "DECODER_ERROR_LOCK_BITMAP_PIXELS";
        break;
      case ERROR_BITMAP_FILL:
        buffer = "DECODER_ERROR_BITMAP_FILL";
        break;
      case ERROR_NOT_ENOUGH_MEMORY:
        buffer = "DECODER_ERROR_NOT_ENOUGH_MEMORY";
        break;
      default:
        buffer = "DECODER_ERROR";
    }
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), buffer);
}

AVFormatContext* initContext(JNIEnv* env, jstring javaFilename)
{
    LOGI(7, "initContext");
    AVFormatContext* formatContext = 0;
    char* filename = (char *)(*env)->GetStringUTFChars(env, javaFilename, 0);
    int result;
    if ((result = avformat_open_input(&formatContext, filename, 0, 0)) != 0) {
        LOGI(7, "initContext, result opening file %d, file %s", result, filename);
        make_exception(env, ERROR_OPEN_FILE);
        return 0;
    }
    (*env)->ReleaseStringUTFChars(env, javaFilename, filename);
    return formatContext;
}

int findVideoStreamIndex(AVFormatContext* formatContext)
{
    LOGI(7, "findVideoStreamIndex");
    int i = 0;
    for (; i < formatContext->nb_streams; ++i) {
        if (formatContext->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            return i;
        }
    }
    return -1;
}

AVCodecContext* initCodecContext(JNIEnv* env, AVStream* videoStream)
{
    LOGI(7, "initCodecContext");
    AVCodecContext* codecContext = videoStream->codec;
    AVCodec* codec = avcodec_find_decoder(codecContext->codec_id);
    if (codec == 0) {
        make_exception(env, ERROR_FIND_VIDEODECODER);
        return 0;
    }
    if (avcodec_open2(codecContext, codec, 0) != 0) {
        make_exception(env, ERROR_OPEN_VIDEODECODER);
        return 0;
    }
    return codecContext;
}

JNIEXPORT jlong JNICALL WRAP_PACKAGE(nativeInit)(JNIEnv* env, jobject self, jstring pFilename)
{
    LOGI(6, WRAP_PACKAGE_STRING(nativeInit));

    AVFormatContext* formatContext = initContext(env, pFilename);
    if (formatContext == 0) {
        return 0;
    }

    if (avformat_find_stream_info(formatContext, 0) < 0) {
        make_exception(env, ERROR_NO_STREAM_INFO);
        return 0;
    }

    int videoStreamIndex = findVideoStreamIndex(formatContext);
    if (videoStreamIndex == -1) {
        make_exception(env, ERROR_FIND_VIDEOSTREAM);
        return 0;
    }
    AVStream* videoStream = formatContext->streams[videoStreamIndex];
    LOGI(10, "number of frames: %lld", videoStream->nb_frames);

    AVCodecContext* codecContext = initCodecContext(env, videoStream);
    if (codecContext == 0) {
        return 0;
    }

    LOGI(8, "codecContext w x h = %d x %d", codecContext->width, codecContext->height);

    DecoderHandle* h = malloc(sizeof(DecoderHandle));
    h->formatContext = formatContext;
    h->videoStream = videoStream;
    h->videoStreamIndex = videoStreamIndex;
    h->codecContext = codecContext;
    h->frameYUV = av_frame_alloc();
    h->frameRGB = av_frame_alloc();

    LOGI(10, "CODEC_CAP = %d", codecContext->codec->capabilities);

    return (jlong)(intptr_t)h;
}

JNIEXPORT void JNICALL WRAP_PACKAGE(nativeRelease)(JNIEnv *pEnv, jobject self, jlong handle)
{
    LOGI(6, WRAP_PACKAGE_STRING(nativeRelease));
    DecoderHandle* h = (DecoderHandle*)(intptr_t)handle;
    LOGI(10, "will close codec");
    avcodec_close(h->codecContext);
    LOGI(10, "will close context");
    avformat_close_input(&h->formatContext);
    LOGI(10, "will free frameYUV");
    av_frame_free(&h->frameYUV);
    LOGI(10, "will free frameRGB");
    av_frame_free(&h->frameRGB);
    LOGI(10, "will free handle");
    free(h);
}

void* lockAndroidBitmapPixels(JNIEnv *env, jobject bitmap)
{
    LOGI(7, "lockAndroidBitmapPixels");
    AndroidBitmapInfo linfo;
    int lret;
    if ((lret = AndroidBitmap_getInfo(env, bitmap, &linfo)) < 0) {
        //make_exception(env, ERROR_BAD_BITMAP);
        LOGE(0, "AndroidBitmap_getInfo failed, error=%d", lret);
        return 0;
    }
    if (linfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        //make_exception(env, ERROR_WRONG_BITMAP_FORMAT);
        LOGE(0, "Wrong bitmap format %d", linfo.format);
        return 0;
    }
    void* bitmapPixels = 0;
    if ((lret = AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels)) < 0) {
        //make_exception(env, ERROR_LOCK_BITMAP_PIXELS);
        LOGE(0, "AndroidBitmap_lockPixels failed, error=%d", lret);
        return 0;
    }
    return bitmapPixels;
}

JNIEXPORT void JNICALL WRAP_PACKAGE(nativeReset)(JNIEnv *env, jobject self, jlong handle)
{
    LOGI(6, WRAP_PACKAGE_STRING(nativeReset));
    DecoderHandle* h = (DecoderHandle*)(intptr_t)handle;
    AVFormatContext* ctx = h->formatContext;
    AVCodecContext* codecContext = h->codecContext;
    int videoStreamIndex = h->videoStreamIndex;

    int res = avformat_seek_file(ctx, videoStreamIndex, INT64_MIN, 0, INT64_MAX, 0);
    if (res >= 0) {
        LOGI(7, "refcounted frames %d", codecContext->refcounted_frames);
        avcodec_flush_buffers(codecContext);
    }
}

jlong decodeFrame(JNIEnv* env, DecoderHandle* h, AVPacket* packet, jobject bitmap, int* stop
#if PROFILING_ON
, clock_t start
#endif
)
{
    LOGI(10, "decodeFrame");
    LOGI(8, "packet: pts %lld, dts %lld", packet->pts, packet->dts);
    AVCodecContext* codecContext = h->codecContext;
    AVStream* videoStream =  h->videoStream;
    AVFrame* frameYUV = h->frameYUV;

    int gotPicture = 0;
    int bytesDecompressed = avcodec_decode_video2(codecContext, frameYUV, &gotPicture, packet);

    LOGI(10, "bytes decompressed %d", bytesDecompressed);

    if (gotPicture == 0) {
        *stop = 0;
        av_frame_unref(frameYUV);
        return -1;
    }

    LOGI(8, "frame: is key = %d, coded num %d, display num %d", frameYUV->key_frame, frameYUV->coded_picture_number, frameYUV->display_picture_number);

    jclass bitmapClass = (*env)->FindClass(env, "android/graphics/Bitmap");
    jmethodID getWidthMethodID = (*env)->GetMethodID(env, bitmapClass, "getWidth", "()I");
    jmethodID getHeightMethodID = (*env)->GetMethodID(env, bitmapClass, "getHeight", "()I");

    int width = (*env)->CallIntMethod(env, bitmap, getWidthMethodID);
    int height = (*env)->CallIntMethod(env, bitmap, getHeightMethodID);

    LOGI(8, "bitmap w x h = %d x %d", width, height);

    // YUV -> RGB24
    struct SwsContext* scaleCtx = sws_getContext(
        frameYUV->width,
        frameYUV->height,
        (enum PixelFormat)frameYUV->format,
        width,
        height,
        PIX_FMT_RGBA,
        SWS_BICUBIC,
        0, 0, 0);

    AVFrame* frameRGB = h->frameRGB;
    void* bitmapPixels = lockAndroidBitmapPixels(env, bitmap);
    if (bitmapPixels == 0) {
        *stop = 1;
        av_frame_unref(frameYUV);
        return -1;
    }
    if (avpicture_fill((AVPicture*)frameRGB, (uint8_t*)bitmapPixels, PIX_FMT_RGBA, width, height) < 0) {
        make_exception(env, ERROR_BITMAP_FILL);
        *stop = 1;
        av_frame_unref(frameYUV);
        return -1;
    }
    sws_scale(
        scaleCtx,
        (uint8_t const* const*)frameYUV->data,
        frameYUV->linesize,
        0,
        frameYUV->height,
        (uint8_t* const*)frameRGB->data,
        frameRGB->linesize);
    AndroidBitmap_unlockPixels(env, bitmap);
    sws_freeContext(scaleCtx);

#if PROFILING_ON
    clock_t end = clock() ;
    double elapsed_time = (end-start)/(double)CLOCKS_PER_SEC;
    LOGI(10, "decode total time in ms: %d", (int)(elapsed_time * 1000));
#endif

    LOGI(8, "frame: pts %lld, pkt_pts %lld, pkt_dts %lld", frameYUV->pts, frameYUV->pkt_pts, frameYUV->pkt_dts);
    jlong timestamp = 0;
    if (frameYUV->pkt_pts != AV_NOPTS_VALUE) {
        timestamp = videoStream->time_base.num * frameYUV->pkt_pts * 1000 / videoStream->time_base.den;
    }
    *stop = 1;
    av_frame_unref(frameYUV);
    return timestamp;
}

JNIEXPORT jlong JNICALL WRAP_PACKAGE(nativeGetNextFrame)(JNIEnv *env, jobject self, jlong handle, jobject bitmap)
{
#if PROFILING_ON
    clock_t start = clock();
#endif
    LOGI(6, WRAP_PACKAGE_STRING(nativeGetNextFrame));
    DecoderHandle* h = (DecoderHandle*)(intptr_t)handle;
    AVFormatContext* ctx = h->formatContext;
    AVCodecContext* codecContext = h->codecContext;
    int videoStreamIndex = h->videoStreamIndex;

    AVPacket packet;
    av_init_packet(&packet);

    while (av_read_frame(ctx, &packet) == 0) {
        if (packet.stream_index != videoStreamIndex) {
            av_free_packet(&packet);
            continue;
        }
        int stop;
        jlong timestamp = decodeFrame(env, h, &packet, bitmap, &stop
#if PROFILING_ON
, start
#endif
);
        av_free_packet(&packet);
        if (stop == 1) {
            LOGI(10, "timestamp %lld", timestamp);
            return timestamp;
        }
    }
    av_free_packet(&packet);
    av_init_packet(&packet);
    if (codecContext->codec->capabilities & CODEC_CAP_DELAY) {
        LOGI(10, "CODEC_CAP_DELAY, data %p, size %d", packet.data, packet.size);
        int stop;
        jlong timestamp = decodeFrame(env, h, &packet, bitmap, &stop
#if PROFILING_ON
, start
#endif
);
        av_free_packet(&packet);
        LOGI(10, "timestamp %lld", timestamp);
        return timestamp;
    }
    av_free_packet(&packet);
    LOGI(10, "timestamp -1");
    return -1;
}

JNIEXPORT jintArray JNICALL WRAP_PACKAGE(nativeGetDimensions)(JNIEnv *env, jobject self, jlong handle)
{
    LOGI(6, WRAP_PACKAGE_STRING(nativeGetDimensions));
    jintArray result;
    result = (*env)->NewIntArray(env, 2);
    if (result == 0) {
        return 0;
    }
    DecoderHandle* h = (DecoderHandle*)(intptr_t)handle;
    AVCodecContext* codecContext = h->codecContext;
    jint tmp[] = {codecContext->width, codecContext->height};
    (*env)->SetIntArrayRegion(env, result, 0, 2, tmp);
    return result;
}

jint JNI_OnLoad(JavaVM* pVm, void* reserved) {
	JNIEnv* env;
	if ((*pVm)->GetEnv(pVm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
		 return -1;
	}
	JNINativeMethod nm[10];
	nm[0].name = "nativeInit";
	nm[0].signature = "(Ljava/lang/String;)J";
	nm[0].fnPtr = WRAP_PACKAGE(nativeInit);

	nm[1].name = "nativeRelease";
	nm[1].signature = "(J)V";
	nm[1].fnPtr = WRAP_PACKAGE(nativeRelease);

	nm[2].name = "nativeGetNextFrame";
    nm[2].signature = "(JLandroid/graphics/Bitmap;)J";
	nm[2].fnPtr = WRAP_PACKAGE(nativeGetNextFrame);

	nm[3].name = "nativeGetDimensions";
	nm[3].signature = "(J)[I";
	nm[3].fnPtr = WRAP_PACKAGE(nativeGetDimensions);

    nm[4].name = "nativeReset";
    nm[4].signature = "(J)V";
    nm[4].fnPtr = WRAP_PACKAGE(nativeReset);

	jclass cls = (*env)->FindClass(env, "bo/pic/android/media/content/animation/AnimationDecoder");
	(*env)->RegisterNatives(env, cls, nm, 5);

	av_register_all();

	return JNI_VERSION_1_6;
}
