package com.adarshahd.indianrailinfo.donate;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ahd on 5/27/13.
 */
public class PNRStat extends SherlockActivity implements View.OnClickListener {
    //private static final String ENQUIRY_PAGE = "https://www.irctc.co.in/cgi-bin/soft.dll/irctc/login.do";
    private static final String ENQUIRY_PAGE = "http://www.indianrail.gov.in/cgi_bin/inet_pnrstat_cgi.cgi";
    private static final String ENQUIRY_INPUT = "lccp_pnrno1";
    private static FrameLayout mFrameLayout;
    private static SherlockActivity mActivity;
    private static AsyncTask mGetPNRSts;
    private static TextView mTextViewPNRSts;
    private static Button mBtnPNR;
    private PassengerDetails mPassengerDetails;
    private TrainDetails mTrainDetails;
    private static String mPageResult = "";
    private static TableLayout mTableLayoutPsn;
    private static TableLayout mTableLayoutTrn;
    private static String mPNRNumber = "";
    private static Util mUtil;

    @Override
    protected void onDestroy() {
        if (mGetPNRSts != null) {
            mGetPNRSts.cancel(true);
            mGetPNRSts = null;
        }
        mUtil.delete();
        super.onDestroy();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                findViewById(R.id.id_btn_get_pnr_sts).performClick();
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onMenuItemSelected(featureId, item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("PSN",mPassengerDetails);
        outState.putParcelable("TRAIN",mTrainDetails);
        outState.putString("PAGE",mPageResult);
        //outState.putString("PNR",mPNRNumber);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_pnr_status);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("PNR Status");
        mUtil = Util.getUtil(this);
        mBtnPNR = ((Button) findViewById(R.id.id_btn_get_pnr_sts));
        mBtnPNR.setOnClickListener(this);

        if(savedInstanceState != null) {
            //mPNRNumber = ((EditText)findViewById(R.id.id_et_pnr_sts)).getText().toString();
            if(!mPNRNumber.equals("")) {
                mFrameLayout.removeAllViews();
                mFrameLayout = null;
                mFrameLayout = (FrameLayout) findViewById(R.id.id_fl_pnr);

    /*            if(mTableLayoutTrn != null) {
                    mTableLayoutTrn.removeAllViews();
                    mTableLayoutTrn = null;
                }

                if(mTableLayoutPsn != null) {
                    mTableLayoutPsn.removeAllViews();
                    mTableLayoutPsn = null;
                }*/

                mPassengerDetails = savedInstanceState.getParcelable("PSN");
                mTrainDetails = savedInstanceState.getParcelable("TRAIN");
                mPageResult = savedInstanceState.getString("PAGE");
                //mPNRNumber = savedInstanceState.getString("PNR");
		if(mPageResult == null) {
			return;
		}

                createTableLayoutTrnDtls();
                createTableLayoutPsnDtls();
                combineTrainAndPsnDetails();
                return;
            }
        }


        mActivity = this;
        mFrameLayout = (FrameLayout) findViewById(R.id.id_fl_pnr);
        mBtnPNR = ((Button) findViewById(R.id.id_btn_get_pnr_sts));
        mBtnPNR.setOnClickListener(this);
        mTextViewPNRSts = new TextView(mActivity);
        mTextViewPNRSts.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mTextViewPNRSts.setGravity(Gravity.CENTER_HORIZONTAL);
        mTextViewPNRSts.setTextColor(Color.RED);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_btn_get_pnr_sts:
                if(!mUtil.isConnected()) {
                    mUtil.showAlert("Alert","Not connected to network, Please check your network connection.");
                    return;
                }
                mBtnPNR = (Button) v;
                mPNRNumber = ((EditText)findViewById(R.id.id_et_pnr_sts)).getText().toString();
                if(PNROkay(mPNRNumber)){
                    mBtnPNR.setText("Getting PNR status, please wait . . .");
                    mBtnPNR.setEnabled(false);
                    mGetPNRSts = new GetPNRStatus().execute(mPNRNumber);
                    ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(((EditText)findViewById(R.id.id_et_pnr_sts)).getWindowToken(),0);
                } else {
                    ((EditText)findViewById(R.id.id_et_pnr_sts)).setError("Invalid PNR Number");
                    mTextViewPNRSts.setText("Invalid PNR number");
                    mFrameLayout.removeAllViews();
                    mFrameLayout.addView(mTextViewPNRSts);
                }
            break;
            default:
                return ;
        }
    }

