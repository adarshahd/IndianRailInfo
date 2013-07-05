package com.adarshahd.indianrailinfo.donate;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import java.io.File;

/**
 * Created by ahd on 6/20/13.
 */
public class PrefActivityGeneral extends SherlockPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static SharedPreferences prefs;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.pref_irctc);
        addPreferencesFromResource(R.xml.pref_general);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
        loadInitialPrefs();
        findPreference("clear_pnr_data").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                clearPNRData();
                return true;
            }
        });

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            findPreference("pnr_track_list").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(PNRDatabase.getPNRDatabase(PrefActivityGeneral.this).getPNRTrackList().size() < 1) {
                        Toast.makeText(PrefActivityGeneral.this,"Nothing is being tracked",Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    startActivity(new Intent(PrefActivityGeneral.this,PNRTrackList.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    return true;
                }
            });
        }

        findPreference("pnr_track_add").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                dialogAddPNR();
                return true;
            }
        });

        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(PrefActivityGeneral.this,AboutActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            }
        });

        findPreference("rate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://market.android.com/details?id=" + getPackageName())));
                return true;
            }
        });


        /*//For testing purpose
        PNRDatabase.getPNRDatabase(this).addPNRToTrack("1235467890");
        PNRDatabase.getPNRDatabase(this).addPNRToTrack("1235467891");
        PNRDatabase.getPNRDatabase(this).addPNRToTrack("1235467892");
        PNRDatabase.getPNRDatabase(this).addPNRToTrack("1235467893");
        PNRDatabase.getPNRDatabase(this).addPNRToTrack("1235467894");
        PNRDatabase.getPNRDatabase(this).addPNRToTrack("1235467895");
        PNRDatabase.getPNRDatabase(this).addPNRToTrack("1235467896");
        PNRDatabase.getPNRDatabase(this).addPNRToTrack("1235467897");
        PNRDatabase.getPNRDatabase(this).addPNRToTrack("1235467898");*/
    }

    private void loadInitialPrefs() {
        String key = "track_schedule";
        switch (Integer.parseInt(prefs.getString(key,""))) {
            case 3:
                findPreference(key).setSummary("Every 3 hour");
                break;
            case 6:
                findPreference(key).setSummary("Every 6 hour");
                break;
            case 12:
                findPreference(key).setSummary("Every 12 hour");
                break;
            case 24:
                findPreference(key).setSummary("Every day");
                break;
            case 168:
                findPreference(key).setSummary("Every week");
                break;
        }
    }

    private void clearPNRData() {
        //Delete the PNR directory
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm delete");
        builder.setMessage("This will remove all the stored offline PNR Status data. Are you sure?");
        builder.setPositiveButton("OK, Remove",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PNRDatabase.getPNRDatabase(PrefActivityGeneral.this).clearPNRDatabase();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel",null);
        builder.create().show();
    }

    @Override
    protected void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("track_schedule")){
            switch (Integer.parseInt(sharedPreferences.getString(key,""))) {
                case 3:
                    findPreference(key).setSummary("Every 3 hour");
                    break;
                case 6:
                    findPreference(key).setSummary("Every 6 hour");
                    break;
                case 12:
                    findPreference(key).setSummary("Every 12 hour");
                    break;
                case 24:
                    findPreference(key).setSummary("Every day");
                    break;
                case 168:
                    findPreference(key).setSummary("Every week");
                    break;
            }
        }
        if(key.equals("enable_tracking")) {
            if(sharedPreferences.getBoolean(key,false)) {
                scheduleBackup();
            } else {
                cancelBackupSchedule();
            }
        }
    }

    private void dialogAddPNR() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText et = new EditText(this);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setHint("Type your 10 digit PNR");
        builder.setTitle("Track PNR");
        builder.setView(et);
        builder.setPositiveButton("Track", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(et.getText().toString().length() != 10) {
                    Toast.makeText(PrefActivityGeneral.this,"Invalid PNR",Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    return;
                }
                PNRDatabase.getPNRDatabase(PrefActivityGeneral.this).addPNRToTrack(et.getText().toString());
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel",null);
        builder.create().show();
    }

    private void scheduleBackup() {
        Intent alarmIntent = new Intent(this,PNRTracker.class);

        int schedule = Integer.parseInt(prefs.getString("track_schedule", "24"));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                0, alarmIntent, 0);
        alarmIntent.addFlags(PNRTracker.ALARM_RECEIVER);

        AlarmManager alm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alm.setInexactRepeating(AlarmManager.RTC, AlarmManager.INTERVAL_HOUR, (AlarmManager.INTERVAL_HOUR * schedule), pendingIntent);
    }

    private void cancelBackupSchedule() {
        Intent alarmIntent = new Intent(this,PNRTracker.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                0, alarmIntent, 0);
        alarmIntent.addFlags(PNRTracker.ALARM_RECEIVER);

        AlarmManager alm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alm.cancel(pendingIntent);
    }
}