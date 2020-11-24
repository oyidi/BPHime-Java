package com.windworkshop.bphime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {
    SharedPreferences sp;
    Toolbar toolbar;
    CheckBox showSendTime, showMemberIn;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_activity);
        sp = getSharedPreferences("config", Context.MODE_PRIVATE);

        showSendTime = findViewById(R.id.setting_show_send_time);
        showSendTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sp.edit().putBoolean("is_show_send_time", b).apply();
            }
        });
        showSendTime.setChecked(sp.getBoolean("is_show_send_time", false));

        showMemberIn = findViewById(R.id.setting_show_member_in);
        showMemberIn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sp.edit().putBoolean("is_show_member_in", b).apply();
            }
        });
        showMemberIn.setChecked(sp.getBoolean("is_show_member_in", false));
        toolbar = (Toolbar) findViewById(R.id.main_normal_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle("设置");

        Button webLoginButton = findViewById(R.id.setting_user_web_login_button);
        webLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), WebLoginActivity.class));
            }
        });

        Button clearButton = findViewById(R.id.setting_user_web_login_clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                } else {
                    CookieSyncManager cookieSyncMngr=CookieSyncManager.createInstance(getApplicationContext());
                    cookieSyncMngr.startSync();
                    CookieManager cookieManager=CookieManager.getInstance();
                    cookieManager.removeAllCookie();
                    cookieManager.removeSessionCookie();
                    cookieSyncMngr.stopSync();
                    cookieSyncMngr.sync();
                }
                sp.edit().remove("sid")
                        .remove("DedeUserID")
                        .remove("DedeUserID__ckMd5")
                        .remove("SESSDATA")
                        .remove("bili_jct")
                        .remove("LIVE_BUVID")
                        .apply();
                Toast.makeText(getApplicationContext(), "用户数据已清理", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                this.finish(); break;
        }
        return super.onOptionsItemSelected(item);
    }
}
