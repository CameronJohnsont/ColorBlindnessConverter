package com.example.camsfinalproject;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = "AndroidCameraApi";
  private Button takePictureButton;
  private Button camera;
  private TextureView textureView;
  private ImageView imageView;
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

  private String cameraId;
  protected CameraDevice cameraDevice;
  protected CameraCaptureSession cameraCaptureSessions;
  protected CaptureRequest captureRequest;
  protected CaptureRequest.Builder captureRequestBuilder;
  private Size imageDimension;
  private ImageReader imageReader;
  private File file;
  private static final int REQUEST_CAMERA_PERMISSION = 200;
  private boolean mFlashSupported;
  private Handler mBackgroundHandler;
  private HandlerThread mBackgroundThread;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    textureView = (TextureView) findViewById(R.id.texture);
    assert textureView != null;
    textureView.setSurfaceTextureListener(textureListener);
    takePictureButton = (Button) findViewById(R.id.btn_takepicture);
    assert takePictureButton != null;
    camera = (Button) findViewById(R.id.btn_camera);
    assert camera != null;
    camera.setVisibility(View.INVISIBLE);
    imageView = (ImageView) findViewById(R.id.imageView);
    assert imageView != null;
    takePictureButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        takePicture();
      }
    });
    camera.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        imageView.setVisibility(View.INVISIBLE);
        camera.setVisibility(View.INVISIBLE);
        textureView.setVisibility(View.VISIBLE);
        takePictureButton.setVisibility(View.VISIBLE);
      }
    });
  }

  TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
      //open your camera here
      openCamera();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
      // Transform you image captured size according to the surface width and height
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
      return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    }
  };
  private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(CameraDevice camera) {
      //This is called when the camera is open
      Log.e(TAG, "onOpened");
      cameraDevice = camera;
      createCameraPreview();
    }

    @Override
    public void onDisconnected(CameraDevice camera) {
      cameraDevice.close();
    }

    @Override
    public void onError(CameraDevice camera, int error) {
      cameraDevice.close();
      cameraDevice = null;
    }
  };
  final CameraCaptureSession.CaptureCallback captureCallbackListener = new CameraCaptureSession.CaptureCallback() {
    @Override
    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
      super.onCaptureCompleted(session, request, result);
      Toast.makeText(MainActivity.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
      createCameraPreview();
    }
  };

  protected void startBackgroundThread() {
    mBackgroundThread = new HandlerThread("Camera Background");
    mBackgroundThread.start();
    mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
  }

  protected void stopBackgroundThread() {
    mBackgroundThread.quitSafely();
    try {
      mBackgroundThread.join();
      mBackgroundThread = null;
      mBackgroundHandler = null;
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  protected void takePicture() {
    if (null == cameraDevice) {
      Log.e(TAG, "cameraDevice is null");
      return;
    }

    convertImage();
  }

  private void convertImage() {
    Bitmap bmp = textureView.getBitmap();
    for (int x = 0; x < bmp.getWidth(); x++) {
      for (int y = 0; y < bmp.getHeight(); y++) {
        int pixel = bmp.getPixel(x, y);
        int converted = convertPixel_Protanopia(pixel);
        bmp.setPixel(x, y, converted);
      }
    }
    Canvas canvas = new Canvas();
    canvas.drawBitmap(bmp, 0, 0, null);
    try {
      imageView.setVisibility(View.VISIBLE);
      camera.setVisibility(View.VISIBLE);
      textureView.setVisibility(View.INVISIBLE);
      takePictureButton.setVisibility(View.INVISIBLE);
      imageView.setImageDrawable(new BitmapDrawable(getResources(), bmp));
    } catch (Exception e) {
      Log.e(e.getMessage(), e.getMessage());
    }
  }

  private int convertPixel_Protanopia(int pixel) {
    int b = (pixel) & 0xFF;
    int g = (pixel >> 8) & 0xFF;
    int r = (pixel >> 16) & 0xFF;
    int a = (pixel >> 24) & 0xFF;

    //Multiply the ints to change the pixels
    int r2 = (int) (r * 56.667 / 100.0 + g * 43.333 / 100.0 + b * 0 / 100.0);
    int g2 = (int) (r * 55.833 / 100.0 + g * 44.167 / 100.0 + b * 0 / 100.0);
    int b2 = (int) (r * 0 / 100.0 + g * 24.167 / 100.0 + b * 75.833 / 100.0);
    return Color.rgb(r2, g2, b2);
  }

  protected void createCameraPreview() {
    try {
      SurfaceTexture texture = textureView.getSurfaceTexture();
      assert texture != null;
      texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
      Surface surface = new Surface(texture);
      captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
      captureRequestBuilder.addTarget(surface);
      cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
          //The camera is already closed
          if (null == cameraDevice) {
            return;
          }
          // When the session is ready, we start displaying the preview.
          cameraCaptureSessions = cameraCaptureSession;
          updatePreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
          Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
        }
      }, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  private void openCamera() {
    CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    Log.e(TAG, "is camera open");
    try {
      cameraId = manager.getCameraIdList()[0];
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
      StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      assert map != null;
      imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
      // Add permission for camera and let user grant the permission
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
        return;
      }
      manager.openCamera(cameraId, stateCallback, null);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
    Log.e(TAG, "openCamera X");
  }

  protected void updatePreview() {
    if (null == cameraDevice) {
      Log.e(TAG, "updatePreview error, return");
    }
    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    try {
      cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
    } catch (CameraAccessException e) {
      e.printStackTrace();
    }
  }

  private void closeCamera() {
    if (null != cameraDevice) {
      cameraDevice.close();
      cameraDevice = null;
    }
    if (null != imageReader) {
      imageReader.close();
      imageReader = null;
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == REQUEST_CAMERA_PERMISSION) {
      if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
        // close the app
        Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
        finish();
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.e(TAG, "onResume");
    startBackgroundThread();
    if (textureView.isAvailable()) {
      openCamera();
    } else {
      textureView.setSurfaceTextureListener(textureListener);
    }
  }

  @Override
  protected void onPause() {
    Log.e(TAG, "onPause");
    stopBackgroundThread();
    super.onPause();
  }
}
