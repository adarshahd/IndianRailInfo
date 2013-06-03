package com.adarshahd.indianrailinfo.donate;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.actionbarsherlock.app.SherlockActivity;


/**
 * Created by ahd on 5/27/13.
 */
public class Presenter extends SherlockActivity implements View.OnClickListener{

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ((Button)findViewById(R.id.id_btn_trn_book)).setOnClickListener(this);
        ((Button)findViewById(R.id.id_btn_pnr_sts)).setOnClickListener(this);
        ((Button)findViewById(R.id.id_btn_trn_enq)).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_btn_trn_book:
                startActivity(new Intent(this,IRCTCWeb.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return;
            case R.id.id_btn_pnr_sts:
                startActivity(new Intent(this,PNRStat.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return;
            case R.id.id_btn_trn_enq:
                startActivity(new Intent(this,TrainEnquiry.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                return;
        }
    }
}