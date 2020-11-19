package com.windworkshop.bphime;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;

public class SettingActivity extends AppCompatActivity {
    EditText userCookieEncodeText;
    Button userCookieSaveButton;
    SharedPreferences sp;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_activity);
        sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        userCookieEncodeText = findViewById(R.id.setting_user_login_info_text);
        userCookieEncodeText.setText(sp.getString("cookie_raw", ""));

        userCookieSaveButton = findViewById(R.id.setting_user_login_info_save_button);
        userCookieSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String cookieRaw = userCookieEncodeText.getText().toString();
                    MainModule.showLog(""+cookieRaw);
                    String cookieRawString = Base64.decode(cookieRaw, Base64.DEFAULT).toString();
                    MainModule.showLog(""+cookieRawString);
                    String[] cookiesString = cookieRawString.split("; ");
                    HashMap<String, String> cookies = new HashMap<>();
                    for(String cookieString : cookiesString) {
                        MainModule.showLog(""+cookieString);
                        String cookieKey = cookieString.substring(0, cookieString.indexOf("="));
                        String cookieValue = cookieString.substring(cookieString.indexOf("=")+1, cookieString.length());
                        cookies.put(cookieKey, cookieValue);
                    }

                    MainModule.showLog("sid=" + cookies.get("sid"));
                    MainModule.showLog("DedeUserID=" + cookies.get("DedeUserID"));
                    MainModule.showLog("DedeUserID__ckMd5=" + cookies.get("DedeUserID__ckMd5"));
                    MainModule.showLog("SESSDATA=" + cookies.get("SESSDATA"));
                    MainModule.showLog("bili_jct=" + cookies.get("bili_jct"));
                    MainModule.showLog("LIVE_BUVID=" + cookies.get("LIVE_BUVID"));
                    /*
                    sp.edit().putString("sid", cookies.get("sid"))
                            .putString("DedeUserID", cookies.get("DedeUserID"))
                            .putString("DedeUserID__ckMd5", cookies.get("DedeUserID__ckMd5"))
                            .putString("SESSDATA", cookies.get("SESSDATA"))
                            .putString("bili_jct", cookies.get("bili_jct"))
                            .putString("LIVE_BUVID", cookies.get("LIVE_BUVID"))
                            .apply();
                     */
                    Toast.makeText(getApplicationContext(), "用户信息保存成功", Toast.LENGTH_SHORT).show();
                } catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "内容错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
