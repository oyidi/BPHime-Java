package com.windworkshop.bphime;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class NotificationService extends Service {
    Messenger clientMessenger;
    Messenger mMessenger;
    @Override
    public IBinder onBind(Intent intent) {
        mMessenger = new Messenger(new ServiceMessageHandler(this));
        return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    class ServiceMessageHandler extends Handler {
        Context context;
        ServiceMessageHandler(Context context) {
            this.context = context;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i(MainActivity.logTag, "service handle:"+ msg.what);

            Message testMessage = Message.obtain(msg);
            testMessage.what = 999;

            clientMessenger = msg.replyTo;
            if(clientMessenger != null) {
                try {
                    clientMessenger.send(testMessage);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
