package com.adarshahd.indianrailinfo.donate.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.adarshahd.indianrailinfo.donate.R;
import com.adarshahd.indianrailinfo.donate.models.train.Availability;
import com.adarshahd.indianrailinfo.donate.models.train.TrainAvailability;
import com.adarshahd.indianrailinfo.donate.utilities.Utility;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

public class AvailabilityActivity extends AppCompatActivity {

    private static final Integer NO_NETWORK = 222;
    private static final Integer ERROR = 224;
    private static final Integer SUCCESS = 225;

    private String trainName;
    private String trainNumber;
    private String source;
    private String destination;
    private String date;
    private String month;
    private String travelClass;
    private String quota;
    private String actualSource;
    private String actualDestination;
    private String actualSourceStnCode;
    private String actualDestinationStnCode;
    private String errorMessage;
    private Boolean isNextPressed;

    private ProgressDialog mDialog;
    private List<Availability> mAvailability;
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_availability);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        isNextPressed = false;

        Bundle trainData;
        if (savedInstanceState == null) {
            trainData = getIntent().getExtras();
        } else {
            trainData = savedInstanceState;
        }

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.train_availability_ad_interstitial));

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                showAvailability();
            }
        });
        requestNewInterstitial();

        trainNumber = trainData.getString("train_number");
        trainName = trainData.getString("train_name");
        source = trainData.getString("source");
        actualSource = trainData.getString("actual_source");
        destination = trainData.getString("destination");
        actualDestination = trainData.getString("actual_destination");
        date = trainData.getString("date");
        month = trainData.getString("month");
        travelClass = trainData.getString("travel_class");
        quota = trainData.getString("quota");

        ((TextView) findViewById(R.id.trainName)).setText(trainName);
        ((TextView) findViewById(R.id.trainNumber)).setText(trainNumber);
        ((TextView) findViewById(R.id.source)).setText(source);
        ((TextView) findViewById(R.id.destination)).setText(destination);

        mDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);
        mDialog.setMessage("Getting details . . .");
        mDialog.show();

        findViewById(R.id.next).setEnabled(false);
        new GetAvailability().execute();

        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setEnabled(false);
                Availability availability = mAvailability.get(mAvailability.size() - 1);
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(availability.getDate().split("-")[0]));
                calendar.set(Calendar.MONTH, Integer.parseInt(availability.getDate().split("-")[1]) - 1);
                //calendar.add(Calendar.DAY_OF_MONTH, 6);
                date = calendar.get(Calendar.DAY_OF_MONTH) + "";
                month = (calendar.get(Calendar.MONTH) + 1) + "";
                mDialog.show();
                new GetAvailability().execute();
                isNextPressed = true;
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        bundle.putString("train_number", trainNumber);
        bundle.putString("train_name", trainName);
        bundle.putString("source", source);
        bundle.putString("destination", destination);
        bundle.putString("date", date);
        bundle.putString("month", month);
        bundle.putString("travel_class", travelClass);
        bundle.putString("quota", quota);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class GetAvailability extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            if (!Utility.isConnected(AvailabilityActivity.this)) {
                return NO_NETWORK;
            }
            Gson gson = new GsonBuilder().create();
            String jsonData = Utility.getTrainAvailability(trainNumber, source, destination, date, month, travelClass, quota);
            TrainAvailability trainAvailability;
            try {
                trainAvailability = gson.fromJson(jsonData, TrainAvailability.class);
                if (trainAvailability == null) {
                    throw new JsonParseException("error");
                }
                if (trainAvailability.getTrainAvailability() == null) {
                    throw new JsonParseException("error");
                }
                mAvailability = trainAvailability.getTrainAvailability();
            } catch (JsonParseException e) {
                try {
                    JSONObject object = new JSONObject(jsonData);
                    if (object.has("error")) {
                        errorMessage = object.getString("error");
                        if (errorMessage.contains("is not covered")){
                            String [] stationArray = getResources().getStringArray(R.array.stn_codes);
                            for (String station : stationArray) {
                                if (station.contains(actualSource)) {
                                    actualSourceStnCode = station.split("-")[1].trim().replaceAll("[+#*]", "");
                                }
                                if (station.contains(actualDestination)) {
                                    actualDestinationStnCode = station.split("-")[1].trim().replaceAll("[+#*]", "");
                                }
                            }
                        }
                    } else {
                        errorMessage = "There was some error processing your request. Please try after some time!";
                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                return ERROR;
            }
            return SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer result) {
            findViewById(R.id.next).setEnabled(true);
            if (result.equals(NO_NETWORK)) {
                mDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(AvailabilityActivity.this);
                builder.setTitle("Alert");
                builder.setMessage("No Network connection!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.create().show();
                return;
            }

            if (result.equals(ERROR)) {
                mDialog.dismiss();
                if(errorMessage.contains("is not covered")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AvailabilityActivity.this);
                    builder.setTitle("Info");
                    builder.setMessage("Selected station is not covered by this train." +
                            "\n\nWould you like to get availability from " +
                            actualSource + "(" + actualSourceStnCode + ")" + " and " +
                            actualDestination + "(" + actualDestinationStnCode + ")" + " instead?");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            source = actualSourceStnCode;
                            destination = actualDestinationStnCode;
                            mDialog.show();
                            new GetAvailability().execute();
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
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(AvailabilityActivity.this);
                builder.setTitle("Error");
                builder.setMessage(errorMessage);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        finish();
                    }
                });
                builder.create().show();
                return;
            }

            mDialog.dismiss();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(AvailabilityActivity.this);
            if (mInterstitialAd.isLoaded() && !isNextPressed && (preferences.getInt("ad_count",0) < 5) ) {
                mInterstitialAd.show();
                preferences.edit().putInt("ad_count", (preferences.getInt("ad_count",0) + 1)).apply();
            } else {
                showAvailability();
            }
        }
    }

    private void showAvailability() {
        ListView listView = (ListView) findViewById(R.id.availabilityList);
        AvailabilityAdapter adapter = new AvailabilityAdapter(AvailabilityActivity.this, R.layout.list_item_train_availability);
        listView.setAdapter(adapter);
    }

    private class AvailabilityAdapter extends ArrayAdapter<String> {

        public AvailabilityAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public int getCount() {
            return mAvailability.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.list_item_train_availability, parent, false);
                ((TextView) convertView.findViewById(R.id.date)).setText(mAvailability.get(position).getDate());
                ((TextView) convertView.findViewById(R.id.status)).setText(mAvailability.get(position).getStatus());
            }
            return convertView;
        }
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("YOUR DEVICE ID")
                .build();
        mInterstitialAd.loadAd(adRequest);
    }
}
