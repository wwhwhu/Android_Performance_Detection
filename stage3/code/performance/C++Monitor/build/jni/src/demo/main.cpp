#include <stdio.h>
#include <iostream>
#include <map>
#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <regex>
#include <string>
#include <sstream>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <err.h>
#include <errno.h>
#include <libgen.h>
#include <stdlib.h>
#include <string.h>
#include <fstream>
#include <math.h>
#include <thread>
#include <chrono>
#include <thread>
#include <chrono>
#include <vector>

using namespace std;
using namespace std::chrono;
#define MAX_BUFFER_SIZE 1024

string zhixing(char *command)
{
    FILE *pipe = popen(command, "r");
    string result = "";
    if (!pipe)
    {
        cout << "打开管道失败！" << endl;
        return "error";
    }
    // 读取输出到缓冲区
    char buffer[MAX_BUFFER_SIZE];
    memset(buffer, 0, sizeof(buffer));
    while (fgets(buffer, MAX_BUFFER_SIZE, pipe) != NULL)
    {
        result += buffer;
    }
    // 关闭管道并检查状态
    int status = pclose(pipe);
    if (status == -1)
    {
        cout << "关闭管道失败！" << endl;
        return "error";
    }
    return result;
}

/*class powerThread
{
private:
    int count = 0;
    double power = 0;

public:
    void run()
    {
        while (1)
        {
            // 读取CPU耗电
            // char *commands = "dumpsys batterystats |grep cpu:";
            // string output = zhixing(commands);
            // cout<<output<<endl;
            // regex pattern("cpu: (\\d+.\\d+)");
            // smatch match;
            // if (regex_search(output, match, pattern))
            // {
            //     double cpucost = stod(match[1]);
            // }

            直接用电压电流算 但是充电时读不准
            int voltage;
            int current;
            char *commands = "echo \"$(cat /sys/class/power_supply/battery/current_now)current\"";
            string output = zhixing(commands);
            regex pattern("(\\d+)current");
            smatch match;
            cout<<output<<endl;
            if (regex_search(output, match, pattern))
            {
                current = stoi(match[1]);
            }
            commands = "echo \"$(cat /sys/class/power_supply/battery/voltage_now)voltage\"";
            output = zhixing(commands);
            cout<<output<<endl;
            pattern = "(\\d+)voltage";
            if (regex_search(output, match, pattern))
            {
                voltage = stoi(match[1]);
            }
            int p = current * voltage;

            // commands = "date '+%s%3N'";
            // output = zhixing(commands);
            // long ptime = stol(output);
            // cout << "PowerMonitor: ptime: " << ptime << endl;
            // cout << "PowerMonitor: current: " << current << endl;
            // cout << "PowerMonitor: voltage: " << voltage << endl;
            // 取平均
            this->count += 1;
            this->power = this->power + p;
        }
    }
    double getPower()
    {
        double p = this->power / this->count;
        this->power = 0;
        this->count = 0;
        return p;
    }
};*/

class FpsThread
{
private:
    string threadname;
    string packageName;
    string focusWindow;
    long lastTimestamp;
    double FPS;
    double jankRate;
    bool pflag;

public:
    void setFpsThread(string threadname, string packageName, string focusWindow, bool pflag)
    {
        this->threadname = threadname;
        this->packageName = packageName;
        this->focusWindow = focusWindow;
        this->lastTimestamp = 0;
        this->FPS = 0;
        this->jankRate = 0;
        this->pflag = pflag;
        // 清空记录
        char *commands = "dumpsys SurfaceFlinger --latency-clear";
        string output = zhixing(commands);
    }

    double getFPS()
    {
        return this->FPS;
    }

    double getJankRate()
    {
        return this->jankRate;
    }

    int getLastTimestamp()
    {
        return this->lastTimestamp;
    }

    void runMonitor()
    {
        char *commands = (char *)(("dumpsys SurfaceFlinger --latency " + this->focusWindow).data());
        string output = zhixing(commands);
        istringstream iss1(output);
        string line;
        vector<string> text;
        while (getline(iss1, line))
        {
            text.push_back(line);
        }
        if (text.size() == 0)
        {
            cout << "Error: 获取帧数据为空！，启动失败！" << endl;
            return;
        }
        vector<vector<long>> timestamps;
        double ns_per_ms = 1e6;
        // 数据第一行：屏幕刷新时长
        int refresh_period = stoi(text[0]) / ns_per_ms;
        // 防止数据溢出
        long long pending_fence_timestamp = 9223372036854775807;
        for (int i = 1; i < text.size(); i++)
        {
            long ts0;
            long ts1;
            long ts2;
            cmatch match;
            regex pattern("(\\d+)\\s+(\\d+)\\s+(\\d+)");
            if (regex_search(text[i].c_str(), match, pattern))
            {
                ts0 = stol(match[1]);
                ts1 = stol(match[2]);
                ts2 = stol(match[3]);
            }
            else                
            {
                // 忽略残缺数据
                // cout<<"FPSMonitor: 数据残缺！！"<< endl;
                continue;
            }
            // 忽略空帧
            if (ts0 == 0)
            {
                continue;
            }
            // 忽略数据溢出的记录
            if (ts0 == pending_fence_timestamp || ts1 == pending_fence_timestamp || ts2 == pending_fence_timestamp)
            {
                continue;
            }
            // 记录每一帧绘制的时间点
            vector<long> timestamp{ts0, ts1, ts2};
            timestamps.push_back(timestamp);
        }
        vector<long> duration;
        for (int i = 0; i < timestamps.size(); i++)
        {
            if (timestamps[i][0] > this->lastTimestamp)
            {
                duration.push_back((timestamps[i][2] - timestamps[i][0]) / ns_per_ms);
                this->lastTimestamp = timestamps[i][0];
            }
        }
        if (duration.size() < 3)
        {
            cout << "FPS: 屏幕无更新" << endl;
            // continue;
            return;
        }
        // 计算jank率
        int jankFrame = 0;
        for (int i = 0; i < duration.size(); i++)
        {
            if (duration[i] > refresh_period)
                jankFrame += 1;
        }
        jankRate = (jankFrame + 0.0) / duration.size();
        // 计算FPS
        // cout << "总帧数："<< timestamps.size();
        // cout << "超时帧数：" << jankFrame;
        // cout << "时间间隔："<< (this->lastTimestamp - timestamps[0][0])/1000000000.00 << "s"<<endl;
        this->FPS = (timestamps.size() * 1000000000 + 0.00) / (this->lastTimestamp - timestamps[0][0]);
        if (this->pflag)
        {
            cout << "FPSMonitor: FPS: " << this->FPS << endl;
            cout << "FPSMonitor: jankRate: " << this->jankRate << endl;
        }
    }
};

