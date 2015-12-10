package com.adarshahd.indianrailinfo.donate.fragments;

import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;

import com.adarshahd.indianrailinfo.donate.R;
import com.adarshahd.indianrailinfo.donate.activities.StationSelectActivity;
import com.adarshahd.indianrailinfo.donate.adapters.TrainsAdapter;
import com.adarshahd.indianrailinfo.donate.models.train.Train;
import com.adarshahd.indianrailinfo.donate.models.train.Trains;
import com.adarshahd.indianrailinfo.donate.utilities.Utility;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by ahd on 4/19/15.
 */
public class TrainInfoFragment extends Fragment implements DatePickerDialog.OnDateSetListener,
        TrainsAdapter.CheckAvailabilityListener,
        TrainsAdapter.CheckFareListener {

    private static final int STATION_FROM = 22;
    private static final int STATION_TO = 23;

    private static final Integer NO_NETWORK = 222;
    private static final Integer ERROR = 224;
    private static final Integer SUCCESS = 225;

    private String stationFrom = "SRF";
    private String stationTo = "SBC";
    private String travelDate;
    private String travelMonth;
    private String travelYear;
    //private String trainNumber;
    private String travelClass;
    private String errorMessage;

    private Button from;
    private Button to;
    private Button date;
    private Spinner selectedClass;
    private ProgressDialog mDialog;
    private HashMap<String, String> classMap;
    private View rootView;
    private Trains trains;
    private AvailabilityClickListener availabilityClickListener;
    private FareClickListener fareClickListener;


    public interface AvailabilityClickListener {
        void onAvailabilityClick(
                Train train,
                String trainNumber,
                String trainName,
                String source,
                String destination,
                String date,
                String month,
                String year,
                String travelClass,
                String quota);
    }

    public interface FareClickListener {
        void onFareClick(
                Train train,
                String trainNumber,
                String trainName,
                String source,
                String destination,
                String date,
                String month,
                String year,
                String travelClass,
                String quota);
    }

    public TrainInfoFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        availabilityClickListener = (AvailabilityClickListener) activity;
        fareClickListener = (FareClickListener) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_train_info, container, false);

        from = (Button) rootView.findViewById(R.id.source);
        to = (Button) rootView.findViewById(R.id.destination);
        date = (Button) rootView.findViewById(R.id.travelDate);
        selectedClass = (Spinner) rootView.findViewById(R.id.travelClass);
        mDialog = new ProgressDialog(getActivity());
        mDialog.setCancelable(false);
        mDialog.setIndeterminate(true);

        //Fill the classMap
        fillClassMap();

        //From selection
        rootView.findViewById(R.id.source).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(getActivity(), StationSelectActivity.class),
                        STATION_FROM);
            }
        });

        //To Selection
        rootView.findViewById(R.id.destination).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(new Intent(getActivity(), StationSelectActivity.class),
                        STATION_TO);
            }
        });

        rootView.findViewById(R.id.travelDate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        getActivity(), TrainInfoFragment.this, calendar.get(Calendar.YEAR)
                        , calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH));
                long minDate = System.currentTimeMillis() - 1000;
                datePickerDialog.getDatePicker().setMinDate(minDate);
                datePickerDialog.show();
            }
        });

        //Fetch trains
        rootView.findViewById(R.id.getTrains).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (date.getText().toString().toLowerCase().equals("select date")) {
                    date.setError("Invalid date");
                    return;
                }

                travelClass = classMap.get(selectedClass.getSelectedItem().toString());

                mDialog.setMessage("Getting trains . . . ");
                mDialog.show();
                new GetTrains().execute();
            }
        });

        //Show the search area on newSearch click
        rootView.findViewById(R.id.newSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSearchArea(true);
            }
        });

        rootView.findViewById(R.id.swap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String temp = stationFrom;
                stationFrom = stationTo;
                stationTo = temp;
                from.setText(stationFrom);
                to.setText(stationTo);
            }
        });

        //Load the admob ad
        AdView mAdView = (AdView) rootView.findViewById(R.id.pnrStatusAd);
        AdRequest.Builder builder = new AdRequest.Builder();
        builder.addTestDevice("YOUR DEVICE ID");
        AdRequest adRequest = builder.build();
        mAdView.loadAd(adRequest);

        return rootView;
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

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        travelDate = dayOfMonth + "";
        travelMonth = monthOfYear + 1 + "";
        travelYear = year + "";
        date.setError(null);
        date.setText(travelDate + "/" + travelMonth + "/" + year);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case STATION_FROM:
                    stationFrom = data.getStringExtra("station_name");
                    if (!stationFrom.equals("")) {
                        stationFrom = stationFrom.split("-")[1].trim();
                        from.setText(stationFrom);
                    }
                    break;
                case STATION_TO:
                    stationTo = data.getStringExtra("station_name");
                    if (!stationTo.equals("")) {
                        stationTo = stationTo.split("-")[1].trim();
                        to.setText(stationTo);
                    }
                    break;
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private class GetTrains extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... voids) {
            if (!Utility.isConnected(getActivity())) {
                return NO_NETWORK;
            }
            String jsonData = Utility.getTrainBetweenStations(stationFrom, stationTo, travelDate, travelMonth, travelYear, travelClass);
            Gson gson = new GsonBuilder().create();
            try {
                trains = gson.fromJson(jsonData, Trains.class);
                if (trains == null) {
                    throw new JsonParseException("error");
                }
                if (trains.getTrains() == null) {
                    throw new JsonParseException("error");
                }
            } catch (JsonParseException e) {
                try {
                    JSONObject object = new JSONObject(jsonData == null ? "" : jsonData);
                    if (object.has("error")) {
                        errorMessage = object.getString("error");
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

            if (result.equals(ERROR)) {
                mDialog.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

            RecyclerView trainList = (RecyclerView) rootView.findViewById(R.id.trainList);
            trainList.setLayoutManager(new GridLayoutManager(getActivity(), 1));
            trainList.setHasFixedSize(true);
            TrainsAdapter adapter = new TrainsAdapter(TrainInfoFragment.this);
            trainList.setAdapter(adapter);

            mDialog.dismiss();
            showSearchArea(false);
        }
    }

    private void showSearchArea(Boolean bShow) {
        if (!bShow) {
            rootView.findViewById(R.id.search_area).animate().alpha(0f).setDuration(500).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    rootView.findViewById(R.id.search_area).setVisibility(View.GONE);
                    rootView.findViewById(R.id.newSearch).animate().alpha(1f).setDuration(500).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            rootView.findViewById(R.id.newSearch).setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });

                    rootView.findViewById(R.id.trainList).animate().alpha(1f).setDuration(500).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            rootView.findViewById(R.id.trainList).setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
        } else {
            rootView.findViewById(R.id.trainList).animate().alpha(0f).setDuration(500).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    rootView.findViewById(R.id.trainList).setVisibility(View.GONE);
                    rootView.findViewById(R.id.newSearch).animate().alpha(0f).setDuration(500).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            rootView.findViewById(R.id.newSearch).setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });

                    rootView.findViewById(R.id.search_area).animate().alpha(1f).setDuration(500).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            rootView.findViewById(R.id.search_area).setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
        }
    }

    public Trains getTrains() {
        return trains;
    }

    @Override
    public void onAvailabilityCheck(Integer position) {
        availabilityClickListener.onAvailabilityClick(
                trains.getTrains().get(position),
                trains.getTrains().get(position).getTrainNumber().replaceAll("[+#*]", ""),
                trains.getTrains().get(position).getTrainName().replaceAll("[+#*]", ""),
                stationFrom,
                stationTo,
                travelDate,
                travelMonth,
                travelYear,
                travelClass,
                "GN");
    }

    @Override
    public void onFareCheck(Integer position) {
        fareClickListener.onFareClick(
                trains.getTrains().get(position),
                trains.getTrains().get(position).getTrainNumber().replaceAll("[+#*]", ""),
                trains.getTrains().get(position).getTrainName().replaceAll("[+#*]", ""),
                stationFrom,
                stationTo,
                travelDate,
                travelMonth,
                travelYear,
                travelClass,
                "GN");
    }
}
