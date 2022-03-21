package com.example.camerademo;

import android.app.Activity;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by dang on 2020-12-15.
 * Time will tell.
 * 录像的工具类
 * @description
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class VideoRecordUtils {
    public static Size WH_720X480 = new Size(1080,1920);

    private MediaRecorder mediaRecorder;
    private SurfaceView surfaceView;
    private CameraDevice mCameraDevice;
    List<Surface> surfaces = new ArrayList<>();
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private Size size;

    /**
     *  初始化MediaRecorder，接受Camera有关参数
     */
    public void create(SurfaceView surfaceView, CameraDevice cameraDevice, Size size){
        this.surfaceView = surfaceView;
        this.size = size;
        mCameraDevice = cameraDevice;
        try{
            //创建录制请求
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
        mediaRecorder = new MediaRecorder();
    }
    /**
     * 停止录制
     */
    public void stopRecord(){
        mediaRecorder.release();
        mediaRecorder = null;
        mediaRecorder = new MediaRecorder();
        surfaces.clear();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startRecord(Activity activity, Handler handler){
        //为mediaRecord设置一系列的属性
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        mediaRecorder.setVideoSize(size.getWidth(),size.getHeight());
        mediaRecorder.setVideoFrameRate(24);
        mediaRecorder.setVideoEncodingBitRate(700 * 1024);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setOrientationHint(90);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
        String fname = "video_" + sdf.format(new Date()) + ".mp4";

        File parentFile = new File(activity.getCacheDir().getAbsolutePath() + File.separator + "file");
        if(!parentFile.exists()){
            parentFile.mkdirs();
        }
        File file = new File(parentFile, fname);
        Log.d("1111", "startRecord: "+file.getAbsolutePath());
        //设置视频录制文件的保存路径
        mediaRecorder.setOutputFile(file);
        try{
            //初始化mediaRecord，然后开启
            mediaRecorder.prepare();
            mediaRecorder.start();
        }catch(IOException e){
            e.printStackTrace();
        }

        //设置CaptureRequest
        Surface previewSurface = surfaceView.getHolder().getSurface();
        surfaces.add(previewSurface);
        mPreviewBuilder.addTarget(previewSurface);
        Surface recordSurface = mediaRecorder.getSurface();
        surfaces.add(recordSurface);
        mPreviewBuilder.addTarget(recordSurface);
        try{
            //创建会话session
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    //录像时同时开启预览，使得一直有画面
                    mCameraCaptureSession = session;
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(),null,handler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            },handler);
        }catch(CameraAccessException e){
            e.printStackTrace();
        }
    }
}
