package com.adarshahd.donate.activities;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.adarshahd.donate.R;
import com.adarshahd.donate.fragments.PNRCheckFragment;
import com.adarshahd.donate.fragments.PreferenceFragment;
import com.adarshahd.donate.fragments.TrainInfoFragment;
import com.adarshahd.donate.models.train.Train;
import com.baasbox.android.BaasBox;
import com.baasbox.android.BaasCloudMessagingService;
import com.baasbox.android.BaasHandler;
import com.baasbox.android.BaasResult;
import com.baasbox.android.BaasUser;
/*import com.crashlytics.android.Crashlytics;*/

import java.util.ArrayList;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements TrainInfoFragment.AvailabilityClickListener, TrainInfoFragment.FareClickListener {

    private static final String LAUNCH_COUNT = "LAUNCH_COUNT";
    private static final String RATING = "RATING";
    private static final String INITIAL = "INITIAL";
    private static final String USER_LEARNED_NAV_DRAWER = "_user_learned";
    private static final String CURRENT_APP_VERSION = "_version";

    public static final String USER_REGISTERED = "_user_registered";
    private static final int MY_PERMISSIONS_REQUEST_GET_ACCOUNTS = 100;
    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;
    private String pnrNumber;
    private boolean getPNRStatus;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        String action = getIntent().getAction();
        if (action != null && action.equals("open_pnr_status")) {
            pnrNumber = getIntent().getStringExtra("pnr_number");
            getPNRStatus = true;
        } else {
            getPNRStatus = false;
        }

        if (savedInstanceState != null) {
            return;
        }
        setOnNavigationDrawerItemSelectedListener(navigationView);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolBar, R.string.open_drawer, R.string.close_drawer) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
        mDrawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.getMenu().performIdentifierAction(R.id.pnrStatus, 0);


        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (!mPrefs.getBoolean(USER_REGISTERED, false)) {
            //Request for GET_ACCOUNTS permission and go ahead with user registration
            //getPermission();
        }

        //Limit 5 interstitial ads per day.
        int today = Integer.parseInt(DateFormat.format("d", Calendar.getInstance()).toString());
        if(mPrefs.getInt("today",0) != today){
            mPrefs.edit().putInt("ad_count",0).apply();
            mPrefs.edit().putInt("today",today).apply();
        }

        if(!mPrefs.getBoolean(USER_LEARNED_NAV_DRAWER,false)){
            mDrawerLayout.openDrawer(navigationView);
            mPrefs.edit().putBoolean(USER_LEARNED_NAV_DRAWER,true).apply();
        }

        Integer currentVersion = 0;
        try {
            currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if(mPrefs.getInt(CURRENT_APP_VERSION,0) == 0){
            mPrefs.edit().putInt(CURRENT_APP_VERSION,currentVersion).apply();
        }

        if(currentVersion > mPrefs.getInt(CURRENT_APP_VERSION,0)){
            mPrefs.edit().putInt(CURRENT_APP_VERSION,currentVersion).apply();
            mPrefs.edit().putBoolean("mandatory_update", false).apply();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mPrefs.getBoolean("mandatory_update",false)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setMessage("There is a mandatory update which should be installed before using the application");
            builder.setTitle("Update needed");
            builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                            "http://market.android.com/details?id=" + getPackageName()));
                    startActivity(marketIntent);
                    finish();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    finish();
                }
            });
            builder.create().show();
        }

        rateMyApp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_GET_ACCOUNTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupUser();
                } else {
                    Log.e("GCM: ", "Could not get the required permission for registering user");
                }
            }
        }
    }

    private void setOnNavigationDrawerItemSelectedListener(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {

                        //Closing drawer on item click
                        mDrawerLayout.closeDrawers();

                        //Check to see which item was being clicked and perform appropriate action
                        switch (menuItem.getItemId()) {
                            case R.id.pnrStatus:
                                if (getSupportFragmentManager().findFragmentByTag("pnr") == null) {
                                    PNRCheckFragment fragment = new PNRCheckFragment();
                                    if (getPNRStatus) {
                                        Bundle bundle = new Bundle();
                                        bundle.putString("pnr_number", pnrNumber);
                                        fragment.setArguments(bundle);
                                    }
                                    getSupportFragmentManager().beginTransaction().replace(
                                            R.id.frameLayoutMain, fragment, "pnr")
                                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                            .commit();
                                    setTitle("PNR Status");
                                }
                                break;
                            case R.id.trainDetails:
                                if (getSupportFragmentManager().findFragmentByTag("train") == null) {
                                    getSupportFragmentManager().beginTransaction().replace(
                                            R.id.frameLayoutMain, new TrainInfoFragment(), "train")
                                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                            .commit();
                                    setTitle("Train Info");
                                }
                                break;
                            case R.id.prefs:
                                if (getSupportFragmentManager().findFragmentByTag("prefs") == null) {
                                    getSupportFragmentManager().beginTransaction().replace(
                                            R.id.frameLayoutMain, new PreferenceFragment(), "prefs")
                                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                            .commit();
                                    setTitle("Preferences");
                                }
                                break;
                        }
                        setMenuItemCheck(menuItem);
                        return true;
                    }
                }
        );
    }

    @Override
    public void onAvailabilityClick(Train train, String trainNumber, String trainName, String source, String destination, String date, String month, String travelClass, String quota) {
        Bundle bundle = new Bundle();
        bundle.putString("train_number", trainNumber);
        bundle.putString("train_name", trainName);
        bundle.putString("source", source);
        bundle.putString("destination", destination);
        bundle.putString("date", date);
        bundle.putString("month", month);
        bundle.putString("travel_class", travelClass);
        bundle.putString("quota", quota);
        bundle.putString("actual_source", train.getSource().replaceAll("[+#*]", ""));
        bundle.putString("actual_destination", train.getDestination().replaceAll("[+#*]", ""));

        Intent intent = new Intent(this, AvailabilityActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);
    }

    @Override
    public void onFareClick(Train train, String trainNumber, String trainName, String source, String destination, String date, String month, String travelClass, String quota) {
        Bundle bundle = new Bundle();
        bundle.putString("train_number", trainNumber);
        bundle.putString("train_name", trainName);
        bundle.putString("source", source);
        bundle.putString("destination", destination);
        bundle.putString("date", date);
        bundle.putString("month", month);
        bundle.putString("travel_class", travelClass);
        bundle.putString("quota", quota);
        bundle.putString("actual_source", train.getSource().replaceAll("[+#*]", ""));
        bundle.putString("actual_destination", train.getDestination().replaceAll("[+#*]", ""));

        Intent intent = new Intent(this, FaresActivity.class);
        intent.putExtras(bundle);

        startActivity(intent);
    }

    private void setMenuItemCheck(MenuItem menuItem) {

        int currentGroupId = menuItem.getGroupId();
        ArrayList<Integer> allGroupsIds = new ArrayList<Integer>() {{
            add(R.id.top);
            add(R.id.bottom);
        }};

        for (int i = 0; i < allGroupsIds.size(); ++i) {

            if (currentGroupId == allGroupsIds.get(i)) {
                navigationView.getMenu().setGroupCheckable(allGroupsIds.get(i), true, true);
            } else {
                navigationView.getMenu().setGroupCheckable(allGroupsIds.get(i), false, true);
            }
        }
        menuItem.setChecked(true);
    }

    private void setupUser() {
        Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
        if (accounts == null || accounts.length == 0) {
            Log.e("GCM: ", "Could not find a valid account in the device");
            mPrefs.edit().putBoolean(USER_REGISTERED, true).apply();
            return;
        }
        final String userName = accounts[0].name.split("@")[0];
        /*Crashlytics.setUserEmail(userName + "@gmail.com");*/
        signInUser(userName);
    }

    private void getPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.GET_ACCOUNTS)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Request");
                builder.setMessage("To bring you timely alerts we would require few permissions such as account details. Please grant the permissions to continue using these features");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.GET_ACCOUNTS},
                                MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
                    }
                });
                builder.create().show();

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.GET_ACCOUNTS},
                        MY_PERMISSIONS_REQUEST_GET_ACCOUNTS);
            }
        } else {
            setupUser();
        }
    }

    private void signUpUser(String userName){
        BaasUser user = BaasUser.withUserName(userName)
                .setPassword(userName);

        user.signup(new BaasHandler<BaasUser>() {
            @Override
            public void handle(BaasResult<BaasUser> result) {
                if (result.isSuccess()) {
                    Log.i("GCM: ", "User created: " + result.value());

                    BaasCloudMessagingService box = BaasBox.messagingService();

                    box.enable(new BaasHandler<Void>() {
                        @Override
                        public void handle(BaasResult<Void> res) {
                            if (!res.isSuccess()) {
                                Log.e("GCM: ", "Error enabling GCM on this device", res.error());
                            } else {
                                Log.i("GCM: ", "User registered for push messaging service");
                                mPrefs.edit().putBoolean(USER_REGISTERED, true).apply();
                            }
                        }
                    });
                } else {
                    Log.e("GCM: ", "Could not create user", result.error());
                }
            }
        });
    }

    private void signInUser(final String userName){
        BaasUser user = BaasUser.withUserName(userName)
                .setPassword(userName);

        user.login(new BaasHandler<BaasUser>() {
            @Override
            public void handle(BaasResult<BaasUser> result) {
                if (result.isSuccess()) {
                    Log.i("GCM: ", "User created: " + result.value());

                    BaasCloudMessagingService box = BaasBox.messagingService();

                    box.enable(new BaasHandler<Void>() {
                        @Override
                        public void handle(BaasResult<Void> res) {
                            if (!res.isSuccess()) {
                                Log.e("GCM: ", "Error enabling GCM on this device", res.error());
                            } else {
                                Log.i("GCM: ", "User registered for push messaging service");
                                mPrefs.edit().putBoolean(USER_REGISTERED, true).apply();
                            }
                        }
                    });
                } else {
                    Log.e("GCM: ", "Could not Login user, trying to sign up", result.error());
                    signUpUser(userName);
                }
            }
        });
    }

    private void rateMyApp() {
        if (mPrefs.getString(RATING,"").equals("Not Interested")) {
            return;
        }
        if (mPrefs.getString(RATING,"").equals("RATED")) {
            return;
        }
        if(mPrefs.getBoolean(INITIAL,true)) {
            mPrefs.edit().putBoolean(INITIAL,false).apply();
        }
        int launchCount = mPrefs.getInt(LAUNCH_COUNT,0);
        if(launchCount >= 2 && !mPrefs.getString(RATING,"").equals("May be later")) {
            mPrefs.edit().putInt(LAUNCH_COUNT,0).apply();
            showRatingDialog();
        }
        if(launchCount >= 4 && mPrefs.getString(RATING,"").equals("May be later")) {
            mPrefs.edit().putInt(LAUNCH_COUNT,0).apply();
            showRatingDialog();
        }
        mPrefs.edit().putInt(LAUNCH_COUNT,launchCount+1).apply();
    }

    private void showRatingDialog() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        android.support.v7.app.AlertDialog dialog;
        builder.setTitle("Please rate");
        builder.setMessage("Thank you for installing IndianRailInfo. If you like the app, please consider rating it on Google Play.");
        builder.setPositiveButton("Rate it!",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPrefs.edit().putString(RATING,"RATED").apply();
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        "http://market.android.com/details?id=" + getPackageName()));
                startActivity(marketIntent);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Not Interested", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPrefs.edit().putString(RATING,"Not Interested").apply();
                dialog.dismiss();
            }
        });
        builder.setNeutralButton("Later", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPrefs.edit().putString(RATING,"May be later").apply();
                mPrefs.edit().putInt(LAUNCH_COUNT,0).apply();
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.show();
    }
}