class Monitor
{
private:
    bool dflag; // 是否动态切换
    bool wflag; // 是否写入文件
    bool pflag; // 是否打印监控信息
    string FrontAPPName;
    long timestamp;
    long lastTimestamp;
    int nativeHeapFreeSize;
    double usedNativeMemPercent;
    int FrontAppCpuCore;
    int FrontAPPJavaHeap;
    int FrontAPPNativeHeap;
    int FrontAPPCodeMem;
    int FrontAPPStackMem;
    int FrontAPPGraphicsMem;
    int FrontAPPPrivateOtherMem;
    int FrontAPPSystemMem;
    int Cpu0Freq;
    int Cpu1Freq;
    int Cpu2Freq;
    int Cpu3Freq;
    int Cpu4Freq;
    int Cpu5Freq;
    int Cpu6Freq;
    int Cpu7Freq;
    int FrontAppCpuFreq;
    double gpuUsage;
    int gpuFreq;
    int cpuUser;
    int cpuNice;
    int cpuSystem;
    int cpuIdle;
    int cpuIowait;
    int cpuIrq;
    int cpuSortirq;
    int cpuSteal;
    int cpuGuest;
    int cpuGnice;
    double cpuUsage;
    int gpuPercent;
    int cpuLast;
    int cpuLastIdle;
    int FrontApptime;
    double FrontAppCpuUsage;
    int lastFrontApptime;
    int MemFree;
    int MemAvailable;
    int SwapFree;
    int threshold;
    double jankRate;
    double cpusome10;
    double memsome10;
    double iosome10;
    double tempCPU0;
    double tempCPU1;
    double tempCPU2;
    double tempCPU3;
    double tempCPU4;
    double tempCPU5;
    double tempCPU6;
    double tempCPU7;
    double batteryTemp;
    int thermalStatus;
    bool LowMemory;
    int missedGPU;
    int missedHWC;
    int missedTotal;
    int missedGPUCounts;
    int missedHWCCounts;
    int missedTotalMissed;
    int MissedVsync;
    int TotalFrames;
    int JankyFrames;
    double JankyRate;
    int lastTotalFrames;
    int lastMissedVsync;
    int lastJankyFrames;
    double wifiSpeed;
    double wifiRssi;
    double FPS;
    double power;
    string packageName;
    int pid;
    string resultStr;
    string focus_window;
    map<string, string> appName;
    map<string, string> appView;
    string dir_csv;
    // powerThread powerthd;
    FpsThread fpsthd;
    void init()
    {
        FrontAPPName = "";
        timestamp = 0;
        lastTimestamp = 0;
        nativeHeapFreeSize = 0;
        usedNativeMemPercent = 0.0;
        FrontAPPJavaHeap = 0;
        FrontAPPNativeHeap = 0;
        FrontAPPCodeMem = 0;
        FrontAPPStackMem = 0;
        FrontAPPGraphicsMem = 0;
        FrontAPPPrivateOtherMem = 0;
        FrontAPPSystemMem = 0;
        gpuUsage = 0.0;
        gpuFreq = 0;
        cpuUser = 0;
        cpuNice = 0;
        cpuSystem = 0;
        cpuIdle = 0;
        cpuIowait = 0;
        cpuIrq = 0;
        cpuSortirq = 0;
        cpuSteal = 0;
        cpuGuest = 0;
        cpuGnice = 0;
        cpuUsage = 0.0;
        cpuLast = 0;
        cpuLastIdle = 0;
        FrontApptime = 0;
        FrontAppCpuCore = 0;
        FrontAppCpuUsage = 0.0;
        lastFrontApptime = 0;
        MemFree = 0;
        MemAvailable = 0;
        SwapFree = 0;
        threshold = 226492416 / 1024;
        tempCPU0 = 0.0;
        tempCPU1 = 0.0;
        tempCPU2 = 0.0;
        tempCPU3 = 0.0;
        tempCPU4 = 0.0;
        tempCPU5 = 0.0;
        tempCPU6 = 0.0;
        tempCPU7 = 0.0;
        Cpu0Freq = 0;
        Cpu1Freq = 0;
        Cpu2Freq = 0;
        Cpu3Freq = 0;
        Cpu4Freq = 0;
        Cpu5Freq = 0;
        Cpu6Freq = 0;
        Cpu7Freq = 0;
        FrontAppCpuFreq = 0;
        batteryTemp = 0.0;
        thermalStatus = 0;
        LowMemory = false;
        cpusome10 = 0;
        iosome10 = 0;
        memsome10 = 0;
        missedGPU = 0;
        missedHWC = 0;
        missedTotal = 0;
        missedGPUCounts = 0;
        missedHWCCounts = 0;
        missedTotalMissed = 0;
        MissedVsync = 0;
        TotalFrames = 0;
        JankyFrames = 0;
        JankyRate = 0.0;
        lastTotalFrames = 0;
        lastMissedVsync = 0;
        lastJankyFrames = 0;
        gpuPercent = 0;
        wifiSpeed = 0.0;
        wifiRssi = 0.0;
        FPS = 0.0;
        jankRate = 0.0;
        power = 0;
        packageName = "";
        pid = 0;
        focus_window = "";
        resultStr = "";
        appName["com.miHoYo.Yuanshen"] = "genshin";
        appName["com.taobao.taobao"] = "taobao";
        appName["com.xingin.xhs"] = "xhs";
        appName["com.tencent.mm"] = "wechat";
        appName["com.ss.android.ugc.aweme"] = "tiktok";
        appName["tv.danmaku.bili"] = "bilibili";
        appName["com.zhihu.android"] = "zhihu";
        appName["com.android.gallery3d"] = "gallery";

        appView["com.miHoYo.Yuanshen"] = "SurfaceView[com.miHoYo.Yuanshen/com.miHoYo.GetMobileInfo.MainActivity](BLAST)#0";
        appView["com.taobao.taobao"] = "com.taobao.taobao/com.taobao.tao.TBMainActivity#0";
        appView["com.xingin.xhs"] = "com.xingin.xhs/com.xingin.xhs.index.v2.IndexActivityV2#0";
        appView["com.tencent.mm"] = "com.tencent.mm/com.tencent.mm.ui.LauncherUI#37592";
        appView["com.ss.android.ugc.aweme"] = "com.ss.android.ugc.aweme/com.ss.android.ugc.aweme.splash.SplashActivity#0";
        appView["tv.danmaku.bili"] = "SurfaceView[tv.danmaku.bili/com.bilibili.video.videodetail.VideoDetailsActivity](BLAST)#0";
        appView["com.zhihu.android"] = "com.zhihu.android/com.zhihu.android.app.ui.activity.MainActivity#0";
        appView["com.android.gallery3d"] = "SurfaceView[com.android.gallery3d/com.android.gallery3d.app.GalleryActivity](BLAST)#0";
        dir_csv = "";
    }

public:
    Monitor()
    {
        this->dflag = false;
        this->wflag = false;
        this->pflag = true;
        init();
        // 如果动态切换前台应用
        if (this->dflag)
        {
            initCount();
            getFrontApp();
            if (this->wflag)
                dir_csv = makeDir("mix");
        }
        else
        {
            // 前台应用不变情况：初始化统计值,统计前台APP相关信息，包括包名，图层名，pid等
            initCount();
            getFrontApp();
            cout << "Init: focusWindow: " << focus_window << endl;
            string app = this->appName[this->packageName];
            // this->focus_window = this->appView[this->packageName];
            // 如果要写入csv文件 创建文件保存监控数据
            if (this->wflag)
                this->dir_csv = this->makeDir(app);
        }
        // 开启线程计算FPS
        fpsthd.setFpsThread("fps_demo", packageName, focus_window, pflag);
        thread t([this]()
                 {
        while (true) {
            fpsthd.runMonitor();
            this_thread::sleep_for(chrono::seconds(2));
        } });
        t.detach();

        // 开启线程计算功耗
        // thread tt(&powerThread::run, &powerthd);
        // tt.detach();
    }
    Monitor(bool d, bool w, bool p)
    {
        this->dflag = d;
        this->wflag = w;
        this->pflag = p;
        init();
        // 如果动态切换前台应用
        if (this->dflag)
        {
            initCount();
            getFrontApp();
            if (this->wflag)
                dir_csv = makeDir("mix");
        }
        else
        {
            // 前台应用不变情况：初始化统计值,统计前台APP相关信息，包括包名，图层名，pid等
            initCount();
            getFrontApp();
            cout << "Init: focusWindow:" << focus_window << endl;
            string app = this->appName[this->packageName];
            // this->focus_window = this->appView[this->packageName];
            // 如果要写入csv文件 创建文件保存监控数据
            if (this->wflag)
                this->dir_csv = this->makeDir(app);
        }
        //开启线程计算FPS
        fpsthd.setFpsThread("fps_demo", packageName, focus_window, pflag);
        thread t([this]()
                 {
        while (true) {
            fpsthd.runMonitor();
            this_thread::sleep_for(chrono::seconds(2));
        } });
        t.detach();

        // 开启线程计算功耗
        // thread tt(&powerThread::run, &powerthd);
        // tt.detach();
    }
    void print()
    {
        cout << "是否动态切换:" << this->dflag << endl;
        cout << "是否写入文件:" << this->wflag << endl;
        cout << "是否打印监控信息:" << this->pflag << endl;
        return;
    }

