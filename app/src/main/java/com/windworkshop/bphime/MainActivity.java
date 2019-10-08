package com.windworkshop.bphime;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    Handler handler = new Handler();
    JWebSocketClient client = null;
    DanmuListAdapter adapter;
    RecyclerView listView;
    ArrayList<DanmuItem> mainDanmue = new ArrayList<DanmuItem>();
    EditText roomIdEdittext;
    Button startButton;
    String roomId;
    SharedPreferences sp;

    boolean hasStart = false;
    static String logTag = "BP-Hime";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sp = getSharedPreferences("config",Context.MODE_WORLD_WRITEABLE);
        adapter = new DanmuListAdapter(getApplicationContext(), mainDanmue);
        listView = findViewById(R.id.danmu_list);
        listView.setAdapter(adapter);
        listView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        roomIdEdittext = findViewById(R.id.room_id_edittext);
        roomIdEdittext.setText(sp.getString("roomid", ""));
        startButton = findViewById(R.id.start_revice_danmu_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(hasStart == false){
                    roomId = roomIdEdittext.getText().toString();
                    if(roomId.length() > 0) {
                        sp.edit().putString("roomid", roomId).commit();
                        startButton.setEnabled(false);
                        Toast.makeText(getApplicationContext(), "启动中...", Toast.LENGTH_SHORT).show();
                        Thread thread = new Thread(startConnectRunnable);
                        thread.start();
                    }
                } else {
                    handler.post(stopConnection);
                    hasStart = false;
                }
            }
        });
    }

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
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startButton.setEnabled(true);
                }
            });
        }
    };



    public class JWebSocketClient extends WebSocketClient {
        public JWebSocketClient(URI serverUri) {
            super(serverUri, new Draft_6455());
        }

        @Override
        public void onOpen(ServerHandshake handshakeData) {
            Log.e(logTag, "onOpen() " + handshakeData.getHttpStatusMessage());
            // 启动的时候发送认证封包
            try {
                String authString = "{\"uid\": 0,\"roomid\": " + roomId +",\"protover\": 1,\"platform\": \"web\",\"clientver\": \"1.8.5\"}";
                LivePacket packet = LivePacket.createAuthPacket(authString);
                ByteBuffer bf = packet.toBuffer();
                this.send(bf);
            } catch (Exception e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "启动成功", Toast.LENGTH_SHORT).show();
                    startButton.setText("STOP");
                }
            });
            hasStart = true;
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            super.onMessage(bytes);
            Log.e(logTag, "onMessage(ByteBuffer)");

            LivePacket packet = new LivePacket(bytes);
            DanmuItem danmu = new DanmuItem(packet.packetData);
            if(danmu.cmd != null) {
                if(danmu.cmd.equals("DANMU_MSG") || danmu.cmd.equals("SEND_GIFT")) {
                    adapter.addDanmu(danmu);
                    handler.post(updateDanmuListRunnable);
                }
            }
        }

        @Override
        public void onMessage(String message) {
            Log.i(logTag, "onMessage : "+ message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Log.e(logTag, "onClose() " + code + " " + reason);
            handler.post(stopConnection);
        }

        @Override
        public void onError(Exception ex) {
            Log.e(logTag, "onError()");
            ex.printStackTrace();
            handler.post(stopConnection);
        }
    }

    /**
     * 封包处理类
     */
    public static class LivePacket {
        int packetLength = 0;
        int headerLength = 16; //  默认封包头为16长度
        int protocolVersion = 1;
        int packetType = 0;
        int sequence = 1;
        String packetData = "";

        private LivePacket() {

        }

        /**
         * 从Buffer中初始化封包
         * @param buffer ByteBuffer数据
         */
        public LivePacket(ByteBuffer buffer) {
            // 初始化封包
            packetLength = buffer.getInt(0); // 第一个0-3位数据为总封包长数据
            int nums = buffer.getInt(4); // 第二个4-7位数据为需要分类的两个数据
            headerLength = nums >> 16; // 高两位（前16bit）为头部长度
            protocolVersion = (nums << 16) >> 16; // 反复横跳获得低两位（后16bit）控制版本号
            packetType = buffer.getInt(8); // 第三个8-11为封包类型
            sequence = buffer.getInt(12); // 第四个12-16为sequence

            packetData = "";
            if(packetLength > headerLength) {
                String dataString = new String(buffer.array()); // 把所有buffer转换为String
                if(dataString.indexOf("{") != -1 && dataString.indexOf("}")!= -1) {
                    // 挑出json格式的数据
                    dataString = dataString.substring(dataString.indexOf("{"), dataString.lastIndexOf("}")+1);
                    // 保存数据
                    packetData = dataString;
                }
                Log.i(logTag, "packetData : " + packetData);
            }
        }
        // 创建封包基础方法
        public static LivePacket createPacket(PacketType type) {
            LivePacket packet = new LivePacket();
            packet.packetType = type.id;
            return packet;
        }
        // 创建认证包
        public static LivePacket createAuthPacket(String dataJson) {
            LivePacket packet = createPacket(PacketType.JOIN_ROOM); // 设置封包类型
            packet.packetData = dataJson;
            return packet;
        }

        /**
         * 发送钱需要将封包转换为Buffer数据
         * @return
         */
        public ByteBuffer toBuffer() {
            // 总包长
            packetLength = headerLength + packetData.length();
            // 创建封包缓冲
            ByteBuffer bf =  ByteBuffer.allocate(packetLength);
            //ByteBuffer的一个Int（Byte）为四位8bit数据。

            // 第一个Int装入总封包长数据
            bf.putInt(0, packetLength);
            // 第二个Int因为要拆成两个2x8bit来分别储存头部长度和控制版本号，因为放char和byte转换非常麻烦，直接偏移16位处理还快些
            int nums = 0;
            nums = (nums << 16) | headerLength; // 头部长度
            nums = (nums << 16) | protocolVersion; // 控制版本号
            bf.putInt(4,nums); // 装入头部长度和控制版本号数据
            bf.putInt(8,packetType); // 装入封包类型
            bf.putInt(12,sequence); // 装入sequence（不知道这是干嘛的）
            // 有数据的话在一个byte一个byte的装入
            if(packetData.length() > 0) {
                byte[] dataBytes = packetData.getBytes();
                for(int i = 0;i < dataBytes.length;i++) {
                    bf.put(16+i,dataBytes[i]);
                }
            }

            return bf;
        }
    }

    /**
     * 封包类型
     */
    enum PacketType {
        CLIENT_HEARTBEAT(2), COMMAND(5), JOIN_ROOM(7), SERVER_HEARTBEAT(8);
        int id;
        PacketType(int id){
            this.id = id;
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
                LivePacket packet = LivePacket.createPacket(PacketType.CLIENT_HEARTBEAT);
                client.send(packet.toBuffer());
                Log.e(logTag, "heartBell");
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
            startButton.setText("START");
            hasStart = false;
            Toast.makeText(getApplicationContext(), "已停止", Toast.LENGTH_SHORT).show();
        }
    };
    /**
     * 弹幕接收显示处理
     */
    Runnable updateDanmuListRunnable = new Runnable() {
        @Override
        public void run() {
            adapter.notifyDataSetChanged();
            listView.scrollToPosition(adapter.getItemCount()-1);
        }
    };
}
