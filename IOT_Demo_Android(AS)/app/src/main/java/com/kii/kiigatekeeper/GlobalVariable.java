package com.kii.kiigatekeeper;

import android.app.Application;

import com.kii.cloud.storage.Kii;

/**
 * Created by yue on 14/12/1.
 */
public class GlobalVariable extends Application{

    @Override
    public void onCreate() {

        // Configures the SDK to use the specified Application ID and Key.
        // It must be called prior to any API calls.
        // It is ok to call this method multiple times
        Kii.initialize(Constants.KII_APP_ID, Constants.KII_APP_KEY, Constants.KIIS_SITE);
    }

}
