package com.adarshahd.indianrailinfo;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

/**
 * Created by ahd on 7/11/15.
 */
public class IndianRailInfo extends MultiDexApplication {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Fabric.with(this, new Crashlytics());

        /*BaasBox.builder(this).setAuthentication(BaasBox.Config.AuthType.SESSION_TOKEN)
                .setApiDomain(Config.API_DOMAIN)
                .setPort(Config.PORT)
                .setUseHttps(true)
                .setAppCode(Config.APPCODE)
                .setPushSenderId(Config.GCM_SENDER_ID)
                .init();*/
    }
}
