package com.adarshahd.indianrailinfo.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.adarshahd.indianrailinfo.R;
import com.adarshahd.indianrailinfo.activities.MainActivity;
import com.adarshahd.indianrailinfo.models.pnr.Passenger;
import com.adarshahd.indianrailinfo.models.pnr.PnrStatus;
import com.adarshahd.indianrailinfo.utilities.Utility;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by ahd on 4/6/15.
 */
public class PNRTracker extends BroadcastReceiver{

    public static final int ALARM_RECEIVER = 777;
    public static final String NO_NETWORK = "no_network";
    public static final String ERROR = "error";
    private static final String SUCCESS = "success";
    private static Context mContext;

    private SharedPreferences mPrefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        ArrayList<String> list = Utility.getTrackedPNRs(context);
        for (String pnrNumber : list) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                try {
                    new PNRStatus().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,pnrNumber);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    new PNRStatus().execute(pnrNumber);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private class PNRStatus extends AsyncTask<String,Void,String> {
        private String pnrNumber = "";
        private PnrStatus pnrStatus;
        private String errorText;
        @Override
        protected String doInBackground(String... params) {
            pnrNumber = params[0];
            if(!Utility.isConnected(mContext)) {
                return NO_NETWORK;
            }

            String jsonData = Utility.getPNRStatus(pnrNumber);

            Gson gson = new GsonBuilder().create();
            try {
                pnrStatus = gson.fromJson(jsonData,PnrStatus.class);
                if (pnrStatus.getReservedUpto() == null) {
                    throw new JsonParseException("Failed to parse");
                }
            } catch (JsonParseException e) {
                try {
                    JSONObject object = new JSONObject(jsonData);
                    if (object.has("error")){
                        errorText = object.getString("error");
                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                    errorText = "error";
                    return ERROR;
                }
                return ERROR;
            }

            int canCount = 0;
            int cnfCount = 0;
            Passenger [] passengers = pnrStatus.getPassengers();
            for(Passenger passenger : passengers) {
                if(passenger.getCurrentStatus().contains("CNF")){
                    ++cnfCount;
                }
                if (passenger.getCurrentStatus().contains("CAN")){
                    ++canCount;
                }
            }

            if(canCount == passengers.length){
                ArrayList<String> message = new ArrayList<>();
                message.add("All the seats for the PNR number ");
                message.add(pnrNumber + " has been cancelled.");
                message.add("Deleting the same from database");

                if (mPrefs.getBoolean("enable_notification",true)) {
                    showNotification("PNR Info","All tickets are cancelled",message,pnrNumber);
                }
                ArrayList<String> list = new ArrayList<>();
                list.add(pnrNumber);
                Utility.unTrackPNRs(mContext, list);
            }

            if (cnfCount == passengers.length) {
                ArrayList<String> message = new ArrayList<>();
                message.add("Congratulations! Your train ticket");
                message.add("with PNR number " + pnrNumber );
                message.add("has been confirmed.");
                message.add("Click here to check the booking status!");
                if (mPrefs.getBoolean("enable_notification",true)) {
                    showNotification("Congratulations!","Your ticket is Confirmed!",message,pnrNumber);
                }

                //Since the ticket is confirmed, remove this particular PNR from tracking database
                ArrayList<String> list = new ArrayList<>();
                list.add(pnrNumber);
                Utility.unTrackPNRs(mContext, list);
            }

            return SUCCESS;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.equals(ERROR)){
                Log.e("PNRTracker", "Error retrieving pnr status: " + errorText);
                return;
            }

            if(result.equals(NO_NETWORK)){
                Log.e("PNRTracker", "Could not connect to server at this time, please try later");
                return;
            }
            if(!pnrNumber.equals("")) {
                //Copy the PNR number to clipboard so that user can paste it directly to the edit box!
                if((Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1)){
                    ((android.text.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setText(pnrNumber);
                } else {
                    ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PNR", pnrNumber));
                }
            }
            super.onPostExecute(result);
        }
    }

    private void showNotification(String title, String contentText, ArrayList<String> inboxVal, String pnrNumber) {
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
        intent.setAction("open_pnr_status");
        intent.putExtra("pnr_number", pnrNumber);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
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
        //notify.sound = Uri.parse("content://settings/system/notification_sound");
        notify.sound = Uri.parse(mPrefs.getString("notification_ringtone", "content://settings/system/notification_sound"));
        notify.ledARGB = 0xFF00FF00;
        notify.ledOffMS = 10000;
        notify.ledOnMS = 1500;
        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        notify.flags |= Notification.FLAG_SHOW_LIGHTS;
        if(mPrefs.getBoolean("notification_vibrate",true)) {
            notify.vibrate = new long[]{0,200};
        }
        notificationManager.notify(0,notify);
    }
}
