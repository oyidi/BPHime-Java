package com.windworkshop.bphime;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(logTag, "client handle:"+ msg.what);
            if(msg.what == NotificationService.START_CONNECTION_FINISH) {

            } else if(msg.what == NotificationService.START_CONNECTION_SUCCESS) {
                startButton.setEnabled(true);
                Toast.makeText(getApplicationContext(), "启动成功", Toast.LENGTH_SHORT).show();
                startButton.setText("STOP");
            } else if(msg.what == NotificationService.RECIVE_DANMU) {
                DanmuItem danmu = (DanmuItem) msg.obj;
                if(danmu.cmd != null) {
                    if(danmu.cmd.equals("DANMU_MSG") || danmu.cmd.equals("SEND_GIFT")) {
                        adapter.addDanmu(danmu);
                        handler.post(updateDanmuListRunnable);
                    }
                }
            } else if(msg.what == NotificationService.STOP_CONNECTION) {
                startButton.setText("START");
                startButton.setEnabled(true);
            } else if(msg.what == NotificationService.RELOAD_STATUE) {
                boolean hasStart = msg.getData().getBoolean("hasStart");
                if(hasStart == true) {
                    startButton.setText("STOP");
                    startButton.setEnabled(true);
                } else {
                    startButton.setText("START");
                    startButton.setEnabled(true);
                }
            }
        }
    };
    BroadcastReceiver serverPing = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int action = intent.getIntExtra("action", 0);
            if(action == NotificationService.START_CONNECTION_FINISH) {

            } else if(action == NotificationService.START_CONNECTION_SUCCESS) {
                startButton.setEnabled(true);
                Toast.makeText(getApplicationContext(), "启动成功", Toast.LENGTH_SHORT).show();
                startButton.setText("STOP");
            } else if(action == NotificationService.RECIVE_DANMU) {
                byte[] rawData = intent.getByteArrayExtra("danmu_byte");
                LivePacket packet = new LivePacket(ByteBuffer.wrap(rawData));
                DanmuItem danmu = new DanmuItem(packet.packetData);
                if(danmu.cmd != null) {
                    if(danmu.cmd.equals("DANMU_MSG") || danmu.cmd.equals("SEND_GIFT")) {
                        adapter.addDanmu(danmu);
                        handler.post(updateDanmuListRunnable);
                    }
                }
            } else if(action == NotificationService.STOP_CONNECTION) {
                startButton.setText("START");
                startButton.setEnabled(true);
            } else if(action == NotificationService.RELOAD_STATUE) {
                boolean hasStart = intent.getBooleanExtra("hasStart", false);
                if(hasStart == true) {
                    startButton.setText("STOP");
                    startButton.setEnabled(true);
                } else {
                    startButton.setText("START");
                    startButton.setEnabled(true);
                }
            }
        }
    };

    DanmuListAdapter adapter;
    RecyclerView listView;
    ArrayList<DanmuItem> mainDanmue = new ArrayList<DanmuItem>();
    EditText roomIdEdittext;
    Button startButton;

    SharedPreferences sp;


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
                String roomId = roomIdEdittext.getText().toString();
                Intent pongIntent = new Intent("com.windworkshop.bphime.client").putExtra("action", NotificationService.START_CONNECTION).putExtra("roomId", roomId);
                sendBroadcast(pongIntent);
                startButton.setEnabled(false);
                sp.edit().putString("roomid", roomId).commit();
            }
        });
        registerReceiver(serverPing, new IntentFilter("com.windworkshop.bphime.service"));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(logTag, "onStart");
        if(!isServiceRun(getApplicationContext(), "com.windworkshop.bphime.NotificationService")) {
            startService(new Intent(this, NotificationService.class));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(logTag, "onStop");
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
     * 弹幕接收显示处理
     */
    Runnable updateDanmuListRunnable = new Runnable() {
        @Override
        public void run() {
            adapter.notifyDataSetChanged();
            listView.scrollToPosition(adapter.getItemCount()-1);
        }
    };
    public static boolean isServiceRun(Context mContext, String className) {
        boolean isRun = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(40);
        int size = serviceList.size();
        for (int i = 0; i < size; i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRun = true;
                break;
            }
        }
        return isRun;
    }
}
