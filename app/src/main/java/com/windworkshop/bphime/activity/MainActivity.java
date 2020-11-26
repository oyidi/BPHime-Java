package com.windworkshop.bphime.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.baidu.mobstat.StatService;
import com.windworkshop.bphime.object.DanmuItem;
import com.windworkshop.bphime.service.NotificationService;
import com.windworkshop.bphime.R;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //LogUtils.i( "client handle:"+ msg.what);
            if(msg.what == NotificationService.START_CONNECTION_FINISH) {

            } else if(msg.what == NotificationService.START_CONNECTION_SUCCESS) {
                startButton.setEnabled(true);
                Toast.makeText(getApplicationContext(), "启动成功", Toast.LENGTH_SHORT).show();
                startButton.setText(R.string.stop);
            } else if(msg.what == NotificationService.RECIVE_DANMU) {
                DanmuItem danmu = (DanmuItem) msg.obj;
                if(danmu.getCmd() != null) {
                    if(danmu.getCmd().equals("DANMU_MSG") || danmu.getCmd().equals("SEND_GIFT")) {
                        adapter.addDanmu(danmu);
                        handler.post(updateDanmuListRunnable);
                    }
                }
            } else if(msg.what == NotificationService.STOP_CONNECTION) {
                startButton.setText(R.string.start);
                startButton.setEnabled(true);
            } else if(msg.what == NotificationService.RELOAD_STATUE) {
                boolean hasStart = msg.getData().getBoolean("hasStart");
                if(hasStart == true) {
                    startButton.setText(R.string.stop);
                    startButton.setEnabled(true);
                } else {
                    startButton.setText(R.string.start);
                    startButton.setEnabled(true);
                }
            }
        }
    };
    BroadcastReceiver serverPing = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int action = intent.getIntExtra("action", 0);
           // LogUtils.i( "client handle:" + action);
            if(action == NotificationService.START_CONNECTION_FINISH) {

            } else if(action == NotificationService.START_CONNECTION_SUCCESS) { // 接收到新弹幕
                startButton.setEnabled(true);
                Toast.makeText(getApplicationContext(), "启动成功", Toast.LENGTH_SHORT).show();
                startButton.setText(R.string.stop);
            } else if(action == NotificationService.RECIVE_DANMU) {
                boolean isLog = intent.getBooleanExtra("isLog", false);
                if(isLog == true) { // 日志
                    DanmuItem danmu = intent.getParcelableExtra("log");//new DanmuItem("log", logString, time);
                    adapter.addDanmu(danmu);
                    handler.post(updateDanmuListRunnable);
                } else { // 普通弹幕
                    DanmuItem danmu = intent.getParcelableExtra("danmu_item");
                    if(danmu.getCmd() != null) {
                        if(danmu.getCmd().equals("DANMU_MSG") || danmu.getCmd().equals("SEND_GIFT") || danmu.getCmd().equals("log") || danmu.getCmd().equals("INTERACT_WORD")) {
                            if(danmu.getCmd().equals("INTERACT_WORD")) {
                                if(isMemberIn) {
                                    adapter.addDanmu(danmu);
                                }
                            } else {
                                adapter.addDanmu(danmu);
                            }
                            handler.post(updateDanmuListRunnable);
                        }
                    }
                }
            } else if(action == NotificationService.STOP_CONNECTION) {
                startButton.setText(R.string.start);
                startButton.setEnabled(true);
            } else if(action == NotificationService.LOAD_REMOTE_HISTORY) { // 加载服务端提供的历史弹幕
                adapter.clear();
                ArrayList<DanmuItem> danmuList = intent.getParcelableArrayListExtra("history_danmus");
                for(DanmuItem danmuData : danmuList){
                    adapter.addDanmu(danmuData);
                }
                handler.post(updateDanmuListRunnable);

            } else if(action == NotificationService.RELOAD_STATUE) { // 重新加载弹幕内容
                    boolean hasStart = intent.getBooleanExtra("hasStart", false);
                    LogUtils.i( "client RELOAD_STATUE:" + hasStart);
                    if(hasStart == true) { // 设置按钮状态
                        startButton.setText(R.string.stop);
                        startButton.setEnabled(true);
                    } else {
                        startButton.setText(R.string.start);
                        startButton.setEnabled(true);
                    }
                    // 恢复弹幕到列表
                    adapter.clear();
                    isMemberIn = sp.getBoolean("is_show_member_in",false);
                    ArrayList<DanmuItem> danmuItems = intent.getParcelableArrayListExtra("danmu_items");
                    for(DanmuItem danmu : danmuItems){
                        if(danmu.getCmd() != null) {
                            if(danmu.getCmd().equals("DANMU_MSG") || danmu.getCmd().equals("SEND_GIFT") || danmu.getCmd().equals("log") || danmu.getCmd().equals("INTERACT_WORD")) {
                                if(danmu.getCmd().equals("INTERACT_WORD")) {
                                    if(isMemberIn) {
                                        adapter.addDanmu(danmu);
                                    }
                                } else {
                                    adapter.addDanmu(danmu);
                                }
                            }
                        }
                    }
                    handler.post(updateDanmuListRunnable);
            }
        }
    };

    DanmuListAdapter adapter;
    RecyclerView listView;
    ArrayList<DanmuItem> mainDanmue = new ArrayList<DanmuItem>();
    EditText roomIdEdittext;
    Button startButton;
    CheckBox vibrateCheck;
    EditText danmuSendEdittext;
    Button danmuSendButton;

    Toolbar toolbar;
    DrawerLayout drawerLayout;
    NavigationView navigationView;

    SharedPreferences sp;
    boolean hasloaded = false; // 初次载入
    boolean isMemberIn = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StatService.setAuthorizedState(getApplicationContext(), false);
        sp = getSharedPreferences("config",Context.MODE_PRIVATE);
        adapter = new DanmuListAdapter(getApplicationContext(), mainDanmue);
        listView = findViewById(R.id.danmu_list);
        listView.setAdapter(adapter);
        listView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        roomIdEdittext = findViewById(R.id.room_id_edittext);
        roomIdEdittext.setText(sp.getString("roomid", ""));
        roomIdEdittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if(!hasFocus){
                    InputMethodManager manager = ((InputMethodManager)getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE));
                    if (manager != null)
                        manager.hideSoftInputFromWindow(view.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });
        startButton = findViewById(R.id.start_revice_danmu_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomId = roomIdEdittext.getText().toString();
                Intent pongIntent = new Intent(NotificationService.FOR_SERVICE).putExtra("action", NotificationService.START_CONNECTION).putExtra("roomId", roomId);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(pongIntent);
                startButton.setEnabled(false);
                sp.edit().putString("roomid", roomId).commit();
            }
        });
        vibrateCheck = findViewById(R.id.vibrate_check);
        vibrateCheck.setChecked(sp.getBoolean("vibrate", false));
        vibrateCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sp.edit().putBoolean("vibrate", isChecked).commit();
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(NotificationService.FOR_SERVICE).putExtra("action", NotificationService.REFRESH_CONFIG));
            }
        });

        danmuSendEdittext = findViewById(R.id.send_danmu_context);
        danmuSendButton = findViewById(R.id.send_danmu_button);
        danmuSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!sp.getString("LIVE_BUVID", "").equals("")) {
                    String danmu = danmuSendEdittext.getText().toString();
                    if(!danmu.equals("")) {
                        if(danmu.length() <= 30) {
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(NotificationService.FOR_SERVICE).putExtra("action", NotificationService.SEND_DANMU).putExtra("send_danmu", danmuSendEdittext.getText().toString()));
                            danmuSendEdittext.setText("");
                        } else {
                            Toast.makeText(getApplicationContext(), "弹幕超出30个字符长度", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "弹幕为空", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "尚未登录，需要在设置中登录账号才能发送弹幕", Toast.LENGTH_SHORT).show();
                }
            }
        });
        drawerLayout = findViewById(R.id.main_drawer_layout);
        navigationView = findViewById(R.id.main_navigation_view);
        navigationView.setItemIconTintList(null);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                if(menuItem.getItemId() == R.id.main_menu_histoty) {
                    startActivity(new Intent(getApplicationContext(), HistoryDanmuActivity.class));
                } else if(menuItem.getItemId() == R.id.main_menu_setting) {
                    startActivity(new Intent(getApplicationContext(), SettingActivity.class));
                }
                return true;
            }
        });

        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open,R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.i( "onResume");
        if(!isServiceRun(getApplicationContext(), "com.windworkshop.bphime.service.NotificationService")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplicationContext().startForegroundService(new Intent(this, NotificationService.class));
            } else {
                getApplicationContext().startService(new Intent(this, NotificationService.class));
            }
        }
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(serverPing, new IntentFilter(NotificationService.FOR_CLIENT));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(NotificationService.FOR_SERVICE).putExtra("action", NotificationService.RELOAD_STATUE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.i( "onPause");
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(serverPing);
    }

    @Override
    protected void onStop() {
        super.onStop();

    }




    /**
     * 弹幕接收显示处理
     */
    Runnable updateDanmuListRunnable = new Runnable() {
        @Override
        public void run() {
            adapter.setShowSendingTime(sp.getBoolean("is_show_send_time",false));
            adapter.setShowMemberIn(isMemberIn);
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
