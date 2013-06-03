package com.adarshahd.indianrailinfo.donate;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ahd on 5/29/13.
 */
public class TrainDetails extends SherlockActivity {
    //For fare enquiry
    //"http://www.indianrail.gov.in/cgi_bin/inet_frenq_cgi.cgi"
    //"lccp_trnno","lccp_day","lccp_month","lccp_srccode","lccp_dstncode","lccp_classopt","lccp_age","lccp_conc"-"ZZZZZZ",
    // "lccp_enrtcode"-"NONE", "lccp_viacode"-"NONE","lccp_frclass1"-"ZZ", "lccp_frclass2"-"ZZ","lccp_frclass3"-"ZZ",
    // "lccp_frclass4"-"ZZ", "lccp_frclass5"-"ZZ", "lccp_frclass6"-"ZZ", "lccp_frclass7"-"ZZ", "lccp_disp_avl_flg"-"1",
    // "getIt", "Please+Wait...").post();

    //For seat availability with train number
    //http://www.indianrail.gov.in/cgi_bin/inet_accavl_cgi.cgi
    //"lccp_trnno", "lccp_day", "lccp_month","lccp_srccode""lccp_dstncode","lccp_class1",
    // "lccp_quota","lccp_classopt","lccp_class2"- "ZZ","lccp_class3"- "ZZ","lccp_class4"-"ZZ",
    // "lccp_class5"-"ZZ","lccp_class6"-"ZZ","lccp_class7"-"ZZ","lccp_class8"-"ZZ","lccp_class9"-"ZZ" .post();

    private static final String TRAIN_NUMBER = "lccp_trnno";
    private static final String TO = "lccp_dstncode";
    private static final String FROM = "lccp_srccode";
    private static final String  CLASS = "lccp_classopt";
    private static final String  DAY = "lccp_day";
    private static final String MONTH = "lccp_month";
    private static final String AGE = "lccp_age";


