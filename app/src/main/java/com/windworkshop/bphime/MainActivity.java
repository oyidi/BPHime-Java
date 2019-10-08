package com.windworkshop.bphime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
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

    Runnable startConnectRunnable = new Runnable() {
        @Override
        public void run() {
            try {
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
    class DanmuItem {
        String danmuData;
        String cmd;
        String danmuText;
        String userName;
        String giftName;
        String giftUserName;
        int giftNum;
        String welcomeName;
        public DanmuItem(String rawdata) {
            danmuData = rawdata;
            try {
                JSONObject json = new JSONObject(danmuData);
                cmd = json.getString("cmd");
                if(cmd.equals("DANMU_MSG")){
                    JSONArray info = json.getJSONArray("info");
                    danmuText = info.getString(1);
                    userName = info.getJSONArray(2).getString(1);
                } else if(cmd.equals("SEND_GIFT")) {
                    JSONObject data = json.getJSONObject("data");
                    giftName = data.getString("giftName");
                    giftNum = data.getInt("num");
                    giftUserName = data.getString("uname");
                } else if(cmd.equals("WELCOME")) {
                    JSONObject data = json.getJSONObject("data");
                    welcomeName = data.getString("uname");
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    class DanmuListAdapter extends RecyclerView.Adapter<DanmuViewHolder> {
        ArrayList<DanmuItem> danmus;
        Context context;
        public DanmuListAdapter(Context context, ArrayList<DanmuItem> danmus) {
            this.danmus = danmus;
            this.context = context;
        }
        @NonNull
        @Override
        public DanmuViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(context).inflate(R.layout.danmu_item, viewGroup, false);

            return new DanmuViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DanmuViewHolder danmuViewHolder, int i) {
            danmuViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
            danmuViewHolder.itemView.setTag(i);
            DanmuItem danmu = danmus.get(i);

            if(danmu.cmd.equals("DANMU_MSG")){
                SpannableString snString = new SpannableString(danmu.userName+" : "+danmu.danmuText);
                snString.setSpan(new ForegroundColorSpan(Color.parseColor("#42b7e8")),0, danmu.userName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                danmuViewHolder.danmuTextView.setText(snString);
            } else if(danmu.cmd.equals("SEND_GIFT")){
                SpannableString snString = new SpannableString(danmu.giftUserName + " 赠送 " + danmu.giftNum + " 个" + danmu.giftName);
                snString.setSpan(new ForegroundColorSpan(Color.parseColor("#42b7e8")),0, danmu.giftUserName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                danmuViewHolder.danmuTextView.setText(snString);
            } else if(danmu.cmd.equals("WELCOME")){
                //danmuViewHolder.danmuTextView.setText(danmu.);
            }

        }
        public void addDanmu(DanmuItem danmu) {
            danmus.add(danmu);
        }
        public void clear() {
            danmus.clear();
        }
        @Override
        public int getItemCount() {
            return danmus.size();
        }
    }


    class DanmuViewHolder extends RecyclerView.ViewHolder {
        TextView danmuTextView;
        public DanmuViewHolder(View itemView) {
            super(itemView);
            danmuTextView = itemView.findViewById(R.id.danmu_item_danmutext);
        }
    }

    public class JWebSocketClient extends WebSocketClient {
        public JWebSocketClient(URI serverUri) {
            super(serverUri, new Draft_6455());
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Log.e(logTag, "onOpen() " + handshakedata.getHttpStatusMessage());
            try {
                String authString = "{\"uid\": 0,\"roomid\": " + roomId +",\"protover\": 1,\"platform\": \"web\",\"clientver\": \"1.8.5\"}";
                LivePacket packet = LivePacket.createAuthPacket(authString);
                ByteBuffer bf = packet.toBuffer();
                this.send(bf);
                //byte[] data = bf.array().;
                //Log.i(logTag, "bf:"+new String(data));
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

        }

        @Override
        public void onError(Exception ex) {
            Log.e(logTag, "onError()");
            ex.printStackTrace();
        }
    }
    public static class LivePacket {
        int packetLenght = 0;
        int headerLength = 16; //  默认16长度头
        int protocolVersion = 1;
        int packetType = 0;
        int sequence = 1;
        String packetData = "";

        private LivePacket() {

        }
        public LivePacket(ByteBuffer buffer) {
            //this = new LivePacket();
            packetLenght = buffer.getInt(0);
            int nums = buffer.getInt(4);
            headerLength = nums >> 16;
            protocolVersion = (nums << 16) >> 16; // 反复横跳
            packetType = buffer.getInt(8);
            sequence = buffer.getInt(12);

            //Log.e(logTag, "packetLenght " +packetLenght);
            //Log.e(logTag, "headerLength " +headerLength);
            //Log.e(logTag, "protocolVersion " +protocolVersion);
            //Log.e(logTag, "packetType " +packetType);
            //Log.e(logTag, "sequence " +sequence);

            packetData = "";
            if(packetLenght > headerLength) {
                String dataString = new String(buffer.array());
                if(dataString.indexOf("{") != -1 && dataString.indexOf("}")!= -1) {
                    dataString = dataString.substring(dataString.indexOf("{"), dataString.lastIndexOf("}")+1);
                    packetData = dataString;
                }
                Log.i(logTag, "packetData : " + packetData);
            }
        }

        public static LivePacket createPacket(PacketType type) {
            LivePacket packet = new LivePacket();
            packet.packetType = type.id;
            return packet;
        }
        // 创建认证包
        public static LivePacket createAuthPacket(String dataJson) {
            LivePacket packet = createPacket(PacketType.JOIN_ROOM);
            packet.packetData = dataJson;
            return packet;
        }

        /**
         * 转换为封包
         * @return
         */
        public ByteBuffer toBuffer() {
            // 总包长
            packetLenght = headerLength + packetData.length();
            // 创建封包缓冲
            ByteBuffer bf =  ByteBuffer.allocate(packetLenght);
            //ByteBuffer的一个Int（Byte）为四位8bit数据。

            // 第一个Int装入总包长数据
            bf.putInt(0,packetLenght);
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
    enum PacketType {
        CLIENT_HEARTBEAT(2), COMMAND(5), JOIN_ROOM(7), SERVER_HEARTBEAT(8);
        int id;
        PacketType(int id){
            this.id = id;
        }

    }
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
                handler.postDelayed(heartBeatRunnable, 30000);
            } else {
                handler.post(stopConnection);
            }

        }
    };
    Runnable stopConnection = new Runnable() {
        @Override
        public void run() {
            client.close();
            handler.removeCallbacks(heartBeatRunnable);
            startButton.setText("START");
            hasStart = false;
            Toast.makeText(getApplicationContext(), "已停止", Toast.LENGTH_SHORT).show();
        }
    };
    Runnable updateDanmuListRunnable = new Runnable() {
        @Override
        public void run() {
            adapter.notifyDataSetChanged();
            listView.scrollToPosition(adapter.getItemCount()-1);
        }
    };
}
