LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := loki-wrapper
LOCAL_SRC_FILES := loki_wrapper.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../../../sub_projects/Loki
LOCAL_STATIC_LIBRARIES := libloki_static
include $(BUILD_SHARED_LIBRARY)

include $(LOCAL_PATH)/../../../../sub_projects/Loki/Android.mk
