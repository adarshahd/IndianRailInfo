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

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
//import com.actionbarsherlock.view.ContextMenu;

/**
 * Created by ahd on 5/27/13.
 */
public class TrainEnquiry extends SherlockActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener {

    public static final String ACTION = "ACTION";
    public static final String FARE = "FARE";
    public static final String AVAILABILITY = "AVAILABILITY";
    public static final String TRAIN = "TRAIN";
    public static final String SRC = "SRC";
    public static final String DST = "DST";
    public static final String AGE = "AGE";
    public static final String DAY_TRAVEL = "DAY";
    public static final String MONTH_TRAVEL = "MONTH";
    public static final String  CLS = "CLS";
    public static final String ISINT = "ISINT";
    public static final String SEARCH = "SEARCH";
    public static final String PAGE = "PAGE";

    private static final String POST_PAGE_STN = "http://www.indianrail.gov.in/cgi_bin/inet_srcdest_cgi_time.cgi";
    private static final String POST_PAGE_TRN = "http://www.indianrail.gov.in/cgi_bin/inet_trnnum_cgi.cgi";
    private static final String TRAIN_NAME = "lccp_trnname";
    private static final String TO = "lccp_dstn_stncode";
    private static final String FROM = "lccp_src_stncode";
    private static final String  CLASS = "lccp_classopt";
    private static final String  DAY = "lccp_day";
    private static final String MONTH = "lccp_month";
    private static final String DEP_TIME = "lccp_dep_time";     //Any Time
    private static final String DEPB_TIME = "lccp_depb_time";   //Any Time
    private static final String ARR_TIME = "lccp_ari_time";     //Any Time
    private static final String ARRB_TIME = "lccp_arib_time";   //Any Time
    private static final String TRAIN_TYPE = "lccp_trn_type";   //All types
    private static boolean searchUsingTrnNumber;
    private static boolean isInteger;


    private static AutoCompleteTextView mACTFrom;
    private static AutoCompleteTextView mACTTo;
    private static EditText mETTrainNumber;

    private static Button mBtnTrnDtls;
    private static ArrayAdapter mAdapterStnCodes;
    private static SherlockActivity mActivity;
    private static FrameLayout mFrameLayout;

