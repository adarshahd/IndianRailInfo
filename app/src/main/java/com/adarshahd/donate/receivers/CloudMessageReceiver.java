package com.adarshahd.donate.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.adarshahd.donate.R;
import com.adarshahd.donate.activities.MainActivity;
import com.google.android.gms.gcm.GcmReceiver;

import java.util.ArrayList;

/**
 * Created by ahd on 16/11/15.
 */
public class CloudMessageReceiver extends GcmReceiver {
    Context mContext;
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        mContext = context;

        Bundle bundle = intent.getExtras();
        String customString = bundle.getString("custom");
        if(customString!= null && customString.contains("mandatory_update")){
            PreferenceManager.getDefaultSharedPreferences(mContext).edit().putBoolean("mandatory_update",true).apply();
        }
        String message = bundle.getString("message");
        if(message != null && !message.equals("")){
            ArrayList<String> messageList = new ArrayList<>();
            String [] messageArray = message.split(" ");
            String temp = "";
            Integer wordCount = 0;
            for (int i=0; i<messageArray.length; ++i, ++wordCount) {
                temp += messageArray[i] + " ";
                if(wordCount == 5){
                    messageList.add(temp);
                    temp = "";
                    wordCount = 0;
                }
            }
            messageList.add(temp);
            showNotification("IndianRailInfo", message, messageList);
        }
    }

    private void showNotification(String title, String contentText, ArrayList<String> inboxVal) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher).
                setContentTitle(title).
                setContentText(contentText);
        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        for (String str : inboxVal) {
            inboxStyle.addLine(str);
        }
        notificationBuilder.setStyle(inboxStyle);
        notificationBuilder.setOnlyAlertOnce(true);
        Intent intent = new Intent(mContext, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        notificationBuilder.setContentIntent(pendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notify = notificationBuilder.build();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        notify.sound = Uri.parse(preferences.getString("notification_ringtone", "content://settings/system/notification_sound"));
        notify.ledARGB = 0xFF00FF00;
        notify.ledOffMS = 10000;
        notify.ledOnMS = 1500;
        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        notify.flags |= Notification.FLAG_SHOW_LIGHTS;
        if(preferences.getBoolean("notification_vibrate",true)) {
            notify.vibrate = new long[]{0,200};
        }
        notificationManager.notify(0,notify);
    }
}
