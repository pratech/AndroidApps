package com.android.smartmaps;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;


public class LocationStatus implements LocationListener {
    private static final String TAG = LocationStatus.class.getSimpleName();

    /**
     * Location manager to get the current location
     */
    private LocationManager locationManager;

    /**
     * Provider from which Location is received
     */
    private String provider;


    private static boolean update = false;

    /**
     * String to store and send the error message to the server.
     */
    private static String serverMessage = "";

    /**
     * Singleton Instance for this class
     */
    private static LocationStatus mInstance = null;

    /**
     * Current activity context
     */
    private static Activity activityContext = null;

    /**
     * Singleton Instance of the Class
     *
     * @return
     */
    public static LocationStatus getInstance(Activity context) {
        if (mInstance == null) {
            activityContext = context;
            mInstance = new LocationStatus(context);
        }
        return mInstance;
    }

    private LocationStatus(Context context) {
        Location location = null;

        try {
            // Get the location manager
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

            // Define the criteria how to select the location provider -> use default
            Criteria criteria = new Criteria();

            provider = locationManager.getBestProvider(criteria, true);

            if (provider != null) {
                serverMessage = "- Provider " + provider + " has been selected.";

                Log.d(TAG, serverMessage);

                if (ActivityCompat.checkSelfPermission(activityContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(activityContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activityContext, "You have to accept to enjoy all app's services!", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(activityContext, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, SmartMaps.MY_PERMISSION_ACCESS_COARSE_LOCATION);
                } else {
                    location = locationManager.getLastKnownLocation(provider);

                    // Initialize the location fields
                    if (location != null) {
                        onLocationChanged(location);
                        serverMessage = "";
                    } else {
                        if (provider.equals(LocationManager.GPS_PROVIDER)) {
                            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                                provider = LocationManager.NETWORK_PROVIDER;
                                location = locationManager.getLastKnownLocation(provider);

                                if (location != null) {
                                    onLocationChanged(location);
                                    serverMessage = "- Location not Available in Cache for the Best (GPS) Provider. Hence using NETWORK PROVIDER.";
                                }
                            } else {
                                serverMessage = "- Location not Available in Cache for the GPS Provider. No other Provider is Enabled.";
                            }
                        } else {
                            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                provider = LocationManager.GPS_PROVIDER;
                                location = locationManager.getLastKnownLocation(provider);

                                if (location != null) {
                                    onLocationChanged(location);
                                    serverMessage = "- Location not Available in Cache for the Best (NETWORK) Provider. Hence using GPS PROVIDER.";
                                }
                            } else {
                                serverMessage = "- Location not Available in Cache for the NETWORK Provider. No other Provider is Enabled.";
                            }
                        }

                        Log.d(TAG, serverMessage);
                    }
                }
            } else {
                serverMessage = "- Location Provider not Available, Please check if GPS or Google Location Service is Enabled!";
                Log.d(TAG, serverMessage);
            }
        } catch (Exception e) {
            Log.e(TAG, " Error creating Location Listener. " + e.getMessage() + e.getStackTrace());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        float speed = (float) (location.getSpeed());
        speed = speed * 3.6f; //Converting m/s to km/hr

        if (speed > SmartMaps.SPEED_LIMIT) {
            Intent locationIntent = new Intent(TAG);
            LocalBroadcastManager.getInstance(SmartMaps.GetAppContext()).sendBroadcast(locationIntent);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, " Disabled provider: " + provider + ".");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, " Enabled new provider: " + provider + ".");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    /**
     * Function to Add Location Updates
     */
    public void AddLocationUpdates() {
        try {
            if (locationManager != null && provider != null) {
                locationManager.requestLocationUpdates(provider, 60 * 1000, 100.0f, this); //Get the update for every  60 Secs or 100 Metre
                } else {
                if (ActivityCompat.checkSelfPermission(activityContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(activityContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(activityContext, "You have to accept to enjoy all app's services!", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(activityContext, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, SmartMaps.MY_PERMISSION_ACCESS_COARSE_LOCATION);

                }
            }
        }
        catch(Exception e)
        {
            Log.e(TAG , " Error Initiating Request for Location. " + e.getMessage() + e.getStackTrace());
        }
    }

    /**
     * Function to remove location updates
     */
    public void RemoveLocationUpdates()
    {
        if(update)
        {
            try
            {
                if(locationManager != null)
                {
                    locationManager.removeUpdates(this);
                    update = false;
                    Log.e(TAG, "Stop receiving Location update!");
                }
            }
            catch(Exception e)
            {
                Log.e(TAG , " Error while stopping to send the Location! " + e.getMessage() + e.getStackTrace());
            }
        }
    }
}
