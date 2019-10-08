package com.windworkshop.bphime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DanmuItem {
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
        // 初始化弹幕并进行分类
        try {
            JSONObject json = new JSONObject(danmuData);
            // 取得弹幕类型
            cmd = json.getString("cmd");
            if(cmd.equals("DANMU_MSG")){ // 正常弹幕
                JSONArray info = json.getJSONArray("info");
                danmuText = info.getString(1);
                userName = info.getJSONArray(2).getString(1);
            } else if(cmd.equals("SEND_GIFT")) { // 赠送礼物
                JSONObject data = json.getJSONObject("data");
                giftName = data.getString("giftName");
                giftNum = data.getInt("num");
                giftUserName = data.getString("uname");
            } else if(cmd.equals("WELCOME")) { // 大佬入场
                JSONObject data = json.getJSONObject("data");
                welcomeName = data.getString("uname");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