    void initCount()
    {
        char *commands = "dumpsys SurfaceFlinger| grep missed";
        string output = zhixing(commands);
        regex pattern("GPU missed frame count: (\\d+)");
        smatch match;
        if (regex_search(output, match, pattern))
        {
            missedGPUCounts = stoi(match[1]);
            cout << "Init: GPU missed frame count: " << missedGPUCounts << endl;
        }

        pattern = "HWC missed frame count: (\\d+)";
        if (regex_search(output, match, pattern))
        {
            missedHWCCounts = stoi(match[1]);
            cout << "Init: HWC missed frame count: " << missedHWCCounts << endl;
        }

        pattern = "Total missed frame count: (\\d+)";
        if (regex_search(output, match, pattern))
        {
            missedTotalMissed = stoi(match[1]);
            cout << "Init: Total missed frame count: " << missedTotalMissed << endl;
        }

        commands = "cat /proc/stat";
        output = zhixing(commands);
        pattern = "cpu\\s+(\\d+)\\s+(\\d+)+\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)";
        if (regex_search(output, match, pattern))
        {
            int cpuUser = stoi(match[1]);
            int cpuNice = stoi(match[2]);
            int cpuSystem = stoi(match[3]);
            int cpuIdle = stoi(match[4]);
            int cpuIowait = stoi(match[5]);
            int cpuIrq = stoi(match[6]);
            int cpuSortirq = stoi(match[7]);
            int cpuSteal = stoi(match[8]);
            int cpuGuest = stoi(match[9]);
            int cpuGnice = stoi(match[10]);
            cpuLast = cpuUser + cpuNice + cpuSystem + cpuIdle + cpuIowait + cpuIrq + cpuSortirq + cpuSteal + cpuGuest + cpuGnice;
            cpuLastIdle = cpuIdle;
            cout << "Init: cpuLast: " << cpuLast << endl;
            cout << "Init: cpuLastIdle: " << cpuLastIdle << endl;
        }

        commands = "date '+%s%3N'";
        output = zhixing(commands);
        lastTimestamp = stol(output);
        cout << "Init: lastTimestamp: " << lastTimestamp << endl;

        commands = "dumpsys batterystats --reset";
        output = zhixing(commands);
        cout << "Init: batterystats --reset" << endl;
    }

