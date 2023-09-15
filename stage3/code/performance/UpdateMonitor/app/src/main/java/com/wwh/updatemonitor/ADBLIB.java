package com.wwh.updatemonitor;

import android.content.Context;
import android.os.Build;
import android.os.Trace;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cgutman.adblib.AdbBase64;
import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbStream;

import org.apache.commons.codec.binary.Base64;
import org.jpmml.evaluator.Evaluator;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;


public class ADBLIB {
    private static String TAG = "ADBLIB";
    private String pak = "";//存放前台app包名
    private static String pid;//存放前台pid
    public ArrayList<AppInfo> appBeanList1;
    public TextUpdater textUp;

    AdbConnection adb;
    AdbStream stream;
    private File fileDir;
    private int memtotal;
    // 存放全局监控信息
    private Map parsedResults = new HashMap();

    private static int missedHWCCounts = 0;
    private static int missedGPUCounts = 0;
    private static int missedTotalCounts = 0;
    private static int cpuLastIdle = 0;
    private static int cpuLast = 0;
    private static int FrontApptime = 0;
    private static int lastTotalFrames = 0;
    private static int lastMissedVsync = 0;
    private static int lastJankyFrames = 0;

    private static int interval;
    private static boolean sinterval = false;

//    private LinkedList<Double> foreLinkedlist = new LinkedList<Double>();
//    //存放最近12个预测帧数
//    private LinkedList<Double> MissedLinkedlist = new LinkedList<Double>();

    private Context mContext;

    public ADBLIB(Context context) {
        mContext = context;
        textUp = new TextUpdater(MainActivity.textView);
    }

    public static AdbBase64 getBase64Impl() {
        return new AdbBase64() {
            @Override
            public String encodeToString(byte[] arg0) {
                return Base64.encodeBase64String(arg0);
            }
        };
    }

    public boolean openStream(File Dir, String IP) {
        Socket sock;
        AdbCrypto crypto;
        fileDir = Dir;
        //region
        // Setup the crypto object required for the AdbConnection
        Log.d(TAG, "setup keys ");
        try {
            crypto = setupCrypto(fileDir + "/pub.key", fileDir + "/priv.key");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        // Connect the socket to the remote host
        Log.d(TAG, "Socket connecting... ");
        Log.d(TAG, "IP: " + IP);
        try {
            InetAddress IPAddr = InetAddress.getByName(IP);
            sock = new Socket(IPAddr, 5555);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Log.d(TAG, "Socket connected");

        // Construct the AdbConnection object
        try {
            adb = AdbConnection.create(sock, crypto);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Start the application layer connection process
        Log.d(TAG, "ADB connecting");
        try {
            adb.connect();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        Log.d(TAG, "ADB connected");

        // Open the shell stream of ADB
        // Log.d(TAG, "runCommand: open shell stream");
        try {
            stream = adb.open("shell:");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Log.d(TAG, "error: " + e.toString());
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "error: " + e.toString());
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.d(TAG, "error: " + e.toString());
            return false;
        }
        //endregion
        // 开启接收线程
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 0;
                while (!stream.isClosed()) {
                    // Print each thing we read from the shell stream
                    String results = null;
                    try {
                        byte[] bytes = stream.read();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                            results = new String(bytes, StandardCharsets.US_ASCII);
                        }
                        Log.d(TAG, "raw result: " + results);
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                    if (results != null)
                        parseLog(results);
                }
            }
        }).start();
        return true;
    }

    // This function loads a keypair from the specified files if one exists, and if not,
    // it creates a new keypair and saves it in the specified files
    private static AdbCrypto setupCrypto(String pubKeyFile, String privKeyFile)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        File pub = new File(pubKeyFile);
        File priv = new File(privKeyFile);
        AdbCrypto c = null;
        // Try to load a key pair from the files
        if (pub.exists() && priv.exists()) {
            try {
                c = AdbCrypto.loadAdbKeyPair(ADBLIB.getBase64Impl(), priv, pub);
            } catch (IOException e) {
                // Failed to read from file
                c = null;
            } catch (InvalidKeySpecException e) {
                // Key spec was invalid
                c = null;
            } catch (NoSuchAlgorithmException e) {
                // RSA algorithm was unsupported with the crypo packages available
                c = null;
            }
        }

