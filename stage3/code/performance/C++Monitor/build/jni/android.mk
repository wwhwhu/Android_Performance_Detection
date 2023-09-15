LOCAL_PATH := $(call my-dir)

# 添加C++ stl 头文件支持
LOCAL_C_INCLUDES := $(NDK_ROOT)/sources/cxx-stl/stlport/stlport
LOCAL_C_INCLUDES += ./src/demo ./src/func
LOCAL_STATIC_LIBRARIES := $(NDK_ROOT)/sources/cxx-stl/stlport/libs/armeabi/libstlport_static.a

include $(CLEAR_VARS)
LOCAL_MODULE := Monitor_Demo  # exe name
CPP_LIST += $(wildcard $(LOCAL_PATH)/src/demo/*.cpp)
CPP_LIST += $(wildcard $(LOCAL_PATH)/src/func/*.cpp)
LOCAL_SRC_FILES := $(CPP_LIST:$(LOCAL_PATH)/%=%)

#cortex-a7, armv7
#cortex-a53, armv8a
# LOCAL_CFLAGS += -mfpu=neon -mfloat-abi=softfp -mcpu=cortex-a53 -O2 -fPIC -Wall
# LOCAL_CFLAGS += -pie -FPIE
# LOCAL_CFLAGS += -DNDK_TEST        # 程序宏定义
include $(BUILD_EXECUTABLE)       # 生成可执行EXE
#include $(BUILD_SHARED_LIBRARY)    # 动态库
#include $(BUILD_STATIC_LIBRARY)   # 生成静态库