    void getFrontApp()
    {
        char *commands = "dumpsys window| grep CurrentFocus";
        string output = zhixing(commands);
        // regex pattern("mCurrentFocus=Window{([a-z]|\\d)+ u0 (com|tv)([a-zA-Z]|\\.|\\d)+/com([a-zA-Z]|\\.|\\d)+}");
        regex pattern("mCurrentFocus=Window\\{(([a-z]|\\d)+) u0 ((com|tv)([a-zA-Z]|\\.|\\d)+)/((com|tv)([a-zA-Z]|\\.|\\d)+)");
        smatch match;
        if (regex_search(output, match, pattern))
        { // 前台应用包名
            packageName = match[3];
            focus_window = match[6];
            focus_window = packageName + "/" + focus_window;
            if (!packageName.empty())
                cout << "Init: FrontAPPpackageName: " << packageName << endl;
            else
            {
                cout << "error!" << endl;
                return;
            }
        }
        // 前台应用pid
        commands = (char *)("echo $(pidof " + packageName + ")FrontPid").data();
        output = zhixing(commands);
        pattern = "(\\d+)FrontPid";
        if (regex_search(output, match, pattern))
        {
            pid = stoi(match[1]);
        }
        if (pflag)
        {
            cout << "Init: FrontAppPid: " << pid << endl;
        }

        commands = "dumpsys SurfaceFlinger | grep -i focus -A 20";
        output = zhixing(commands);
        stringstream ss(output); // 使用字符串构造一个stringstream类型（流）数据
        char c = '\n';           // 设定好分隔符号(只能使用一个字符进行分割)
        string str;              // 用来接收每个分割的字符串
        getline(ss, str, c);
        getline(ss, str, c);
        getline(ss, str, c);
        focus_window = str;
        cout << "Init: MyCurrentFocus: " << focus_window << endl;

        // 获取CPU信息
        commands = (char *)("cat proc/" + to_string(pid) + "/stat").data();
        output = zhixing(commands);
        pattern = to_string(pid) + " \\(([a-zA-Z]|\\.|\\d)*\\) [a-zA-Z] \\d+ \\d+ \\d+ \\d+ -\\d+ \\d+ \\d+ \\d+ \\d+ \\d+ (\\d+) (\\d+)";
        if (regex_search(output, match, pattern))
        {
            int FrontApputime = stoi(match[2]);
            int FrontAppstime = stoi(match[3]);
            lastFrontApptime = FrontApputime + FrontAppstime;
            cout << "Init: FrontApputime:" << FrontApputime << " FrontAppstime: " << FrontAppstime << " lastFrontApptime: " << lastFrontApptime << endl;
        }

        // 获取前台掉帧信息
        commands = (char *)("dumpsys gfxinfo " + packageName + "|grep -E 'frames|Vsync'").data();
        output = zhixing(commands);
        pattern = "Total frames rendered: (\\d+)";
        if (regex_search(output, match, pattern))
        {
            lastTotalFrames = stoi(match[1]);
            cout << "Init: lastTotalFrames: " << lastTotalFrames << endl;
        }

        pattern = "Number Missed Vsync: (\\d+)";
        if (regex_search(output, match, pattern))
        {
            lastMissedVsync = stoi(match[1]);
            cout << "Init: lastMissedVsync: " << lastMissedVsync << endl;
        }

        pattern = "Janky frames: (\\d+) [(]((100|([1-9]?\\d)(\\.\\d+)?)%)[)]";
        if (regex_search(output, match, pattern))
        {
            lastJankyFrames = stoi(match[1]);
            cout << "Init: lastJankyFrames: " << lastJankyFrames << endl;
        }
    }

    string makeDir(string app)
    {
        string dir0 = "./data/local/tmp/c_plus_plus/" + app + ".csv";
        string dir = "./data/local/tmp/c_plus_plus";
        cout << "Init: record file at: " << dir0 << endl;
        char *path = (char *)dir.data(); // 要创建的文件夹路径
        if ((access(path, 0)) != -1)
        {
            cout << "Init: 目录" + dir + "存在\n";
        }
        else
        {
            cout << "Init: 目录" + dir + "不存在，创键目录\n";
            mkdir(path, S_IRWXU);
        }

        // 判断文件是否存在
        ifstream file(dir0.c_str());
        if (file.good())
        {
            cout << "Init: 文件" + dir0 + "已存在！" << endl;
            file.close();
        }
        else
        {
            // 创建新文件
            ofstream new_file(dir0.c_str());
            if (!new_file)
            {
                cout << "Init: 创建文件" + dir0 + "失败！" << endl;
            }
            else
            {
                cout << "Init: 创建文件" + dir0 + "成功！" << endl;
                ofstream location_out;
                string ss = "timestamp,frontAppPackageName,cpuUsage,frontAppCpuUsage,frontAppCpuCore,frontAppCpuFreq,cpu0Freq,cpu1Freq,cpu2Freq,cpu3Freq,cpu4Freq,cpu5Freq,cpu6Freq,cpu7Freq,MemFree,MemAvailable,SwapFree,threshold,LowMemory,nativeHeapFreeSize,usedNativeMemPercent,frontAppJavaHeap,frontAppNativeHeap,frontAppCodeMem,frontAppStackMem,frontAppGraphicsMem,frontAppPrivateOtherMem,frontAppSystemMem,cpuSome10,memSome10,ioSome10,gpuUsage,gpuFreq,gpuMissed,hwcMissed,totalMissed,totalFrames,missedVsync,jankyNum,jankyRate,fps,jankRate2,tempCPU0,tempCPU1,tempCPU2,tempCPU3,tempCPU4,tempCPU5,tempCPU6,tempCPU7,batteryTemp,thermalStatus,wifiRssi,wifiSpeed,power";
                location_out.open(dir0, std::ios::out | std::ios::app); // 以写入和在文件末尾添加的方式打开.txt文件，没有的话就创建该文件。
                if (!location_out.is_open())
                    cout << "Init: 写入csv文件失败！" << endl;
                else
                {
                    location_out << ss << endl;
                    location_out.close();
                    cout << "Init: 写入csv文件成功！" << endl;
                }
            }
        }
        return dir0;
    }

