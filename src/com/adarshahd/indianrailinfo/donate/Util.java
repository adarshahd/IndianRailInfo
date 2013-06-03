package com.adarshahd.indianrailinfo.donate;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by ahd on 5/31/13.
 */
public class Util {
    private static Context mContext;
    private static Util mUtil;

    private Util (Context context) {
        mContext = context;
    }

    public static boolean isConnected() {
        ConnectivityManager mgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(mgr.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED && mgr.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED) {
            //Toast.makeText(mContext, "No network available", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public static void showAlert(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        AlertDialog dialog;
        builder.setTitle(title);
        builder.setIcon(R.drawable.irctc);
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    public static Util getUtil (Context context) {
        if(mUtil == null) {
            mUtil = new Util(context);
        }
        return mUtil;
    }

    public static void delete() {
        if(mUtil != null) {
            mUtil = null;
        }
    }
}
