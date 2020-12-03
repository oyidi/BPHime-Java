package com.windworkshop.bphime.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import com.windworkshop.bphime.object.DanmuItem;

import java.util.ArrayList;

public class LiveHistoryDatabase {

    LiveHistoryDataBaseOpenHelper dbHelper;
    public LiveHistoryDatabase(Context context) {
        dbHelper = new LiveHistoryDataBaseOpenHelper(context,"history.db", null, 1);
    }
    public void addDanmuHistory(String roomId, DanmuItem danmu, long time) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("room_id", roomId);
        cv.put("uid", danmu.getUid());
        cv.put("username", danmu.getUserName());
        cv.put("danmu_text", danmu.getDanmuText());
        cv.put("revice_time", time);
        db.insert("danmu", null, cv);
        //db.execSQL("insert into danmu ('room', ) values ('"+roomId+"', danmu)");
    }
    public ArrayList<DanmuItem> loadDanmuHistory(int page) {
        page = page * 100;
        ArrayList<DanmuItem> danmus = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from danmu order by id desc limit ?, 100", new String[]{""+page});
        //cursor.moveToFirst();
        while (cursor.moveToNext()) {
            DanmuItem item = new DanmuItem(cursor.getInt(cursor.getColumnIndex("uid")),
                    cursor.getString(cursor.getColumnIndex("username")),
                    cursor.getString(cursor.getColumnIndex("danmu_text")),
                    cursor.getLong(cursor.getColumnIndex("revice_time")));
            danmus.add(item);
        }
        return danmus;
    }

    public void addGiftHistory(String roomId, DanmuItem danmu, long time) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("room_id", roomId);
        cv.put("uid", danmu.getUid());
        cv.put("username", danmu.getGiftUserName());
        cv.put("gift_name", danmu.getGiftName());
        cv.put("gift_count", danmu.getGiftNum());
        cv.put("revice_time", time);
        db.insert("gift", null, cv);
    }
    public ArrayList<DanmuItem> loadGiftHistory(int page) {
        page = page * 100;
        ArrayList<DanmuItem> danmus = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from gift order by id desc limit ?, 100", new String[]{""+page});
        //cursor.moveToFirst();
        while (cursor.moveToNext()) {
            DanmuItem item = new DanmuItem(cursor.getInt(cursor.getColumnIndex("uid")),
                    cursor.getString(cursor.getColumnIndex("username")),
                    cursor.getString(cursor.getColumnIndex("gift_name")),
                    cursor.getInt(cursor.getColumnIndex("gift_count")),
                    cursor.getLong(cursor.getColumnIndex("revice_time")));
            danmus.add(item);
        }
        return danmus;
    }
    public ArrayList<DanmuItem> groupGiftHistory() {
        ArrayList<DanmuItem> danmus = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select uid, username, gift_name, sum(gift_count) as count from gift group by username, gift_name order by uid desc ", new String[]{});
        //cursor.moveToFirst();
        while (cursor.moveToNext()) {
            DanmuItem item = new DanmuItem(cursor.getInt(cursor.getColumnIndex("uid")),
                    cursor.getString(cursor.getColumnIndex("username")),
                    cursor.getString(cursor.getColumnIndex("gift_name")),
                    cursor.getInt(cursor.getColumnIndex("count")),
                    cursor.getLong(0));
            danmus.add(item);
        }
        return danmus;
    }

    class LiveHistoryDataBaseOpenHelper extends SQLiteOpenHelper {

        public LiveHistoryDataBaseOpenHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase sqLiteDatabase) {
            sqLiteDatabase.execSQL("create table if not exists danmu(id int primary key autoincrement, room_id text, uid int, username text, danmu_text text, revice_time int)");
            sqLiteDatabase.execSQL("create table if not exists gift(id int primary key autoincrement, room_id text, uid int, username text, gift_name text, gift_count int, revice_time int)");
            sqLiteDatabase.execSQL("create table if not exists interact(id int primary key autoincrement, room_id text, uid int, username text, revice_time int)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        }
    }
}
