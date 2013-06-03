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
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class IRCTCWeb extends SherlockActivity {

    private WebView mWebViewMain;
    private static SharedPreferences mPrefs;
    private static boolean mAskForSave;
    private static SherlockActivity mActivity;
    private static Util mUtil;

    //    private static ProgressBar mProgBar;
    private static MenuItem mMenuStopLoading;


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mUtil.delete();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.layout_irctcweb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("IRCTC Mobile");
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAskForSave = mPrefs.getBoolean("ask_save_user_info",true);
        mActivity = this;
        mUtil = Util.getUtil(this);



        setupWebView();
    }

    private void setupWebView() {
        mWebViewMain = (WebView) mActivity.findViewById(R.id.id_wv);
        mWebViewMain.getSettings().setJavaScriptEnabled(true);
        mWebViewMain.getSettings().setDomStorageEnabled(true);
        mWebViewMain.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebViewMain.addJavascriptInterface(new JSInterface(mActivity),"jsi");
        mWebViewMain.setWebViewClient(new IRCTCClient(mActivity));
        mWebViewMain.setWebChromeClient(new IRCTCChromeClient());
        mWebViewMain.loadUrl("file:///android_asset/mobile.htm");
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
            case R.id.action_preferences:
                startActivity(new Intent(this,PrefActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
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
    }

    public class IRCTCClient extends WebViewClient {
        private Context mContext;

        public IRCTCClient(Context context) {
            mContext = context;
        }

        @Override
        public void onPageFinished(WebView view, String url) {

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
}
