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
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by ahd on 6/2/13.
 */
public class TrainEnquiryDetails extends SherlockActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener {

    private static TableLayout mTableLayout;
    private static TableRow mRow;
    private static String mTrainNumber = "";
    private static String mSrc = "";
    private static String mDst = "";
    private static boolean searchUsingTrainNumber;
    private static boolean isInteger;
    private static String mPage = "";
    private static Elements mElements;
    private static List<List<String>> mDetails;
    private static String mSelectedTrain = "";
    private static AutoCompleteTextView mACTFrom;
    private static AutoCompleteTextView mACTTo;
    private static Spinner mSpinnerClass;
    private static Spinner mSpinnerAge;
    private static Button mBtnDate;
    private static Map <String, String> mMapCls;
    private static Map <String, String> mMapAge;
    private static ArrayAdapter<String> mAdapterStnCodes;
    private static Calendar mCal;
    private static Util mUtil;
    private static String mAge = "";
    private static ProgressDialog mDialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("TrainDetails");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        setContentView(R.layout.layout_train_enquiry_details);
        if (savedInstanceState != null) {
            mTableLayout = null;
            mTableLayout = (TableLayout) findViewById(R.id.id_tl_en_details);
            initControls();
            if (mDetails != null) {
                createTableLayout();
            }
            return;
        }
        mTrainNumber = intent.getStringExtra(TrainEnquiry.TRAIN);
        /*mSrc = intent.getStringExtra(TrainEnquiry.SRC);
        mDst = intent.getStringExtra(TrainEnquiry.DST);*/
        searchUsingTrainNumber = intent.getBooleanExtra(TrainEnquiry.SEARCH,false);
        isInteger = intent.getBooleanExtra(TrainEnquiry.ISINT, false);
        mPage = intent.getStringExtra(TrainEnquiry.PAGE);

