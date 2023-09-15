import os
import re
import threading
from time import sleep


class powerThread(threading.Thread):
    def __init__(self):
        threading.Thread.__init__(self, name='power', daemon=True)
        # 记录当前的采样次数和平均功耗
        self.count = 0
        self.power = 0

    def run(self):
        while 1:
            # 读取CPU耗电 dumpsys数据更新太慢 不用这个了
            # with os.popen("adb shell dumpsys batterystats | grep cpu:") as r:
            #     line = r.read()
            #     cpucost = re.search("cpu: (\\d+.\\d+)", line).group(1)

            # 直接用电压电流算 但是充电时读不准
            with os.popen('adb shell echo \"$(cat /sys/class/power_supply/battery/current_now)current\"',"r") as r:
                text = r.read()
                c = re.search("(-?\\d+)current", text).group(1)

            with os.popen('adb shell echo \"$(cat /sys/class/power_supply/battery/voltage_now)voltage\"',"r") as r:
                text = r.read()
                v = re.search("(\\d+)voltage", text).group(1)
            p = int(c) * int(v)

            # with os.popen("adb shell date '+%s%N'", "r") as subp:
            #     text = subp.read()
            # timestamp = round(int(text) / 1000000, 0)
            # print('ptime',timestamp)
            # print('current', c)
            # print('volt', v)

            # 取平均
            self.count += 1
            self.power = (self.power + p)/self.count

    def getPower(self):
        p = self.power
        print("power",p)
        # 清空近期功耗记录
        self.count = 0
        self.power = 0
        return p



class fpsThread(threading.Thread):
    def __init__(self, threadname, packageName, focusWindow, wflag=True):
        threading.Thread.__init__(self, name=threadname,daemon=True)
        self.threadname = threadname
        self.packageName = packageName
        self.focusWindow = focusWindow
        self.lastTimestamp = 0
        self.FPS = 0
        self.jankRate = 0
        self.wflag = wflag
        # 清空记录
        os.popen('adb shell dumpsys SurfaceFlinger --latency-clear ', "r")

    def getFps(self):
        return self.FPS, self.jankRate, self.lastTimestamp

    def run(self):
        while 1:
            with os.popen('adb shell dumpsys SurfaceFlinger --latency ' + self.focusWindow, "r") as subp:
                text = subp.read()
                text = text.split('\n')
                if not len(text):
                    raise RuntimeError("Frame Data is Empty.")
                    return -1
                timestamps = []
                ns_per_ms = 1e6
                # 数据第一行：屏幕刷新时长
                refresh_period = int(text[0]) / ns_per_ms
                # 防止数据溢出
                pending_fence_timestamp = (1 << 63) - 1
                # 处理每一行的数据
                for line in text[1:]:
                    fields = line.split()
                    # 忽略残缺数据
                    if len(fields) != 3:
                        continue
                    # 忽略空帧
                    if int(fields[0]) == 0:
                        continue
                    # 记录每一帧绘制的时间点
                    timestamp = [int(fields[0]), int(fields[1]), int(fields[2])]
                    # 忽略数据溢出的记录
                    if timestamp[0] == pending_fence_timestamp or timestamp[1] == pending_fence_timestamp or timestamp[2] == pending_fence_timestamp:
                        continue
                    timestamps.append(timestamp)

            # 计算每一帧的绘制时长
            duration = []
            for timestamp in timestamps:
                # print(timestamp)# 与上次记录不重合
                if timestamp[0] > self.lastTimestamp:
                    duration.append((timestamp[2] - timestamp[0]) / ns_per_ms)
                    self.lastTimestamp = timestamp[0]
            # (duration)
            if len(duration) < 3:
                print("no update")
                continue
            # 计算jank率
            jankFrame = 0
            for t in duration:
                if t > refresh_period:
                    jankFrame += 1
            jankRate = jankFrame / len(timestamps)
            self.jankRate = jankRate

            # 计算fps
            startTime = timestamps[0][2]
            self.FPS = ((len(timestamps)-1)*1000000000) / (self.lastTimestamp - startTime )
            if self.wflag:
                print("FPS",self.FPS)

            # os.popen('adb shell dumpsys SurfaceFlinger --latency-clear ', "r")

            sleep(2)













