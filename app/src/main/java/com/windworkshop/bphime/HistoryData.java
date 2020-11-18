package com.windworkshop.bphime;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class HistoryData {
    HistoryDataBaseOpenHelper dbHelper;
    public HistoryData(Context context) {
        dbHelper = new HistoryDataBaseOpenHelper(context,"history.db", null, 1);
    }
    public void addHistory(String roomId, DanmuItem danmu, long time) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("room_id", roomId);
        cv.put("username", danmu.userName);
        cv.put("context", danmu.danmuText);
        cv.put("revice_time", time);
        db.insert("danmu", null, cv);
        //db.execSQL("insert into danmu ('room', ) values ('"+roomId+"', danmu)");
    }
}
