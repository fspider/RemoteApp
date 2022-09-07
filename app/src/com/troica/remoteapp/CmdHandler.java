package com.troica.remoteapp;

import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.json.JSONObject;

public class CmdHandler {
    private static final String TAG = "[cmdHandler]";
    public boolean handle(JSONObject obj) {
        try {
            String frame_no = obj.getString("no");
            EnumCMD cmd = EnumCMD.fromInt(obj.getInt("cmd"));
            String data = obj.getString("data");
            Log.e(TAG, "Recv <- " + frame_no + " " + cmd.name() + " -> " + data);

            if(cmd == EnumCMD.CMD_MOUSE_UP) {

            }


            return true;
        } catch(Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }

    }

    public void sendClickEvent() {
// get the coordinates of the view
        int[] coordinates = new int[2];

// MotionEvent parameters
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        int action = MotionEvent.ACTION_DOWN;
        int x = 500;
        int y = 500;
        int metaState = 0;

// dispatch the event
        MotionEvent event = MotionEvent.obtain(downTime, eventTime, action, x, y, metaState);
        myView.dispatchTouchEvent(event);
    }

}
