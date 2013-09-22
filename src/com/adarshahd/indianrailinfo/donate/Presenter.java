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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


/**
 * Created by ahd on 5/27/13.
 */
public class Presenter extends SherlockActivity implements View.OnClickListener{
    private static final String LAUNCH_COUNT = "LAUNCH_COUNT";
    private static final String RATING = "RATING";
    private static final String INITIAL = "INITIAL";
    //public static Util mUtil;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //((Button)findViewById(R.id.id_btn_trn_book)).setOnClickListener(this);
        ((Button)findViewById(R.id.id_btn_pnr_sts)).setOnClickListener(this);
        ((Button)findViewById(R.id.id_btn_trn_enq)).setOnClickListener(this);
        rateMyApp();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.id_action_pref:
                startActivity(new Intent(this,PrefActivityGeneral.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            /*case R.id.id_btn_trn_book:
                startActivity(new Intent(this,IRCTCWeb.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return;*/
            case R.id.id_btn_pnr_sts:
                startActivity(new Intent(this,PNRStat.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return;
            case R.id.id_btn_trn_enq:
                startActivity(new Intent(this,TrainEnquiry.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return;
        }
    }

    private void rateMyApp() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getString(RATING,"").equals("Not Interested")) {
            return;
        }
        if (prefs.getString(RATING,"").equals("RATED")) {
            return;
        }
        if(prefs.getBoolean(INITIAL,true)) {
            prefs.edit().putBoolean(INITIAL,false).commit();
        }
        int launchCount = prefs.getInt(LAUNCH_COUNT,0);
        if(launchCount >= 2 && !prefs.getString(RATING,"").equals("May be later")) {
            prefs.edit().putInt(LAUNCH_COUNT,0).commit();
            showRatingDialog();
        }
        if(launchCount >= 4 && prefs.getString(RATING,"").equals("May be later")) {
            prefs.edit().putInt(LAUNCH_COUNT,0).commit();
            showRatingDialog();
        }
        prefs.edit().putInt(LAUNCH_COUNT,launchCount+1).commit();
    }

    private void showRatingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        builder.setTitle("Please rate");
        builder.setMessage("Thank you for installing my app. If you liked my app, please consider rating it on Google Play. Would you like to rate?");
        builder.setPositiveButton("Rate it!",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PreferenceManager.getDefaultSharedPreferences(Presenter.this).edit().putString(RATING,"RATED").commit();
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://market.android.com/details?id=" + getPackageName()));
                startActivity(marketIntent);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Not Interested", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PreferenceManager.getDefaultSharedPreferences(Presenter.this).edit().putString(RATING,"Not Interested").commit();
                dialog.dismiss();
            }
        });
        builder.setNeutralButton("May be later", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PreferenceManager.getDefaultSharedPreferences(Presenter.this).edit().putString(RATING,"May be later").commit();
                PreferenceManager.getDefaultSharedPreferences(Presenter.this).edit().putInt(LAUNCH_COUNT,0).commit();
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.show();
    }
}
