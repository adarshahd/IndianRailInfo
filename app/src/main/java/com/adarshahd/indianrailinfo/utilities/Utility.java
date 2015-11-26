package com.adarshahd.indianrailinfo.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.adarshahd.indianrailinfo.configs.Config;
import com.adarshahd.indianrailinfo.models.pnr.PnrStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Created by ahd on 3/10/15.
 */
public class Utility {

    // Check if are connected to internet
    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) {
            return false;
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
            connection.setRequestProperty("User-Agent", "test");
            connection.setRequestProperty("Connection", "close");
            connection.setConnectTimeout(5000);
            connection.connect();
            if (connection.getResponseCode() != 200) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    // REST Get request
    public static synchronized String get(String endPoint, String query, String subQuery, String data) {

        String url = Config.SERVER1 + Config.API + endPoint + "/" + query + "/" + subQuery
                + "?" + data;
        OkHttpClient client = new OkHttpClient();
        client.setHostnameVerifier(new NullHostNameVerifier());
        client.setConnectTimeout(5000, TimeUnit.MILLISECONDS);
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response;
        String jsonData;
        try {
            response = client.newCall(request).execute();
            jsonData = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return jsonData;
    }

    public static synchronized String getPNRStatus(String pnrNumber) {
        String data = "pnr_number=" + pnrNumber;
        return get("pnr", "get", "", data);
    }

    public static synchronized String getTrainBetweenStations(String source, String destination, String date, String month, String travelClass) {
        String data = "source=" + source +
                "&destination=" + destination +
                "&date=" + date +
                "&month=" + month +
                "&travel_class=" + travelClass;
        return get("tbs", "get", "", data);
    }

    public static synchronized String getTrainAvailability(String trainNumber, String source, String destination, String date, String month, String travelClass, String quota) {
        String data = "train_number=" + trainNumber +
                "&source=" + source +
                "&destination=" + destination +
                "&date=" + date +
                "&month=" + month +
                "&travel_class=" + travelClass +
                "&quota=" + quota;
        return get("ta", "get", "", data);
    }

    public static synchronized String getTrainFare(String trainNumber, String source, String destination, String date, String month, String travelClass, String quota) {
        String data = "train_number=" + trainNumber +
                "&source=" + source +
                "&destination=" + destination +
                "&date=" + date +
                "&month=" + month +
                "&travel_class=" + travelClass +
                "&quota=" + quota;
        return get("tf", "get", "", data);
    }

    public static ArrayList<String> getOfflinePNRs(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String pnr_items = preferences.getString("offline_pnr_items", "");
        if (pnr_items.equals("")) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(Arrays.asList(pnr_items.split(",")));
        }
    }

    public static void savePNR(Context context, PnrStatus pnrStatus) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(pnrStatus);
        preferences.edit().putString(pnrStatus.getPnr(), jsonString).apply();

        String pnr_items = preferences.getString("offline_pnr_items", "");
        ArrayList<String> pnrList = new ArrayList<>(Arrays.asList(pnr_items.split(",")));
        if (pnrList.contains(pnrStatus.getPnr())) {
            return;
        }
        pnrList.add(pnrStatus.getPnr());
        String temp = "";
        for (String pnr : pnrList) {
            temp += pnr + ",";
        }
        preferences.edit().putString("offline_pnr_items", temp).apply();
    }

    public static PnrStatus getPNROffline(Context context, String pnrNumber) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Gson gson = new GsonBuilder().create();
        String jsonData = preferences.getString(pnrNumber, "");
        if (jsonData.equals("")) {
            return null;
        } else {
            return gson.fromJson(jsonData, PnrStatus.class);
        }
    }

    public static void clearOfflinePNRData(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        ArrayList<String> pnrList = getOfflinePNRs(context);
        for (String pnr : pnrList) {
            preferences.edit().remove(pnr).apply();
        }
        preferences.edit().putString("offline_pnr_items", "").apply();
    }

    public static ArrayList<String> getTrackedPNRs(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String pnr_items = preferences.getString("tracked_pnr_items", "");
        if (pnr_items.equals("")) {
            return new ArrayList<>();
        } else {
            return new ArrayList<>(Arrays.asList(pnr_items.split(",")));
        }
    }

    public static void addPNRToTrack(Context context, String pnrNumber) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String pnr_items = preferences.getString("tracked_pnr_items", "");
        ArrayList<String> pnrList = new ArrayList<>(Arrays.asList(pnr_items.split(",")));
        if (pnrList.contains(pnrNumber)) {
            return;
        }
        pnrList.add(pnrNumber);
        String temp = "";
        for (String pnr : pnrList) {
            temp += pnr + ",";
        }
        preferences.edit().putString("tracked_pnr_items", temp).apply();
    }

    public static void unTrackPNRs(Context context, ArrayList<String> list) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String pnr_items = preferences.getString("tracked_pnr_items", "");
        ArrayList<String> pnrList = new ArrayList<>(Arrays.asList(pnr_items.split(",")));
        for (String pnr : list) {
            pnrList.remove(pnr);
        }

        String temp = "";
        for (String pnr : pnrList) {
            temp += pnr + ",";
        }
        preferences.edit().putString("tracked_pnr_items", temp).apply();
    }

    public static class NullHostNameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }

    }
}
