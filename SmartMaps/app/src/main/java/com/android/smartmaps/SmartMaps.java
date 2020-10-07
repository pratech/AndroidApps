package com.android.smartmaps;

import android.app.Application;
import android.content.Context;

public class SmartMaps extends Application {

    public static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    public static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    public static final int BEEP_RADIUS = 20;
    public static final int VOICE_RADIUS = 100;

    public static final float SPEED_LIMIT = 80f;
    /**
     * Static Instance for Current Application Context
     */
    private static Context applicationContext;

    /**
     * Static Instance for Current Base Context
     */
    private static Context baseContext;

    @Override
    public void onCreate() {
        super.onCreate();

        applicationContext = getApplicationContext();
        baseContext = getBaseContext();
    }

    /**
     * Returns the static Instance of the application context
     *
     * @return
     */
    public static Context GetAppContext() {
        return applicationContext;
    }

    /**
     * Returns the static Instance of the base context
     *
     * @return
     */
    public static Context GetBaseContext() {
        return baseContext;
    }

}
