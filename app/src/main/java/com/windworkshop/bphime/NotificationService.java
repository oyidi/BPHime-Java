package com.windworkshop.bphime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
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
    public static int START_CONNECTION = 10, START_CONNECTION_FINISH = 11, START_CONNECTION_SUCCESS = 12, RECIVE_DANMU = 20, STOP_CONNECTION = 30, RELOAD_STATUE = 40, REFRESH_CONFIG = 50;
    int NEW_DANMU = 10;
    NotificationManager notificationManager;
    NotificationCompat.Builder builder;
    Handler handler = new Handler();

    SharedPreferences sp;
    boolean vibrateNotification = false;
    Vibrator vibrator;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    BroadcastReceiver clientPing = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int action = intent.getIntExtra("action", 0);
            Log.i(MainActivity.logTag, "service handle:" + action);
            if(action == START_CONNECTION) {
                if(hasStart == false){
                    roomId = intent.getStringExtra("roomId");
                    if(roomId.length() > 0) {
                        Toast.makeText(getApplicationContext(), "启动中...", Toast.LENGTH_SHORT).show();
                        Thread thread = new Thread(startConnectRunnable);
                        thread.start();
                    }
                } else {
                    handler.post(stopConnection);
                    hasStart = false;
                }
            } else if(action == STOP_CONNECTION) {

            } else if(action == RELOAD_STATUE) {
                 Intent pongIntent = new Intent("com.windworkshop.bphime.service");
                pongIntent.putExtra("action", RELOAD_STATUE);
                 pongIntent.putExtra("hasStart", hasStart);
                 sendBroadcast(pongIntent);
                Log.i(MainActivity.logTag, "service RELOAD_STATUE:" + hasStart);
            } else if(action == REFRESH_CONFIG) {
                loadProfile();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        sp = getSharedPreferences("config",Context.MODE_PRIVATE);
        loadProfile();
        //startForeground(1, new Notification());
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder = new NotificationCompat.Builder(this, "danmu")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("BP姬运行中")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("danmu", "new_danmu", NotificationManager.IMPORTANCE_LOW);
            // 设置渠道描述
            //notificationChannel.setDescription("测试通知组");
            // 是否绕过请勿打扰模式
            //notificationChannel.canBypassDnd();
            // 设置绕过请勿打扰模式
            //notificationChannel.setBypassDnd(true);
            // 桌面Launcher的消息角标
            notificationChannel.canShowBadge();
            // 设置显示桌面Launcher的消息角标
            notificationChannel.setShowBadge(true);
            // 设置通知出现时声音，默认通知是有声音的
            notificationChannel.setSound(null, null);
            // 设置通知出现时的闪灯（如果 android 设备支持的话）
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(R.color.colorPrimary);
            // 设置通知出现时的震动（如果 android 设备支持的话）
            //notificationChannel.enableVibration(true);
            //notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400,
            //        300, 200, 400});

            notificationManager.createNotificationChannel(notificationChannel);

            startForeground(1, builder.build());
        }

        Log.i(MainActivity.logTag, "service onCreate");

        registerReceiver(clientPing, new IntentFilter("com.windworkshop.bphime.client"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(MainActivity.logTag, "service onDestroy");
        unregisterReceiver(clientPing);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(MainActivity.logTag, "service onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(MainActivity.logTag, "service onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    private void loadProfile() {
        vibrateNotification = sp.getBoolean("vibrate", false);
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
                sendBroadcast(new Intent("com.windworkshop.bphime.service").putExtra("action", START_CONNECTION_FINISH));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
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
                sendBroadcast(new Intent("com.windworkshop.bphime.service").putExtra("action", START_CONNECTION_SUCCESS));
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
            DanmuItem danmu = new DanmuItem(packet.packetData);
            if(danmu.cmd != null) {
                if(danmu.cmd.equals("DANMU_MSG") || danmu.cmd.equals("SEND_GIFT")) {
                    if(danmu.cmd.equals("DANMU_MSG")) {
                        builder.setContentText(danmu.userName+" : "+danmu.danmuText);
                    } else if(danmu.cmd.equals("SEND_GIFT")) {
                        builder.setContentText(danmu.giftUserName + " 赠送 " + danmu.giftNum + " 个" + danmu.giftName);
                    }
                    notificationManager.notify(NEW_DANMU, builder.build());
                    if(vibrateNotification == true) {
                        vibrator.vibrate(500);
                    }
                }
            }
           // Log.e(MainActivity.logTag, "hasStart:"+hasStart);

            Intent pongIntent = new Intent("com.windworkshop.bphime.service").putExtra("action", RECIVE_DANMU);
            pongIntent.putExtra("danmu_byte", bytes.array());
            sendBroadcast(pongIntent);
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
                if(hasStart == true) {
                    Log.i(MainActivity.logTag, "HeartBeat try reconnect");
                    client.reconnect();
                } else {
                    handler.post(stopConnection);
                }
            }

        }
    };
    /**
     * 停止处理
     */
    Runnable stopConnection = new Runnable() {
        @Override
        public void run() {
            if(hasStart == true) {
                Toast.makeText(getApplicationContext(), "检测到已断开连接等待重连", Toast.LENGTH_SHORT).show();
            } else{
                if(!client.isClosing() || !client.isClosed()){
                    client.close();
                }
                handler.removeCallbacks(heartBeatRunnable);
                sendBroadcast(new Intent("com.windworkshop.bphime.service").putExtra("action", STOP_CONNECTION));
                hasStart = false;
                Toast.makeText(getApplicationContext(), "已停止", Toast.LENGTH_SHORT).show();
            }
        }
    };
}
