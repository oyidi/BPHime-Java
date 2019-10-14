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
        Log.i(logTag, log);
        /*
        if(context != null){
            Intent intent = new Intent(NotificationService.FOR_SERVICE);
            intent.putExtra("action", NotificationService.RECIVE_LOG);
            intent.putExtra("log", log);
            context.sendBroadcast(intent);
        }
*/
    }
}