    private boolean PNROkay(String pnr) {
        if(pnr == null) {
            return false;
        }
        if (pnr.length() != 10) {
            return false;
        }
        return true;
    }

    private class GetPNRStatus extends AsyncTask<String,Integer,String> {
        String result = "";
        @Override
        protected String doInBackground(String... params) {
            HttpClient client = new DefaultHttpClient();
            ArrayList<NameValuePair> postData = new ArrayList<NameValuePair>();
            postData.add(new BasicNameValuePair(ENQUIRY_INPUT,params[0]));
            HttpPost post = new HttpPost(ENQUIRY_PAGE);
            try {
                post.setEntity(new UrlEncodedFormEntity(postData));
                HttpResponse response = client.execute(post);
                HttpEntity entity = response.getEntity();

                if(entity != null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(),"UTF-8"));
                    String tmp;
                    while((tmp = reader.readLine()) != null) {
                        result += tmp;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mPageResult = result;
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            Toast.makeText(mActivity, "Received data, processing . . .",Toast.LENGTH_LONG).show();
            mBtnPNR.setText("Get PNR status");
            mBtnPNR.setEnabled(true);
            if(!s.equals("")) {
                createTableLayoutTrnDtls();
                createTableLayoutPsnDtls();
                combineTrainAndPsnDetails();
            }

            super.onPostExecute(s);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
    }

    private class PassengerDetails implements Parcelable {

        private List<List<String>> mPassengerList;
        private String mPNR;

        public PassengerDetails(List<List<String>> list, String pnr) {
            mPassengerList = list;
            mPNR = pnr;
        }

        public List<List<String>> getPassengerList() {
            return mPassengerList;
        }

        public String getPNR() {
            return mPNR;
        }
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            //dest = getPa
        }
    }

    private class TrainDetails implements Parcelable {
        private List<String> mTrainDetails;
        private String mPNR;

        public TrainDetails(List<String> list, String pnr) {
            mTrainDetails = list;
            mPNR = pnr;
        }

        public List<String> getTrainDetails() {
            return mTrainDetails;
        }

        public String getPNR() {
            return mPNR;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }
    }