    vector<string> runMonitor()
    {
        if (dflag)
        {
            getFrontAPP2();
            fpsthd.setFpsThread("fps_demo",packageName,focus_window,pflag);
        }
        // 查看系统时间
        char *commands = "date '+%s%3N'";
        string output = zhixing(commands);
        timestamp = stol(output);
        if (pflag)
            cout << "Monitor: timestamp: " << timestamp << endl;

        // 获取全局CPU使用时间
        commands = "cat /proc/stat";
        output = zhixing(commands);
        smatch match;
        int cpuNow;
        regex pattern("cpu\\s+(\\d+)\\s+(\\d+)+\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)");
        if (regex_search(output, match, pattern))
        {
            cpuUser = stoi(match[1]);
            cpuNice = stoi(match[2]);
            cpuSystem = stoi(match[3]);
            cpuIdle = stoi(match[4]);
            cpuIowait = stoi(match[5]);
            cpuIrq = stoi(match[6]);
            cpuSortirq = stoi(match[7]);
            cpuSteal = stoi(match[8]);
            cpuGuest = stoi(match[9]);
            cpuGnice = stoi(match[10]);
            cpuNow = cpuUser + cpuNice + cpuSystem + cpuIdle + cpuIowait + cpuIrq + cpuSortirq + cpuSteal + cpuGuest + cpuGnice;
            cpuUsage = 1 - (cpuIdle - cpuLastIdle + 0.00) / (cpuNow - cpuLast);
            cpuLastIdle = cpuIdle;

            if (pflag)
            {
                cout << "Monitor: cpuUser: " << cpuUser << endl;
                cout << "Monitor: cpuNice: " << cpuNice << endl;
                cout << "Monitor: cpuSystem: " << cpuSystem << endl;
                cout << "Monitor: cpuIdle: " << cpuIdle << endl;
                cout << "Monitor: cpuIowait: " << cpuIowait << endl;
                cout << "Monitor: cpuIrq: " << cpuIrq << endl;
                cout << "Monitor: cpuSortirq: " << cpuSortirq << endl;
                cout << "Monitor: cpuSteal: " << cpuSteal << endl;
                cout << "Monitor: cpuGuest: " << cpuGuest << endl;
                cout << "Monitor: cpuGnice: " << cpuGnice << endl;
                cout << "Monitor: cpuUsage: " << cpuUsage << endl;
            }
        }

        commands = (char *)("cat proc/" + to_string(pid) + "/stat").data();
        output = zhixing(commands);
        pattern = to_string(pid) + " \\(([a-zA-Z]|\\.|\\d)*\\) [a-zA-Z] \\d+ \\d+ \\d+ \\d+ -\\d+ \\d+ \\d+ \\d+ \\d+ \\d+ (\\d+) (\\d+)";
        if (regex_search(output, match, pattern))
        {
            int FrontApputime = stoi(match[2]);
            int FrontAppstime = stoi(match[3]);
            int FrontApptime = FrontApputime + FrontAppstime;
            if (pflag)
            {
                cout << "Monitor: FrontApputime: " << FrontApputime << endl;
                cout << "Monitor: FrontAppstime: " << FrontAppstime << endl;
            }
            FrontAppCpuUsage = (FrontApptime - lastFrontApptime + 0.00) / (cpuNow - cpuLast);
            if (pflag)
                cout << "Monitor: FrontAppCpuUsage: " << FrontAppCpuUsage << endl;
            lastFrontApptime = FrontApptime;
            cpuLast = cpuNow;
        }

        commands = (char *)(("ps -o pid,psr,comm -p " + to_string(pid)).data());
        output = zhixing(commands);
        // commands = (char*)(("top -o PID,CPU -p" + to_string(pid) + " -n 1").data());
        // output = zhixing(commands);
        pattern = to_string(pid) + "\\s+(\\d)";
        if (regex_search(output, match, pattern))
        {
            FrontAppCpuCore = stoi(match[1]);
            if (pflag)
                cout << "Monitor: FrontAppCpuCore: " << FrontAppCpuCore << endl;
        }

        // commands = (char *)("echo $(cat /sys/devices/system/cpu/cpu" + corecore + "/cpufreq/scaling_cur_freq)FrontCpuFreq").data();
        // output = zhixing(commands);
        // pattern = "(\\d+)FrontCpuFreq";
        // if (regex_search(output, match, pattern))
        // {
        //     FrontAppCpuFreq = stoi(match[1]);
        //     if (pflag)
        //         cout << "Monitor: FrontAppCpuFreq: " << FrontAppCpuFreq << endl;
        // }

        // 获取CPU频率
        commands = "echo $(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq)Cpu0Freq";
        output = zhixing(commands);
        pattern = "(\\d+)Cpu0Freq";
        if (regex_search(output, match, pattern))
        {
            Cpu0Freq = stoi(match[1]);
            if (pflag)
                cout << "Monitor: Cpu0Freq: " << Cpu0Freq << endl;
        }

        commands = "echo $(cat /sys/devices/system/cpu/cpu1/cpufreq/scaling_cur_freq)Cpu1Freq";
        output = zhixing(commands);
        pattern = "(\\d+)Cpu1Freq";
        if (regex_search(output, match, pattern))
        {
            Cpu1Freq = stoi(match[1]);
            if (pflag)
                cout << "Monitor: Cpu1Freq: " << Cpu1Freq << endl;
        }

        commands = "echo $(cat /sys/devices/system/cpu/cpu2/cpufreq/scaling_cur_freq)Cpu2Freq";
        output = zhixing(commands);
        pattern = "(\\d+)Cpu2Freq";
        if (regex_search(output, match, pattern))
        {
            Cpu2Freq = stoi(match[1]);
            if (pflag)
                cout << "Monitor: Cpu2Freq: " << Cpu2Freq << endl;
        }

        commands = "echo $(cat /sys/devices/system/cpu/cpu3/cpufreq/scaling_cur_freq)Cpu3Freq";
        output = zhixing(commands);
        pattern = "(\\d+)Cpu3Freq";
        if (regex_search(output, match, pattern))
        {
            Cpu3Freq = stoi(match[1]);
            if (pflag)
                cout << "Monitor: Cpu3Freq: " << Cpu3Freq << endl;
        }

        commands = "echo $(cat /sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq)Cpu4Freq";
        output = zhixing(commands);
        pattern = "(\\d+)Cpu4Freq";
        if (regex_search(output, match, pattern))
        {
            Cpu4Freq = stoi(match[1]);
            if (pflag)
                cout << "Monitor: Cpu4Freq: " << Cpu4Freq << endl;
        }

        commands = "echo $(cat /sys/devices/system/cpu/cpu5/cpufreq/scaling_cur_freq)Cpu5Freq";
        output = zhixing(commands);
        pattern = "(\\d+)Cpu5Freq";
        if (regex_search(output, match, pattern))
        {
            Cpu5Freq = stoi(match[1]);
            if (pflag)
                cout << "Monitor: Cpu5Freq: " << Cpu5Freq << endl;
        }

        commands = "echo $(cat /sys/devices/system/cpu/cpu6/cpufreq/scaling_cur_freq)Cpu6Freq";
        output = zhixing(commands);
        pattern = "(\\d+)Cpu6Freq";
        if (regex_search(output, match, pattern))
        {
            Cpu6Freq = stoi(match[1]);
            if (pflag)
                cout << "Monitor: Cpu6Freq: " << Cpu6Freq << endl;
        }

        commands = "echo $(cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq)Cpu7Freq";
        output = zhixing(commands);
        pattern = "(\\d+)Cpu7Freq";
        if (regex_search(output, match, pattern))
        {
            Cpu7Freq = stoi(match[1]);
            if (pflag)
                cout << "Monitor: Cpu7Freq: " << Cpu7Freq << endl;
        }

        if (FrontAppCpuCore == 0)
            FrontAppCpuFreq = Cpu0Freq;
        else if (FrontAppCpuCore == 1)
            FrontAppCpuFreq = Cpu1Freq;
        else if (FrontAppCpuCore == 2)
            FrontAppCpuFreq = Cpu2Freq;
        else if (FrontAppCpuCore == 3)
            FrontAppCpuFreq = Cpu3Freq;
        else if (FrontAppCpuCore == 4)
            FrontAppCpuFreq = Cpu4Freq;
        else if (FrontAppCpuCore == 5)
            FrontAppCpuFreq = Cpu5Freq;
        else if (FrontAppCpuCore == 6)
            FrontAppCpuFreq = Cpu6Freq;
        else if (FrontAppCpuCore == 7)
            FrontAppCpuFreq = Cpu7Freq;

        // 获取整机内存使用情况
        commands = "cat /proc/meminfo";
        output = zhixing(commands);
        pattern = "MemFree:\\s+(\\d+)";
        if (regex_search(output, match, pattern))
        {
            MemFree = stoi(match[1]);
            if (pflag)
                cout << "Monitor: MemFree: " << MemFree << endl;
        }
        pattern = "MemAvailable:\\s+(\\d+)";
        if (regex_search(output, match, pattern))
        {
            MemAvailable = stoi(match[1]);
            if (pflag)
                cout << "Monitor: MemAvailable: " << MemAvailable << endl;
        }
        pattern = "SwapFree:\\s+(\\d+)";
        if (regex_search(output, match, pattern))
        {
            SwapFree = stoi(match[1]);
            if (pflag)
                cout << "Monitor: SwapFree: " << SwapFree << endl;
        }

        // 获取前台应用的内存使用情况
        commands = (char *)(("dumpsys meminfo " + packageName).data());
        output = zhixing(commands);
        istringstream iss(output);
        string line;
        while (getline(iss, line))
        {
            if (line.find("Native Heap") != string::npos)
            {
                pattern = "Native Heap\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+(\\d+)\\s+\\d+\\s+(\\d+)";
                if (regex_search(line, match, pattern))
                {
                    nativeHeapFreeSize = stoi(match[2]);
                    usedNativeMemPercent = (stoi(match[1]) - nativeHeapFreeSize + 0.0) / stoi(match[1]);
                    if (pflag)
                    {
                        cout << "Monitor: nativeHeapFreeSize: " << nativeHeapFreeSize << endl;
                        cout << "Monitor: usedNativeMemPercent: " << usedNativeMemPercent << endl;
                    }
                }
                continue;
            }
            else if (line.find("App Summary") != string::npos)
            {
                getline(iss, line);
                getline(iss, line);
                getline(iss, line);
                pattern = "Java Heap:\\s+(\\d+)";
                if (regex_search(line, match, pattern))
                {
                    FrontAPPJavaHeap = stoi(match[1]);
                    if (pflag)
                    {
                        cout << "Monitor: FrontAPPJavaHeap: " << FrontAPPJavaHeap << endl;
                    }
                }
                getline(iss, line);

                pattern = "Native Heap:\\s+(\\d+)";
                if (regex_search(line, match, pattern))
                {
                    FrontAPPNativeHeap = stoi(match[1]);
                    if (pflag)
                    {
                        cout << "Monitor: FrontAPPNativeHeap: " << FrontAPPNativeHeap << endl;
                    }
                }
                getline(iss, line);

                pattern = "Code:\\s+(\\d+)";
                if (regex_search(line, match, pattern))
                {
                    FrontAPPCodeMem = stoi(match[1]);
                    if (pflag)
                    {
                        cout << "Monitor: FrontAPPCodeMem: " << FrontAPPCodeMem << endl;
                    }
                }
                getline(iss, line);

                pattern = "Stack:\\s+(\\d+)";
                if (regex_search(line, match, pattern))
                {
                    FrontAPPStackMem = stoi(match[1]);
                    if (pflag)
                    {
                        cout << "Monitor: FrontAPPStackMem: " << FrontAPPStackMem << endl;
                    }
                }
                getline(iss, line);

                pattern = "Graphics:\\s+(\\d+)";
                if (regex_search(line, match, pattern))
                {
                    FrontAPPGraphicsMem = stoi(match[1]);
                    if (pflag)
                    {
                        cout << "Monitor: FrontAPPGraphicsMem: " << FrontAPPGraphicsMem << endl;
                    }
                }
                getline(iss, line);

                pattern = "Private Other:\\s+(\\d+)";
                if (regex_search(line, match, pattern))
                {
                    FrontAPPPrivateOtherMem = stoi(match[1]);
                    if (pflag)
                    {
                        cout << "Monitor: FrontAPPPrivateOtherMem: " << FrontAPPPrivateOtherMem << endl;
                    }
                }
                getline(iss, line);

                pattern = "System:\\s+(\\d+)";
                if (regex_search(line, match, pattern))
                {
                    FrontAPPSystemMem = stoi(match[1]);
                    if (pflag)
                    {
                        cout << "Monitor: FrontAPPSystemMem: " << FrontAPPSystemMem << endl;
                    }
                }
                break;
            }
        }

        // 读取整机PSI值
        commands = "echo \"cpu $(cat /proc/pressure/cpu)\"";
        output = zhixing(commands);
        pattern = "cpu some avg10=(\\d+.\\d+)\\s+avg60=(\\d+.\\d+)\\s+avg300=(\\d+.\\d+)\\s+total=(\\d+)";
        if (regex_search(output, match, pattern))
        {
            cpusome10 = stod(match[1]);
            if (pflag)
                cout << "Monitor: cpusome10: " << cpusome10 << endl;
        }

        commands = "echo \"mem $(cat /proc/pressure/memory)\"";
        output = zhixing(commands);
        pattern = "mem some avg10=(\\d+.\\d+)\\s+avg60=(\\d+.\\d+)\\s+avg300=(\\d+.\\d+)\\s+total=(\\d+)";
        if (regex_search(output, match, pattern))
        {
            memsome10 = stod(match[1]);
            if (pflag)
                cout << "Monitor: memsome10: " << memsome10 << endl;
        }

        commands = "echo \"io $(cat /proc/pressure/io)\"";
        output = zhixing(commands);
        pattern = "io some avg10=(\\d+.\\d+)\\s+avg60=(\\d+.\\d+)\\s+avg300=(\\d+.\\d+)\\s+total=(\\d+)";
        if (regex_search(output, match, pattern))
        {
            iosome10 = stod(match[1]);
            if (pflag)
                cout << "Monitor: iosome10: " << iosome10 << endl;
        }

        // 获取GPU使用率
        commands = "cat /sys/class/kgsl/kgsl-3d0/gpubusy";
        output = zhixing(commands);
        pattern = "(\\d+)\\s+(\\d+)";
        if (regex_search(output, match, pattern))
        {
            int did = stoi(match[2]);
            if (did == 0)
            {
                gpuUsage = 0;
            }
            else
            {
                gpuUsage = (stoi(match[1]) + 0.0) / did;
            }
            if (pflag)
                cout << "Monitor: gpuUsage: " << gpuUsage << endl;
        }

        // 直接获取GPU使用率百分比 和上面计算的结果基本一样
        commands = "echo \"$(cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage)GpuPercent\"";
        output = zhixing(commands);
        pattern = "(\\d+) %GpuPercent";
        if (regex_search(output, match, pattern))
        {
            gpuPercent = stoi(match[1]);
            if (pflag)
                cout << "Monitor: gpuPercent: " << gpuPercent << endl;
        }

        // 获取GPU频率
        commands = "echo \"$(cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq)GpuFreq\"";
        output = zhixing(commands);
        pattern = "(\\d+)GpuFreq";
        if (regex_search(output, match, pattern))
        {
            gpuFreq = stoi(match[1]);
            if (pflag)
                cout << "Monitor: gpuFreq: " << gpuFreq << endl;
        }

        // 获取掉帧信息,三条信息
        commands = "dumpsys SurfaceFlinger| grep missed";
        output = zhixing(commands);
        pattern = "GPU missed frame count: (\\d+)";
        if (regex_search(output, match, pattern))
        {
            int missed1 = stoi(match[1]);
            missedGPU = missed1 - missedGPUCounts;
            missedGPUCounts = missed1;
            if (pflag)
                cout << "Monitor: missedGPU: " << missedGPU << endl;
        }

        pattern = "HWC missed frame count: (\\d+)";
        if (regex_search(output, match, pattern))
        {
            int missed2 = stoi(match[1]);
            missedHWC = missed2 - missedHWCCounts;
            missedHWCCounts = missed2;
            if (pflag)
                cout << "Monitor: missedHWC: " << missedHWC << endl;
        }

        pattern = "Total missed frame count: (\\d+)";
        if (regex_search(output, match, pattern))
        {
            int missed3 = stoi(match[1]);
            missedTotal = missed3 - missedTotalMissed;
            missedTotalMissed = missed3;
            if (pflag)
                cout << "Monitor: missedTotal: " << missedTotal << endl;
        }

        // 计算Jank比例
        commands = (char *)("dumpsys gfxinfo " + packageName + "|grep -E 'frames|Vsync'").data();
        output = zhixing(commands);
        pattern = "Total frames rendered: (\\d+)";
        if (regex_search(output, match, pattern))
        {
            int nowTotalFrames = stoi(match[1]);
            TotalFrames = nowTotalFrames - lastTotalFrames;
            lastTotalFrames = nowTotalFrames;
            if (pflag)
                cout << "Monitor: TotalFrames: " << TotalFrames << endl;
        }

        pattern = "Number Missed Vsync: (\\d+)";
        if (regex_search(output, match, pattern))
        {
            int nowMissedVsync = stoi(match[1]);
            MissedVsync = nowMissedVsync - lastMissedVsync;
            lastMissedVsync = nowMissedVsync;
            if (pflag)
                cout << "Monitor: MissedVsync: " << MissedVsync << endl;
        }

        pattern = "Janky frames: (\\d+) [(]((100|([1-9]?\\d)(\\.\\d+)?)%)[)]";
        if (regex_search(output, match, pattern))
        {
            int nowJankyFrames = stoi(match[1]);
            JankyFrames = nowJankyFrames - lastJankyFrames;
            lastJankyFrames = nowJankyFrames;
            if (pflag)
                cout << "Monitor: JankyFrames: " << JankyFrames << endl;
        }

        if (TotalFrames != 0)
        {
            JankyRate = round(((JankyFrames + 0.0) / TotalFrames) * 10000) / 10000;
            if (pflag)
            {
                cout << "Monitor: JankyRate: " << JankyRate << endl;
            }
        }
        else
        {
            JankyRate = 1;
        }

        // 获取fps线程计算的帧率
        FPS = fpsthd.getFPS();
        jankRate = fpsthd.getJankRate();
        if (pflag)
        {
            cout << "FPSMonitor: FPS: " << this->FPS << endl;
            cout << "FPSMonitor: jankRate: " << this->jankRate << endl;
        }

        // 获取power线程计算的功耗
        // power = powerthd.getPower();
        power = getbatteryInfo();
        if (pflag)
        {
            cout << "PowerMonitor: power: " << this->power << endl;
        }
        // 获取CPU和电池温度
        commands = "dumpsys thermalservice| grep Temperature";
        output = zhixing(commands);
        map<string, double> temp;
        istringstream iss1(output);
        while (getline(iss1, line))
        {
            pattern = "mValue=(-?\\d+\\.\\d+), mType=\\d+, mName=(\\S+), mStatus=\\d+";
            if (regex_search(line, match, pattern))
            {
                string myName = match[2];
                if (myName.find("CPU") != string::npos || myName.find("battery") != string::npos)
                {
                    myName = regex_replace(myName, regex("CPU"), "tempCPU");
                    myName = regex_replace(myName, regex("battery"), "batteryTemp");
                    string myvalue = match[1];
                    temp[myName] = stod(myvalue);
                }
            }
        }
        tempCPU0 = temp["tempCPU0"];
        tempCPU1 = temp["tempCPU1"];
        tempCPU2 = temp["tempCPU2"];
        tempCPU3 = temp["tempCPU3"];
        tempCPU4 = temp["tempCPU4"];
        tempCPU5 = temp["tempCPU5"];
        tempCPU6 = temp["tempCPU6"];
        tempCPU7 = temp["tempCPU7"];
        batteryTemp = temp["batteryTemp"];
        if (pflag)
        {
            cout << "Monitor: tempCPU0: " << tempCPU0 << endl;
            cout << "Monitor: tempCPU1: " << tempCPU1 << endl;
            cout << "Monitor: tempCPU2: " << tempCPU2 << endl;
            cout << "Monitor: tempCPU3: " << tempCPU3 << endl;
            cout << "Monitor: tempCPU4: " << tempCPU4 << endl;
            cout << "Monitor: tempCPU5: " << tempCPU5 << endl;
            cout << "Monitor: tempCPU6: " << tempCPU6 << endl;
            cout << "Monitor: tempCPU7: " << tempCPU7 << endl;
            cout << "Monitor: batteryTemp: " << batteryTemp << endl;
        }

        // 获取热缓解状态
        commands = "dumpsys thermalservice| grep Thermal\\ Status";
        pattern = "Thermal Status: (\\d)";
        output = zhixing(commands);
        if (regex_search(output, match, pattern))
        {
            thermalStatus = stoi(match[1]);
            if (pflag)
            {
                cout << "Monitor: thermalStatus: " << thermalStatus << endl;
            }
        }

        // 获取网络状态
        commands = "dumpsys connectivity | grep Link\\ speed";
        pattern = "RSSI: (-?\\d+)";
        output = zhixing(commands);
        if (regex_search(output, match, pattern))
        {
            wifiRssi = stoi(match[1]);
            if (pflag)
            {
                cout << "Monitor: wifiRssi: " << wifiRssi << endl;
            }
        }
        pattern = "Link speed: (\\d+)Mbps";
        if (regex_search(output, match, pattern))
        {
            wifiSpeed = stoi(match[1]);
            if (pflag)
            {
                cout << "Monitor: wifiSpeed: " << wifiSpeed << endl;
            }
        }
        ostringstream oss;
        string str;
        // 是否写入csv文件
        if (wflag)
        {
            oss << timestamp << "," << packageName << "," << cpuUsage << "," << FrontAppCpuUsage << "," << FrontAppCpuCore << ","
                << FrontAppCpuFreq << "," << Cpu0Freq << "," << Cpu1Freq << "," << Cpu2Freq << "," << Cpu3Freq << ","
                << Cpu4Freq << "," << Cpu5Freq << "," << Cpu6Freq << "," << Cpu7Freq << "," << MemFree << ","
                << MemAvailable << "," << SwapFree << "," << threshold << "," << LowMemory << "," << nativeHeapFreeSize << ","
                << usedNativeMemPercent << "," << FrontAPPJavaHeap << "," << FrontAPPNativeHeap << "," << FrontAPPCodeMem << "," << FrontAPPStackMem << ","
                << FrontAPPGraphicsMem << "," << FrontAPPPrivateOtherMem << "," << FrontAPPSystemMem << "," << cpusome10 << "," << memsome10 << ","
                << iosome10 << "," << gpuUsage << "," << gpuFreq << "," << missedGPU << "," << missedHWC << ","
                << missedTotal << "," << TotalFrames << "," << MissedVsync << "," << JankyFrames << "," << JankyRate << ","
                << FPS << "," << jankRate << "," << tempCPU0 << "," << tempCPU1 << "," << tempCPU2 << ","
                << tempCPU3 << "," << tempCPU4 << "," << tempCPU5 << "," << tempCPU6 << "," << tempCPU7 << ","
                << batteryTemp << "," << thermalStatus << "," << wifiRssi << "," << wifiSpeed << "," << power << "\n";
            str = oss.str(); // 将 oss 中的内容转换为 std::string 类型
            // 打开文件
            ofstream file(dir_csv, ios::app);
            // 将字符串写入文件
            file << str;
            cout << "Monitor: 写入文件: " << dir_csv << " ,内容: " << str << endl;
            // 关闭文件
            file.close();
        }
        vector<string> result;
        string segment;
        istringstream tokenStream(str);
        while (getline(tokenStream, segment, ','))
        {
            result.push_back(segment);
        }
        return result;
    }

