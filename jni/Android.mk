LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)
 
LOCAL_MODULE    := decoder
LOCAL_SRC_FILES := decoder.c
LOCAL_LDLIBS := -llog -ljnigraphics -lz -landroid
LOCAL_STATIC_LIBRARIES := libavformat-55 libavcodec-55 libswscale-2 libavutil-52
 
include $(BUILD_SHARED_LIBRARY)
$(call import-module,ffmpeg/android/armv7)