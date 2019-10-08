package com.windworkshop.bphime;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationService extends Service {
    public static int START_CONNECTION = 10, START_CONNECTION_FINISH = 11, START_CONNECTION_SUCCESS = 12, RECIVE_DANMU = 20, STOP_CONNECTION = 30;
    Messenger clientMessenger;
    Messenger mMessenger;
    ServiceMessageHandler handler = new ServiceMessageHandler(this);
    @Override
    public IBinder onBind(Intent intent) {
        mMessenger = new Messenger(handler);
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    class ServiceMessageHandler extends Handler {
        Context context;
        ServiceMessageHandler(Context context) {
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(MainActivity.logTag, "service handle:"+ msg.what);
            if(msg.what == 666) {
                Message testMessage = Message.obtain(msg);
                testMessage.what = 999;

                clientMessenger = msg.replyTo;
                if(clientMessenger != null) {
                    try {
                        clientMessenger.send(testMessage);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            } else if(msg.what == START_CONNECTION) {

                if(hasStart == false){
                    roomId = (String)msg.obj;
                    if(roomId.length() > 0) {
                        Toast.makeText(getApplicationContext(), "启动中...", Toast.LENGTH_SHORT).show();
                        Thread thread = new Thread(startConnectRunnable);
                        thread.start();
                    }
                } else {
                    handler.post(stopConnection);
                    hasStart = false;
                }
            } else if(msg.what == STOP_CONNECTION) {

            }
            
        }
    }

    JWebSocketClient client = null;
    boolean hasStart = false;
    String roomId;


    /**
     * 在Thread中的启动线程
     */
    Runnable startConnectRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                // 通过请求指定地址取得房间号，浏览器url中的的房间号不一定是真实websocket请求的房间号，可以请求看看排头的几位大佬的房间号就知道了
                OkHttpClient httpClient = new OkHttpClient.Builder().writeTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).connectTimeout(10, TimeUnit.SECONDS).build();
                Request request = new Request.Builder().url("https://api.live.bilibili.com/room/v1/Room/room_init?id="+roomId).build();
                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responResult = response.body().string();
                    JSONObject resultJson = new JSONObject(responResult);
                    int code = resultJson.getInt("code");
                    if(code == 0) {
                        roomId = String.valueOf(resultJson.getJSONObject("data").getInt("room_id"));
                        client = new JWebSocketClient(URI.create("wss://broadcastlv.chat.bilibili.com:2245/sub"));
                        client.connect();
                        handler.postDelayed(heartBeatRunnable, 30000);

                    } else {
                        Toast.makeText(getApplicationContext(), "启动错误："+resultJson.getString("message"), Toast.LENGTH_SHORT).show();
                    }
                }
                clientMessenger.send(handler.obtainMessage(START_CONNECTION_FINISH));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
    };



    public class JWebSocketClient extends WebSocketClient {
        public JWebSocketClient(URI serverUri) {
            super(serverUri, new Draft_6455());
        }

        @Override
        public void onOpen(ServerHandshake handshakeData) {
            Log.e(MainActivity.logTag, "onOpen() " + handshakeData.getHttpStatusMessage());
            // 启动的时候发送认证封包
            try {
                String authString = "{\"uid\": 0,\"roomid\": " + roomId +",\"protover\": 1,\"platform\": \"web\",\"clientver\": \"1.8.5\"}";
                LivePacket packet = LivePacket.createAuthPacket(authString);
                ByteBuffer bf = packet.toBuffer();
                this.send(bf);
                clientMessenger.send(handler.obtainMessage(START_CONNECTION_SUCCESS));
            } catch (Exception e) {
                e.printStackTrace();
            }
            hasStart = true;
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            super.onMessage(bytes);
            Log.e(MainActivity.logTag, "onMessage(ByteBuffer)");
            LivePacket packet = new LivePacket(bytes);

            Message danmuMessage = handler.obtainMessage(RECIVE_DANMU);
            danmuMessage.obj = packet;


            try {
                clientMessenger.send(danmuMessage);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            /*
            DanmuItem danmu = new DanmuItem(packet.packetData);
            if(danmu.cmd != null) {
                if(danmu.cmd.equals("DANMU_MSG") || danmu.cmd.equals("SEND_GIFT")) {
                    adapter.addDanmu(danmu);
                    handler.post(updateDanmuListRunnable);
                }
            }

            */
        }

        @Override
        public void onMessage(String message) {
            Log.i(MainActivity.logTag, "onMessage : "+ message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.e(MainActivity.logTag, "onClose() " + code + " " + reason);
            handler.post(stopConnection);
        }

        @Override
        public void onError(Exception ex) {
            Log.e(MainActivity.logTag, "onError()");
            ex.printStackTrace();
            handler.post(stopConnection);
        }
    }
    /**
     * 心跳包维护
     */
    Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            /*
            ByteBuffer bf =  ByteBuffer.allocate(16);
            bf.putInt(0,16 );
            bf.putInt(4,1048577);
            bf.putInt(8,2);
            bf.putInt(12,1);
            client.send(bf);
            */
            if(client.isClosed() == false){
                LivePacket packet = LivePacket.createPacket(MainActivity.PacketType.CLIENT_HEARTBEAT);
                client.send(packet.toBuffer());
                Log.e(MainActivity.logTag, "heartBell");
                //
                handler.postDelayed(heartBeatRunnable, 30000);
            } else {
                handler.post(stopConnection);
            }

        }
    };
    /**
     * 停止处理
     */
    Runnable stopConnection = new Runnable() {
        @Override
        public void run() {
            if(!client.isClosing() || !client.isClosed()){
                client.close();
            }
            handler.removeCallbacks(heartBeatRunnable);
            try {
                clientMessenger.send(handler.obtainMessage(STOP_CONNECTION));
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            hasStart = false;
            Toast.makeText(getApplicationContext(), "已停止", Toast.LENGTH_SHORT).show();
        }
    };
}