    void getFrontAPP2()
    {
        char *commands = "dumpsys window| grep CurrentFocus";
        string output = zhixing(commands);
        // regex pattern("mCurrentFocus=Window{([a-z]|\\d)+ u0 (com|tv)([a-zA-Z]|\\.|\\d)+/com([a-zA-Z]|\\.|\\d)+}");
        regex pattern("mCurrentFocus=Window\\{(([a-z]|\\d)+) u0 ((com|tv)([a-zA-Z]|\\.|\\d)+)/((com|tv)([a-zA-Z]|\\.|\\d)+)");
        smatch match;
        if (regex_search(output, match, pattern))
        { // 前台应用包名
            packageName = match[3];
            focus_window = match[6];
            focus_window = packageName + "/" + focus_window;
            if (!packageName.empty())
            {
                if (pflag)
                    cout << "Monitor: FrontAPPpackageName: " << packageName << endl;
            }
            else
            {
                cout << "Monitor: FrontAPPpackageName: error!" << endl;
                return;
            }
        }
        // 前台应用pid
        commands = (char *)("echo $(pidof " + packageName + ")FrontPid").data();
        output = zhixing(commands);
        pattern = "(\\d+)FrontPid";
        if (regex_search(output, match, pattern))
        {
            pid = stoi(match[1]);
        }
        if (pflag)
        {
            cout << "Monitor: FrontAppPid: " << pid << endl;
        }

        commands = "dumpsys SurfaceFlinger | grep -i focus -A 20";
        output = zhixing(commands);
        stringstream ss(output); // 使用字符串构造一个stringstream类型（流）数据
        char c = '\n';           // 设定好分隔符号(只能使用一个字符进行分割)
        string str;              // 用来接收每个分割的字符串
        getline(ss, str, c);
        getline(ss, str, c);
        getline(ss, str, c);
        focus_window = str;
        if (pflag)
            cout << "Monitor: MyCurrentFocus: " << focus_window << endl;
    }

