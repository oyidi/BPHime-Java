package com.windworkshop.bphime.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.apkfuns.logutils.LogUtils;
import com.windworkshop.bphime.R;

import java.util.HashMap;
import java.util.Map;

public class WebLoginActivity extends AppCompatActivity {
    WebView web;
    SharedPreferences sp;
    ConstraintLayout loadingView;
    Toolbar toolbar;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_login_activity);
        sp = getSharedPreferences("config", Context.MODE_PRIVATE);
        toolbar = (Toolbar) findViewById(R.id.main_normal_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle("浏览器登录");

        web = findViewById(R.id.web_login_webview);
        WebSettings webSettings = web.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAppCacheEnabled(true);
        web.setWebViewClient(new WebClient());

        loadingView = findViewById(R.id.web_login_loading_view);

        LogUtils.i("web login start");
        web.loadUrl("https://passport.bilibili.com/login?gourl=https://live.bilibili.com/h5");
    }
    private class WebClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            loadingView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            LogUtils.i("load url Finished:" + url);
            HashMap<String, String> cookies = checkCookie("https://bilibili.com/");
            if(cookies.containsKey("SESSDATA")) { // 已登录
                if(cookies.containsKey("LIVE_BUVID")) { // 登录过后打开过直播页面有发弹幕权限
                    // 确认所需cookie都在
                    if(cookies.containsKey("bili_jct") && cookies.containsKey("DedeUserID") && cookies.containsKey("DedeUserID__ckMd5") && cookies.containsKey("sid")) {
                        LogUtils.i("sid=" + cookies.get("sid"));
                        LogUtils.i("DedeUserID=" + cookies.get("DedeUserID"));
                        LogUtils.i("DedeUserID__ckMd5=" + cookies.get("DedeUserID__ckMd5"));
                        LogUtils.i("SESSDATA=" + cookies.get("SESSDATA"));
                        LogUtils.i("bili_jct=" + cookies.get("bili_jct"));
                        LogUtils.i("LIVE_BUVID=" + cookies.get("LIVE_BUVID"));

                        sp.edit().putString("sid", cookies.get("sid"))
                                .putString("DedeUserID", cookies.get("DedeUserID"))
                                .putString("DedeUserID__ckMd5", cookies.get("DedeUserID__ckMd5"))
                                .putString("SESSDATA", cookies.get("SESSDATA"))
                                .putString("bili_jct", cookies.get("bili_jct"))
                                .putString("LIVE_BUVID", cookies.get("LIVE_BUVID"))
                                .apply();

                        Toast.makeText(getApplicationContext(), "登录成功", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    web.loadUrl("https://live.bilibili.com/h5/");
                }
            }
            loadingView.setVisibility(View.GONE);
            //checkCookie("https://live.bilibili.com/");
            //checkCookie("https://passport.bilibili.com");
        }
    }
    private HashMap<String, String> checkCookie(String site) {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookieRawString = cookieManager.getCookie(site);
        LogUtils.i("site:" + site);
        LogUtils.i("cookies:" + cookieRawString);
        HashMap<String, String> cookies = new HashMap<>();
        if(cookieRawString != null){
            String[] cookiesString = cookieRawString.split("; ");

            for(String cookieString : cookiesString) {
                String cookieKey = cookieString.substring(0, cookieString.indexOf("="));
                String cookieValue = cookieString.substring(cookieString.indexOf("=")+1, cookieString.length());
                cookies.put(cookieKey, cookieValue);
            }
            for(Map.Entry<String, String> data :cookies.entrySet()) {
                LogUtils.i("k:" + data.getKey() + " v:" + data.getValue());
            }
        }
        return cookies;
    }
}
