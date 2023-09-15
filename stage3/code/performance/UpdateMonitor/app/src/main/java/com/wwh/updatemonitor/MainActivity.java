package com.wwh.updatemonitor;

import static androidx.core.app.ServiceCompat.stopForeground;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ServiceCompat;


import com.bigkoo.pickerview.builder.OptionsPickerBuilder;
import com.bigkoo.pickerview.listener.OnOptionsSelectListener;
import com.bigkoo.pickerview.view.OptionsPickerView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private Button btnMonitor, btnMonitor2;
    //Intent机制来协助应用间的交互与通讯
    private static Intent intent_monitor;
    //IP地址
    public String IP;
    //预测方式
    public int fore = 0;

    public ArrayList<AppInfo> appBeanList = new ArrayList<>();
    public static TextView textView;
    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
        setContentView(R.layout.activity_main);
        intent_monitor = new Intent(this, MonitorService.class);
        IP = getIp();
        //开始监控按钮
        appBeanList = getAllAppInfo(this, true);
        Log.d("get_allApp", "安装了" + String.valueOf(appBeanList.size()) + "个第三方APP");
        for (AppInfo bean : appBeanList) {
            Log.d("allApp", bean.package_name);
        }

        btnMonitor = findViewById(R.id.button);
        btnMonitor.setOnClickListener(this::onClick1);

        btnMonitor2 = findViewById(R.id.button2);
        btnMonitor2.setOnClickListener(this::onClick2);
        btnMonitor2.setEnabled(false);

        textView = findViewById(R.id.textView2);



        // 创建一个 SpannableString 对象，设置标题的颜色
        String titleText = "重要提示";
        SpannableString spannableString = new SpannableString(titleText);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(Color.RED);
        spannableString.setSpan(colorSpan, 0, titleText.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        // 弹出提示框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(spannableString);
        builder.setMessage("1.请使用USB线连接电脑与本设备\n2.使用”win+r+cmd”打开命令行窗口\n3.运行”adb root”以及”adb tcpip 5555”命令");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 点击确定后执行的操作
            }
        });
        builder.create().show();
    }



    private final List<String> options1Items = new ArrayList<>();

    public void onClick1(View v) {
        Button t = (Button) findViewById(R.id.button);
        if (t.getText().toString().equals("启用监控")) {
            OptionsPickerView pvOptions = new OptionsPickerBuilder(this, new OnOptionsSelectListener() {
                @Override
                public void onOptionsSelect(int options1, int options2, int options3, View v) {
                    String date = options1Items.get(options1);
                    if (Objects.equals(date, "使用云端预测(Server)")) {
                        Log.d("ADBLIB", "使用云端预测(Server)");
                        fore = 1;
                        t.setText("监控+云端预测");
                        Bundle bundle = new Bundle();
                        bundle.putString("fore", String.valueOf(fore));
                        intent_monitor.putExtras(bundle);
                        startForegroundService(intent_monitor);
                        t.setEnabled(false);
                        btnMonitor2.setEnabled(true);
                    } else if (Objects.equals(date, "使用本地预测(Local)")) {
                        Log.d("ADBLIB", "使用本地预测(Local)");
                        fore = 2;
                        t.setText("监控+本地预测");
                        Bundle bundle = new Bundle();
                        bundle.putString("fore", String.valueOf(fore));
                        intent_monitor.putExtras(bundle);
                        startForegroundService(intent_monitor);
                        t.setEnabled(false);
                        btnMonitor2.setEnabled(true);
                    } else if (Objects.equals(date, "不进行预测")) {
                        Log.d("ADBLIB", "不使用预测");
                        fore = 0;
                        t.setText("监控中...");
                        Bundle bundle = new Bundle();
                        bundle.putString("fore", String.valueOf(fore));
                        intent_monitor.putExtras(bundle);
                        startForegroundService(intent_monitor);
                        t.setEnabled(false);
                        btnMonitor2.setEnabled(true);
                    }
                    //在此获取选择到的内容
                }
            }).setTitleText("选择预测方式").setContentTextSize(25).setTitleSize(20).setOutSideCancelable(false).isDialog(true).build();
            pvOptions.setPicker(options1Items);
            pvOptions.show();
            //用于后台监控Service

        } else {
            // 停止前台Service
            stopService(new Intent(this,MonitorService.class));
            btnMonitor.setEnabled(true);
            btnMonitor2.setEnabled(false);
            btnMonitor.setText("启用监控");
        }
    }

    // 关闭前台Service
    private void onClick2(View view) {
        // 停止前台Service
        stopService(new Intent(this,MonitorService.class));
        btnMonitor.setEnabled(true);
        btnMonitor2.setEnabled(false);
        btnMonitor.setText("启用监控");

    }



    private void initData() {
        //加入第一个列表数据
        options1Items.add("使用云端预测(Server)");
        options1Items.add("使用本地预测(Local)");
        options1Items.add("不进行预测");
    }


    private String getIp() {
        WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //检查Wifi状态
        if (!wm.isWifiEnabled())
            wm.setWifiEnabled(true);
        WifiInfo wi = wm.getConnectionInfo();
        //获取32位整型IP地址
        int ipAdd = wi.getIpAddress();
        //把整型地址转换成“*.*.*.*”地址
        String ip = intToIp(ipAdd);
        Bundle bundle = new Bundle();
        bundle.putString("ipADDR", ip);
        intent_monitor.putExtras(bundle);
        //Log.d("IP_WLAN0",intent_monitor.getExtras().getString("ipADDR"));
        TextView f = (TextView) findViewById(R.id.textView);
        f.setText("已获取本机IP：" + ip);
        return ip;
    }

    private String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }

    /**
     * 获取手机已安装应用列表
     *
     * @param ctx
     * @param isFilterSystem 是否过滤系统应用
     * @return
     */
    private ArrayList<AppInfo> getAllAppInfo(Context ctx, boolean isFilterSystem) {
        ArrayList<AppInfo> appBeanList = new ArrayList<>();
        AppInfo bean = null;

        PackageManager packageManager = ctx.getPackageManager();
        List<PackageInfo> list = packageManager.getInstalledPackages(0);
        for (PackageInfo p : list) {
            bean = new AppInfo();
            // bean.setIcon(p.applicationInfo.loadIcon(packageManager));
            bean.setLabel(packageManager.getApplicationLabel(p.applicationInfo).toString());
            bean.setPackage_name(p.applicationInfo.packageName);
            int flags = p.applicationInfo.flags;
            // 判断是否是属于系统的apk
            if ((flags & ApplicationInfo.FLAG_SYSTEM) != 0 && isFilterSystem) {
                bean.setSystem(true);
            } else {
                bean.setSystem(false);
                appBeanList.add(bean);
            }
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable("appBeanList", appBeanList);
        intent_monitor.putExtras(bundle);
        return appBeanList;
    }

}

