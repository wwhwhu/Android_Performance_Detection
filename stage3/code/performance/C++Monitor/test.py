import os
os.system("cd C:/Users/DELL/Desktop/monitorTool/build && ndk-build")
os.system("adb push C:/Users/DELL/Desktop/monitorTool/build/libs/arm64-v8a/Monitor_Demo /data/local/tmp/monitor")
os.system("adb shell chmod 777 data/local/tmp/monitor/Monitor_Demo")
os.system("adb shell ./data/local/tmp/monitor/Monitor_Demo")