LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE    := AnomalySimCPU
LOCAL_SRC_FILES := hello_CPU.c
include $(BUILD_EXECUTABLE)