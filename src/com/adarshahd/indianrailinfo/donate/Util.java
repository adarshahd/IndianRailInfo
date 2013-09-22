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
