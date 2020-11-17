package com.windworkshop.bphime;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Inflater;

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
    /**
     * @param inputByte 待解压缩的字节数组
     * @return 解压缩后的字节数组
     * @throws IOException
     */
    public static byte[] uncompress(byte[] inputByte) throws IOException {
        int len = 0;
        Inflater infl = new Inflater();
        infl.setInput(inputByte);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] outByte = new byte[1024];
        try {
            while (!infl.finished()) {
                // 解压缩并将解压缩后的内容输出到字节输出流bos中
                len = infl.inflate(outByte);
                if (len == 0) {
                    break;
                }
                bos.write(outByte, 0, len);
            }
            infl.end();
        } catch (Exception e) {
            //
        } finally {
            bos.close();
        }
        return bos.toByteArray();
    }
}
