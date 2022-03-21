package com.example.camerademo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.camerademo.databinding.ActivityMainBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private SurfaceHolder surfaceHolder;
    private CameraManager cameraManager;
    private HandlerThread handlerThread;
    private CameraDevice cameraDevice;
    private VideoRecordUtils videoRecorderUtils;
    private CameraCaptureSession cameraCaptureSession;
    private Handler handler;
    private ImageReader imageReader;
    private CaptureRequest.Builder captureBuilder;
    private CaptureRequest.Builder previewBuilder;
    private String currentCameraId;

    /**
     * 打开相机的回调，
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //打开相机后开启预览
            cameraDevice = camera;
            videoRecorderUtils = new VideoRecordUtils();
            videoRecorderUtils.create(binding.surfaceView, cameraDevice, VideoRecordUtils.WH_720X480);
            //打开照相机时初始化,videoRecord
//            videoRecorderUtils = new VideoRecorderUtils();
//            videoRecorderUtils.create(binding.surfaceView, cameraDevice, VideoRecorderUtils.WH_720X480);
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            finish();
        }
    };

    /**
     * session的回调
     */
    private final CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            //会话已经建立，可以开始预览了
            cameraCaptureSession = session;
            //设置自动对焦
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            //发送预览请求
            try {
                cameraCaptureSession.setRepeatingRequest(previewBuilder.build(), null, handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
//            //关闭会话
//            session.close();
//            cameraCaptureSession = null;
//            cameraDevice.close();
//            cameraDevice = null;
        }
    };

    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
//            try {
//                //自动对焦
//                captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
//                //重新打开预览
//                cameraCaptureSession.setRepeatingRequest(previewBuilder.build(), null, handler);
//            } catch (CameraAccessException e) {
//                e.printStackTrace();
//            }
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
//            cameraCaptureSession.close();
//            cameraCaptureSession = null;
//            cameraDevice.close();
//            cameraDevice = null;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "拒绝权限无法使用", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    initView();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //动态获取权限
        List<String> permissionList = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
            Log.d(TAG, "onCreate: 请求权限");
        } else {
            initView();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void initView() {
        Log.d(TAG, "initView: 初始化视图");
        binding.surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {

                Log.d("预览尺寸", "surfaceCreated: " + binding.surfaceView.getHeight() + ".." + binding.surfaceView.getWidth());
                //打开相机
                openCamera();
                int height = binding.surfaceView.getHeight();
                int width = binding.surfaceView.getWidth();
                if (height > width) {
                    float justH = width * 4.f / 3;
                    //设置View在水平方向的缩放比例,保证宽高比为3:4
                    binding.surfaceView.setScaleX(height / justH);
                } else {
                    float justW = height * 4.f / 3;
                    binding.surfaceView.setScaleY(width / justW);
                }
                Log.d("变更后预览尺寸", "surfaceCreated: " + binding.surfaceView.getHeight() + ".." + binding.surfaceView.getWidth());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });
        binding.btnTakePhoto.setOnClickListener(v -> {
            //点击按钮拍照的事件
            takePhoto();
        });
        binding.btnSwitch.setOnClickListener(v -> {
            //点击按钮切换摄像头的事件
//                switchCamera();
        });
        binding.record.setOnClickListener(v -> {
           /* config();
            startRecorder();*/
            videoRecorderUtils.startRecord(MainActivity.this, handler);
        });
        binding.stop.setOnClickListener(v -> {
            /* stopRecorder();*/
            videoRecorderUtils.stopRecord();
            startPreview();
        });
        //获取摄像头管理，并开启子线程
        initCameraManager();
    }

    private void initCameraManager() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    private void startPreview() {
        try {
            //构建预览请求
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //设置预览输出的界面
            previewBuilder.addTarget(binding.surfaceView.getHolder().getSurface());
            //创建相机的会话Session
            cameraDevice.createCaptureSession(Arrays.asList(binding.surfaceView.getHolder().getSurface(), imageReader.getSurface()), sessionStateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openCamera() {
        try {
            //获取摄像头属性描述
            CameraCharacteristics cameraCharacteristics = null;
            for (String cameraId : cameraManager.getCameraIdList()){
                cameraCharacteristics = cameraManager.getCameraCharacteristics(String.valueOf(cameraId));
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if(facing == CameraCharacteristics.LENS_FACING_BACK){
                    Log.d(TAG, "openCamera: 后置摄像头:"+facing);
                    currentCameraId = cameraId;
                    continue;
//                    break;
                }
                Log.d(TAG, "openCamera: 摄像头为："+facing);
            }
            //获取摄像头支持的配置属性
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Integer level = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            //获取预览画面的输出尺寸，使用SurfaceView做预览

            initImageReader();
            //打开相机,先获取权限
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //打开摄像头
            cameraManager.openCamera(String.valueOf(currentCameraId), stateCallback, handler);
        } catch (CameraAccessException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        try {
            //创建拍照请求的Request
            captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            //设置拍照的画面
            captureBuilder.addTarget(imageReader.getSurface());
            //自动对焦
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            //自动曝光
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //停止预览
            cameraCaptureSession.stopRepeating();
            //拍照
            cameraCaptureSession.capture(captureBuilder.build(), captureCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initImageReader() {
        imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(reader
                -> handler.post(new ImageSaver(reader.acquireNextImage()))
                , handler);
    }

    /**
     * 关闭相机
     */
    private void closeCamera() {
        //关闭捕捉会话
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        //关闭相机
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        //关闭拍照处理器
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }


    public class ImageSaver implements Runnable {

        private Image mImage;
        private File mFile;

        public ImageSaver(Image image) {
            this.mImage = image;
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fname = "IMG_" + sdf.format(new Date()) + ".jpg";
//            mFile = new File(getApplication().getExternalFilesDir(null), fname);
            //图片保存方式一
            String cachePath = getApplication().getCacheDir().getAbsolutePath();
            File parentFile = new File(cachePath + File.separator + "file");
            if(!parentFile.exists()){
                parentFile.mkdirs();
            }
            mFile = new File(parentFile, fname);

            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if(!mFile.exists()){
                Log.d(TAG, "run: 图片文件不存在");
            }else {
                Log.d(TAG, "run: 图片路径："+mFile.getAbsolutePath());
            }

            //图片保存方式二
            // 把file里面的图片插入到系统相册中
            /*try {
                MediaStore.Images.Media.insertImage(getApplication().getContentResolver(),
                        mFile.getAbsolutePath(), fname, null);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "run: 通知系统更新图片");
            // 通知相册更新
            getApplication().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(mFile)));*/
        }
    }


}