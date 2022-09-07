package com.troica.remoteapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class TemplateMatcher {

    private static final String TAG = "TemplateMatcher";

    void initialize(Context context) {
        Log.v(TAG, "initialize");
    }

    void reset(){
        Log.v(TAG, "reset");
    }

    int match(Bitmap bitmap, int counter){
        // CPP native call goes here
        // must return DetectedAction item
        return EnumCMD.NOTHING.getValue();

        // debug
//        Mat debugFrame = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
//        Utils.bitmapToMat(bitmap, debugFrame);
//        debug_saveMatAsPNGFile(debugFrame);
    }

    @SuppressWarnings("unused")
    private void debug_saveMatAsPNGFile(Mat frame){
        try {
            File baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            Bitmap debugImage = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(frame, debugImage);
            FileOutputStream fos = new FileOutputStream(baseDir + "/remoteapp_" + frame + ".png");
            debugImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
