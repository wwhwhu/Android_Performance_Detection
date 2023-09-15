package com.wwh.updatemonitor;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitorService extends ForegroundService{
    //IP地址
    public String IP;
    public int foreW = 0;
    public ArrayList<AppInfo> appBeanList;
    // 在Service中定义一个标识变量，用于标记线程是否应该继续运行
    private volatile boolean isRunning = true;
    private Thread thd;
    private TextUpdater textUpdater;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // t = intent;
        // Log.d("IP_WLAN1",t.getExtras().getString("ipADDR"));
        Log.d("WRITEEEE","Binder");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("WRITEEEE","StartCommand");
        Bundle exx=new Bundle();
        exx = intent.getExtras();
        if(exx!=null)
        {
            IP = exx.getString("ipADDR");
            foreW = Integer.parseInt(exx.getString("fore"));
           appBeanList = (ArrayList<AppInfo>) exx.getSerializable("appBeanList");
        }
        Log.d("IP_WLAN", "onStartCommand: "+IP);
        Log.d("allApp", String.valueOf(appBeanList.size()));
        // textUpdater = new TextUpdater(MainActivity.textView);

        // 在此处执行其他操作或设置前台Service的通知
        return super.onStartCommand(intent, flags, startId);
    }

    //不在是一个循环，而是执行一个新的线程
    @Override
    public void doSomething() throws InterruptedException {
        // 循环执行状态读取
        // 通过ADBLib执行指令
        List<String> commands = new ArrayList<>();
        //获取时间
        commands.add(" echo \"$(date '+%s')time\"");
        //获取前台应用package
        commands.add(" dumpsys window| grep mCurrentFocus");
        // 获取掉帧数量
        commands.add(" dumpsys SurfaceFlinger| grep missed");
        // 获取CPU和电池温度
        commands.add(" dumpsys thermalservice| grep Temperature");
        commands.add(" dumpsys thermalservice| grep 'Thermal Status'");
        // 获取Memory信息
        commands.add(" cat /proc/meminfo");
        // 获取GPU使用率与频率
        commands.add(" echo \"$(cat /sys/class/kgsl/kgsl-3d0/gpubusy)GpuUsage\"");
        commands.add(" echo \"$(cat /sys/class/kgsl/kgsl-3d0/devfreq/cur_freq)GpuFreq\"");
        // 获取CPU频率
        commands.add(" echo \"$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq)Cpu0Freq\"");
        commands.add(" echo \"$(cat /sys/devices/system/cpu/cpu1/cpufreq/scaling_cur_freq)Cpu1Freq\"");
        commands.add(" echo \"$(cat /sys/devices/system/cpu/cpu2/cpufreq/scaling_cur_freq)Cpu2Freq\"");
        commands.add(" echo \"$(cat /sys/devices/system/cpu/cpu3/cpufreq/scaling_cur_freq)Cpu3Freq\"");
        commands.add(" echo \"$(cat /sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq)Cpu4Freq\"");
        commands.add(" echo \"$(cat /sys/devices/system/cpu/cpu5/cpufreq/scaling_cur_freq)Cpu5Freq\"");
        commands.add(" echo \"$(cat /sys/devices/system/cpu/cpu6/cpufreq/scaling_cur_freq)Cpu6Freq\"");
        commands.add(" echo \"$(cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_cur_freq)Cpu7Freq\"");
        //  获取网络相关情况
        commands.add(" dumpsys connectivity| grep 'Link speed'");
        //  获取FPS,暂定
        //commands.add(" top -m 10 -s 9");
        //commands.add(" top -m 10 -s 10");

        ADBLIB adbl = new ADBLIB(this);
        // 获取结果
        File filesDir = getApplicationContext().getFilesDir();
        //Init 执行一次
        thd = new Thread(new Runnable() {
            @Override
            public void run() {
                if (adbl.openStream(filesDir,IP)) {
                    Log.d("ADBInfo", "Init");
//                    for(AppInfo app:appBeanList) {
//                        commands.add( " echo \"Target$( dumpsys meminfo "+app.package_name + "|grep 'TOTAL PSS')   Name:"+ app.package_name+ "\"");
//                        commands.add( " echo $(ps -p $(pidof "+app.package_name+") -o %cpu)"+app.package_name);
//                    }
                    adbl.runCommand(commands,appBeanList);
                    HashMap info = new HashMap();
                    try {
                        Thread.sleep(2 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!adbl.stream.isClosed()) {
                        adbl.closeStream(info, IP, false,0);
                    }
                    Log.d("ADBLIB", "Init Finished");
                    Log.d("ADBInfo", "Init Finished");
                }
            }
        });
        thd.start();
        // 休眠10s
        Thread.sleep(10 * 1000);
        while (isRunning) {
            thd = new Thread(new Runnable() {
                @Override
                public void run() {
                    if (adbl.openStream(filesDir,IP)) {
                        Log.d("ADBLIB", "StartMonitor: " + filesDir);
                        adbl.runCommand(commands,appBeanList);
                        // 获取内存信息
                        HashMap info = new HashMap();
                        getMemInfo(info);
                        Log.d("MAP", "result: " + info);
                        try {
                            Thread.sleep(5 * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (!adbl.stream.isClosed()) {
                            adbl.closeStream(info, IP, true,foreW);
                        }
                        Log.d("ADBLIB", "results saved in" + filesDir.toString());
                        Log.d("ADBInfo", "results saved in" + filesDir.toString()+"\n\n");
                    }
                }
            });
            thd.start();
            // 休眠10s
            Thread.sleep(10 * 1000);
        }
    }

    private void getMemInfo(Map info) {
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info2 = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(info2);
        //是否处于低内存状态
        info.put("LowMemory", String.valueOf(info2.lowMemory));
        //临界值
        info.put("threshold", String.valueOf(info2.threshold));
        //        List<String> commands = new ArrayList<>();
//        commands.add(" dumpsys meminfo " + foregroundPackageName + "|grep -A 13 Summary" + "\n");
//        Log.d("seeee"," dumpsys meminfo " + foregroundPackageName + "|grep -A 13 Summary");
//        获取前台应用信息
//        adbl.runCommand(commands, appList);
    }


    // 在Service中实现onDestroy方法，用于释放资源和停止线程
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        isRunning = false; // 设置标识变量为 false，通知线程停止循环
        // 等待线程结束
        try {
            // 需要在子线程中等待，避免阻塞主线程
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (thd.isAlive()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    stopSelf();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