    private static String mSrc;
    private static String mDst;
    private static Util mUtil;
    private static String mPage;
    private static String mTrainNumber;
    private static Calendar mCal;
    private static Button mBtnDate;
    // private static Elements mElements;



    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_train_enquiry);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Train Enquiry");
        mActivity = this;
        mUtil = Util.getUtil(this);
        initControls();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUtil.delete();
    }

    private void initControls() {
        mETTrainNumber = (EditText) findViewById(R.id.id_et_trn_nm);
        mACTFrom = (AutoCompleteTextView) findViewById(R.id.id_act_from);
        mACTTo = (AutoCompleteTextView) findViewById(R.id.id_act_to);
        mAdapterStnCodes = new ArrayAdapter<String>(this,R.layout.layout_dropdown_list,getResources().getStringArray(R.array.stn_codes));
        mACTFrom.setAdapter(mAdapterStnCodes);
        mACTTo.setAdapter(mAdapterStnCodes);
        mCal = Calendar.getInstance();
        mBtnTrnDtls = (Button) findViewById(R.id.id_btn_details);
        mBtnDate = (Button) findViewById(R.id.id_btn_date);
        mFrameLayout = (FrameLayout) findViewById(R.id.id_fl_trn_info);
        mBtnTrnDtls.setOnClickListener(this);
        mBtnDate.setOnClickListener(this);
        mBtnDate.setText(DateFormat.format("dd MMM yyyy", mCal));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.id_btn_date:
                DatePickerDialog datePickerDialog = new DatePickerDialog(this,this,mCal.get(Calendar.YEAR),mCal.get(Calendar.MONTH),mCal.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();
                break;
            case R.id.id_btn_details:
                if(!mUtil.isConnected()) {
                    mUtil.showAlert("Alert","Network unavailable, Please check your network connection.");
                    return;
                }
                if(isFormOK()) {
                    ((Button)v).setText("Getting details, please wait . . .");
                    v.setEnabled(false);
                    if (!searchUsingTrnNumber) {
                        mSrc = mACTFrom.getText().toString().split("- ",0)[1];
                        mDst = mACTTo.getText().toString().split("- ",0)[1];
                    }
                    new GetTrainDetails().execute();
                } else {
                    break;
                }
                break;
            default:
                return;
        }
    }

    private boolean isFormOK() {
        if(!mETTrainNumber.getText().toString().equals("")) {
            //OK. Train number is entered, check next
            mTrainNumber = mETTrainNumber.getText().toString();
            searchUsingTrnNumber = true;

        } else {
            //Train number not entered check for FROM/TO and other details
            searchUsingTrnNumber = false;
            if(mACTFrom.getText().toString().equals("")) {
                mACTFrom.setError("Source required!");
                return false;
            }
            if(mACTTo.getText().toString().equals("")) {
                mACTTo.setError("Destination required!");
                return false;
            }
            if(!mACTFrom.getText().toString().contains("- ")) {
                mACTFrom.setError("Invalid code, Please select from drop down list");
                return false;
            }
            if(!mACTTo.getText().toString().contains("- ")) {
                mACTTo.setError("Invalid code, Please select from drop down list");
                return false;
            }
            if(mACTTo.getText().toString().equals(mACTFrom.getText().toString())) {
                mACTFrom.setError("Station names can not be same");
                return false;
            }
        }

        return true;
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        mCal.set(Calendar.getInstance().get(Calendar.YEAR),monthOfYear,dayOfMonth);
        mBtnDate.setText(DateFormat.format("dd MMM yyyy", mCal));
    }

    private class GetTrainDetails extends AsyncTask<Void,Void,String> {

        @Override
        protected String doInBackground(Void... params) {

            HttpClient client = new DefaultHttpClient();
            HttpPost post;
            String result = "";
            if(searchUsingTrnNumber) {
                post = new HttpPost(POST_PAGE_TRN);
                ArrayList<BasicNameValuePair> paramsPost = new ArrayList<BasicNameValuePair>();
                paramsPost.add(new BasicNameValuePair(TRAIN_NAME,mETTrainNumber.getText().toString()));
                try {
                    post.setEntity(new UrlEncodedFormEntity(paramsPost));
                    HttpResponse response = client.execute(post);
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(),"UTF-8"));
                        String tmp;
                        while((tmp = reader.readLine()) != null) {
                            result += tmp;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //mElements = Jsoup
            } else {
                post = new HttpPost(POST_PAGE_STN);
                ArrayList<BasicNameValuePair> paramsPost = new ArrayList<BasicNameValuePair>();
                paramsPost.add(new BasicNameValuePair(TO,mDst));
                paramsPost.add(new BasicNameValuePair(FROM,mSrc));
                paramsPost.add(new BasicNameValuePair(CLASS,"ZZ"));
                paramsPost.add(new BasicNameValuePair(DAY, String.valueOf(mCal.get(Calendar.DAY_OF_MONTH))));
                paramsPost.add(new BasicNameValuePair(MONTH, String.valueOf(mCal.get(Calendar.MONTH) + 1)));
                paramsPost.add(new BasicNameValuePair(DEP_TIME, "0"));
                paramsPost.add(new BasicNameValuePair(DEPB_TIME, "24"));
                paramsPost.add(new BasicNameValuePair(ARR_TIME, "0"));
                paramsPost.add(new BasicNameValuePair(ARRB_TIME, "24"));
                paramsPost.add(new BasicNameValuePair(TRAIN_TYPE, "Z"));

                try {
                    post.setEntity(new UrlEncodedFormEntity(paramsPost));
                    HttpResponse response = client.execute(post);
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(),"UTF-8"));
                        String tmp;
                        while((tmp = reader.readLine()) != null) {
                            result += tmp;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            mPage = result;
            return result;
        }

        @Override
        protected void onPostExecute(String s) {

            mBtnTrnDtls.setEnabled(true);
            mBtnTrnDtls.setText("Get Details");
            if(!s.equals("")) {
                Toast.makeText(mActivity, "Received data, processing . . .", Toast.LENGTH_LONG).show();
                startDetailsActivity();
            }
            super.onPostExecute(s);
        }
    }

    private void startDetailsActivity () {

        //For testing!!!
        /*File file = new File("/sdcard/train_details_trn_number.html");
        String result = "";
        try {
            FileInputStream stream = new FileInputStream(file);
            InputStreamReader readerStream = new InputStreamReader(stream);
            BufferedReader reader = new BufferedReader(readerStream);
            String str;
            while((str = reader.readLine()) != null) {
                result += str;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        if(mPage.contains("unavailable")) {
            TextView tv = new TextView(mActivity);
            tv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL));
            tv.setTextColor(Color.RED);
            tv.setTextSize(20);
            tv.setText("Response from server: \n\n\"The requested service is currently unavailable. Please try again later\"");
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(tv);
            return;
        }
        if(mPage.contains("invalid")) {
            TextView tv = new TextView(mActivity);
            tv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL));
            tv.setTextColor(Color.RED);
            tv.setTextSize(20);
            tv.setText("Invalid Station code! Please select the station code from drop down list.");
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(tv);
            return;
        }
        if(mPage.contains("No Matching Trains")) {
            TextView tv = new TextView(mActivity);
            tv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL));
            tv.setTextColor(Color.RED);
            tv.setTextSize(20);
            tv.setText("No Matching train found! Please check the train number/name");
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(tv);
            return;
        }

        //put the required intent data and start TrainEnquiryDetails activity.
        Intent intent = new Intent(this,TrainEnquiryDetails.class);
        intent.putExtra(TRAIN,mTrainNumber);
        intent.putExtra(SEARCH,searchUsingTrnNumber);
        intent.putExtra(PAGE,mPage);
        intent.putExtra(SRC,mACTFrom.getText().toString());
        intent.putExtra(DST,mACTTo.getText().toString());
        intent.putExtra(DAY_TRAVEL,mCal.get(Calendar.DAY_OF_MONTH));
        intent.putExtra(MONTH_TRAVEL,mCal.get(Calendar.MONTH));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //Try to parse the mTrainNumber;
        try {
            Integer.parseInt(mTrainNumber);
            isInteger = true;
        } catch(Exception e) {
            isInteger = false;
        }
        intent.putExtra(ISINT,isInteger);
        startActivity(intent);
    }
}