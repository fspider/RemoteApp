package com.troica.remoteapp;

import static com.troica.remoteapp.EnumCMD.SEND_IMAGE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import android.util.Base64;

// @see https://docs.opencv.org/master/d4/dc6/tutorial_py_template_matching.html
// @see https://github.com/mtsahakis/MediaProjectionDemo/blob/master/src/com/mtsahakis/mediaprojectiondemo/ScreenCaptureImageActivity.java

public class DetectionActivity extends Activity {
    private boolean isOCVSetUp = false;
    private static String TAG = "ObjectDetectionScreenAct";
    private static final int REQUEST_CODE = 1000;

    private static final float CONF_CAPTURE_RATIO = 0.5f;

    private int mScreenDensity;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private Handler mHandler;
    private int mImagesProduced = 0;

    private MediaProjectionManager mProjectionManager;
    private MediaProjection mMediaProjection;
    private MediaProjectionCallback mMediaProjectionCallback;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private TemplateMatcher mTemplateMatcher;

    private ToggleButton mToggleButton;
    private WSClient wsClient;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                Log.i(TAG, "OpenCV loaded successfully");
                mTemplateMatcher.initialize(mAppContext);
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.obj_detection_screen_cap);
        OpenCVLoader.initDebug();

        // start capture handling thread
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mToggleButton = findViewById(R.id.toggle);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleScreenShare(v);
            }
        });

        wsClient = new WSClient(this);
        wsClient.createWebSocketClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTemplateMatcher = new TemplateMatcher();
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        if (!isOCVSetUp) { // if OCV hasn't been setup yet, init it
            if (!OpenCVLoader.initDebug()) {
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
                Log.i(TAG, "Something's wrong, I can feel it..");
            } else {
                isOCVSetUp = true;
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyMediaProjection();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            mToggleButton.setChecked(false);
            return;
        }

        mMediaProjectionCallback = new MediaProjectionCallback();
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, mHandler);
        mVirtualDisplay = createVirtualDisplay();
    }

    private void onToggleScreenShare(View view) {
        if (((ToggleButton) view).isChecked()) {
//            mImagesProduced = 0;
            mTemplateMatcher.reset();
            shareScreen();
        }
        else {
            Log.v(TAG, "Stopping Recording");
            stopScreenSharing();
        }
    }

    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
    }

    private VirtualDisplay createVirtualDisplay() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mDisplayWidth = (int) (size.x * CONF_CAPTURE_RATIO);
        mDisplayHeight = (int) (size.y * CONF_CAPTURE_RATIO);

        mImageReader = ImageReader.newInstance(mDisplayWidth, mDisplayHeight, PixelFormat.RGBA_8888, 3);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);

        Surface recorder = mImageReader.getSurface();

        return mMediaProjection.createVirtualDisplay("MainActivity",
                mDisplayWidth, mDisplayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                recorder, null, mHandler);
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay != null) mVirtualDisplay.release();
        if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
        destroyMediaProjection();
    }

    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG, "MediaProjection Stopped");
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (mToggleButton.isChecked()) {
                mToggleButton.setChecked(false);
                Log.v(TAG, "Recording Stopped");
            }
            mMediaProjection = null;
            stopScreenSharing();
        }
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Bitmap bitmap = null;
            try (Image image = reader.acquireLatestImage()) {
                if (image != null) {
                    mImagesProduced++;
                    Log.e(TAG, "captured image: " + mImagesProduced);

                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mDisplayWidth;

                    // create bitmap
                    int offset = 0;
                    bitmap = Bitmap.createBitmap(mDisplayWidth, mDisplayHeight, Bitmap.Config.ARGB_8888);

                    for (int i = 0; i < mDisplayHeight; ++i) {
                        for (int j = 0; j < mDisplayWidth; ++j) {
                            int pixel = 0;
                            pixel |= (buffer.get(offset) & 0xff) << 16;     // R
                            pixel |= (buffer.get(offset + 1) & 0xff) << 8;  // G
                            pixel |= (buffer.get(offset + 2) & 0xff);       // B
                            pixel |= (buffer.get(offset + 3) & 0xff) << 24; // A
                            bitmap.setPixel(j, i, pixel);
                            offset += pixelStride;
                        }
                        offset += rowPadding;
                    }

//                    // create bitmap
//                    bitmap = Bitmap.createBitmap(mDisplayWidth + rowPadding / pixelStride, mDisplayHeight, Bitmap.Config.ARGB_8888);
//                    bitmap.copyPixelsFromBuffer(buffer);

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();
                    String encodedString = Base64.encodeToString(byteArray, Base64.DEFAULT);
                    JSONObject obj = new JSONObject();
                    obj.put("no", mImagesProduced);
                    obj.put("cmd", SEND_IMAGE.getValue());
                    obj.put("data", encodedString);
                    wsClient.sendObj(obj);
                    // match it
//                    mTemplateMatcher.match(bitmap, mImagesProduced);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        }
    }
}