    private void createTableLayoutPsnDtls () {
        if(mPageResult.contains("FLUSHED PNR / ") || mPageResult.contains("Invalid PNR")) {
            mTextViewPNRSts.setText("The PNR entered is either invalid or expired! Please check.");
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(mTextViewPNRSts);
            return;
        }
        if(mPageResult.contains("Connectivity Failure") || mPageResult.contains("try again")) {
            mTextViewPNRSts.setText("Looks like server is busy or currently unavailable. Please try again later!");
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(mTextViewPNRSts);
            return;
        }
        List<List<String>> passengersList;
        if (mPassengerDetails == null || mPassengerDetails.getPNR() != mPNRNumber) {
            Elements elements = Jsoup.parse(mPageResult).select("table tr td:containsOwn(S. No.)");
            Iterator iterator = elements.first().parent().parent().getElementsByTag("tr").iterator();
            passengersList = new ArrayList<List<String>>();
            List<String> list;
            Element tmp;
            while(iterator.hasNext()) {
                tmp = (Element) iterator.next();
                if(tmp.toString().contains("Passenger")) {
                    list = new ArrayList<String>();
                    list.add(tmp.select("td").get(0).text());
                    list.add(tmp.select("td").get(1).text());
                    list.add(tmp.select("td").get(2).text());
                    passengersList.add(list);
                }
            }
            mPassengerDetails = new PassengerDetails(passengersList,mPNRNumber);
        } else {
            passengersList = mPassengerDetails.getPassengerList();
        }

        mTableLayoutPsn = new TableLayout(mActivity);
        TableRow row;
        TextView tv1, tv2, tv3,tv4;
        mTableLayoutPsn.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for(int i=0; i<passengersList.size(); ++i) {
            row = new TableRow(mActivity);
            row.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            tv1 = new TextView(mActivity);
            tv2 = new TextView(mActivity);
            tv3 = new TextView(mActivity);
            tv4 = new TextView(mActivity);

            tv1.setText("" + (i+1) +".");
            tv2.setText("   " + passengersList.get(i).get(0));
            tv3.setText("   " + passengersList.get(i).get(1));
            tv4.setText("   " + passengersList.get(i).get(2));

            tv1.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Medium);
            tv2.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Medium);
            tv3.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Medium);
            tv4.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Medium);

            tv1.setPadding(10,10,10,10);
            tv2.setPadding(10,10,10,10);
            tv3.setPadding(10,10,10,10);
            tv4.setPadding(10,10,10,10);

            /*tv2.setBackgroundResource(R.drawable.card_divider);
            tv3.setBackgroundResource(R.drawable.card_divider);
            tv4.setBackgroundResource(R.drawable.card_divider);*/

            row.addView(tv1);
            row.addView(tv2);
            row.addView(tv3);
            row.addView(tv4);

            row.setBackgroundResource(R.drawable.card_background);
            row.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            mTableLayoutPsn.addView(row);
        }
    }

    private void createTableLayoutTrnDtls() {
        if(mPageResult.contains("FLUSHED PNR / ") || mPageResult.contains("Invalid PNR")) {
            mTextViewPNRSts.setText("The PNR entered is either invalid or expired! Please check.");
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(mTextViewPNRSts);
            return;
        }
        if(mPageResult.contains("Connectivity Failure") || mPageResult.contains("try again")) {
            mTextViewPNRSts.setText("Looks like server is busy or currently unavailable. Please try again later!");
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(mTextViewPNRSts);
            return;
        }
        List<String> trainList;
        if (mTrainDetails == null || mTrainDetails.getPNR() != mPNRNumber) {
            Elements eleTrain = Jsoup.parse(mPageResult).select("table tr tr td:containsOwn(Train Number)");
            Iterator iteTrain = eleTrain.first().parent().parent().parent().getElementsByTag("tr").iterator();
            trainList = new ArrayList<String>();
            Element tmp;
            //Get the third row for train details
            iteTrain.next();
            iteTrain.next();
            if(iteTrain.hasNext()) {
                tmp = (Element) iteTrain.next();
                trainList.add(tmp.select("td").get(0).text());
                trainList.add(tmp.select("td").get(1).text());
                trainList.add(tmp.select("td").get(2).text());
//              trainList.add(tmp.select("td").get(3).text());
//              trainList.add(tmp.select("td").get(4).text());
                trainList.add(tmp.select("td").get(5).text());
                trainList.add(tmp.select("td").get(6).text());
                trainList.add(tmp.select("td").get(7).text());
            }
            mTrainDetails = new TrainDetails(trainList, mPNRNumber);
        } else {
            trainList = mTrainDetails.getTrainDetails();
        }
        mTableLayoutTrn = new TableLayout(mActivity);
        mTableLayoutTrn.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TableRow row = new TableRow(mActivity);
        row.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for(String list : trainList) {
            TextView tv = new TextView(mActivity);
            tv.setText(list);
            tv.setPadding(10,10,10,10);
            tv.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Small);
            row.addView(tv);
        }
        row.setBackgroundResource(R.drawable.card_background);
        row.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        mTableLayoutTrn.addView(row);
    }

    private void combineTrainAndPsnDetails() {
        if(mPageResult.contains("FLUSHED PNR / ") || mPageResult.contains("Invalid PNR")) {
            mTextViewPNRSts.setText("The PNR entered is either invalid or expired! Please check.");
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(mTextViewPNRSts);
            return;
        }
        if(mPageResult.contains("Connectivity Failure") || mPageResult.contains("try again")) {
            mTextViewPNRSts.setText("Looks like server is busy or currently unavailable. Please try again later!");
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(mTextViewPNRSts);
            return;
        }
        //Combine both Train & Passenger details table into a single LinearLayout and add it to FrameLayout
        LinearLayout ll = new LinearLayout(mActivity);
        TextView textViewTrnDtls = new TextView(mActivity);
        TextView textViewPsnDtls = new TextView(mActivity);

        textViewTrnDtls.setText("Train Details: " + mPNRNumber);
        textViewTrnDtls.setFocusable(true);
        textViewPsnDtls.setText("Passenger Details");
        textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
        textViewPsnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
        textViewTrnDtls.setPadding(10, 10, 10, 10);
        textViewPsnDtls.setPadding(10,10,10,10);
        textViewTrnDtls.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        textViewPsnDtls.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        ll.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(textViewTrnDtls);
        ll.addView(mTableLayoutTrn);
        ll.addView(textViewPsnDtls);
        ll.addView(mTableLayoutPsn);
        mFrameLayout.removeAllViews();
        mFrameLayout.addView(ll);
    }
}
