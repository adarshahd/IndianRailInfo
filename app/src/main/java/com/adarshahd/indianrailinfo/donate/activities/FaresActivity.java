package com.adarshahd.indianrailinfo.donate.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.adarshahd.indianrailinfo.donate.R;
import com.adarshahd.indianrailinfo.donate.models.train.TrainFare;
import com.adarshahd.indianrailinfo.donate.utilities.Utility;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class FaresActivity extends AppCompatActivity {

    private static final Integer NO_NETWORK = 222;
    private static final Integer ERROR = 224;
    private static final Integer SUCCESS = 225;

    private String trainName;
    private String trainNumber;
    private String source;
    private String destination;
    private String date;
    private String month;
    private String year;
    private String travelClass;
    private String quota;
    private String actualSource;
    private String actualDestination;
    private String actualSourceStnCode;
    private String actualDestinationStnCode;
    private Boolean fareRefreshed;

    private String errorMessage;

    private ProgressDialog mDialog;
    private TrainFare trainFare;
    private HashMap<String, String> classMap;
    private Spinner selectedClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_fares);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        selectedClass = (Spinner) findViewById(R.id.travelClass);
        fillClassMap();

        Bundle bundleTrainFare;
        if (savedInstanceState == null) {
            bundleTrainFare = getIntent().getExtras();
        } else {
            bundleTrainFare = savedInstanceState;
        }
        trainNumber = bundleTrainFare.getString("train_number");
        trainName = bundleTrainFare.getString("train_name");
        source = bundleTrainFare.getString("source");
        actualSource = bundleTrainFare.getString("actual_source");
        destination = bundleTrainFare.getString("destination");
        actualDestination = bundleTrainFare.getString("actual_destination");
        date = bundleTrainFare.getString("date");
        month = bundleTrainFare.getString("month");
        year = bundleTrainFare.getString("year");
        travelClass = bundleTrainFare.getString("travel_class");
        quota = bundleTrainFare.getString("quota");
        fareRefreshed = false;

        ((TextView) findViewById(R.id.trainName)).setText(trainName);
        ((TextView) findViewById(R.id.trainNumber)).setText(trainNumber);
        ((TextView) findViewById(R.id.source)).setText(source);
        ((TextView) findViewById(R.id.destination)).setText(destination);

        mDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);
        mDialog.setMessage("Getting details . . .");
        mDialog.show();

        //Refresh the train fare on selecting refresh button
        findViewById(R.id.refreshFare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDialog.show();
                travelClass = classMap.get(selectedClass.getSelectedItem().toString());
                new GetFare().execute();
            }
        });

        selectedClass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(!fareRefreshed){
                    fareRefreshed = true;
                    return;
                }
                mDialog.show();
                travelClass = classMap.get(selectedClass.getSelectedItem().toString());
                new GetFare().execute();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        new GetFare().execute();

        //Load the admob ad
        AdView mAdView = (AdView) findViewById(R.id.pnrStatusAd);
        AdRequest.Builder builder = new AdRequest.Builder();
        builder.addTestDevice("YOUR DEVICE ID");
        AdRequest adRequest = builder.build();
        mAdView.loadAd(adRequest);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

    private void fillClassMap() {
        classMap = new HashMap<>();
        classMap.put("SLEEPER CLASS", "SL");
        classMap.put("FIRST AC", "1A");
        classMap.put("SECOND AC", "2A");
        classMap.put("THIRD AC", "3A");
        classMap.put("3 AC Economy", "3E");
        classMap.put("AC CHAIR CAR", "CC");
        classMap.put("FIRST CLASS", "FC");
        classMap.put("SECOND SEATING", "2S");
    }

    private class GetFare extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            if (!Utility.isConnected(FaresActivity.this)) {
                return NO_NETWORK;
            }
            Gson gson = new GsonBuilder().create();
            String jsonData = Utility.getTrainFare(
                    trainNumber,
                    source,
                    destination,
                    date,
                    month,
                    year,
                    travelClass,
                    quota);
            try {
                trainFare = gson.fromJson(jsonData, TrainFare.class);
                if (trainFare == null) {
                    throw new JsonParseException("error");
                }
                if (trainFare.getBaseFare() == null) {
                    throw new JsonParseException("error");
                }
            } catch (JsonParseException e) {
                try {
                    JSONObject object = new JSONObject(jsonData == null ? "" : jsonData);
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
            if (result.equals(NO_NETWORK)) {
                mDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(FaresActivity.this);
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(FaresActivity.this);
                    builder.setTitle("Info");
                    builder.setMessage("Selected station is not covered by this train." +
                            "\n\nWould you like to get train fare from " +
                            actualSource + "(" + actualSourceStnCode + ")" + " and " +
                            actualDestination + "(" + actualDestinationStnCode + ")" + " instead?");
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            source = actualSourceStnCode;
                            destination = actualDestinationStnCode;
                            mDialog.show();
                            new GetFare().execute();
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
                AlertDialog.Builder builder = new AlertDialog.Builder(FaresActivity.this);
                builder.setTitle("Error");
                builder.setMessage(errorMessage);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.create().show();
                return;
            }

            mDialog.dismiss();

            ((TextView)findViewById(R.id.baseFare)).setText(trainFare.getBaseFare());
            ((TextView)findViewById(R.id.reservationCharges)).setText(trainFare.getReservationCharge());
            ((TextView)findViewById(R.id.superFastCharges)).setText(trainFare.getSuperfaseCharge());
            ((TextView)findViewById(R.id.otherCharges)).setText(trainFare.getOtherCharge());
            ((TextView)findViewById(R.id.serviceTax)).setText(trainFare.getServiceTax());
            ((TextView)findViewById(R.id.totalCharges)).setText(trainFare.getTotal());
        }
    }
}
