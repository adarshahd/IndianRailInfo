package com.adarshahd.indianrailinfo.donate.fragments;


import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.adarshahd.indianrailinfo.donate.R;
import com.adarshahd.indianrailinfo.donate.activities.AboutActivity;
import com.adarshahd.indianrailinfo.donate.activities.PNRTrackingListActivity;
import com.adarshahd.indianrailinfo.donate.receivers.PNRTracker;
import com.adarshahd.indianrailinfo.donate.utilities.Utility;


/**
 * A simple {@link Fragment} subclass.
 */
public class PreferenceFragment extends com.github.machinarius.preferencefragment.PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static SharedPreferences mPrefs;

    public PreferenceFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        loadInitialPrefs();
        setPreferenceClickListeners();
    }

    @Override
    public void onDestroy() {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
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
                scheduleTrack();
            } else {
                cancelTrack();
            }
        }
    }

    private void setPreferenceClickListeners() {
        findPreference("clear_pnr_data").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                clearPNRData();
                return true;
            }
        });

        findPreference("pnr_track_list").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if(Utility.getTrackedPNRs(getActivity()).isEmpty()) {
                    Toast.makeText(getActivity(),"Nothing is being tracked",Toast.LENGTH_SHORT).show();
                    return false;
                }
                startActivity(new Intent(getActivity(), PNRTrackingListActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            }
        });

        findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(getActivity(), AboutActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return true;
            }
        });

        findPreference("rate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://market.android.com/details?id=" + getActivity().getPackageName())));
                return true;
            }
        });
    }

    private void loadInitialPrefs() {
        String key = "track_schedule";
        switch (Integer.parseInt(mPrefs.getString(key,""))) {
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
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Confirm delete");
        builder.setMessage("This will remove all the stored offline PNR Status data. Are you sure?");
        builder.setPositiveButton("OK, Remove", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Utility.clearOfflinePNRData(getActivity());
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }



    private void scheduleTrack() {
        Intent alarmIntent = new Intent(getActivity(),PNRTracker.class);

        int schedule = Integer.parseInt(mPrefs.getString("track_schedule", "24"));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(),
                0, alarmIntent, 0);
        alarmIntent.addFlags(PNRTracker.ALARM_RECEIVER);

        AlarmManager alm = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        alm.setInexactRepeating(AlarmManager.RTC, AlarmManager.INTERVAL_HOUR, (AlarmManager.INTERVAL_HOUR * schedule), pendingIntent);
    }

    private void cancelTrack() {
        Intent alarmIntent = new Intent(getActivity(),PNRTracker.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(),
                0, alarmIntent, 0);
        alarmIntent.addFlags(PNRTracker.ALARM_RECEIVER);

        AlarmManager alm = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        alm.cancel(pendingIntent);
    }
}
