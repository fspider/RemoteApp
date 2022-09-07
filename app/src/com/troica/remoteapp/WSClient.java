package com.troica.remoteapp;

import android.os.Environment;
import android.util.Log;

import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import tech.gusavila92.websocketclient.WebSocketClient;

public class WSClient {
    public static final String TAG = "[Websocket Client] ";
    private WebSocketClient webSocketClient;
    private String server_uri = ""; // + DebugOption
    private static String server_ip = "66.29.133.111:8765/0/1";
    private DetectionActivity par;
    private int isWebsocketAlive = 0;
    private static CmdHandler cmdHandler;
    WSClient(DetectionActivity par) {
        this.par = par;
        cmdHandler = new CmdHandler();
    }

    public void createWebSocketClient() {
        if(isWebsocketAlive != 0) return;
        isWebsocketAlive = 1;

//        if(vnumber.equalsIgnoreCase("3"))
//        {
//            server_uri = "ws://" + server_ip + "/3/";
//        }
        server_uri = "ws://" + server_ip;

        URI uri;
        try {
            uri = new URI(server_uri);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                isWebsocketAlive = 2;
                Log.e(TAG, "onOpen!");
                // Request Model Input Size again
                JSONObject obj = new JSONObject();
                try {
                    obj.put("no", -1);
                    obj.put("cmd", EnumCMD.NOTHING);
                    obj.put("data", 0);
                    send(obj.toString());
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            @Override
            public void onTextReceived(String message) {
                try {
                    JSONObject obj = new JSONObject(message);
                    cmdHandler.handle(obj);
                } catch(Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            @Override
            public void onBinaryReceived(byte[] data) {
                Log.e(TAG, "onBinaryReceived");
            }

            @Override
            public void onPingReceived(byte[] data) {
                Log.e(TAG, "onPingReceived");
            }

            @Override
            public void onPongReceived(byte[] data) {
                Log.e(TAG, "onPongReceived");
            }

            @Override
            public void onException(Exception e) {
                Log.e("Failed to connect:", e.getMessage());
//                par.strStatus = "Connection Error!";
                isWebsocketAlive = 0;
            }

            @Override
            public void onCloseReceived() {
                Log.e(TAG, "onCloseReceived");
                isWebsocketAlive = 0;
            }
        };

        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }

    public WebSocketClient getWebSocketClient(){
        return this.webSocketClient;
    }
    public boolean sendObj(JSONObject obj) {
        if (isWebsocketAlive == 2) {
            this.webSocketClient.send(obj.toString());
        } else if(isWebsocketAlive == 0){
            Log.e(TAG, "Connecting again ... ");
            createWebSocketClient();
        }
        return true;
    }
}
