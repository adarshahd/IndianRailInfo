package com.adarshahd.indianrailinfo.donate.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.adarshahd.indianrailinfo.donate.R;
import com.adarshahd.indianrailinfo.donate.models.pnr.Passenger;
import com.adarshahd.indianrailinfo.donate.models.pnr.PnrStatus;
import com.adarshahd.indianrailinfo.donate.utilities.Utility;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class PNRCheckFragment extends Fragment {

    public static final String INVALID_PNR = "invalid_pnr";
    public static final String NO_NETWORK = "no_network";
    public static final String ERROR = "error";
    private static final String SUCCESS = "success";

    private String mPNR;
    private boolean isOffline;
    private PnrStatus pnrStatus;
    private ProgressDialog mDialog;

    private InterstitialAd mInterstitialAd;


    public PNRCheckFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mDialog = new ProgressDialog(getActivity());
        mDialog.setCancelable(false);
        mDialog.setIndeterminate(true);
        isOffline = false;

        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId(getString(R.string.pnr_check_ad_interstitial));

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                showPNRDetails();
            }
        });
        requestNewInterstitial();

        if (getArguments() != null) {
            mPNR = getArguments().getString("pnr_number");
            mDialog.setMessage("Getting PNR status . . .");
            mDialog.show();
            new GetPNRStatus().execute();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_pnrcheck, container, false);
        ((AutoCompleteTextView) view.findViewById(R.id.act_pnr_number))
                .setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, getSavedPNRs()));
        ((AutoCompleteTextView) view.findViewById(R.id.act_pnr_number))
                .setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                        if(actionId == EditorInfo.IME_ACTION_GO){
                            view.findViewById(R.id.btn_get_pnr_status).performClick();
                            return true;
                        }
                        return false;
                    }
                });
        view.findViewById(R.id.btn_get_pnr_status).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPNR = ((AutoCompleteTextView) view.findViewById(R.id.act_pnr_number)).getText().toString();
                if (PNROkay(mPNR)) {
                    v.setEnabled(false);
                    LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id.ll_container_pnr_status);
                    linearLayout.findViewById(R.id.cardTrainDetails).setVisibility(View.GONE);
                    linearLayout.findViewById(R.id.passengerDetails).setVisibility(View.GONE);
                    mDialog.setMessage("Getting PNR status . . .");
                    mDialog.show();
                    new GetPNRStatus().execute();
                } else {
                    ((AutoCompleteTextView) view.findViewById(R.id.act_pnr_number)).setError("Invalid PNR Number");
                }
            }
        });

        //Load the admob ad
        AdView mAdView = (AdView) view.findViewById(R.id.pnrStatusAd);
        AdRequest.Builder builder = new AdRequest.Builder();
        builder.addTestDevice("YOUR DEVICE ID");
        AdRequest adRequest = builder.build();
        mAdView.loadAd(adRequest);

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private boolean PNROkay(String pnr) {
        if (pnr == null) {
            return false;
        }
        if (pnr.length() != 10) {
            return false;
        }
        return true;
    }

    private class GetPNRStatus extends AsyncTask<Void, Void, String> {
        private String errorText;

        @Override
        protected String doInBackground(Void... params) {
            if (!Utility.isConnected(getActivity())) {
                return NO_NETWORK;
            }

            String jsonData = Utility.getPNRStatus(mPNR);

            Gson gson = new GsonBuilder().create();
            try {
                pnrStatus = gson.fromJson(jsonData, PnrStatus.class);
                if(pnrStatus == null) {
                    throw new JsonParseException("Failed to parse");
                }
                if (pnrStatus.getReservedUpto() == null) {
                    throw new JsonParseException("Failed to parse");
                }
            } catch (JsonParseException e) {
                try {
                    JSONObject object = new JSONObject(jsonData);
                    if (object.has("error")) {
                        errorText = object.getString("error");
                    }
                } catch (JSONException e1) {
                    e1.printStackTrace();
                    errorText = "error";
                    return ERROR;
                }
                return ERROR;
            }

            return SUCCESS;
        }

        @Override
        protected void onPostExecute(String result) {
            getActivity().findViewById(R.id.btn_get_pnr_status).setEnabled(true);
            mDialog.dismiss();

            if (result.equals(ERROR)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Error");
                builder.setMessage(errorText);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.create().show();
                return;
            }

            if (result.equals(NO_NETWORK)) {
                //Toast.makeText(getActivity(), "No Network connection", Toast.LENGTH_SHORT).show();
                isOffline = true;
                if (Utility.getOfflinePNRs(getActivity()).contains(mPNR)) {
                    pnrStatus = Utility.getPNROffline(getActivity(), mPNR);
                    showPNRDetails();
                    return;
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            if (mInterstitialAd.isLoaded() && (preferences.getInt("ad_count",0) < 5) ) {
                mInterstitialAd.show();
                preferences.edit().putInt("ad_count", (preferences.getInt("ad_count",0) + 1)).apply();
            } else {
                showPNRDetails();
            }

            //OK, let us save the pnr number for offline access is settings allow it
            if (preferences.getBoolean("pnr_offline", false)) {
                new SavePNRDetails().execute();
            }

        }
    }

    private class PassengerAdapter extends ArrayAdapter<String> {

        public PassengerAdapter(Context context, List<String> objects) {
            super(context, R.layout.list_item_passengers, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater(null).inflate(R.layout.list_item_passengers, parent, false);
            }
            ((TextView) convertView.findViewById(R.id.passengerItem)).setText("Passenger " + (position + 1));
            String[] status = pnrStatus.getPassengers()[position].getBookingStatus().split(",");
            String bookingStatus = "";
            for (String s : status) {
                bookingStatus += s.trim() + " ";
            }
            ((TextView) convertView.findViewById(R.id.passengerBookingStatus)).setText(bookingStatus);
            ((TextView) convertView.findViewById(R.id.passengerStatus)).setText(pnrStatus.getPassengers()[position].getCurrentStatus());
            return convertView;
        }
    }

    private ArrayList<String> getSavedPNRs() {
        ArrayList<String> offlinePNRs = Utility.getOfflinePNRs(getActivity());
        if (offlinePNRs == null) {
            offlinePNRs = new ArrayList<>();
        }
        return offlinePNRs;
    }

    private class SavePNRDetails extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Utility.savePNR(getActivity(), pnrStatus);
            return null;
        }
    }

    private void showPNRDetails() {
        //Set the train details
        LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id.ll_container_pnr_status);
        ((TextView) linearLayout.findViewById(R.id.trainNumber)).setText(pnrStatus.getTrainNumber().replace('*', ' ').trim());
        ((TextView) linearLayout.findViewById(R.id.dateOfTravel)).setText(pnrStatus.getBoardingDate());
        ((TextView) linearLayout.findViewById(R.id.fromTo)).setText(pnrStatus.getFrom() + " - " + pnrStatus.getTo());
        ((TextView) linearLayout.findViewById(R.id.trainName)).setText(pnrStatus.getTrainName());

        //Fill the Passenger details
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < pnrStatus.getPassengers().length; ++i) {
            list.add("");
        }
        PassengerAdapter passengerAdapter = new PassengerAdapter(getActivity(), list);
        ListView listView = (ListView) linearLayout.findViewById(R.id.listPassengers);
        listView.setAdapter(passengerAdapter);

        //Now show both details
        linearLayout.findViewById(R.id.cardTrainDetails).setVisibility(View.VISIBLE);
        linearLayout.findViewById(R.id.passengerDetails).setVisibility(View.VISIBLE);

        if (!isOffline) {

            //If the PNR is already being tracked do not prompt for tracking.
            if (Utility.getTrackedPNRs(getActivity()).contains(mPNR)) {
                return;
            }

            Passenger[] passengerList = pnrStatus.getPassengers();
            Boolean isWaitingList = false;
            for (Passenger passenger : passengerList) {
                if (passenger.getCurrentStatus().contains("W/L") || passenger.getCurrentStatus().contains("WL")) {
                    isWaitingList = true;
                }
            }

            Boolean showAlertDialog = true;
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            String travelDateString = pnrStatus.getBoardingDate();
            try {
                Date date = formatter.parse(travelDateString);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                if (calendar.before(Calendar.getInstance())) {
                    showAlertDialog = false;
                }
            } catch (ParseException e) {
                showAlertDialog = true;
            }
            if (isWaitingList && showAlertDialog) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Track PNR?");
                builder.setMessage("Found passengers with unconfirmed ticket." +
                        " Would you like to track this PNR for status change?" +
                        "\n\nYou would automatically get status change alerts");
                builder.setPositiveButton("Track", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        Utility.addPNRToTrack(getActivity(), pnrStatus.getPnr());
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.create().show();
            }
        }
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("YOUR DEVICE ID")
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

}
