package com.windworkshop.bphime.object;

import android.os.Parcel;
import android.os.Parcelable;

import com.windworkshop.bphime.service.NotificationService;

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
    long receiveTime;
    String receiveTimeString;
    long uid;
    public DanmuItem(String cmd, String log, String time) {
        this.cmd = cmd;
        this.danmuText = log;
        this.userName = time;
    }

    /**
     * 数据库加载弹幕用
     * @param username
     * @param text
     * @param time
     */
    public DanmuItem(long uid, String username, String text, long time) {
        this.cmd = "DANMU_MSG";
        this.uid = uid;
        this.userName = username;
        this.danmuText = text;
        this.receiveTime = time;
        this.receiveTimeString = NotificationService.sdf.format(receiveTime);
    }

    /**
     * 数据库加载礼物记录用
     * @param uid
     * @param giftUserName
     * @param giftName
     * @param giftNum
     */
    public DanmuItem(long uid, String giftUserName, String giftName, int giftNum, long time) {
        this.cmd = "SEND_GIFT";
        this.uid = uid;
        this.giftUserName = giftUserName;
        this.giftName = giftName;
        this.giftNum = giftNum;
        this.receiveTime = time;
        this.receiveTimeString = NotificationService.sdf.format(receiveTime);
    }

    public DanmuItem(String rawdata) {
        danmuData = rawdata;
        // 初始化弹幕并进行分类
        try {
            JSONObject json = new JSONObject(danmuData);
            if(json.has("cmd")) {
                // 取得弹幕类型
                cmd = json.getString("cmd");
                if(cmd.equals("DANMU_MSG")){ // 正常弹幕
                    JSONArray info = json.getJSONArray("info");
                    uid = info.getJSONArray(2).getLong(0);
                    danmuText = info.getString(1);
                    userName = info.getJSONArray(2).getString(1);
                    receiveTime = info.getJSONObject(9).getLong("ts")*1000;
                    receiveTimeString = NotificationService.sdfmini.format(receiveTime);
                } else if(cmd.equals("SEND_GIFT")) { // 赠送礼物
                    JSONObject data = json.getJSONObject("data");
                    uid = data.getLong("uid");
                    giftName = data.getString("giftName");
                    giftNum = data.getInt("num");
                    giftUserName = data.getString("uname");
                    receiveTime = data.getLong("timestamp")*1000;
                    receiveTimeString = NotificationService.sdfmini.format(receiveTime);
                } else if(cmd.equals("INTERACT_WORD")) { // 一般路过入场
                    JSONObject data = json.getJSONObject("data");
                    uid = data.getLong("uid");
                    userName = data.getString("uname");
                    receiveTime = data.getLong("timestamp")*1000;
                    receiveTimeString = NotificationService.sdfmini.format(receiveTime);
                } else if(cmd.equals("WELCOME")) { // 大佬入场
                    JSONObject data = json.getJSONObject("data");
                    welcomeName = data.getString("uname");
                } else if(cmd.equals("log")) {
                    danmuText = json.getString("log");
                    userName = json.getString("time");
                }
            } else {
                // 无法获知弹幕类型，返回空弹幕用于后续检测
                cmd = "EMPTY";
            }
        } catch (JSONException e) {
            e.printStackTrace();
            // 错误的情况下返回空弹幕用于后续检测
            cmd = "EMPTY";
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
        receiveTime = in.readLong();
        receiveTimeString = in.readString();
        uid = in.readLong();
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
        parcel.writeLong(receiveTime);
        parcel.writeString(receiveTimeString);
        parcel.writeLong(uid);
    }

    public String getDanmuData() {
        return danmuData;
    }

    public void setDanmuData(String danmuData) {
        this.danmuData = danmuData;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public String getDanmuText() {
        return danmuText;
    }

    public void setDanmuText(String danmuText) {
        this.danmuText = danmuText;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getGiftName() {
        return giftName;
    }

    public void setGiftName(String giftName) {
        this.giftName = giftName;
    }

    public String getGiftUserName() {
        return giftUserName;
    }

    public void setGiftUserName(String giftUserName) {
        this.giftUserName = giftUserName;
    }

    public int getGiftNum() {
        return giftNum;
    }

    public void setGiftNum(int giftNum) {
        this.giftNum = giftNum;
    }

    public String getWelcomeName() {
        return welcomeName;
    }

    public void setWelcomeName(String welcomeName) {
        this.welcomeName = welcomeName;
    }

    public long getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(long receiveTime) {
        this.receiveTime = receiveTime;
    }

    public String getReceiveTimeString() {
        return receiveTimeString;
    }

    public void setReceiveTimeString(String receiveTimeString) {
        this.receiveTimeString = receiveTimeString;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }
}
