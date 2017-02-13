LOCAL_PATH := $(call my-dir)

# LOKI usable only for ARMv7-based phones
ifeq ($(TARGET_ARCH_ABI), armeabi-v7a)
    LOKI_PATH               := $(abspath $(LOCAL_PATH)/../../../../sub_projects/Loki)

    include $(CLEAR_VARS)
    LOCAL_MODULE            := loki_wrapper
    LOCAL_SRC_FILES         := loki_wrapper.c
    LOCAL_STATIC_LIBRARIES  := libloki_static
    LOCAL_C_INCLUDES        += $(LOKI_PATH)
    include $(BUILD_SHARED_LIBRARY)

    include $(LOKI_PATH)/Android.mk
endif
