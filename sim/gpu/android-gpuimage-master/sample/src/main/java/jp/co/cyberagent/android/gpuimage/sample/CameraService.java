package jp.co.cyberagent.android.gpuimage.sample;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.IBinder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.filter.GPUImageGrayscaleFilter;

public class CameraService extends Service {
    public CameraService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
    private Camera mCamera;
    private GPUImage mGPUImage;

    // 其他成员变量和方法

    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化相机
        mCamera = Camera.open();

        // 初始化GPUImage
        mGPUImage = new GPUImage(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 启动相机预览
        try {
            mCamera.setPreviewTexture(new SurfaceTexture(0));
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 设置GPUImage处理器
        mGPUImage.setFilter(new GPUImageGrayscaleFilter());

        // 将相机预览输出设置为GPUImage的输入
        mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                // 将预览帧数据转换为Bitmap对象
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, outputStream);
                byte[] jpegData = outputStream.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
                mGPUImage.saveToPictures(bitmap,"GPUImage","filterImage", null);
                camera.addCallbackBuffer(data);
            }
        });
        mCamera.addCallbackBuffer(new byte[mCamera.getParameters().getPreviewSize().width *
                mCamera.getParameters().getPreviewSize().height * 3 / 2]);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }
}