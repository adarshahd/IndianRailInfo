/**
 *   Copyright (C) 2013  Adarsha HD
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 
package com.adarshahd.indianrailinfo.donate;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ahd on 6/8/13.
 */
public class PNRTracker extends BroadcastReceiver {
    private static final String ENQUIRY_PAGE = "http://www.indianrail.gov.in/cgi_bin/inet_pnrstat_cgi.cgi";
    private static final String ENQUIRY_INPUT = "lccp_pnrno1";

    private List<String> mStrPassengerDetails;
    private String mStrTrainDetails = "";
    private static SharedPreferences mPref;
    private static Context mContext;

    public static final int ALARM_RECEIVER = 777;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        mPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        ArrayList<String> list = (ArrayList<String>) PNRDatabase.getPNRDatabase(mContext).getPNRTrackList();
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

    private class PNRStatus extends AsyncTask<String,Void,Void> {
        private String pnrNumber = "";
        @Override
        protected Void doInBackground(String... params) {
            if(!Util.getUtil(mContext).isConnected()) {
                Log.i("PNRTracker","No internet connection while fetching data for " + params[0]);
                return null;
            }
            Log.i("PNRTracker","Started fetching data for " + params[0]);
            String mPageResult = "";
            HttpClient client = new DefaultHttpClient();
            ArrayList<NameValuePair> postData = new ArrayList<NameValuePair>();
            postData.add(new BasicNameValuePair(ENQUIRY_INPUT,params[0]));
            HttpPost post = new HttpPost(ENQUIRY_PAGE);
            try {
                post.setEntity(new UrlEncodedFormEntity(postData));
                HttpResponse response = client.execute(post);
                HttpEntity entity = response.getEntity();

                if(entity != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(),"UTF-8"));
                    String tmp;
                    while((tmp = reader.readLine()) != null) {
                        mPageResult += tmp;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Check the result page
            if(mPageResult.contains("FLUSHED PNR / ") || mPageResult.contains("Invalid PNR")) {
                Log.i("PNRTracker","The PNR " + params[0] +" is either invalid or expired. Removing it from tracking database ");
                //Also we should notify the user that we are removing the particular PNR from database;
                if (mPref.getBoolean("enable_notification",true)) {
                    ArrayList<String> list = new ArrayList<String>();
                    list.add("The PNR " + params[0] + " is either ");
                    list.add("invalid or expired.");
                    list.add("Removing it from tracking database");
                    showNotification("IndianRailInfo", "Invalid/Flushed PNR", list,"");
                }
                ArrayList<String> list = new ArrayList<String>();
                list.add(params[0]);
                PNRDatabase.getPNRDatabase(mContext).stopTrackingPNRs(list);
                return null;
            }
            if(mPageResult.contains("Connectivity Failure") || mPageResult.contains("try again")) {
                Log.i("PNRTracker","Connectivity failure while fetching data for " + params[0]);
                return null;
            }

            //Train details
            Elements eleTrain = Jsoup.parse(mPageResult).select("table tr tr td:containsOwn(Train Number)");
            Iterator iteTrain = eleTrain.first().parent().parent().parent().getElementsByTag("tr").iterator();
            Element tmp;
            //Get the third row for train details
            iteTrain.next();
            iteTrain.next();
            mStrTrainDetails = new String();
            if(iteTrain.hasNext()) {
                tmp = (Element) iteTrain.next();
                mStrTrainDetails = tmp.select("td").get(0).text() + " " +
                        tmp.select("td").get(1).text() + " " +
                        tmp.select("td").get(2).text() + " " +
                        tmp.select("td").get(5).text() + " " +
                        tmp.select("td").get(6).text() + " " +
                        tmp.select("td").get(7).text();
            }

            //Passenger Details
            Elements elements = Jsoup.parse(mPageResult).select("table tr td:containsOwn(S. No.)");
            Iterator iterator = elements.first().parent().parent().getElementsByTag("tr").iterator();
            mStrPassengerDetails = new ArrayList<String>();

            List<String> list;
            String passn = "";
            int current = 1;
            while(iterator.hasNext()) {
                tmp = (Element) iterator.next();
                if(tmp.toString().contains("Passenger")) {
                    passn = "" + current + ". " + tmp.select("td").get(0).text() + "   " +
                            tmp.select("td").get(1).text() + "   " +
                            tmp.select("td").get(2).text();
                    mStrPassengerDetails.add(passn);
                    ++current;
                }
            }

            //Update the details into the respective file;
            PNRDatabase.getPNRDatabase(mContext).addPNR(params[0],mStrTrainDetails,mStrPassengerDetails);
            Log.i("PNRTracker","Successfully fetched data for " + params[0]);
            iterator = mStrPassengerDetails.iterator();

            //Now lets check if all the passengers got their seats confirmed or not
            //if number of confirmed seats equals difference of total passengers and canceled seats
            //we can conclude all other passengers got their seats confirmed. OK, congratulate the user.
            int canCount = 0;
            int cnfCount = 0;
            while (iterator.hasNext()){
                String passnDetails = (String) iterator.next();
                if(passnDetails.contains("Can")){
                    ++canCount;
                }
                if(passnDetails.contains("CNF")) {
                    ++cnfCount;
                }
            }
            if(cnfCount == (mStrPassengerDetails.size() - canCount)) {
                ArrayList<String> message = new ArrayList<String>();
                message.add("All the seats for the PNR number ");
                message.add(params[0] + " has been cancelled.");
                message.add("Deleting the same from database");

                if (mPref.getBoolean("enable_notification",true)) {
                    showNotification("PNR Info","All tickets are cancelled",message,"PNRStat");
                }
                list = new ArrayList<String>();
                list.add(params[0]);
                PNRDatabase.getPNRDatabase(mContext).stopTrackingPNRs((ArrayList<String>) list);
                return null;
            }
            if(cnfCount == (mStrPassengerDetails.size() - canCount)) {
                //All passenger's seats are confirmed
                ArrayList<String> message = new ArrayList<String>();
                message.add("Congratulations! Your train ticket");
                message.add("with PNR number " + params[0] );
                message.add("has been confirmed.");
                message.add("Click here to check the booking status!");
                message.add("\n");
                message.add("Long click and select paste in the ");
                message.add("PNR Number Edit box");
                if (mPref.getBoolean("enable_notification",true)) {
                    showNotification("Congratulations!","Your ticket is Confirmed!",message,"PNRStat");
                }

                pnrNumber = params[0];

                //Since the ticket is confirmed, remove this particular PNR from tracking database
                list = new ArrayList<String>();
                list.add(params[0]);
                PNRDatabase.getPNRDatabase(mContext).stopTrackingPNRs((ArrayList<String>) list);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(!pnrNumber.equals("")) {
                //Copy the PNR number to clipboard so that user can paste it directly to the edit box!
                if((Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1)){
                    ((android.text.ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setText(pnrNumber);
                } else {
                    ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PNR",pnrNumber));
                }
            }
            super.onPostExecute(aVoid);
        }
    }

    private void showNotification(String title, String contentText, ArrayList<String>inboxVal, String activityToStart) {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext);
        notificationBuilder.setSmallIcon(R.drawable.irctc).
                setContentTitle(title).
                setContentText(contentText);
        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();
        for (String str : inboxVal) {
            inboxStyle.addLine(str);
        }
        notificationBuilder.setStyle(inboxStyle);
        notificationBuilder.setOnlyAlertOnce(true);
        if(!activityToStart.equals("")) {
            Intent intent = new Intent(mContext, PNRStat.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(Presenter.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(intent);
            PendingIntent pendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            notificationBuilder.setContentIntent(pendingIntent);
        }
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notify = notificationBuilder.build();
        //notify.sound = Uri.parse("content://settings/system/notification_sound");
        notify.sound = Uri.parse(mPref.getString("notification_ringtone","content://settings/system/notification_sound"));
        notify.ledARGB = 0xFF00FF00;
        notify.ledOffMS = 10000;
        notify.ledOnMS = 1500;
        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        notify.flags |= Notification.FLAG_SHOW_LIGHTS;
        if(mPref.getBoolean("notification_vibrate",true)) {
            //Should find out a way to vibrate
            long [] pattern = {0,200};
            notify.vibrate = pattern;
        }
        /*notify.flags = Notification.DEFAULT_ALL;
        notify.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
        notify.flags |= Notification.DEFAULT_SOUND;
        notify.flags |= Notification.DEFAULT_LIGHTS;*/
        notificationManager.notify(0,notify);
    }
}
