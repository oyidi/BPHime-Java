package com.windworkshop.bphime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

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
    public ArrayList<DanmuItem> loadHistory(int page) {
        page = page * 100;
        ArrayList<DanmuItem> danmus = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from danmu order by id desc limit ?, 100", new String[]{""+page});
        //cursor.moveToFirst();
        while (cursor.moveToNext()) {
            DanmuItem item = new DanmuItem(cursor.getString(cursor.getColumnIndex("username")), cursor.getString(cursor.getColumnIndex("context")),
                    cursor.getLong(cursor.getColumnIndex("revice_time")));
            danmus.add(item);
        }
        return danmus;
    }
}
