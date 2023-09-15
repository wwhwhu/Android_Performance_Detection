package jp.co.cyberagent.android.gpuimage.sample;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import jp.co.cyberagent.android.gpuimage.GPUImageView;
import jp.co.cyberagent.android.gpuimage.PixelBuffer;
import jp.co.cyberagent.android.gpuimage.sample.utils.Camera1Loader;

//监控需要启动前台服务
public class ForegroundService extends Service {
    public boolean flag = true;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("service", "Bind......");
        return null;
    }

    //首次创建该服务时，系统将调用此方法执行一次设置程序（在调用onStartCommand或者onbind之前,服务已经运行时不会调用该方法，该方法只能调用一次
    //启动服务时跳出一个通知提醒
    public void onCreate() {
        //服务创建时创建前台通知
        Notification notification = createForegroundNotification();
        //通过前台启动服务，系统创建服务后，应用有五秒的时间来调用该服务的 startForeground() 方法以显示新服务的用户可见通知。
        //如果应用在此时间限制内未调用 startForeground()，则系统将停止服务并声明此应用为 ANR。
        // 参数一：唯一的通知标识；参数二：通知消息。 让安卓服务运行于前台
        startForeground(1, notification);
        // 具体执行
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //做一些事情
                    doSomething();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void doSomething() throws InterruptedException {
        while (true) {
            // 等待一段时间
            try {


                Log.d("service", "Running......");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasStoragePermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    //服务销毁时
    @Override
    public void onDestroy() {
        //在服务被销毁时，关闭前台服务
        Log.d("service", "onDestroy......");
        stopForeground(true);
        this.flag = false;
        super.onDestroy();
    }

    //创建前台通知
    protected Notification createForegroundNotification() {
        //前台通知的id名，任意
        String channelId = "ForegroundService";
        //前台通知的名称，任意
        String channelName = "Service";
        //发送通知的等级，此处为高，根据业务情况而定
        int importance = NotificationManager.IMPORTANCE_HIGH;
        //判断Android版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
        }

        //点击通知时可进入的Activity
        //Intent notificationIntent = new Intent(this,MainActivity.class);
        //PendingIntent pendingIntent = PendingIntent.getActivity(this,0,notificationIntent,0);

        //通知内容
        return new NotificationCompat.Builder(this, channelId)
                .setContentTitle("通知标题")
                .setContentText("内容")
                // .setContentIntent(pendingIntent)//点击通知进入Activity
                .setTicker("提示语")
                .build();
    }
}