        if (c == null) {
            // We couldn't load a key, so let's generate a new one
            c = AdbCrypto.generateAdbKeyPair(ADBLIB.getBase64Impl());

            // Save it
            c.saveAdbKeyPair(priv, pub);
            System.out.println("Generated new keypair");
        } else {
            System.out.println("Loaded existing keypair");
        }
        return c;
    }

    private void parseLog(String results) {
        //匹配时间戳
        //region
        String patternStrTime = "(\\d+)time";
        Pattern patternTime = Pattern.compile(patternStrTime);
        Matcher matcherTime = patternTime.matcher(results);
        if (matcherTime.find()) {
            String time = matcherTime.group(1);
            parsedResults.put("timestamp", time);
            Log.d("ADBInfo", "timestamp: " + time);
        }
        //endregion

        //获取前台应用名称与pid，调用分析FPS与内存信息
        String patternStr10 = "mCurrentFocus=Window\\{([a-z]|\\d)+\\su0\\scom([a-zA-Z]|\\.|\\d)+/com([a-zA-Z]|\\.|\\d)+\\}";
        Pattern pattern10 = Pattern.compile(patternStr10);
        Matcher matcher10 = pattern10.matcher(results);
        if (matcher10.find()) {
            pak = matcher10.group().trim().split(" ")[2].split("/")[0];
            if (pak.length() > 1 && pak.charAt(pak.length() - 1) == '}') {
                pak = pak.substring(0, pak.length() - 1);
            }
            if (pak != null && pak.trim().length() > 0) {
                Log.d("ADBInfo", "FrontAPP: " + pak);
                parsedResults.put("FrontAPP", pak);
                try {
                    stream.write(" echo \"$(pidof " + pak + ")FrontPid\"\n");
                    stream.write(" dumpsys meminfo " + pak + "\n");
                    stream.write(" dumpsys gfxinfo " + pak + "|grep -e 'frames' -e 'Number Missed Vsync'" + "\n");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //匹配CPU信息计算CPU利用率
        //region
        String patternStr5 = "cpu\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)";
        Pattern pattern5 = Pattern.compile(patternStr5);
        Matcher matcher5 = pattern5.matcher(results);
        if (matcher5.find()) {
            int cpuUser = Integer.parseInt(matcher5.group(1));
            int cpuNice = Integer.parseInt(matcher5.group(2));
            int cpuSystem = Integer.parseInt(matcher5.group(3));
            int cpuIdle = Integer.parseInt(matcher5.group(4));
            int cpuIowait = Integer.parseInt(matcher5.group(5));
            int cpuIrq = Integer.parseInt(matcher5.group(6));
            int cpuSoftirq = Integer.parseInt(matcher5.group(7));
            int cpuSteal = Integer.parseInt(matcher5.group(8));
            int cpuGuest = Integer.parseInt(matcher5.group(9));
            int cpuGnice = Integer.parseInt(matcher5.group(10));
            parsedResults.put("cpuUser", matcher5.group(1));
            parsedResults.put("cpuNice", matcher5.group(2));
            parsedResults.put("cpuSystem", matcher5.group(3));
            parsedResults.put("cpuIdle", matcher5.group(4));
            parsedResults.put("cpuIowait", matcher5.group(5));
            parsedResults.put("cpuIrq", matcher5.group(6));
            parsedResults.put("cpuSoftirq", matcher5.group(7));
            parsedResults.put("cpuSteal", matcher5.group(8));
            parsedResults.put("cpuGuest", matcher5.group(9));
            parsedResults.put("cpuGnice", matcher5.group(10));
            Log.d("ADBInfo", "cpuUser: " + matcher5.group(1));
            Log.d("ADBInfo", "cpuNice: " + matcher5.group(2));
            Log.d("ADBInfo", "cpuSystem: " + matcher5.group(3));
            Log.d("ADBInfo", "cpuIdle: " + matcher5.group(4));
            Log.d("ADBInfo", "cpuIowait: " + matcher5.group(5));
            Log.d("ADBInfo", "cpuIrq: " + matcher5.group(6));
            Log.d("ADBInfo", "cpuSoftirq: " + matcher5.group(7));
            Log.d("ADBInfo", "cpuSteal: " + matcher5.group(8));
            Log.d("ADBInfo", "cpuGuest: " + matcher5.group(9));
            Log.d("ADBInfo", "cpuGnice: " + matcher5.group(10));
            int now = cpuUser + cpuNice + cpuSystem + cpuIdle + cpuIowait + cpuIrq + cpuSoftirq + cpuSteal + cpuGuest + cpuGnice;
            interval = now - cpuLast;
            double cpuUsage = 1 - (cpuIdle - cpuLastIdle) / (interval + 0.0);
            cpuLast = now;
            cpuLastIdle = cpuIdle;
            sinterval = true;
            parsedResults.put("cpuUsage", String.valueOf(cpuUsage));
            Log.d("ADBInfo", "cpuUsage: " + String.valueOf(cpuUsage));
        }
        //计算前台CPU平均占用率
        String patternStrFrontCpuUsage = pid + " \\(([a-zA-Z]|\\.)*\\) [a-zA-Z] \\d+ \\d+ \\d+ \\d+ -\\d+ \\d+ \\d+ \\d+ \\d+ \\d+ (\\d+) (\\d+) ";
        Pattern patternFrontCpuUsage = Pattern.compile(patternStrFrontCpuUsage);
        Matcher matcherFrontCpuUsage = patternFrontCpuUsage.matcher(results);
        if (matcherFrontCpuUsage.find()) {
            parsedResults.put("FrontApputime", matcherFrontCpuUsage.group(2));
            Log.d("ADBInfo", "FrontApputime: " + matcherFrontCpuUsage.group(2));
            parsedResults.put("FrontAppstime", matcherFrontCpuUsage.group(3));
            Log.d("ADBInfo", "FrontAppstime: " + matcherFrontCpuUsage.group(3));
            int FrontApptimeNow = Integer.parseInt(matcherFrontCpuUsage.group(2)) + Integer.parseInt(matcherFrontCpuUsage.group(3));
            if (sinterval) {
                double FrontAppCpuUsage = (FrontApptimeNow - FrontApptime) / (interval + 0.0);
                parsedResults.put("FrontAppCpuUsage", String.valueOf(FrontAppCpuUsage));
                Log.d("ADBInfo", "FrontAppCpuUsage: " + String.valueOf(FrontAppCpuUsage));
                sinterval = false;
            }
            FrontApptime = FrontApptimeNow;
        }
        //endregion

        //获取前台pid
        //region
        String patternStrFrontPid = "(\\d+)FrontPid";
        Pattern patternFrontPid = Pattern.compile(patternStrFrontPid);
        Matcher matcherFrontPid = patternFrontPid.matcher(results);
        if (matcherFrontPid.find()) {
            pid = matcherFrontPid.group(1);
            Log.d("ADBInfo", "FrontAppPid: " + pid);
            try {
                // 获取CPU运行时间
                stream.write(" cat /proc/stat" + "\n");
                stream.write(" cat proc/" + pid + "/stat" + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //endregion

        //匹配掉帧信息
        //region
        String patternStr1 = "HWC missed frame count: \\d+";
        String patternStr2 = "GPU missed frame count: \\d+";
        String patternStr0 = "Total missed frame count: \\d+";
        Pattern pattern1 = Pattern.compile(patternStr1);
        Pattern pattern2 = Pattern.compile(patternStr2);
        Pattern pattern0 = Pattern.compile(patternStr0);
        Matcher matcher1 = pattern1.matcher(results);
        Matcher matcher2 = pattern2.matcher(results);
        Matcher matcher0 = pattern0.matcher(results);
        if (matcher0.find()) {
            String missedTotalCount = matcher0.group().replaceAll("Total missed frame count: ", "");
            int missedTotal = Integer.parseInt(missedTotalCount) - missedTotalCounts;
            missedTotalCounts = Integer.parseInt(missedTotalCount);
            parsedResults.put("Total missed", String.valueOf(missedTotal));
            Log.d("ADBInfo", "Total missed: " + missedTotal);
        }
        if (matcher1.find()) {
            String missedHwcCount = matcher1.group().replaceAll("HWC missed frame count: ", "");
            int missedHwc = Integer.parseInt(missedHwcCount) - missedHWCCounts; // 计算掉帧数量差
            missedHWCCounts = Integer.parseInt(missedHwcCount); // 记录掉帧数
            parsedResults.put("HWC missed", String.valueOf(missedHwc));
            Log.d("ADBInfo", "HWC missed: " + missedHwc);
        }
        if (matcher2.find()) {
            String missedGpuCount = matcher2.group().replaceAll("GPU missed frame count: ", "");
            int missedGpu = Integer.parseInt(missedGpuCount) - missedGPUCounts;
            missedGPUCounts = Integer.parseInt(missedGpuCount);
            parsedResults.put("GPU missed", String.valueOf(missedGpu));
            Log.d("ADBInfo", "GPU missed: " + missedGpu);
        }

        //匹配前台应用帧率相关信息
        String patternStrMissedVsync = "Number Missed Vsync: (\\d+)";
        Pattern patternMissedVsync = Pattern.compile(patternStrMissedVsync);
        Matcher matcherMissedVsync = patternMissedVsync.matcher(results);
        if (matcherMissedVsync.find()) {
            int missedvsy = Integer.parseInt(matcherMissedVsync.group(1)) - lastMissedVsync;
            Log.d("ADBInfo", "MissedVsync: " + String.valueOf(missedvsy));
            parsedResults.put("MissedVsync", String.valueOf(missedvsy));
            lastMissedVsync = Integer.parseInt(matcherMissedVsync.group(1));
        }
        String patternStrJanky = "Janky frames: (\\d+) [(]((100|([1-9]?\\d)(\\.\\d+)?)%)[)]";
        Pattern patternJanky = Pattern.compile(patternStrJanky);
        Matcher matcherJanky = patternJanky.matcher(results);
        String patternStrTotalFrames = "Total frames rendered: (\\d+)";
        Pattern patternTotalFrames = Pattern.compile(patternStrTotalFrames);
        Matcher matcherTotalFrames = patternTotalFrames.matcher(results);
        if (matcherJanky.find() && matcherTotalFrames.find()) {
            int jankf = Integer.parseInt(matcherJanky.group(1)) - lastJankyFrames;
            parsedResults.put("JankyNum", String.valueOf(jankf));
            int totalf = Integer.parseInt(matcherTotalFrames.group(1)) - lastTotalFrames;
            parsedResults.put("TotalFrames", String.valueOf(totalf));
            Log.d("ADBInfo", "JankyNum: " + String.valueOf(jankf));
            Log.d("ADBInfo", "TotalFrames: " + String.valueOf(totalf));
            lastJankyFrames = Integer.parseInt(matcherJanky.group(1));
            lastTotalFrames = Integer.parseInt(matcherTotalFrames.group(1));
        }
        //endregion

        //匹配MemFree,MemAvailable全局内存
        //region
        String patternStr3 = "MemFree:\\s+\\d+";
        Pattern pattern3 = Pattern.compile(patternStr3);
        Matcher matcher3 = pattern3.matcher(results);
        if (matcher3.find()) {
            String memFree = matcher3.group().replaceAll("MemFree:\\s+", "");
            parsedResults.put("MemFree", memFree);
            Log.d("ADBInfo", "MemFree: " + memFree);
        }
        String patternStr4 = "MemAvailable:\\s+\\d+";
        Pattern pattern4 = Pattern.compile(patternStr4);
        Matcher matcher4 = pattern4.matcher(results);
        if (matcher4.find()) {
            String memAvail = matcher4.group().replaceAll("MemAvailable:\\s+", "");
            parsedResults.put("MemAvailable", memAvail);
            Log.d("ADBInfo", "MemAvailable: " + memAvail);
        }
        String patternStrMemTotal = "MemTotal:\\s+\\d+";
        Pattern patternMemTotal = Pattern.compile(patternStrMemTotal);
        Matcher matcherMemTotal = patternMemTotal.matcher(results);
        if (matcherMemTotal.find()) {
            String memMemTotal = matcherMemTotal.group().replaceAll("MemTotal:\\s+", "");
            parsedResults.put("MemTotal", memMemTotal);
            memtotal = Integer.parseInt(memMemTotal);
            Log.d("ADBInfo", "MemTotal: " + memMemTotal);
        }
        //endregion

        //匹配前台应用的内存信息
        //region
        String patternStrNative = "Native Heap\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+(\\d+)\\s+\\d+\\s+(\\d+)";
        Pattern patternNative = Pattern.compile(patternStrNative);
        Matcher matcherNative = patternNative.matcher(results);
        if (matcherNative.find()) {
            String nativeHeapFreeSize = matcherNative.group(2);
            Log.d("ADBInfo", "nativeHeapFreeSize: " + nativeHeapFreeSize);
            parsedResults.put("nativeHeapFreeSize", nativeHeapFreeSize);
            double usedNativeMemPercent = (Integer.parseInt(matcherNative.group(1)) - Integer.parseInt(nativeHeapFreeSize)) / (Integer.parseInt(matcherNative.group(1)) + 0.0);
            Log.d("ADBInfo", "usedNativeMemPercent: " + String.valueOf(usedNativeMemPercent));
            parsedResults.put("usedNativeMemPercent", String.valueOf(usedNativeMemPercent));
        }
        String patternStrFrontMemJavaHeap = "Java Heap:\\s+(\\d+)";
        Pattern patternFrontMemJavaHeap = Pattern.compile(patternStrFrontMemJavaHeap);
        Matcher matcherFrontMemJavaHeap = patternFrontMemJavaHeap.matcher(results);
        if (matcherFrontMemJavaHeap.find()) {
            Log.d("ADBInfo", "FrontAPPJavaHeap: " + matcherFrontMemJavaHeap.group(1));
            parsedResults.put("FrontAPPJavaHeap", matcherFrontMemJavaHeap.group(1));
        }
        String patternStrFrontMemNativeHeap = "Native Heap:\\s+(\\d+)";
        Pattern patternFrontMemNativeHeap = Pattern.compile(patternStrFrontMemNativeHeap);
        Matcher matcherFrontMemNativeHeap = patternFrontMemNativeHeap.matcher(results);
        if (matcherFrontMemNativeHeap.find()) {
            Log.d("ADBInfo", "FrontAPPNativeHeap: " + matcherFrontMemNativeHeap.group(1));
            parsedResults.put("FrontAPPNativeHeap", matcherFrontMemNativeHeap.group(1));
        }
        String patternStrFrontMemCode = "Code:\\s+(\\d+)";
        Pattern patternFrontMemCode = Pattern.compile(patternStrFrontMemCode);
        Matcher matcherFrontMemCode = patternFrontMemCode.matcher(results);
        if (matcherFrontMemCode.find()) {
            Log.d("ADBInfo", "FrontAPPCodeMem: " + matcherFrontMemCode.group(1));
            parsedResults.put("FrontAPPCodeMem", matcherFrontMemCode.group(1));
        }
        String patternStrFrontMemStack = " Stack:\\s+(\\d+)";
        Pattern patternFrontMemStack = Pattern.compile(patternStrFrontMemStack);
        Matcher matcherFrontMemStack = patternFrontMemStack.matcher(results);
        if (matcherFrontMemStack.find()) {
            Log.d("ADBInfo", "FrontAPPStackMem: " + matcherFrontMemStack.group(1));
            parsedResults.put("FrontAPPStackMem", matcherFrontMemStack.group(1));
        }

        String patternStrFrontMemGraphics = "Graphics:\\s+(\\d+)";
        Pattern patternFrontMemGraphics = Pattern.compile(patternStrFrontMemGraphics);
        Matcher matcherFrontMemGraphics = patternFrontMemGraphics.matcher(results);
        if (matcherFrontMemGraphics.find()) {
            Log.d("ADBInfo", "FrontAPPGraphicsMem: " + matcherFrontMemGraphics.group(1));
            parsedResults.put("FrontAPPGraphicsMem", matcherFrontMemGraphics.group(1));
        }

        String patternStrFrontMemPrivateOther = " Private Other:\\s+(\\d+)";
        Pattern patternFrontMemPrivateOther = Pattern.compile(patternStrFrontMemPrivateOther);
        Matcher matcherFrontMemPrivateOther = patternFrontMemPrivateOther.matcher(results);
        if (matcherFrontMemPrivateOther.find()) {
            Log.d("ADBInfo", "FrontAPPPrivateOtherMem: " + matcherFrontMemPrivateOther.group(1));
            parsedResults.put("FrontAPPPrivateOtherMem", matcherFrontMemPrivateOther.group(1));
        }

        String patternStrFrontMemSystem = "System:\\s+(\\d+)";
        Pattern patternFrontMemSystem = Pattern.compile(patternStrFrontMemSystem);
        Matcher matcherFrontMemSystem = patternFrontMemSystem.matcher(results);
        if (matcherFrontMemSystem.find()) {
            Log.d("ADBInfo", "FrontAPPSystemMem: " + matcherFrontMemSystem.group(1));
            parsedResults.put("FrontAPPSystemMem", matcherFrontMemSystem.group(1));
        }
        //endregion

        //匹配网络
        //region
        String patternStrNet = "RSSI: (-?\\d+),\\s+Link speed:\\s+(\\d+)Mbps";
        Pattern patternNet = Pattern.compile(patternStrNet);
        Matcher matcherNet = patternNet.matcher(results);
        if (matcherNet.find()) {
            String wifiRssi = matcherNet.group(1);
            parsedResults.put("wifiRssi", wifiRssi);
            Log.d("ADBInfo", "wifiRssi: " + wifiRssi);
            String wifiSpeed = matcherNet.group(2);
            parsedResults.put("wifiSpeed", wifiSpeed);
            Log.d("ADBInfo", "wifiSpeed: " + wifiSpeed);
        }
        //endregion

        //CPU频率
        //region
        String patternStrcpu0Freq = "(\\d+)Cpu0Freq";
        Pattern patterncpu0Freq = Pattern.compile(patternStrcpu0Freq);
        Matcher matchercpu0Freq = patterncpu0Freq.matcher(results);
        if (matchercpu0Freq.find()) {
            String cpu0Freq = matchercpu0Freq.group(1);
            parsedResults.put("cpu0Freq", cpu0Freq);
            Log.d("ADBInfo", "cpu0Freq: " + cpu0Freq);
        }
        String patternStrcpu1Freq = "(\\d+)Cpu1Freq";
        Pattern patterncpu1Freq = Pattern.compile(patternStrcpu1Freq);
        Matcher matchercpu1Freq = patterncpu1Freq.matcher(results);
        if (matchercpu1Freq.find()) {
            String cpu1Freq = matchercpu1Freq.group(1);
            parsedResults.put("cpu1Freq", cpu1Freq);
            Log.d("ADBInfo", "cpu1Freq: " + cpu1Freq);
        }
        String patternStrcpu2Freq = "(\\d+)Cpu2Freq";
        Pattern patterncpu2Freq = Pattern.compile(patternStrcpu2Freq);
        Matcher matchercpu2Freq = patterncpu2Freq.matcher(results);
        if (matchercpu2Freq.find()) {
            String cpu2Freq = matchercpu2Freq.group(1);
            parsedResults.put("cpu2Freq", cpu2Freq);
            Log.d("ADBInfo", "cpu2Freq: " + cpu2Freq);
        }
        String patternStrcpu3Freq = "(\\d+)Cpu3Freq";
        Pattern patterncpu3Freq = Pattern.compile(patternStrcpu3Freq);
        Matcher matchercpu3Freq = patterncpu3Freq.matcher(results);
        if (matchercpu3Freq.find()) {
            String cpu3Freq = matchercpu3Freq.group(1);
            parsedResults.put("cpu3Freq", cpu3Freq);
            Log.d("ADBInfo", "cpu3Freq: " + cpu3Freq);
        }
        String patternStrcpu4Freq = "(\\d+)Cpu4Freq";
        Pattern patterncpu4Freq = Pattern.compile(patternStrcpu4Freq);
        Matcher matchercpu4Freq = patterncpu4Freq.matcher(results);
        if (matchercpu4Freq.find()) {
            String cpu4Freq = matchercpu4Freq.group(1);
            parsedResults.put("cpu4Freq", cpu4Freq);
            Log.d("ADBInfo", "cpu4Freq: " + cpu4Freq);
        }
        String patternStrcpu5Freq = "(\\d+)Cpu5Freq";
        Pattern patterncpu5Freq = Pattern.compile(patternStrcpu5Freq);
        Matcher matchercpu5Freq = patterncpu5Freq.matcher(results);
        if (matchercpu5Freq.find()) {
            String cpu5Freq = matchercpu5Freq.group(1);
            parsedResults.put("cpu5Freq", cpu5Freq);
            Log.d("ADBInfo", "cpu5Freq: " + cpu5Freq);
        }
        String patternStrcpu6Freq = "(\\d+)Cpu6Freq";
        Pattern patterncpu6Freq = Pattern.compile(patternStrcpu6Freq);
        Matcher matchercpu6Freq = patterncpu6Freq.matcher(results);
        if (matchercpu6Freq.find()) {
            String cpu6Freq = matchercpu6Freq.group(1);
            parsedResults.put("cpu6Freq", cpu6Freq);
            Log.d("ADBInfo", "cpu6Freq: " + cpu6Freq);
        }
        String patternStrcpu7Freq = "(\\d+)Cpu7Freq";
        Pattern patterncpu7Freq = Pattern.compile(patternStrcpu7Freq);
        Matcher matchercpu7Freq = patterncpu7Freq.matcher(results);
        if (matchercpu7Freq.find()) {
            String cpu7Freq = matchercpu7Freq.group(1);
            parsedResults.put("cpu7Freq", cpu7Freq);
            Log.d("ADBInfo", "cpu7Freq: " + cpu7Freq);
        }
        //endregion

        // 匹配GPU信息
        //region
        String patternStr8 = "\\s*(\\d+)\\s+(\\d+)GpuUsage";
        Pattern pattern8 = Pattern.compile(patternStr8);
        Matcher matcher8 = pattern8.matcher(results);
        if (matcher8.find()) {
            double a = Double.parseDouble(matcher8.group(1));
            double b = Double.parseDouble(matcher8.group(2));
            if (b != 0 && a != 0 && a < b) {
                parsedResults.put("gpuUsage", new DecimalFormat("#.000").format(a / b));
                Log.d("ADBInfo", "gpuUsage: " + new DecimalFormat("#.000").format(a / b));
            } else if (a == 0 && b == 0) {
                parsedResults.put("gpuUsage", "0.000");
                Log.d("ADBInfo", "gpuUsage: " + "0.000");
            }
        }
        String patternStr9 = "(\\d+)GpuFreq";
        Pattern pattern9 = Pattern.compile(patternStr9);
        Matcher matcher9 = pattern9.matcher(results);
        if (matcher9.find()) {
            Log.d("ADBInfo", "GpuFreq：" + matcher9.group(1));
            parsedResults.put("GpuFreq", matcher9.group(1));
        }
        //endregion

        // 匹配温度信息
        //region
        String patternStrthermalStatus = "Thermal Status: (\\d)";
        Pattern patternthermalStatus = Pattern.compile(patternStrthermalStatus);
        Matcher matcherthermalStatus = patternthermalStatus.matcher(results);
        if (matcherthermalStatus.find()) {
            Log.d("ADBInfo", "thermalStatus: " + matcherthermalStatus.group(1));
            parsedResults.put("thermalStatus", matcherthermalStatus.group(1));
        }


        String patternStr6 = "mValue=\\d+\\.\\d+, mType=\\d+, mName=\\S+, mStatus=\\d+";
        Pattern pattern6 = Pattern.compile(patternStr6);
        Matcher matcher6 = pattern6.matcher(results);
        String nameBattery = "mName=battery";
        Pattern nameB = Pattern.compile(nameBattery);
        String nameCpu = "mName=CPU\\d";
        Pattern nameC = Pattern.compile(nameCpu);
        String mValue = "mValue=\\d+\\.\\d+";
        Pattern value = Pattern.compile(mValue);
        if (matcher6.find()) {
            String[] split = results.split("\n");
            for (int i = 0; i < split.length; i++) {
                String res1 = split[i];
                //Log.d(TAG, "Temperature: " + res1);
                Matcher matchB = nameB.matcher(res1);
                Matcher matchC = nameC.matcher(res1);
                Matcher matchM = value.matcher(res1);
                if (matchM.find()) {
                    String temp = matchM.group().replaceAll("mValue=", "");
                    if (matchB.find()) {
                        parsedResults.put("batteryTemp", temp);
                    } else if (matchC.find()) {
                        String cpuName = matchC.group().replaceAll("mName=", "temp");
                        parsedResults.put(cpuName, temp);
                    }
                }
            }
            Log.d("ADBInfo", "batteryTemp: " + parsedResults.get("batteryTemp"));
            Log.d("ADBInfo", "tempCPU0: " + parsedResults.get("tempCPU0"));
            Log.d("ADBInfo", "tempCPU1: " + parsedResults.get("tempCPU1"));
            Log.d("ADBInfo", "tempCPU2: " + parsedResults.get("tempCPU2"));
            Log.d("ADBInfo", "tempCPU3: " + parsedResults.get("tempCPU3"));
            Log.d("ADBInfo", "tempCPU4: " + parsedResults.get("tempCPU4"));
            Log.d("ADBInfo", "tempCPU5: " + parsedResults.get("tempCPU5"));
            Log.d("ADBInfo", "tempCPU6: " + parsedResults.get("tempCPU6"));
            Log.d("ADBInfo", "tempCPU7: " + parsedResults.get("tempCPU7"));
        }
        //endregion

        //匹配前台应用的内存
        //String patternStrCPUEach = "Target\\s+TOTAL PSS:\\s+(\\d+)\\s+TOTAL RSS:\\s+(\\d+)\\s+TOTAL SWAP PSS:\\s+(\\d+)\\s+Name:(([a-zA-Z0-9]|.)*)$";
//        String patternStrmemEach = "Target\\s+TOTAL PSS:\\s+(\\d+)\\s+TOTAL RSS:\\s+(\\d+)\\s+TOTAL SWAP PSS:\\s+(\\d+)\\s+Name:(([a-zA-Z0-9]|.)*)$";
//        Pattern patternmemEach = Pattern.compile(patternStrmemEach);
//        Matcher matchermemEach = patternmemEach.matcher(results);
//        if (matchermemEach.find()) {
//            float usage = Float.parseFloat(matchermemEach.group(1)) / memtotal;
//            Log.d("BackAPP", matchermemEach.group(4) + "  MemUsage: " + usage);
//            for (AppInfo app : appBeanList1) {
//                if (Objects.equals(app.getPackage_name(), matchermemEach.group(4)))
//                    app.setMemUsage(usage);
//            }
//        }

//        String patternStrCPUEach = "%CPU//s+(//d+.//d+)(([a-zA-Z0-9]|.)*)$";
//        Pattern patternCPUEach = Pattern.compile(patternStrCPUEach);
//        Matcher matcherCPUEach = patternCPUEach.matcher(results);
//        if (matcherCPUEach.find()) {
//            float usage = Float.parseFloat(matcherCPUEach.group(1)) / 800;
//            Log.d("BackAPP", matcherCPUEach.group(2) + "  CpuUsage: " + usage);
//            for (AppInfo app : appBeanList1) {
//                if (Objects.equals(app.getPackage_name(), matcherCPUEach.group(2)))
//                    app.setCpuUsage(usage);
//            }
//        }
        //匹配前台应用的CPU
    }

    public void closeStream(Map info, String IP, boolean t, int foreW) {
        if (t) {
            // 将本次接收的数据写到文件再关闭
            writeLog(info, IP, foreW);
        }
        parsedResults.clear();
        try {
            stream.close();
            Log.d(TAG, "closeStream");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            adb.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean runCommand(List cmds, ArrayList<AppInfo> appBeanList) {
        appBeanList1 = appBeanList;
        for (int i = 0; i < cmds.size(); i++) {
            String cmd = (String) cmds.get(i) + "\n";
            // 输入命令
            try {
                stream.write(cmd);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private void writeLog(Map info, String IP, int foreW) {
        Map combinedResult = new HashMap();
        combinedResult.putAll(info);
        combinedResult.putAll(parsedResults);
        //新建文件csv23

        String csvfilepath = fileDir.toString() + "/result.csv";
        File csvFile = new File(csvfilepath);
        try {
            if (!csvFile.exists()) {
                csvFile.createNewFile();
                BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile));
                bw.write("timestamp,FrontAPP,nativeHeapFreeSize,usedNativeMemPercent,FrontAPPJavaHeap,FrontAPPNativeHeap,FrontAPPCodeMem,FrontAPPStackMem,FrontAPPGraphicsMem,FrontAPPPrivateOtherMem,FrontAPPSystemMem,GPU missed,HWC missed,Total missed,TotalFrames,MissedVsync,JankyNum,tempCPU0,tempCPU1,tempCPU2,tempCPU3,tempCPU4,tempCPU5,tempCPU6,tempCPU7,batteryTemp,thermalStatus,MemFree,MemAvailable,MemTotal,threshold,LowMemory,gpuUsage,GpuFreq,cpu0Freq,cpu1Freq,cpu2Freq,cpu3Freq,cpu4Freq,cpu5Freq,cpu6Freq,cpu7Freq,cpuUsage,FrontAppCpuUsage,wifiRssi,wifiSpeed,FPS");
                bw.newLine();
                Log.d(TAG, "创建csv文件");
                bw.close();
            }
            Log.d(TAG, "写入csv文件");
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvfilepath, true)));
            bw.write(combinedResult.get("timestamp") + "," + combinedResult.get("FrontAPP") + "," + combinedResult.get("nativeHeapFreeSize") + "," + combinedResult.get("usedNativeMemPercent") + "," + combinedResult.get("FrontAPPJavaHeap") + "," + combinedResult.get("FrontAPPNativeHeap") + "," + combinedResult.get("FrontAPPCodeMem") + "," + combinedResult.get("FrontAPPStackMem") + "," + combinedResult.get("FrontAPPGraphicsMem") + "," + combinedResult.get("FrontAPPPrivateOtherMem") + "," + combinedResult.get("FrontAPPSystemMem") + "," + combinedResult.get("GPU missed") + "," + combinedResult.get("HWC missed") + "," + combinedResult.get("Total missed") + "," + combinedResult.get("TotalFrames") + "," + combinedResult.get("MissedVsync") + "," + combinedResult.get("JankyNum") + "," + combinedResult.get("tempCPU0") + "," + combinedResult.get("tempCPU1") + "," + combinedResult.get("tempCPU2") + "," + combinedResult.get("tempCPU3") + "," + combinedResult.get("tempCPU4") + "," + combinedResult.get("tempCPU5") + "," + combinedResult.get("tempCPU6") + "," + combinedResult.get("tempCPU7") + "," + combinedResult.get("batteryTemp") + "," + combinedResult.get("thermalStatus") + "," + combinedResult.get("MemFree") + "," + combinedResult.get("MemAvailable") + "," + combinedResult.get("MemTotal") + "," + combinedResult.get("threshold") + "," + combinedResult.get("LowMemory") + "," + combinedResult.get("gpuUsage") + "," + combinedResult.get("GpuFreq") + "," + combinedResult.get("cpu0Freq") + "," + combinedResult.get("cpu1Freq") + "," + combinedResult.get("cpu2Freq") + "," + combinedResult.get("cpu3Freq") + "," + combinedResult.get("cpu4Freq") + "," + combinedResult.get("cpu5Freq") + "," + combinedResult.get("cpu6Freq") + "," + combinedResult.get("cpu7Freq") + "," + combinedResult.get("cpuUsage") + "," + combinedResult.get("FrontAppCpuUsage") + "," + combinedResult.get("wifiRssi") + "," + combinedResult.get("wifiSpeed") + "," + combinedResult.get("FPS_Mon"));
            bw.newLine();
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //将全局信息写入文件
        String filepath = fileDir.toString() + "/result.txt";
        File xlsFile = new File(filepath);
        try {
            if (!xlsFile.exists()) {
                xlsFile.createNewFile();
                Log.d(TAG, "创建文件");
            }
            Log.d(TAG, "写入文件" + filepath);
        } catch (Exception e) {
            e.printStackTrace();
        }
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filepath, true))); // 追加写入
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            // 阈值可调节
            if((Integer.parseInt((String)combinedResult.get("Total missed")))>0) {
                Log.d("WRITEEEE", "掉帧超过阈值,Total missed = " + combinedResult.get("Total missed"));
                Log.d("WRITEEEE", "writeLog: " + combinedResult.toString());
                if (foreW == 2) {
                    Log.d("WRITEEEE", "调用usemodel本地");
                    useModel(pak, combinedResult);
                } else if (foreW == 1) {
                    Log.d("WRITEEEE", "调用usemodel云端");
                    useModelOnline(pak, combinedResult);
                }
            }
            else{
                Log.d("WRITEEEE", "掉帧未超过阈值,Total missed = " + combinedResult.get("Total missed"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void useModel(String pak, Map parsedResults) {
        DecisionTreePredictor demo = new DecisionTreePredictor(mContext);
        Evaluator model = demo.loadPmml(pak, 1);
        int res = demo.predict(pak, model, parsedResults);
        Evaluator model2 = demo.loadPmml(pak, 2);
        int res2 = demo.predict(pak, model2, parsedResults);
        Evaluator model3 = demo.loadPmml(pak, 3);
        int res3 = demo.predict(pak, model3, parsedResults);
        Log.d("model:", "模型输出CPU异常检测结果：" + String.valueOf(res));
        Log.d("model:", "模型输出GPU异常检测结果：" + String.valueOf(res2));
        Log.d("model:", "模型输出内存异常检测结果：" + String.valueOf(res3));
        Log.d("WRITEEEE", "调用usemodel本地检测结果：\n" + "CPU异常检测结果：" + resg(res) + "\nGPU异常检测结果：" + resg(res2) + "\n内存异常检测结果：" + resg(res3));
        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDate = dateFormat.format(date);
        textUp.addNewText("\n" + formattedDate + "\t\t" + resg(res) + "\t\t\t\t" + resg(res2) + "\t\t\t\t" + resg(res3));
    }

    private String resg(int a) {
        return a == 0 ? "正常" : "异常";
    }
    //模型的部署
    public double[] getdata(Map parsedResults, String pak) {
        double x0 = Double.valueOf((String) parsedResults.get("batteryTemp"));
        double x1_0 = Double.valueOf((String) parsedResults.get("tempCPU0"));
        double x1_1 = Double.valueOf((String) parsedResults.get("tempCPU1"));
        double x1_2 = Double.valueOf((String) parsedResults.get("tempCPU2"));
        double x1_3 = Double.valueOf((String) parsedResults.get("tempCPU3"));
        double x1_4 = Double.valueOf((String) parsedResults.get("tempCPU4"));
        double x1_5 = Double.valueOf((String) parsedResults.get("tempCPU5"));
        double x1_6 = Double.valueOf((String) parsedResults.get("tempCPU6"));
        double x1_7 = Double.valueOf((String) parsedResults.get("tempCPU7"));
        double x4 = Double.valueOf((String) parsedResults.get("gpuUsage"));
        double x5_0 = Double.valueOf((String) parsedResults.get("cpu0Freq"));
        double x5_1 = Double.valueOf((String) parsedResults.get("cpu1Freq"));
        double x5_2 = Double.valueOf((String) parsedResults.get("cpu2Freq"));
        double x5_3 = Double.valueOf((String) parsedResults.get("cpu3Freq"));
        double x5_4 = Double.valueOf((String) parsedResults.get("cpu4Freq"));
        double x5_5 = Double.valueOf((String) parsedResults.get("cpu5Freq"));
        double x5_6 = Double.valueOf((String) parsedResults.get("cpu6Freq"));
        double x5_7 = Double.valueOf((String) parsedResults.get("cpu7Freq"));
        //double x6 = Double.valueOf((String) parsedResults.get("cpu7Freq")) / 1000000;
        double x7 = Double.valueOf((String) parsedResults.get("cpuUsage"));
        double x8 = Double.valueOf((String) parsedResults.get("FrontAppCpuUsage"));
        double x9 = Double.valueOf((String) parsedResults.get("wifiRssi")) + 80;
        double x10 = Double.valueOf((String) parsedResults.get("nativeHeapFreeSize")) / Double.valueOf((String) parsedResults.get("MemTotal"));
        double x11 = Double.valueOf((String) parsedResults.get("FrontAPPSystemMem")) / Double.valueOf((String) parsedResults.get("MemTotal"));
        double x12 = Double.valueOf((String) parsedResults.get("usedNativeMemPercent"));
        double x13 = Double.valueOf((String) parsedResults.get("FrontAPPJavaHeap")) / Double.valueOf((String) parsedResults.get("MemTotal"));
        double x14 = Double.valueOf((String) parsedResults.get("FrontAPPNativeHeap")) / Double.valueOf((String) parsedResults.get("MemTotal"));
        double x15 = Double.valueOf((String) parsedResults.get("FrontAPPCodeMem")) / Double.valueOf((String) parsedResults.get("MemTotal"));
        double x16 = Double.valueOf((String) parsedResults.get("FrontAPPStackMem")) / Double.valueOf((String) parsedResults.get("MemTotal"));
        double x17 = Double.valueOf((String) Objects.requireNonNull(parsedResults.get("FrontAPPGraphicsMem"))) / Double.valueOf((String) parsedResults.get("MemTotal"));
        double x18 = Double.valueOf((String) parsedResults.get("FrontAPPPrivateOtherMem")) / Double.valueOf((String) parsedResults.get("MemTotal"));

        double x20 = Double.valueOf((String) parsedResults.get("GpuFreq"));
        double x21 = Double.valueOf((String) parsedResults.get("MemAvailable")) / Double.valueOf((String) parsedResults.get("MemTotal"));
        double x22 = Double.valueOf((String) parsedResults.get("MemFree")) / Double.valueOf((String) parsedResults.get("MemTotal"));
        double x23 = Double.valueOf((String) parsedResults.get("thermalStatus"));
        double x24 = Double.valueOf((String) parsedResults.get("wifiSpeed"));
        double x25 = Double.valueOf((String) parsedResults.get("JankyNum")) / Double.valueOf((String) parsedResults.get("TotalFrames"));
        if (pak.equals("com.taobao.taobao")||pak.equals("com.ss.android.ugc.aweme")||pak.equals("com.miHoYo.ys.mi")) {
            return new double[]{x8,x12,x11,x13,x14,x15,x16,x17,x18,x1_0,x1_1,x1_2,x1_3,x1_4,x1_5,x1_6,x1_7,x0,x23,x4,x20,x5_0,x5_1,x5_2,x5_3,x5_4,x5_5,x5_6,x5_7};
        } else{
            return new double[]{x1_0,x1_1,x1_2,x1_3,x1_4,x1_5,x1_6,x1_7,x0,x23,x4,x20,x5_0,x5_1,x5_2,x5_3,x5_4,x5_5,x5_6,x5_7,x21,x22,x7};
        }
    }

    //在线方法
    public void useModelOnline(String pak, Map parsedResults) {
        //new Thread(net).start();
        // 使用构造函数传参
        double[] now = getdata(parsedResults, pak);
        Log.d("forecast", "调用在线模型");
        Thread t1 = new Thread(new RunnableTask(now));
        t1.start();
    }

    private class RunnableTask implements Runnable {
        private String IP;
        private double[] tensor;

        public RunnableTask(double[] m) {
            this.tensor = m;
        }

        @Override
        public void run() {
            Socket socket = new Socket();
            try {
                /* 指定Server的IP地址，此地址为局域网地址，如果是使用WIFI上网，则为PC机的WIFI IP地址
                 * 在ipconfig查看到的IP地址如下：
                 * Ethernet adapter 无线网络连接:
                 * Connection-specific DNS Suffix  . : IP Address. . . . . . . . . . . . : 192.168.1.100
                 */
                InetAddress serverAddr = InetAddress.getByName("192.168.1.139");// TCPServer.SERVERIP
                Log.d("model", "Client: 连接中Connecting...");
                // 应用Server的IP和端口建立Socket对象
                socket = new Socket(serverAddr, 6006);
                String message = "---Test_Socket_Android(Data Num:" + tensor.length + " ; FrontApp: "+ pak + ")---";
                Log.d("model", "Client: 发送中Sending: '" + message + "'");
                // 将信息通过这个对象来发送给Server
                PrintWriter out = new PrintWriter(new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream())),
                        true);
                // 把用户输入的内容发送给server
                String toServer = pak + ":" + Arrays.toString(tensor);
                Log.d("model", "To server:'" + toServer + "'");
                out.println(toServer);
                out.flush();
                // 接收服务器信息
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                // 得到服务器信息
                String msg = in.readLine();
                Log.d("model", "From server:'" + msg + "'");
                // 获取Total Missed,存入窗口
                String patternStr = "异常检测结果是：(\\d)(\\d)(\\d)";
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(msg);
                if (matcher.find()) {
                    textUp.addNewText("\n已经连接到192.168.1.139服务器......");
                    int res1 = Integer.parseInt(matcher.group(1));
                    int res2 = Integer.parseInt(matcher.group(2));
                    int res3 = Integer.parseInt(matcher.group(3));
                    Log.d("model:", "模型输出CPU异常检测结果：" + String.valueOf(res1));
                    Log.d("model:", "模型输出GPU异常检测结果：" + String.valueOf(res2));
                    Log.d("model:", "模型输出内存异常检测结果：" + String.valueOf(res3));
                    Log.d("WRITEEEE", "调用usemodel云端检测结果：\n" + "CPU异常检测结果：" + resg(res1) + "\nGPU异常检测结果：" + resg(res2) + "\n内存异常检测结果：" + resg(res3));
                    Calendar calendar = Calendar.getInstance();
                    Date date = calendar.getTime();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault());
                    String formattedDate = dateFormat.format(date);
                    textUp.addNewText("\n" + formattedDate + "\t\t" + resg(res1) + "\t\t\t\t" + resg(res2) + "\t\t\t\t" + resg(res3));
                }
                // 在页面上进行显示
                Log.d("model", "Success");
            } catch (UnknownHostException e) {
                Log.e("model", "192.168.1.139 is unkown server!");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}