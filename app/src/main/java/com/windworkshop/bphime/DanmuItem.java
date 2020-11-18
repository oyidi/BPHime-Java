package com.windworkshop.bphime;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class DanmuItem implements Parcelable {
    String danmuData;
    String cmd;
    String danmuText;
    String userName;
    String giftName;
    String giftUserName;
    int giftNum;
    String welcomeName;
    long reciveTime;
    public DanmuItem(String cmd, String log, String time) {
        this.cmd = cmd;
        this.danmuText = log;
        this.userName = time;
    }
    public DanmuItem(String username, String text, long time) {
        this.cmd = "DANMU_MSG";
        this.userName = username;
        this.danmuText = text;
        this.reciveTime = time;
    }
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
            } else if(cmd.equals("log")) {
                danmuText = json.getString("log");
                userName = json.getString("time");
            }

        } catch (JSONException e) {
            //e.printStackTrace();
        }
    }

    protected DanmuItem(Parcel in) {
        danmuData = in.readString();
        cmd = in.readString();
        danmuText = in.readString();
        userName = in.readString();
        giftName = in.readString();
        giftUserName = in.readString();
        giftNum = in.readInt();
        welcomeName = in.readString();
        reciveTime = in.readLong();
    }

    public static final Creator<DanmuItem> CREATOR = new Creator<DanmuItem>() {
        @Override
        public DanmuItem createFromParcel(Parcel in) {
            return new DanmuItem(in);
        }

        @Override
        public DanmuItem[] newArray(int size) {
            return new DanmuItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(danmuData);
        parcel.writeString(cmd);
        parcel.writeString(danmuText);
        parcel.writeString(userName);
        parcel.writeString(giftName);
        parcel.writeString(giftUserName);
        parcel.writeInt(giftNum);
        parcel.writeString(welcomeName);
        parcel.writeLong(reciveTime);
    }
}
