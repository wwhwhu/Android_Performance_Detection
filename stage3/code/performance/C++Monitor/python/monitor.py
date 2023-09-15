import re
import os
from monitorTool.utilThread import fpsThread, powerThread


class Monitor:
    FrontAPPName = ''
    timestamp = 0
    lastTimestamp = 0
    nativeHeapFreeSize = 0
    usedNativeMemPercent = '0.000%'
    FrontAPPJavaHeap = 0
    FrontAPPNativeHeap = 0
    FrontAPPCodeMem = 0
    FrontAPPStackMem = 0
    FrontAPPGraphicsMem = 0
    FrontAPPrivateOtherMem = 0
    FrontAPPSystemMem = 0
    gpuUsage = 0.0
    gpuFreq = 0
    cpuUser = 0
    cpuNice = 0
    cpuSystem = 0
    cpuIdle = 0
    cpuIowait = 0
    cpuIrq = 0
    cpuSortirq = 0
    cpuSteal = 0
    cpuGuest = 0
    cpuGnice = 0
    cpuUsage = 0
    cpuLast = 0
    cpuLastIdle = 0
    FrontApptime = 0
    FrontAppCpuUsage = 0.0
    lastFrontApptime = 0
    MemFree = 0
    MemAvailable = 0
    SwapFree = 0
    # Low Memory 判断标准
    threshold = 226492416 / 1024
    tempCPU0 = ''
    tempCPU1 = ''
    tempCPU2 = ''
    tempCPU3 = ''
    tempCPU4 = ''
    tempCPU5 = ''
    tempCPU6 = ''
    tempCPU7 = ''
    batteryTemp = ''
    thermalStatus = '0'
    LowMemory = 'false'
    missedGPU = 0
    missedHWC = 0
    missedTotal = 0
    missedGPUCounts = 0
    missedHWCCounts = 0
    missedTotalMissed = 0
    MissedVsync = 0
    TotalFrames = 0
    JankyFrames = 0
    lastTotalFrames = 0
    lastMissedVsync = 0
    lastJankyFrames = 0
    wifiSpeed = 0
    wifiRssi = 0
    FPS_Mon = 0
    packageName = ''
    pid = ''
    resultStr = ''
    appName = {'com.miHoYo.Yuanshen': 'genshin', 'com.taobao.taobao': 'taobao', 'com.xingin.xhs': 'xhs',
               'com.tencent.mm': 'wechat', 'com.ss.android.ugc.aweme': 'tiktok', 'tv.danmaku.bili': 'bilibili',
               'com.zhihu.android': 'zhihu', 'com.android.gallery3d': 'gallery'}
    appView = {
        'com.miHoYo.Yuanshen': 'SurfaceView[com.miHoYo.Yuanshen/com.miHoYo.GetMobileInfo.MainActivity]\(BLAST\)#0',
        'com.taobao.taobao': 'com.taobao.taobao/com.taobao.tao.TBMainActivity#0',
        'com.xingin.xhs': 'com.xingin.xhs/com.xingin.xhs.index.v2.IndexActivityV2#0',
        'com.tencent.mm': 'com.taobao.taobao/com.taobao.tao.TBMainActivity#0',
        'com.ss.android.ugc.aweme': 'com.ss.android.ugc.aweme/com.ss.android.ugc.aweme.splash.SplashActivity#0',
        'tv.danmaku.bili': 'SurfaceView[tv.danmaku.bili/com.bilibili.video.videodetail.VideoDetailsActivity]\(BLAST\)#0',
        'com.zhihu.android': 'com.zhihu.android/com.zhihu.android.app.ui.activity.MainActivity#0',
        'com.android.gallery3d': 'SurfaceView[com.android.gallery3d/com.android.gallery3d.app.GalleryActivity]\(BLAST\)#0'}
    dir_csv = ''
    # 是否写入文件
    wflag = True
    # 是否动态切换
    dflag = False
    # 是否打印监控信息
    pflag = False

    def __init__(self, dflag=False, wflag=True, pflag=False):
        self.dflag = dflag
        self.wflag = wflag
        self.pflag = pflag
        
        if self.dflag:
            # 创建文件保存监控数据
            self.missedGPUCounts, self.missedHWCCounts, self.missedTotalMissed, self.cpuLast, self.cpuLastIdle = self.initCount()
            self.packageName, self.pid, self.fucosWindow, self.lastFrontApptime, self.lastTotalFrames, self.lastMissedVsync, \
            self.lastJankyFrames = self.getFrontApp()
            self.dir_csv = self.makeDir('mix')
        else:
            # 前台应用不变情况：初始化统计值,统计前台APP相关信息，包括包名，图层名，pid等
            self.missedGPUCounts, self.missedHWCCounts, self.missedTotalMissed, self.cpuLast, self.cpuLastIdle = self.initCount()
            self.packageName, self.pid, focusWindow, self.lastFrontApptime, self.lastTotalFrames, self.lastMissedVsync, \
            self.lastJankyFrames = self.getFrontApp()
            print("focusWindow:" + focusWindow)
            app = self.appName[self.packageName]
            self.focusWindow = self.appView[self.packageName]
            # 如果要写入csv文件 创建文件保存监控数据
            if self.wflag:
                self.dir_csv = self.makeDir(app)
        # 开启线程计算FPS
        self.ft = fpsThread("fps", self.packageName, self.focusWindow, self.wflag)
        self.ft.start()
        # 开启线程显示功耗
        self.pt = powerThread()
        self.pt.start()

    @staticmethod
    def makeDir(app):
        dir_csv = r"C:\Users\roots\Desktop\paper\code\data\frontappworkload\\" + app + ".csv"
        # dir_csv = r"C:\Users\roots\Desktop\paper\code\data\background\\" + app + "cpu.csv"
        # dir_csv = r"C:\Users\roots\Desktop\paper\code\data\cuprumTurbo.csv"
        print("record file at:" + dir_csv)
        if not os.path.exists(dir_csv):
            with open(dir_csv, mode='a', encoding='utf-8') as create_csv:
                create_csv.write(
                    'timestamp,frontAppPackageName,cpuUsage,frontAppCpuUsage,frontAppCpuCore,frontAppCpuFreq,'
                    'cpu0Freq,cpu1Freq,cpu2Freq,cpu3Freq,cpu4Freq,cpu5Freq,cpu6Freq,cpu7Freq,'
                    'MemFree,MemAvailable,SwapFree,threshold,LowMemory,'
                    'nativeHeapFreeSize,usedNativeMemPercent,'
                    'frontAppJavaHeap,frontAppNativeHeap,frontAppCodeMem,frontAppStackMem,frontAppGraphicsMem,frontAppPrivateOtherMem,frontAppSystemMem,'
                    'cpuSome10,memSome10,ioSome10,'
                    'gpuUsage,gpuFreq,gpuMissed,hwcMissed,totalMissed,totalFrames,missedVsync,jankyNum,jankyRate,fps,jankRate2,'
                    'tempCPU0,tempCPU1,tempCPU2,tempCPU3,tempCPU4,tempCPU5,tempCPU6,tempCPU7,batteryTemp,thermalStatus,'
                    'wifiRssi,wifiSpeed\n')
        return dir_csv

    @staticmethod
    def initCount():
        with os.popen('adb shell dumpsys SurfaceFlinger| findstr missed', "r") as subp:
            text = subp.read()
        missed1 = re.search("GPU missed frame count: (\\d+)", text).group(1)
        missed2 = re.search("Total missed frame count: (\\d+)", text).group(1)
        missed3 = re.search("HWC missed frame count: (\\d+)", text).group(1)
        with os.popen('adb shell cat /proc/stat', "r") as subp:
            text = subp.read()

        res = re.search(
            "cpu\\s+(\\d+)\\s+(\\d+)+\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)",
            text)
        cpuUser = int(res[1])
        cpuNice = int(res[2])
        cpuSystem = int(res[3])
        cpuIdle = int(res[4])
        cpuIowait = int(res[5])
        cpuIrq = int(res[6])
        cpuSortirq = int(res[7])
        cpuSteal = int(res[8])
        cpuGuest = int(res[9])
        cpuGnice = int(res[10])
        cpulast = cpuUser + cpuNice + cpuSystem + cpuIdle + cpuIowait + cpuIrq + cpuSortirq + cpuSteal + cpuGuest + cpuGnice
        return int(missed1), int(missed3), int(missed2), cpulast, cpuIdle

    @staticmethod
    def getFrontApp():
        with os.popen('adb shell dumpsys window| findstr mCurrentFocus', "r") as subp:
            text = subp.read()
        name = re.search("mCurrentFocus=Window{([a-z]|\\d)+ u0 (com|tv)([a-zA-Z]|\\.|\\d)+/com([a-zA-Z]|\\.|\\d)+}",
                         text).group()
        packageName = name.strip().split(" ")[2].split("/")[0]
        focus_window = name.strip().split(" ")[2]
        # 前台应用包名
        if len(packageName) > 1 and packageName[-1] == '}':
            packageName = packageName[:-1]
        if packageName != '' and len(packageName.strip()) > 0:
            packageName = packageName
        print("FrontAPPpackageName:", packageName)
        # 前台应用pid
        with os.popen('adb shell echo \"$(pidof ' + packageName + ')FrontPid\"', "r") as subp:
            text = subp.read()
        pid = re.search("(\\d+)FrontPid", text).group(1)
        print("FrontAPPPid:", pid)
        if len(focus_window) > 1 and focus_window[-1] == '}':
            focus_window = focus_window[:-1]

        # dumpsys SurfaceFlinger | grep -i focus -A 20
        with os.popen('adb shell "dumpsys SurfaceFlinger | grep -i focus -A 20"', "r") as subp:
            text = subp.read()
            text = text.split("\n")
            print(text[2])
            focus_window = text[2]

        # 获取CPU信息
        with os.popen('adb shell cat proc/' + pid + '/stat', "r") as subp2:
            text2 = subp2.read()
        FrontAPPCpu = re.search(
            pid + ' \\(([a-zA-Z]|\\.|\\d)*\\) [a-zA-Z] \\d+ \\d+ \\d+ \\d+ -\\d+ \\d+ \\d+ \\d+ \\d+ \\d+ (\\d+) (\\d+)',
            text2)
        FrontApputime = int(FrontAPPCpu.group(2))
        FrontAppstime = int(FrontAPPCpu.group(3))
        lastFrontApptime = FrontAppstime + FrontApputime

        # 获取前台掉帧信息
        with os.popen('adb shell dumpsys gfxinfo ' + packageName + '|findstr \"frames Vsync\"',
                      "r") as subp:
            text = subp.read()
        TotalFrames = int(re.search("Total frames rendered: (\\d+)", text).group(1))
        MissedVsync = int(re.search("Number Missed Vsync: (\\d+)", text).group(1))
        JankyFrames = int(re.search("Janky frames: (\\d+) [(]((100|([1-9]?\\d)(\\.\\d+)?)%)[)]", text).group(1))
        # JankyFrames = int(re.search("Janky frames: (\\d+) [(](.*)[)]", text).group(1))
        # print(TotalFrames)
        # print(MissedVsync)
        # print(JankyFrames)
        return packageName, pid, focus_window, lastFrontApptime, TotalFrames, MissedVsync, JankyFrames

    def runMonitor(self):
        # 查看系统时间
        self.lastTimestamp = self.timestamp
        with os.popen("adb shell date '+%s%N'", "r") as subp:
            text = subp.read()
        self.timestamp = round(int(text) / 1000000, 0)
        if self.pflag:
            print('timestamp:', self.timestamp)

        # 获取全局CPU使用时间
        with os.popen('adb shell cat /proc/stat', "r") as subp:
            text = subp.read()
        res = re.search(
            "cpu\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)",
            text)
        cpuUser = int(res[1])
        cpuNice = int(res[2])
        cpuSystem = int(res[3])
        cpuIdle = int(res[4])
        cpuIowait = int(res[5])
        cpuIrq = int(res[6])
        cpuSortirq = int(res[7])
        cpuSteal = int(res[8])
        cpuGuest = int(res[9])
        cpuGnice = int(res[10])
        cpuNow = cpuUser + cpuNice + cpuSystem + cpuIdle + cpuIowait + cpuIrq + cpuSortirq + cpuSteal + cpuGuest + cpuGnice
        cpuUsage = 1 - (cpuIdle - self.cpuLastIdle) / (cpuNow - self.cpuLast)
        self.cpuLastIdle = cpuIdle
        # print('cpuUser:', cpuUser)
        # print('cpuNice:', cpuNice)
        # print('cpuSystem:', cpuSystem)
        # print('cpuIdle:', cpuIdle)
        # print('cpuIowait:', cpuIowait)
        # print('cpuIrq:', cpuIrq)
        # print('cpuSortirq:', cpuSortirq)
        # print('cpuSteal:', cpuSteal)
        # print('cpuGuest:', cpuGuest)
        # print('cpuGnice:', cpuGnice)
        # print('cpuUsage:', cpuUsage)

        # 获取前台应用的CPU使用时间（user,system）
        with os.popen('adb shell cat proc/' + self.pid + '/stat', "r") as subp2:
            text2 = subp2.read()
        FrontAPPCpu = re.search(
            self.pid + ' \\(([a-zA-Z]|\\.|\\d)*\\) [a-zA-Z] \\d+ \\d+ \\d+ \\d+ -\\d+ \\d+ \\d+ \\d+ \\d+ \\d+ (\\d+) (\\d+)',
            text2)
        FrontApputime = int(FrontAPPCpu.group(2))
        FrontAppstime = int(FrontAPPCpu.group(3))
        FrontApptime = FrontAppstime + FrontApputime
        FrontAppCpuUsage = (FrontApptime - self.lastFrontApptime) / (cpuNow - self.cpuLast)
        self.lastFrontApptime = FrontApptime  # 更新前台应用CPU时间
        self.cpuLast = cpuNow  # 更新总CPU时间
        # print("FrontApputime:", FrontApputime)
        # print("FrontAppstime:", FrontAppstime)
        # print('FrontAppCpuUsage:', FrontAppCpuUsage)

        # 查看前台应用运行在哪个CPU核上
        with os.popen('adb shell top -o PID,CPU -p' + self.pid + ' -n 1', "r") as subp:
            text = subp.read()
        frontAppCpuCore = re.search(self.pid + '\\s+\\d+', text)
        frontAppCpuCore = frontAppCpuCore.group(0).split()
        frontAppCpuCore = int(frontAppCpuCore[1])
        # print("FrontAppCpuCore:"+str(frontAppCpuCore))

        # 获取CPU0频率
        cpuFreq = [0, 0, 0, 0, 0, 0, 0, 0]
        with os.popen('adb shell echo \"$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq)Cpu0Freq\"',
                      "r") as subp:
            text = subp.read()
        cpuFreq[0] = re.search("(\\d+)Cpu0Freq", text).group(1)
        # 获取CPU1频率
        with os.popen('adb shell echo \"$(cat /sys/devices/system/cpu/cpu1/cpufreq/scaling_cur_freq)Cpu1Freq\"',
                      "r") as subp:
            text = subp.read()
        cpuFreq[1] = re.search("(\\d+)Cpu1Freq", text).group(1)
        # 获取CPU2频率
        with os.popen('adb shell echo \"$(cat /sys/devices/system/cpu/cpu2/cpufreq/scaling_cur_freq)Cpu2Freq\"',
                      "r") as subp:
            text = subp.read()
        cpuFreq[2] = re.search("(\\d+)Cpu2Freq", text).group(1)
        # 获取CPU3频率
        with os.popen('adb shell echo \"$(cat /sys/devices/system/cpu/cpu3/cpufreq/scaling_cur_freq)Cpu3Freq\"',
                      "r") as subp:
            text = subp.read()
        cpuFreq[3] = re.search("(\\d+)Cpu3Freq", text).group(1)
        # 获取CPU4频率
        with os.popen('adb shell echo \"$(cat /sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq)Cpu4Freq\"',
                      "r") as subp:
            text = subp.read()
        cpuFreq[4] = re.search("(\\d+)Cpu4Freq", text).group(1)
        # 获取CPU5频率
        with os.popen('adb shell echo \"$(cat /sys/devices/system/cpu/cpu5/cpufreq/scaling_cur_freq)Cpu5Freq\"',
                      "r") as subp:
            text = subp.read()
        cpuFreq[5] = re.search("(\\d+)Cpu5Freq", text).group(1)
        # 获取CPU6频率
        with os.popen('adb shell echo \"$(cat /sys/devices/system/cpu/cpu6/cpufreq/scaling_cur_freq)Cpu6Freq\"',
                      "r") as subp:
            text = subp.read()
        cpuFreq[6] = re.search("(\\d+)Cpu6Freq", text).group(1)
        # 获取CPU7频率
        with os.popen('adb shell echo \"$(cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq)Cpu7Freq\"',
                      "r") as subp:
            text = subp.read()
        cpuFreq[7] = re.search("(\\d+)Cpu7Freq", text).group(1)
        frontAppCpuFreq = cpuFreq[frontAppCpuCore]
        # print('cpu0Freq:', cpu0Freq)
        # print('cpu1Freq:', cpu1Freq)
        # print('cpu2Freq:', cpu2Freq)
        # print('cpu3Freq:', cpu3Freq)
        # print('cpu4Freq:', cpu4Freq)
        # print('cpu5Freq:', cpu5Freq)
        # print('cpu6Freq:', cpu6Freq)
        # print('cpu7Freq:', cpu7Freq)

        # 获取整机内存使用情况
        with os.popen('adb shell cat /proc/meminfo', "r") as subp:
            text = subp.read()
            memFree = re.search("MemFree:\\s+\\d+", text).group(0)
            self.MemFree = int(re.search("\\d+", memFree).group(0))
            memAvail = re.search("MemAvailable:\\s+\\d+", text).group(0)
            self.MemAvailable = int(re.search("\\d+", memAvail).group(0))
            swapFree = re.search("SwapFree:\\s+\\d+", text).group(0)
            self.SwapFree = int(re.search("\\d+", swapFree).group(0))
            # print(memFree)

        # 获取前台应用的内存使用情况
        with os.popen('adb shell dumpsys meminfo ' + self.packageName, "r") as subp:
            while True:
                try:
                    line = subp.buffer.readline().decode(encoding='gbk')
                except UnicodeDecodeError:
                    print("chinese characters!")
                else:
                    if 'Native Heap' in line:
                        NativeHeap = re.search(
                            "Native Heap\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+(\\d+)\\s+\\d+\\s+(\\d+)", line)
                        nativeHeapFreeSize = int(NativeHeap.group(2))
                        usedNativeMemPercent = str('{:.3f}'.format(
                            (int(NativeHeap.group(1)) - nativeHeapFreeSize) / int(NativeHeap.group(1)) * 100)) + '%'
                        continue
                    if 'App Summary' in line:
                        subp.buffer.readline().decode(encoding='gbk')
                        subp.buffer.readline().decode(encoding='gbk')
                        line = subp.buffer.readline().decode(encoding='gbk')
                        FrontAPPJavaHeap = int(re.search("Java Heap:\\s+(\\d+)", line).group(1))
                        line = subp.buffer.readline().decode(encoding='gbk')
                        FrontAPPNativeHeap = int(re.search("Native Heap:\\s+(\\d+)", line).group(1))
                        line = subp.buffer.readline().decode(encoding='gbk')
                        FrontAPPCodeMem = int(re.search("Code:\\s+(\\d+)", line).group(1))
                        line = subp.buffer.readline().decode(encoding='gbk')
                        FrontAPPStackMem = int(re.search("Stack:\\s+(\\d+)", line).group(1))
                        line = subp.buffer.readline().decode(encoding='gbk')
                        FrontAPPGraphicsMem = int(re.search("Graphics:\\s+(\\d+)", line).group(1))
                        line = subp.buffer.readline().decode(encoding='gbk')
                        FrontAPPrivateOtherMem = int(re.search("Private Other:\\s+(\\d+)", line).group(1))
                        line = subp.buffer.readline().decode(encoding='gbk')
                        FrontAPPSystemMem = int(re.search("System:\\s+(\\d+)", line).group(1))
                        break
        # print('nativeHeapFreeSize:', nativeHeapFreeSize)
        # print('usedNativeMemPercent:', usedNativeMemPercent)
        # print("FrontAPPNativeHeap:", FrontAPPNativeHeap)
        # print("FrontAPPCodeMem:", FrontAPPCodeMem)
        # print("FrontAPPStackMem:", FrontAPPStackMem)
        # print("FrontAPPGraphicsMem:", FrontAPPGraphicsMem)
        # print("FrontAPPrivateOtherMem:", FrontAPPrivateOtherMem)
        # print("FrontAPPSystemMem:", FrontAPPSystemMem)

        # 获取前台应用和整机的IO信息

        # 读取整机PSI值
        with os.popen('adb shell echo \"cpu $(cat /proc/pressure/cpu)\"', "r") as subp:
            text = subp.read()
            cpusome = re.search(
                "cpu some avg10=(\\d+).(\\d+)\\s+avg60=(\\d+).(\\d+)\\s+avg300=(\\d+).(\\d+)\\s+total=(\\d+)",
                text).group(0)
            cpusome = cpusome.split()
            cpusome10 = float(re.search("\\d+\\.\\d+", cpusome[2]).group(0))
            # print(cpusome10)
        with os.popen('adb shell echo \"mem $(cat /proc/pressure/memory)\"', "r") as subp:
            text = subp.read()
            memsome = re.search(
                "mem some avg10=(\\d+).(\\d+)\\s+avg60=(\\d+).(\\d+)\\s+avg300=(\\d+).(\\d+)\\s+total=(\\d+)",
                text).group(0)
            memsome = memsome.split()
            memsome10 = float(re.search("\\d+\\.\\d+", memsome[2]).group(0))
            # print(memsome10)
        with os.popen('adb shell echo \"io $(cat /proc/pressure/io)\"', "r") as subp:
            text = subp.read()
            iosome = re.search(
                "io some avg10=(\\d+).(\\d+)\\s+avg60=(\\d+).(\\d+)\\s+avg300=(\\d+).(\\d+)\\s+total=(\\d+)",
                text).group(0)
            iosome = iosome.split()
            iosome10 = float(re.search("\\d+\\.\\d+", iosome[2]).group(0))
            # print(iosome10)

        # 获取GPU使用率
        with os.popen('adb shell echo \"$(cat /sys/class/kgsl/kgsl-3d0/gpubusy)GpuUsage\"', "r") as subp:
            text = subp.read()
            divided = int(re.search("(\\d+)\\s+(\\d+)GpuUsage", text).group(2))
            if divided == 0:
                gpuUsage = 0
            else:
                gpuUsage = int(re.search("(\\d+)\\s+(\\d+)GpuUsage", text).group(1)) / divided
        # 直接获取GPU使用率百分比 和上面计算的结果基本一样
        with os.popen('adb shell echo \"$(cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage)GpuPercent\"', "r") as subp:
            text = subp.read()
            gpuPercent = int(re.search("(\\d+) %GpuPercent", text).group(1))
        if self.pflag:
            print('gpuUsage:', gpuUsage)
            print('gpuPercent', gpuPercent)

        # 获取GPU频率
        with os.popen('adb shell echo \"$(cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq)GpuFreq\"', "r") as subp:
            text = subp.read()
            gpuFreq = re.search("(\\d+)GpuFreq", text).group(1)
        # print('GpuFreq:', GpuFreq)

        # 获取掉帧信息,三条信息
        with os.popen('adb shell dumpsys SurfaceFlinger| findstr missed', "r") as subp:
            text = subp.read()
        missed1 = re.search("GPU missed frame count: (\\d+)", text).group(1)
        missed2 = re.search("Total missed frame count: (\\d+)", text).group(1)
        missed3 = re.search("HWC missed frame count: (\\d+)", text).group(1)
        missedGPU = int(missed1) - self.missedGPUCounts
        self.missedGPUCounts = int(missed1)
        missedHWC = int(missed3) - self.missedHWCCounts
        self.missedHWCCounts = int(missed3)
        missedTotal = int(missed2) - self.missedTotalMissed
        self.missedTotalMissed = int(missed2)
        # print("GPU missed:", missedGPU)
        # print("HWC missed:", missedHWC)
        # print("Total missed:", missedTotal)

        # 计算jank比例
        with os.popen('adb shell dumpsys gfxinfo ' + self.packageName + '|findstr \"frames Vsync\"',
                      "r") as subp:
            text = subp.read()
        NowTotalFrames = int(re.search("Total frames rendered: (\\d+)", text).group(1))
        # print(NowTotalFrames)
        TotalFrames = NowTotalFrames - self.lastTotalFrames
        self.lastTotalFrames = NowTotalFrames

        NowMissedVsync = int(re.search("Number Missed Vsync: (\\d+)", text).group(1))
        # print(NowMissedVsync)
        MissedVsync = NowMissedVsync - self.lastMissedVsync
        self.lastMissedVsync = NowMissedVsync

        NowJankyFrames = int(re.search("Janky frames: (\\d+) [(]((100|([1-9]?\\d)(\\.\\d+)?)%)[)]", text).group(1))
        JankyFrames = NowJankyFrames - self.lastJankyFrames
        self.lastJankyFrames = NowJankyFrames
        if (TotalFrames != 0):
            JankyRate = round(JankyFrames / TotalFrames, 4)
            if self.wflag:
                print(JankyRate)
        else:
            JankyRate = 1
        # print("TotalFrames:", TotalFrames)
        # print("MissedVsync:", MissedVsync)
        # print("JankyNum:", JankyFrames)
        # print("JankyRate:", JankyRate)




        # 获取fps线程计算的帧率
        FPS, jankRate, ts = self.ft.getFps()
        if self.pflag:
            print('FPS:',FPS)
            print(jankRate)
        # print("ts:",ts)
        # 获取power线程计算的功耗
        power = self.pt.getPower()

        # 获取CPU和电池温度
        with os.popen('adb shell dumpsys thermalservice| findstr Temperature', "r") as subp:
            text = subp.read()
        textT = text.split('\n')
        temp = {}
        for line in textT:
            Temp = re.search("mValue=-?(\\d+\\.\\d+), mType=\\d+, mName=(\\S+), mStatus=\\d+", line)
            if Temp != None:
                myname = Temp.group(2)
                if ('CPU' in myname or 'battery' in myname):
                    myname = myname.replace('CPU', 'tempCPU')
                    myname = myname.replace('battery', 'batteryTemp')
                    myvalue = re.search("mValue=(\\d+\\.\\d+), mType=\\d+, mName=(\\S+), mStatus=\\d+", line).group(1)
                    temp[myname] = myvalue
        tempCPU0 = temp['tempCPU0']
        tempCPU1 = temp['tempCPU1']
        tempCPU2 = temp['tempCPU2']
        tempCPU3 = temp['tempCPU3']
        tempCPU4 = temp['tempCPU4']
        tempCPU5 = temp['tempCPU5']
        tempCPU6 = temp['tempCPU6']
        tempCPU7 = temp['tempCPU7']
        batteryTemp = temp['batteryTemp']
        # print('tempCPU0:', tempCPU0)
        # print('tempCPU1:', tempCPU1)
        # print('tempCPU2:', tempCPU2)
        # print('tempCPU3:', tempCPU3)
        # print('tempCPU4:', tempCPU4)
        # print('tempCPU5:', tempCPU5)
        # print('tempCPU6:', tempCPU6)
        # print('tempCPU7:', tempCPU7)
        # print('batteryTemp:', batteryTemp)

        # 获取热缓解状态
        with os.popen('adb shell dumpsys thermalservice| find  "Thermal Status"', "r") as subp:
            text = subp.read()
        thermalStatus = re.search("Thermal Status: (\\d)", text).group(1)
        # print('thermalStatus:', thermalStatus)

        # 获取网络状态
        with os.popen('adb shell dumpsys connectivity | find \"Link speed\"', "r") as subp:
            text = subp.read()
        wifiRssi = re.search("RSSI: (-?\\d+)", text).group(1)
        wifiSpeed = re.search("Link speed: (\\d+)Mbps", text).group(1)
        # print('wifiRssi:', wifiRssi)
        # print('wifiSpeed:', wifiSpeed)

        # 是否写入csv文件
        if self.wflag:
            resultStr = '{0},{1},{2},{3},{4},{5},{6},{7},{8},{9},{10},{11},{12},{13},{14},{15},{16},{17},{18},{19},' \
                        '{20},{21},{22},{23},{24},{25},{26},{27},{28},{29},{30},{31},{32},{33},{34},{35},{36},{37},' \
                        '{38},{39},{40},{41},{42},{43},{44},{45},{46},{47},{48},{49},{50},{51},{52},{53}\n '.format(
                str(self.timestamp), str(self.packageName),
                str(cpuUsage), str(FrontAppCpuUsage), str(frontAppCpuCore), str(frontAppCpuFreq), str(cpuFreq[0]),
                str(cpuFreq[1]), str(cpuFreq[2]), str(cpuFreq[3]), str(cpuFreq[4]), str(cpuFreq[5]), str(cpuFreq[6]),
                str(cpuFreq[7]),
                str(self.MemFree), str(self.MemAvailable), str(self.SwapFree), str(self.threshold), str(self.LowMemory),
                str(nativeHeapFreeSize), str(usedNativeMemPercent), str(FrontAPPJavaHeap), str(FrontAPPNativeHeap),
                str(FrontAPPCodeMem), str(FrontAPPStackMem), str(FrontAPPGraphicsMem), str(FrontAPPrivateOtherMem),
                str(FrontAPPSystemMem),
                str(cpusome10), str(memsome10), str(iosome10),
                str(gpuUsage), str(gpuFreq),
                str(missedGPU), str(missedHWC), str(missedTotal),
                str(TotalFrames), str(MissedVsync), str(JankyFrames), str(JankyRate), str(FPS), str(jankRate),
                str(tempCPU0), str(tempCPU1), str(tempCPU2), str(tempCPU3), str(tempCPU4), str(tempCPU5), str(tempCPU6),
                str(tempCPU7), str(batteryTemp), str(thermalStatus),
                str(wifiRssi), str(wifiSpeed)),str(power)
            self.write(resultStr)
            return
        else:
            resultList = [str(self.timestamp), str(self.packageName),
                          str(cpuUsage), str(FrontAppCpuUsage), str(frontAppCpuCore), str(frontAppCpuFreq),
                          str(cpuFreq[0]),
                          str(cpuFreq[1]), str(cpuFreq[2]), str(cpuFreq[3]), str(cpuFreq[4]), str(cpuFreq[5]),
                          str(cpuFreq[6]),
                          str(cpuFreq[7]),
                          str(self.MemFree), str(self.MemAvailable), str(self.SwapFree), str(self.threshold),
                          str(self.LowMemory),
                          str(nativeHeapFreeSize), str(usedNativeMemPercent), str(FrontAPPJavaHeap),
                          str(FrontAPPNativeHeap),
                          str(FrontAPPCodeMem), str(FrontAPPStackMem), str(FrontAPPGraphicsMem),
                          str(FrontAPPrivateOtherMem),
                          str(FrontAPPSystemMem),
                          str(cpusome10), str(memsome10), str(iosome10),
                          str(gpuUsage), str(gpuFreq),
                          str(missedGPU), str(missedHWC), str(missedTotal),
                          str(TotalFrames), str(MissedVsync), str(JankyFrames), str(JankyRate), str(FPS), str(jankRate),
                          str(tempCPU0), str(tempCPU1), str(tempCPU2), str(tempCPU3), str(tempCPU4), str(tempCPU5),
                          str(tempCPU6),
                          str(tempCPU7), str(batteryTemp), str(thermalStatus),
                          str(wifiRssi), str(wifiSpeed),
                          str(power)]
            return resultList

    def write(self, resultStr):
        # print("write")
        # async with open(self.dir_csv, mode='a', encoding='utf-8') as csv:
        #     await csv.write(resultStr)
        with open(self.dir_csv, mode='a', encoding='utf-8') as csv:
            csv.write(resultStr)
        # print("done!")
