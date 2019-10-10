package com.windworkshop.bphime;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MainModule {
    static String logTag = "BP-Hime";
    static Context context;
    public static void setContext(Context context){
        MainModule.context = context;

    }
    public static void showLog(String log){
        Log.i(logTag, "BP-Hime");
        if(context != null){
            Intent intent = new Intent(NotificationService.FOR_CLIENT);
            intent.putExtra("action", NotificationService.RECIVE_DANMU);
            intent.putExtra("danmu_string", log);
            intent.putExtra("isLog", true);
            context.sendBroadcast(intent);
        }
    }
}
