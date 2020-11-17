package com.windworkshop.bphime;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationService extends Service {
    public static int START_CONNECTION = 10, START_CONNECTION_FINISH = 11, START_CONNECTION_SUCCESS = 12, RECIVE_DANMU = 20, RECIVE_LOG = 21, STOP_CONNECTION = 30, RELOAD_STATUE = 40, REFRESH_CONFIG = 50;
    public static String FOR_CLIENT = "com.windworkshop.bphime.service", FOR_SERVICE = "com.windworkshop.bphime.client";
    int NEW_DANMU = 10;
    NotificationManager notificationManager;
    NotificationCompat.Builder builder;
    Handler handler = new Handler();

    SharedPreferences sp;
    boolean vibrateNotification = false;
    Vibrator vibrator;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    ArrayList<String> danmuRawData = new ArrayList<String>();
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    BroadcastReceiver clientPing = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int action = intent.getIntExtra("action", 0);
            //MainModule.showLog( "service handle:" + action);
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

            } else if(action == RECIVE_LOG) {
                Intent sendIntent = new Intent(FOR_CLIENT);
                sendIntent.putExtra("action", RECIVE_DANMU);
                String logString = intent.getStringExtra("log");
                String time = sdf.format(new Date());
                sendIntent.putExtra("isLog", true);
                sendIntent.putExtra("log", logString);
                sendIntent.putExtra("time", time);
                String logJsonString = "{\"cmd\":\"log\", \"log\":\"" + logString + "\",\"time\":\"" + time + "\"}";
                danmuRawData.add(logJsonString);
                sendBroadcast(sendIntent);
            } else if(action == RELOAD_STATUE) {
                  Intent pongIntent = new Intent(FOR_CLIENT);
                  pongIntent.putExtra("action", RELOAD_STATUE);
                  pongIntent.putExtra("hasStart", hasStart);

                  pongIntent.putExtra("danmu_strings", danmuRawData);

                  sendBroadcast(pongIntent);
                  MainModule.showLog( "service RELOAD_STATUE:" + hasStart);
            } else if(action == REFRESH_CONFIG) {
                loadProfile();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        MainModule.setContext(getApplicationContext());
        sp = getSharedPreferences("config",Context.MODE_PRIVATE);
        loadProfile();
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder = new NotificationCompat.Builder(this, "danmu")
                .setContentTitle(getString(R.string.app_name)+"运行中")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            builder.setSmallIcon(R.mipmap.ic_launcher_foreground);
        } else {
            builder.setSmallIcon(R.mipmap.ic_launcher_foreground);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("danmu", "new_danmu", NotificationManager.IMPORTANCE_LOW);
            notificationChannel.canShowBadge();
            notificationChannel.setShowBadge(true);
            notificationChannel.setSound(null, null);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(R.color.colorPrimary);
            notificationManager.createNotificationChannel(notificationChannel);
            startForeground(1, builder.build());
        }

        MainModule.showLog( "service onCreate");
        registerReceiver(clientPing, new IntentFilter(FOR_SERVICE));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MainModule.showLog( "service onDestroy");
        unregisterReceiver(clientPing);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        MainModule.showLog( "service onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MainModule.showLog( "service onStartCommand");
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
                sendBroadcast(new Intent(FOR_CLIENT).putExtra("action", START_CONNECTION_FINISH));
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
            MainModule.showLog( "onOpen() " + handshakeData.getHttpStatusMessage());
            // 启动的时候发送认证封包
            try {
                String authString = "{\"uid\": 0,\"roomid\": " + roomId +",\"protover\": 1,\"platform\": \"web\",\"clientver\": \"1.8.5\"}";
                LivePacket packet = LivePacket.createAuthPacket(authString);
                ByteBuffer bf = packet.toBuffer();
                this.send(bf);
                sendBroadcast(new Intent(FOR_CLIENT).putExtra("action", START_CONNECTION_SUCCESS));
                reconnectCoount = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
            hasStart = true;
        }

        @Override
        public void onMessage(ByteBuffer buffer) {
            super.onMessage(buffer);
            //MainModule.showLog( "onMessage(ByteBuffer)");

            int packetLength = 0;
            int headerLength = 16; //  默认封包头为16长度
            int protocolVersion = 1;
            int packetType = 0;
            int sequence = 1;
            // 初始化封包
            packetLength = buffer.getInt(0); // 第一个0-3位数据为总封包长数据
            int nums = buffer.getInt(4); // 第二个4-7位数据为需要分类的两个数据
            headerLength = nums >> 16; // 高两位（前16bit）为头部长度
            protocolVersion = (nums << 16) >> 16; // 反复横跳获得低两位（后16bit）控制版本号
            packetType = buffer.getInt(8); // 第三个8-11为封包类型
            sequence = buffer.getInt(12); // 第四个12-16为sequence
            //MainModule.showLog( "packetData 包数据raw : " + new String(buffer.array()));
            ArrayList<String> dataArray = new ArrayList<String>();
            if(protocolVersion == 2) {
                try {
                    // 新版协议用flater解压数据
                    byte[] rawByteData = buffer.array();
                    byte[] realByteData = new byte[rawByteData.length - 16];
                    for(int i = 0;i < realByteData.length;i++){
                        realByteData[i] = rawByteData[16+ i];
                    }
                    String zipString = new String(MainModule.uncompress(realByteData));
                    // 解压之后的数据似乎还包含着原有封包的头，长度有差异，并且将多个包拼接在一起，还会出现包数据不全的情况，手动截取包中多个json
                    // 估计是为了减轻服务器压力才这么设计的
                    int leftCount = 0;
                    int startIndex = 0;
                    boolean inString = false;
                    for(int i = 0;i < zipString.length();i++) {
                        char c = zipString.charAt(i);
                        if(c == '"') {
                            inString = !inString;
                        }
                        if(inString == false) {
                            if(leftCount <= 0) {
                                if(c == '{') {
                                    startIndex = i;
                                    leftCount += 1;
                                }
                            } else {
                                if(c == '{') {
                                    leftCount += 1;
                                } else if(c == '}') {
                                    leftCount -= 1;
                                    if(leftCount == 0) {
                                        String jsonString = zipString.substring(startIndex, i+1);
                                        //MainModule.showLog("find json:" + jsonString);
                                        dataArray.add(jsonString);
                                    }
                                }
                            }
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // 旧版协议读取方式，两种协议并存中
                if(packetLength > headerLength) {
                    String dataString = new String(buffer.array()); // 把所有buffer转换为String
                    if(dataString.indexOf("{") != -1 && dataString.indexOf("}")!= -1) {
                        // 挑出json格式的数据
                        dataString = dataString.substring(dataString.indexOf("{"), dataString.lastIndexOf("}")+1);
                        // 保存数据
                        dataArray.add(dataString);
                       // MainModule.showLog( "packetData 包数据 : " + dataString);
                    }
                }
            }
           // LivePacket packet = new LivePacket(buffer);
            // 处理读出封包数据
            for(String dataString : dataArray) {
                DanmuItem danmu = new DanmuItem(dataString);
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
                danmuRawData.add(dataString);
                Intent pongIntent = new Intent(FOR_CLIENT).putExtra("action", RECIVE_DANMU);
                pongIntent.putExtra("danmu_string", dataString);
                sendBroadcast(pongIntent);
            }
        }

        @Override
        public void onMessage(String message) {
            MainModule.showLog( "onMessage : "+ message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            MainModule.showLog( "onClose() " + code + " " + reason);
            handler.post(stopConnection);
        }

        @Override
        public void onError(Exception ex) {
            MainModule.showLog( "onError()");
            ex.printStackTrace();
            handler.post(stopConnection);
        }
    }
    int reconnectCoount = 0;
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
            // 连接没有被关闭，发送心跳包
            if(client.isClosed() == false){
                LivePacket packet = LivePacket.createPacket(MainActivity.PacketType.CLIENT_HEARTBEAT);
                client.send(packet.toBuffer());
                MainModule.showLog( "heartBell");
                // 心跳包30秒一发
                handler.postDelayed(heartBeatRunnable, 30000);
            } else { // 连接被关闭
                if(hasStart == true) { // 如果在运行中，则尝试重连
                    MainModule.showLog( "HeartBeat try reconnect" + reconnectCoount);
                    reconnectCoount += 1;
                    if(reconnectCoount > 5) { // 重试5次不成功就完全停止
                        hasStart = false;
                        reconnectCoount = 0;
                        handler.post(stopConnection);
                        notificationManager.notify(NEW_DANMU, builder.setContentText("多次重连不成功，已停止服务").build());
                    } else {
                        // 重连时候缩短到10秒钟一次
                        client.reconnect();
                        handler.postDelayed(heartBeatRunnable, 10000);
                    }

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
                sendBroadcast(new Intent(FOR_CLIENT).putExtra("action", STOP_CONNECTION));
                hasStart = false;
                Toast.makeText(getApplicationContext(), "已停止", Toast.LENGTH_SHORT).show();
            }
        }
    };
}