    double getbatteryInfo()
    {
        double power = 0;
        char *commands = "dumpsys batterystats| grep Estimated\\ power -A 10";
        string output = zhixing(commands);
        regex pattern("screen:\\s+(\\d+.\\d+)");
        smatch match;
        if (regex_search(output, match, pattern))
        {
            power += stod(match[1]);
        }

        pattern = "cpu:\\s+(\\d+.\\d+)";
        if (regex_search(output, match, pattern))
        {
            power += stod(match[1]);
        }

        pattern = "bluetooth:\\s+(\\d+.\\d+)";
        if (regex_search(output, match, pattern))
        {
            power += stod(match[1]);
        }

        pattern = "system_services:\\s+(\\d+.\\d+)";
        if (regex_search(output, match, pattern))
        {
            power += stod(match[1]);
        }

        pattern = "mobile_radio:\\s+(\\d+.\\d+)";
        if (regex_search(output, match, pattern))
        {
            power += stod(match[1]);
        }

        pattern = "sensors:\\s+(\\d+.\\d+)";
        if (regex_search(output, match, pattern))
        {
            power += stod(match[1]);
        }

        pattern = "wifi:\\s+(\\d+.\\d+)";
        if (regex_search(output, match, pattern))
        {
            power += stod(match[1]);
        }

        pattern = "idle:\\s+(\\d+.\\d+)";
        if (regex_search(output, match, pattern))
        {
            power += stod(match[1]);
        }

        // 电量统计清零
        commands = "dumpsys batterystats --reset";
        output = zhixing(commands);

        return power;
    }
};

int main()
{
    // 初始化监控
    Monitor mo = Monitor(false, true, true);
    mo.print();
    // 停留几秒
    sleep(2);
    while (true)
    {
        // 创建新线程并执行 runMonitor() 方法
        thread t(&Monitor::runMonitor, &mo);
        // 等待 5 秒钟
        this_thread::sleep_for(milliseconds(2000));
        // 等待线程执行完毕
        t.join();
    }
    return 0;
}