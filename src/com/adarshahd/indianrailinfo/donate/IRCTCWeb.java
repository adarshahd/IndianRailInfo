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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Calendar;
import java.util.Iterator;

public class IRCTCWeb extends SherlockActivity {

    public static final String BOOK = "BOOK";
    private WebView mWebViewMain;
    private static SharedPreferences mPrefs;
    private static boolean mAskForSave;
    private static SherlockActivity mActivity;
    private static Util mUtil;
    private static String mDay = "";
    private static String mMonth = "";
    private static String mSrc = "";
    private static String mDst = "";
    private static Calendar mCal;
    private static String mCls = "";
    private static Intent mIntent;
    private static String mPage = "";
    private static String mPNRNumber = "";

    //    private static ProgressBar mProgBar;
    private static MenuItem mMenuStopLoading;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.layout_irctcweb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("IRCTC Mobile");
        mIntent = getIntent();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAskForSave = mPrefs.getBoolean("ask_save_user_info", true);
        mActivity = this;
        mUtil = Util.getUtil(this);
        setupWebView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUtil.delete();
    }

    private void setupWebView() {
        mWebViewMain = (WebView) mActivity.findViewById(R.id.id_wv);
        mWebViewMain.getSettings().setJavaScriptEnabled(true);
        mWebViewMain.getSettings().setDomStorageEnabled(true);
        mWebViewMain.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebViewMain.addJavascriptInterface(new JSInterface(mActivity),"jsi");
        mWebViewMain.setWebViewClient(new IRCTCClient(mActivity));
        mWebViewMain.setWebChromeClient(new IRCTCChromeClient());

        if(mIntent != null && mIntent.getAction() != null && mIntent.getAction().equals(BOOK)) {
            //Directly show the booking screen
            mSrc = mIntent.getStringExtra(TrainEnquiry.SRC);
            mDst = mIntent.getStringExtra(TrainEnquiry.DST);
            mDay = mIntent.getStringExtra(TrainEnquiry.DAY_TRAVEL);
            mMonth = mIntent.getStringExtra(TrainEnquiry.MONTH_TRAVEL);
            mCls = mIntent.getStringExtra(TrainEnquiry.CLS);
            if(mPrefs.getString("username","").equals("")) {
                showAlertLoginInfo();
                return;
            }
            mWebViewMain.loadUrl("file:///android_asset/planner.htm");
            return;
        }
        mWebViewMain.loadUrl("file:///android_asset/mobile.htm");
        //mWebViewMain.loadUrl("file:///android_asset/congratulations.htm");
    }


    @Override
    public void onBackPressed() {
        if(isHomePage(mWebViewMain)) {
            finish();
        }
        if(mWebViewMain.canGoBack()) {
            mWebViewMain.goBack();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_irctcweb, menu);
        mMenuStopLoading = menu.findItem(R.id.action_stop_page);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_reload:
                mWebViewMain.reload();
                return true;
            case R.id.action_back:
                if(isHomePage(mWebViewMain)) {
                    finish();
                    return true;
                }
                if(mWebViewMain.canGoBack()) {
                    mWebViewMain.goBack();
                }
                return true;
            case R.id.action_forward:
                if(mWebViewMain.canGoForward()) {
                    mWebViewMain.goForward();
                }
                return true;
            case R.id.action_stop_page:
                mWebViewMain.stopLoading();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class JSInterface {

        private Context mContext;
        public JSInterface(Context context) {
            mContext = context;
        }

        @JavascriptInterface
        public String isAutoLoginEnabled() {
            if(!mUtil.isConnected()) {
                Toast.makeText(mContext,"Network unavailable",Toast.LENGTH_SHORT).show();
                return "false";
            }
            return mPrefs.getBoolean("enable_auto_login",false) ? "true" : "false";
        }

        @JavascriptInterface
        public String getUserName() {
            return mPrefs.getString("username","");
        }

        @JavascriptInterface
        public String getPassword() {
            return mPrefs.getString("password","");
        }

        @JavascriptInterface
        public void showMessage(String str) {
            Toast.makeText(mActivity,str, Toast.LENGTH_LONG).show();
        }

        @JavascriptInterface
        public void doRequestSave(final String userName, final String password) {
            mAskForSave = mPrefs.getBoolean("ask_save_user_info",true);
            if(!mAskForSave) {
                return;
            }
            if (!mPrefs.getString("username","").equals(userName)) {
                View view = mActivity.getLayoutInflater().inflate(R.layout.layout_save_info, null);
                ((CheckBox)view.findViewById(R.id.id_cb_do_not_show_again)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if(b) {
                            mPrefs.edit().putBoolean("ask_save_user_info", b).commit();
                        }
                    }
                });
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("Save user information?");
                builder.setView(view);
                builder.setCancelable(false);
                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mPrefs.edit().putString("username", userName).commit();
                        mPrefs.edit().putString("password", password).commit();
                        mPrefs.edit().putBoolean("enable_auto_login", true).commit();
                        Toast.makeText(mContext, "user info saved", Toast.LENGTH_LONG).show();
                        dialogInterface.dismiss();
                    }
                });
                builder.setNegativeButton("Do not save",new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                AlertDialog dialog  = builder.create();
                dialog.show();
            }
        }

        @JavascriptInterface
        public String getDay() {
            return mDay;
        }

        @JavascriptInterface
        public String getMonth() {
            return mMonth;
        }

        @JavascriptInterface
        public String getYear() {
            return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        }

        @JavascriptInterface
        public String getSRC() {
            return mSrc;
        }

        @JavascriptInterface
        public String getDST() {
            return mDst;
        }

        @JavascriptInterface
        public String getClassTravel() {
            return mCls;
        }

        @JavascriptInterface
        public void trackSuccessfulBooking(String htmlPage) {
            if(htmlPage.contains("Congratulations")) {
                mPage = htmlPage;
                showTrackDialog();
            }
        }
    }

    public class IRCTCClient extends WebViewClient {
        private Context mContext;

        public IRCTCClient(Context context) {
            mContext = context;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.loadUrl("javascript:window.jsi.trackSuccessfulBooking" +
                    "('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(mContext,description,Toast.LENGTH_LONG).show();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if(url.equals("https://www.irctc.co.in/")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }
            view.loadUrl(url);
            return true;
        }
    }

    public class IRCTCChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {

            //setSupportProgressBarVisibility(true);
            if (newProgress == 100) {
                setSupportProgressBarVisibility(false);
                if(mMenuStopLoading != null) {
                    mMenuStopLoading.setVisible(false);
                }
            } else {
                setSupportProgressBarVisibility(true);
                if(mMenuStopLoading != null) {
                    mMenuStopLoading.setVisible(true);
                }
                setSupportProgress(newProgress * 100);
            }
        }
    }

    private static boolean isHomePage(WebView view) {
        String data = view.getUrl();
        //Toast.makeText(mActivity,data,Toast.LENGTH_LONG).show();
        //Log.i("TAG",data);
        if(data.contains("logOut.do") || data.contains("file:///android_asset/mobile.htm")) {
            return true;
        } else {
            return false;
        }
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(IRCTCWeb.this);
        final View view = getLayoutInflater().inflate(R.layout.layout_login,null);
        builder.setView(view);
        builder.setTitle("Login Information");

        builder.setPositiveButton("OK",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String userName = ((EditText)view.findViewById(R.id.id_et_username)).getText().toString();
                String password = ((EditText)view.findViewById(R.id.id_et_password)).getText().toString();
                if(userName.equals("")) {
                    ((EditText)view.findViewById(R.id.id_et_username)).setError("Username can not be empty");
                    return;
                }
                mPrefs.edit().putString("username", userName).commit();
                mPrefs.edit().putString("password", password).commit();
                mPrefs.edit().putBoolean("enable_auto_login", true).commit();
                IRCTCWeb.this.finish();
            }
        });
        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private void showAlertLoginInfo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(IRCTCWeb.this);
        builder.setTitle("Need Login Info");
        builder.setMessage("Looks like you have not enabled auto login feature and entered your login details of IRCTC. With out these information it is not possible to book tickets directly. Would you like to enter login information now?\n\nAfter entering these you will be taken back to availability page, select the info again to book tickets.");
        builder.setPositiveButton("OK",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showLoginDialog();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private static void showTrackDialog() {
        if (needTracking()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            builder.setTitle("Congratulations!");
            builder.setMessage("Congratulations on your ticket booking! Would you like to track the current PNR number for status change?");
            builder.setPositiveButton("Yes, Please",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(mActivity, "Done. You will be notified of status change", Toast.LENGTH_SHORT).show();
                    if(mPNRNumber != null && !mPNRNumber.equals("") && mPNRNumber.length() == 10) {
                        PNRDatabase.getPNRDatabase(mActivity).addPNRToTrack(mPNRNumber);
                    }
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }
    }

    private static void trackPNR () {

    }

    private static boolean needTracking() {
        boolean needTracking = false;
        Elements elements = Jsoup.parse(mPage).getElementsByClass("productbox");
        Iterator iterator = elements.iterator();
        Element element = (Element) iterator.next();
        mPNRNumber = element.select("div").get(0).text().split(": ",0)[1];
        for(int i=0; i<6;++i) {
            iterator.next();
        }
        while(iterator.hasNext()) {
            String currentStatus = ((Element) iterator.next()).select("div").get(0).text();
            if(!currentStatus.contains("CNF")) {
                needTracking = true;
            }
        }
        return needTracking;
    }
}