    public static final String FARE_ENQ_POST = "http://www.indianrail.gov.in/cgi_bin/inet_frenq_cgi.cgi";
    public static final String AVAILABILITY_ENQ_POST = "http://www.indianrail.gov.in/cgi_bin/inet_accavl_cgi.cgi";
    private static String mTrainNumber;
    private static String mSrc, mDst, mDay, mMonth, mCls, mAge;
    private static SherlockActivity mActivity;
    private static FrameLayout mFrameLayout;
    private static TableLayout mTblLayoutAv;
    private static TableLayout mTblLayoutFr;
    private static List<List<String>> mListAv;
    private static List<List<String>> mListFr;
    private static Elements mElements;
    private static ProgressDialog mDialog;
    private static String mAction;
    private static Details mDetails;
    private static String mPage;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_details);
        mActivity = this;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDialog = new ProgressDialog(mActivity);
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setIndeterminate(true);
        mDialog.show();
        if(savedInstanceState != null) {
            mFrameLayout.removeAllViews();
            mFrameLayout = null;
            mFrameLayout = (FrameLayout) findViewById(R.id.id_fl_details);
            mDetails = savedInstanceState.getParcelable("LIST");
            if(mDetails.getAction().equals(TrainEnquiry.FARE)) {
                createTableLayoutTrainFare();
            }
            if(mDetails.getAction().equals(TrainEnquiry.AVAILABILITY)) {
                createTableLayoutTrainAvailability();
            }
            return;
        }
        mFrameLayout = (FrameLayout) findViewById(R.id.id_fl_details);
        Intent intent = getIntent();
        mAction = intent.getStringExtra(TrainEnquiry.ACTION);
        if(mAction.equals(TrainEnquiry.FARE)) {
            getSupportActionBar().setTitle("Fare Details");
            mTrainNumber = intent.getStringExtra(TrainEnquiry.TRAIN);
            mSrc = intent.getStringExtra(TrainEnquiry.SRC);
            mDst = intent.getStringExtra(TrainEnquiry.DST);
            mDay = intent.getStringExtra(TrainEnquiry.DAY_TRAVEL);
            mMonth = intent.getStringExtra(TrainEnquiry.MONTH_TRAVEL);
            mCls = intent.getStringExtra(TrainEnquiry.CLS);
            mAge = intent.getStringExtra(TrainEnquiry.AGE);
            new GetFare().execute();
            mDialog.setMessage("Fetching train fare . . .");
        }
        if(mAction.equals(TrainEnquiry.AVAILABILITY)) {
            getSupportActionBar().setTitle("Availability Details");
            mTrainNumber = intent.getStringExtra(TrainEnquiry.TRAIN);
            mSrc = intent.getStringExtra(TrainEnquiry.SRC);
            mDst = intent.getStringExtra(TrainEnquiry.DST);
            mDay = intent.getStringExtra(TrainEnquiry.DAY_TRAVEL);
            mMonth = intent.getStringExtra(TrainEnquiry.MONTH_TRAVEL);
            mCls = intent.getStringExtra(TrainEnquiry.CLS);
            new GetAvail().execute();
            mDialog.setMessage("Fetching train availability . . .");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onDestroy() {
        mDetails = null;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("ACTION",mAction);
        outState.putParcelable("LIST",mDetails);
        super.onSaveInstanceState(outState);
    }

    private class Details implements Parcelable {
        private List<List<String>> mList;
        private String mAction;
        private String trainNumber;

        public Details (List<List<String>> list, String action, String number) {
            mList = list;
            mAction = action;
            trainNumber = number;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {

        }

        public List<List<String>> getList() {
            return mList;
        }

        public String getAction() {
            return mAction;
        }

        public String getTrainNumber() {
            return trainNumber;
        }
    }

    private class GetFare extends AsyncTask<Void,Void,String> {

        @Override
        protected String doInBackground(Void... params) {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(FARE_ENQ_POST);
            String result = "";
            ArrayList<BasicNameValuePair> paramsPost = new ArrayList<BasicNameValuePair>();
            paramsPost.add(new BasicNameValuePair(TRAIN_NUMBER,mTrainNumber));
            paramsPost.add(new BasicNameValuePair(TO,mDst));
            paramsPost.add(new BasicNameValuePair(FROM,mSrc));
            paramsPost.add(new BasicNameValuePair(CLASS,mCls));
            paramsPost.add(new BasicNameValuePair(DAY, mDay));
            paramsPost.add(new BasicNameValuePair(MONTH, mMonth));
            paramsPost.add(new BasicNameValuePair(AGE,mAge)); //
            paramsPost.add(new BasicNameValuePair("lccp_conc","ZZZZZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_enrtcode","NONE"));
            paramsPost.add(new BasicNameValuePair("lccp_viacode","NONE"));
            paramsPost.add(new BasicNameValuePair("lccp_frclass1",mCls));
            paramsPost.add(new BasicNameValuePair("lccp_frclass2","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_frclass3","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_frclass4","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_frclass5","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_frclass6","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_frclass7","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_disp_avl_flg","1"));
            paramsPost.add(new BasicNameValuePair("getIt", "Please+Wait..."));

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

            mPage = result;

            if(result.contains("Network Connectivity")) {
                return "Network Connectivity";
            }
            
            if(result.contains("ISL Of")) {
                return "ISL Of";
            }

            if(result.contains("unavailable")) {
                return "unavailable";
            }

            if(result.contains("ERROR")) {
                return "ERROR";
            }

            mElements = Jsoup.parse(result).select("table tbody tr td:containsOwn(Fare/Charges)");
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mDialog.setMessage("Processing data . . .");
            if (!s.equals("")) {
                createTableLayoutTrainFare();
            }
        }
    }

    private void createTableLayoutTrainFare() {
        if(mPage.contains("ERROR")) {
            TextView textViewTrnDtls = new TextView(mActivity);
            textViewTrnDtls.setText("Your request resulted in an error.\nPlease check!");
            textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
            textViewTrnDtls.setPadding(10, 10, 10, 10);
            textViewTrnDtls.setTextColor(Color.RED);
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(textViewTrnDtls);
            if(mDialog.isShowing()) {
                mDialog.cancel();
            }
            return;
        }
        
        if(mPage.contains("Network Connectivity")) {
            TextView textViewTrnDtls = new TextView(mActivity);
            textViewTrnDtls.setText("Looks like the server is busy.\nPlease try later!");
            textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
            textViewTrnDtls.setPadding(10, 10, 10, 10);
            textViewTrnDtls.setTextColor(Color.RED);
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(textViewTrnDtls);
            if(mDialog.isShowing()) {
                mDialog.cancel();
            }
            return;
        }
        
        if(mPage.contains("unavailable")) {
            TextView textViewTrnDtls = new TextView(mActivity);
            textViewTrnDtls.setText("Response from server:\n\nYour request could not be processed now. \nPlease try again later!");
            textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
            textViewTrnDtls.setPadding(10, 10, 10, 10);
            textViewTrnDtls.setTextColor(Color.RED);
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(textViewTrnDtls);
            if(mDialog.isShowing()) {
                mDialog.cancel();
            }
            return;
        }

        if(mPage.contains("ISL Of")) {
            TextView textViewTrnDtls = new TextView(mActivity);
            textViewTrnDtls.setText("Station is not in ISL Of the Train. \nPlease change the source/destination!");
            textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
            textViewTrnDtls.setPadding(10, 10, 10, 10);
            textViewTrnDtls.setTextColor(Color.RED);
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(textViewTrnDtls);
            if(mDialog.isShowing()) {
                mDialog.cancel();
            }
            return;
        }

        if(mDetails == null || !mDetails.getTrainNumber().equals(mTrainNumber)) {
            Iterator iterator = mElements.first().parent().parent().parent().getElementsByTag("tr").iterator();
            mListFr = new ArrayList<List<String>>();
            List<String> list;
            Element tmp;
            while (iterator.hasNext()) {
                tmp = (Element) iterator.next();
                list = new ArrayList<String>();
                list.add(tmp.select("td").get(0).text());
                list.add(tmp.select("td").get(1).text());
                mListFr.add(list);
            }
            mDetails = new Details(mListFr,TrainEnquiry.FARE,mTrainNumber);
        } else {
            mListFr = mDetails.getList();
        }
        mTblLayoutFr = new TableLayout(mActivity);
        TableRow row;
        TextView tv1, tv2;
        mTblLayoutFr.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for(int i=0;i<mListFr.size();i++) {
            row = new TableRow(mActivity);
            tv1 = new TextView(mActivity);
            tv2 = new TextView(mActivity);

            tv1.setText("   " + mListFr.get(i).get(0));
            tv2.setText("   " + mListFr.get(i).get(1));

            tv1.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Medium);
            tv2.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Medium);

            tv1.setPadding(5,5,5,5);
            tv2.setPadding(5,5,5,5);

        /*tv2.setBackgroundResource(R.drawable.card_divider);
        tv3.setBackgroundResource(R.drawable.card_divider);
        tv4.setBackgroundResource(R.drawable.card_divider);*/

            row.addView(tv1);
            row.addView(tv2);

            row.setBackgroundResource(R.drawable.button_selector);
            row.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            mTblLayoutFr.addView(row);
        }
        LinearLayout ll = new LinearLayout(mActivity);
        TextView textViewTrnDtls = new TextView(mActivity);
        textViewTrnDtls.setText("Fare details:");
        textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
        textViewTrnDtls.setPadding(10, 10, 10, 10);
        ll.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(textViewTrnDtls);
        ll.addView(mTblLayoutFr);
        mFrameLayout.removeAllViews();
        mFrameLayout.addView(ll);
        if(mDialog.isShowing()) {
            mDialog.cancel();
        }
    }

    private class GetAvail extends AsyncTask<Void,Void,String> {

        @Override
        protected String doInBackground(Void... params) {
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(AVAILABILITY_ENQ_POST);
            String result = "";
            //For testing!!!
            /*File file = new File("/sdcard/seat_availability.html");
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


            ArrayList<BasicNameValuePair> paramsPost = new ArrayList<BasicNameValuePair>();
            paramsPost.add(new BasicNameValuePair(TRAIN_NUMBER, mTrainNumber));
            paramsPost.add(new BasicNameValuePair(DAY, mDay));
            paramsPost.add(new BasicNameValuePair(MONTH, mMonth));
            paramsPost.add(new BasicNameValuePair(FROM,mSrc));
            paramsPost.add(new BasicNameValuePair(TO,mDst));
            paramsPost.add(new BasicNameValuePair("lccp_class1",mCls));
            paramsPost.add(new BasicNameValuePair("lccp_quota","GN"));
            paramsPost.add(new BasicNameValuePair(CLASS,mCls));
            paramsPost.add(new BasicNameValuePair("lccp_class2","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_class3","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_class4","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_class5","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_class6","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_class7","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_class8","ZZ"));
            paramsPost.add(new BasicNameValuePair("lccp_class9","ZZ"));
            paramsPost.add(new BasicNameValuePair("Submit", "Please+Wait..."));

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
            //Jsoup.par
            mPage = result;
            
            if(result.contains("Network Connectivity")) {
                return "Network Connectivity";
            }

            if(result.contains("ISL Of")) {
                return "ISL Of";
            }

            if(result.contains("unavailable")) {
                return "unavailable";
            }

            if(result.contains("ERROR")) {
                return "ERROR";
            }

            mElements = Jsoup.parse(result).select("table tbody tr th:containsOwn(S.No.)");
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mDialog.setMessage("Processing data . . .");
            if(!s.equals("")) {
                createTableLayoutTrainAvailability();
            }
        }
    }

    private void createTableLayoutTrainAvailability() {

        if(mPage.contains("ERROR")) {
            TextView textViewTrnDtls = new TextView(mActivity);
            textViewTrnDtls.setText("Your request resulted in an error.\nPlease check!");
            textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
            textViewTrnDtls.setPadding(10, 10, 10, 10);
            textViewTrnDtls.setTextColor(Color.RED);
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(textViewTrnDtls);
            if(mDialog.isShowing()) {
                mDialog.cancel();
            }
            return;
        }
        if(mPage.contains("unavailable")) {
            TextView textViewTrnDtls = new TextView(mActivity);
            textViewTrnDtls.setText("Response from server:\n\nYour request could not be processed now.\nPlease try again later!");
            textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
            textViewTrnDtls.setPadding(10, 10, 10, 10);
            textViewTrnDtls.setTextColor(Color.RED);
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(textViewTrnDtls);
            if(mDialog.isShowing()) {
                mDialog.cancel();
            }
            return;
        }
        
        if(mPage.contains("Network Connectivity")) {
            TextView textViewTrnDtls = new TextView(mActivity);
            textViewTrnDtls.setText("Looks like the server is busy.\nPlease try later!");
            textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
            textViewTrnDtls.setPadding(10, 10, 10, 10);
            textViewTrnDtls.setTextColor(Color.RED);
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(textViewTrnDtls);
            if(mDialog.isShowing()) {
                mDialog.cancel();
            }
            return;
        }
        

        if(mPage.contains("ISL Of")) {
            TextView textViewTrnDtls = new TextView(mActivity);
            textViewTrnDtls.setText("Station is not in ISL Of the Train. \nPlease change the source/destination!");
            textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
            textViewTrnDtls.setPadding(10, 10, 10, 10);
            textViewTrnDtls.setTextColor(Color.RED);
            mFrameLayout.removeAllViews();
            mFrameLayout.addView(textViewTrnDtls);
            if(mDialog.isShowing()) {
                mDialog.cancel();
            }
            return;
        }

        if(mDetails == null || !mDetails.getTrainNumber().equals(mTrainNumber)) {
            Iterator iterator = mElements.first().parent().parent().parent().getElementsByTag("tr").iterator();
            mListAv = new ArrayList<List<String>>();
            List<String> list;
            Element tmp;
            tmp = (Element) iterator.next();
            list = new ArrayList<String>();
            list.add(tmp.select("th").get(0).text());
            list.add("Date");
            list.add(tmp.select("th").get(2).text());
            list.add(tmp.select("th").get(3).text());
            mListAv.add(list);
            while(iterator.hasNext()) {
                tmp = (Element) iterator.next();
                if(!tmp.hasText()) {
                    continue;
                }
                list = new ArrayList<String>();
                list.add(tmp.select("td").get(0).text());
                list.add(tmp.select("td").get(1).text());
                list.add(tmp.select("td").get(2).text());
                list.add(tmp.select("td").get(3).text());
                mListAv.add(list);
            }
            mDetails = new Details(mListAv,TrainEnquiry.AVAILABILITY,mTrainNumber);
        } else {
            mListAv = mDetails.getList();
        }
        mTblLayoutAv = new TableLayout(mActivity);
        TableRow row;
        TextView tv1, tv2, tv3,tv4;
        mTblLayoutAv.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        for(int i=0;i<mListAv.size();i++) {
            row = new TableRow(mActivity);
            tv1 = new TextView(mActivity);
            tv2 = new TextView(mActivity);
            tv3 = new TextView(mActivity);
            tv4 = new TextView(mActivity);



            tv1.setText("   " + mListAv.get(i).get(0));
            tv2.setText("   " + mListAv.get(i).get(1));
            tv3.setText("   " + mListAv.get(i).get(2));
            tv4.setText("   " + mListAv.get(i).get(3));


            tv1.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Medium);
            tv2.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Medium);
            tv3.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Medium);
            tv4.setTextAppearance(mActivity,android.R.style.TextAppearance_DeviceDefault_Medium);


            tv1.setPadding(5,5,5,5);
            tv2.setPadding(5,5,5,5);
            tv3.setPadding(5,5,5,5);
            tv4.setPadding(5,5,5,5);

        /*tv2.setBackgroundResource(R.drawable.card_divider);
        tv3.setBackgroundResource(R.drawable.card_divider);
        tv4.setBackgroundResource(R.drawable.card_divider);*/

            row.addView(tv1);
            row.addView(tv2);
            row.addView(tv3);
            row.addView(tv4);

            row.setBackgroundResource(R.drawable.button_selector);
            row.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            mTblLayoutAv.addView(row);
        }
        LinearLayout ll = new LinearLayout(mActivity);
        TextView textViewTrnDtls = new TextView(mActivity);
        textViewTrnDtls.setText("Availability details:");
        textViewTrnDtls.setTextAppearance(mActivity, android.R.style.TextAppearance_DeviceDefault_Large);
        textViewTrnDtls.setPadding(10, 10, 10, 10);
        ll.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(textViewTrnDtls);
        ll.addView(mTblLayoutAv);
        mFrameLayout.removeAllViews();
        mFrameLayout.addView(ll);
        if(mDialog.isShowing()) {
            mDialog.cancel();
        }
    }
}