        mUtil = Util.getUtil(this);
        mCal = Calendar.getInstance();
        mCal.set(Calendar.getInstance().get(Calendar.YEAR),intent.getIntExtra(TrainEnquiry.MONTH_TRAVEL,0),intent.getIntExtra(TrainEnquiry.DAY_TRAVEL,1));
        initControls();
        mACTFrom.setText(intent.getStringExtra(TrainEnquiry.SRC));
        mACTTo.setText(intent.getStringExtra(TrainEnquiry.DST));
        mDialog = new ProgressDialog(this);
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setMessage("Processing . . . ");
        mDialog.show();
        //parse the page and create table
        new CreateList().execute();

    }

    private void initControls() {
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        mACTFrom = (AutoCompleteTextView) findViewById(R.id.id_act_from);
        mACTTo = (AutoCompleteTextView) findViewById(R.id.id_act_to);
        mSpinnerClass = (Spinner) findViewById(R.id.id_sp_class);
        mSpinnerAge = (Spinner) findViewById(R.id.id_sp_age);
        mBtnDate = (Button) findViewById(R.id.id_btn_date);
        mMapCls = new HashMap();

        mMapAge = new HashMap();
        mBtnDate.setOnClickListener(this);
        mAdapterStnCodes = new ArrayAdapter<String>(this,R.layout.layout_dropdown_list,getResources().getStringArray(R.array.stn_codes));
        mACTFrom.setAdapter(mAdapterStnCodes);
        mACTTo.setAdapter(mAdapterStnCodes);
        //mCal = Calendar.getInstance();
        mBtnDate.setText(DateFormat.format("dd MMM yyyy", mCal));
        mTableLayout = (TableLayout) findViewById(R.id.id_tl_en_details);

        mMapCls.put("ALL CLASS","ZZ");
        mMapCls.put("FIRST AC","1A");
        mMapCls.put("SECOND AC","2A");
        mMapCls.put("THIRD AC","3A");
        mMapCls.put("3 AC Economy","3E");
        mMapCls.put("AC CHAIR CAR","CC");
        mMapCls.put("FIRST CLASS","FC");
        mMapCls.put("SLEEPER CLASS","SL");
        mMapCls.put("SECOND SEATING","2S");

        mMapAge.put("CHILD AGE","8");
        mMapAge.put("ADULT AGE","30");
        mMapAge.put("SENIOR CITIZEN FEMALE","61");
        mMapAge.put("SENIOR CITIZEN MALE","60");

        String [] cls = {"ALL CLASS","FIRST AC","SECOND AC","THIRD AC","3 AC Economy","AC CHAIR CAR","FIRST CLASS","SLEEPER CLASS","SECOND SEATING"};
        String [] age = {"CHILD AGE","ADULT AGE","SENIOR CITIZEN FEMALE","SENIOR CITIZEN MALE"};
        mSpinnerClass.setAdapter(new ArrayAdapter<String>(this,R.layout.layout_dropdown_list,cls));
        mSpinnerClass.setSelection(7);
        mSpinnerAge.setAdapter(new ArrayAdapter<String>(this,R.layout.layout_dropdown_list,age));
        mSpinnerAge.setSelection(1);
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
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        TextView textView = (TextView) ((TableRow)v).getChildAt(0);
        if(textView.getText().toString().contains("Train No.") || textView.getText().toString().contains("Train Number")) {
            return;
        }
        mSelectedTrain = textView.getText().toString();
        //Toast.makeText(mActivity,"selected train: " + mSelectedTrain + "Src: " + mSrc + "Dst: " + mDst,Toast.LENGTH_LONG).show();
        getMenuInflater().inflate(R.menu.context_menu_trn_details,menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.id_action_get_av:
                if(!mUtil.isConnected()) {
                    mUtil.showAlert("Alert","Network unavailable, Please check your network connection.");
                    return false;
                }
                if(isAvFormOK()){
                    String cls = mMapCls.get(mSpinnerClass.getSelectedItem().toString());

                    startActivity(new Intent(this, TrainDetails.class)
                            .putExtra(TrainEnquiry.ACTION, TrainEnquiry.AVAILABILITY)
                            .putExtra(TrainEnquiry.TRAIN, mSelectedTrain.trim())
                            .putExtra(TrainEnquiry.SRC, mSrc)
                            .putExtra(TrainEnquiry.DST, mDst)
                            .putExtra(TrainEnquiry.CLS, cls)
                            .putExtra(TrainEnquiry.DAY_TRAVEL, String.valueOf(mCal.get(Calendar.DAY_OF_MONTH)))
                            .putExtra(TrainEnquiry.MONTH_TRAVEL, String.valueOf(mCal.get(Calendar.MONTH) + 1))
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                }
                return true;
            case R.id.id_action_get_fare:
                if(!mUtil.isConnected()) {
                    mUtil.showAlert("Alert","Network unavailable, Please check your network connection.");
                    return false;
                }
                if(isFareFormOK()) {
                    startActivity(new Intent(this, TrainDetails.class)
                            .putExtra(TrainEnquiry.ACTION, TrainEnquiry.FARE)
                            .putExtra(TrainEnquiry.TRAIN, mSelectedTrain.trim())
                            .putExtra(TrainEnquiry.SRC, mSrc)
                            .putExtra(TrainEnquiry.DST, mDst)
                            .putExtra(TrainEnquiry.AGE, mAge)
                            .putExtra(TrainEnquiry.CLS, mMapCls.get(mSpinnerClass.getSelectedItem().toString()))
                            .putExtra(TrainEnquiry.DAY_TRAVEL, String.valueOf(mCal.get(Calendar.DAY_OF_MONTH)))
                            .putExtra(TrainEnquiry.MONTH_TRAVEL, String.valueOf(mCal.get(Calendar.MONTH) +1))
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                }
                return true;
            default:
                return true;
        }

    }

    private void createTableLayout() {
        //mTableLayout = new TableLayout(this);
        int sizeTableRow = mDetails.size();
        int sizeTableColumn = mDetails.get(0).size();
        TextView [] tv = new TextView[sizeTableColumn];
        for(int i=0; i<sizeTableRow; ++i) {
            mRow = new TableRow(this);
            mRow.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mRow.setOnCreateContextMenuListener(this);
            mRow.setBackgroundResource(R.drawable.button_selector);
            mRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView textView = (TextView) ((TableRow)v).getChildAt(0);
                    if(textView.getText().toString().contains("Train No") || textView.getText().toString().contains("Train Number")) {
                        return;
                    }
                    mSelectedTrain = textView.getText().toString();
                    v.showContextMenu();
                    //Toast.makeText(mActivity,"selected train: " + mSelectedTrain + "Src: " + mSrc + "Dst: " + mDst,Toast.LENGTH_LONG).show();
                }
            });
            for(int j=0; j<sizeTableColumn; ++j) {
                tv[j] = new TextView(this);
                tv[j].setTextAppearance(this, android.R.style.TextAppearance_DeviceDefault_Medium);
                tv[j].setPadding(5,5,5,5);
                tv[j].setText(mDetails.get(i).get(j));
                mRow.addView(tv[j]);
            }
            mTableLayout.addView(mRow);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.id_btn_date:
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,this,mCal.get(Calendar.YEAR),mCal.get(Calendar.MONTH),mCal.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
            break;
        }

    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        mCal.set(Calendar.getInstance().get(Calendar.YEAR),monthOfYear,dayOfMonth);
        mBtnDate.setText(DateFormat.format("dd MMM yyyy",mCal));
    }

    private class CreateList extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if(searchUsingTrainNumber) {
                if(isInteger) {
                    mElements = Jsoup.parse(mPage).select("table tbody tr td:containsOwn(Train No)");
                } else {
                    mElements = Jsoup.parse(mPage).select("table tbody tr td:containsOwn(Train Number)");
                }
                Iterator iterator = null;
                try {
                    iterator = mElements.first().parent().parent().parent().getElementsByTag("tr").iterator();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("TrainEnquiryDetails", mPage);
                    return null;
                }
                mDetails = new ArrayList<List<String>>();
                List<String> list;
                Element tmp;
                while(iterator.hasNext()) {
                    tmp = (Element) iterator.next();
                    list = new ArrayList<String>();
                    list.add(tmp.select("td").get(0).text());
                    list.add(tmp.select("td").get(1).text());
                    list.add(tmp.select("td").get(2).text());
                    if(!isInteger) {
                        list.add(tmp.select("td").get(3).text());
                        list.add(tmp.select("td").get(4).text());
                        list.add(tmp.select("td").get(5).text());
                    }
                    mDetails.add(list);
                }
            } else {
                Elements elements = Jsoup.parse(mPage).select("table tr td:containsOwn(Train No.)");
                Iterator iterator = null;
                try {
                    iterator = elements.first().parent().parent().getElementsByTag("tr").iterator();
                } catch (Exception e) {
                    Log.i("TrainEnquiryDetails",mPage);
                    e.printStackTrace();
                    return null;
                }
                mDetails = new ArrayList<List<String>>();
                List<String> list;
                Element tmp;
                //iterator.next();
                //iterator.next();
                while(iterator.hasNext()) {
                    tmp = (Element) iterator.next();
                    list = new ArrayList<String>();
                    if(tmp.select("td").get(0).text().equals("M")) {
                        continue;
                    }
                    list.add(tmp.select("td").get(0).text());
                    list.add(tmp.select("td").get(1).text());
                    list.add(tmp.select("td").get(2).text());
                    list.add(tmp.select("td").get(3).text());
                    list.add(tmp.select("td").get(4).text());
                    list.add(tmp.select("td").get(5).text());
                    list.add(tmp.select("td").get(6).text());
                    mDetails.add(list);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            mDialog.dismiss();
            createTableLayout();
        }
    }

    private boolean isFareFormOK() {

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
        mSrc = mACTFrom.getText().toString().split("- ",0)[1];
        mDst = mACTTo.getText().toString().split("- ",0)[1];
        mAge = mMapAge.get(mSpinnerAge.getSelectedItem());
        return true;
    }

    private boolean isAvFormOK() {
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
        mSrc = mACTFrom.getText().toString().split("- ",0)[1];
        mDst = mACTTo.getText().toString().split("- ",0)[1];
        return true;
    }

}