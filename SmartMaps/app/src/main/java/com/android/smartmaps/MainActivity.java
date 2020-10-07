package com.android.smartmaps;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

import static android.content.pm.PackageManager.*;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, TextToSpeech.OnInitListener {

    GoogleMap googleMap;
    LocationManager locationManager;

    PendingIntent pendingIntent, pendingBeepIntent, pendingVoiceIntent;

    private TextToSpeech textToSpeech;

    SharedPreferences sharedPreferences;

    int locationCount = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textToSpeech = new TextToSpeech(this, this);

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());

        // Showing status
        if (status != ConnectionResult.SUCCESS) { // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();

        } else { // Google Play Services are available

            // Getting reference to the SupportMapFragment of activity_main.xml
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            // Getting LocationManager object from System Service LOCATION_SERVICE
            try {
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                } else {
                    // Getting GoogleMap object from the fragment
                    mapFragment.getMapAsync(this);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Opening the sharedPreferences object
            sharedPreferences = getSharedPreferences("location", 0);

            // Getting number of locations already stored
            locationCount = sharedPreferences.getInt("locationCount", 0);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocationStatus.getInstance(this).AddLocationUpdates();

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String eventType = intent.getAction();

                if(eventType.equals(LocationStatus.class.getSimpleName()))
                {
                    if (textToSpeech != null) {
                        String text = "Exceeding Speed Limit";
                        if (text != null) {
                            if (textToSpeech.isSpeaking())
                                textToSpeech.stop();

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                            } else {
                                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                            }
                        }
                    }
                }
            }
        }, new IntentFilter(LocationStatus.class.getSimpleName()));
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.getDefault());
        } else {
            textToSpeech = null;
            Toast.makeText(this, "Failed to initialize TextToSpeech engine.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    private void drawCircle(LatLng point, int radius) {

        // Instantiating CircleOptions to draw a circle around the marker
        CircleOptions circleOptions = new CircleOptions();

        // Specifying the center of the circle
        circleOptions.center(point);

        if (radius == SmartMaps.BEEP_RADIUS) {
            // Radius of the circle
            circleOptions.radius(20);

            // Fill color of the circle
            circleOptions.fillColor(0x30ff0000);
        } else if (radius == SmartMaps.VOICE_RADIUS) {
            // Radius of the circle
            circleOptions.radius(100);

            // Fill color of the circle
            circleOptions.fillColor(0x30ffa264);
        }

        // Border color of the circle
        circleOptions.strokeColor(Color.BLACK);

        // Border width of the circle
        circleOptions.strokeWidth(2);

        // Adding the circle to the GoogleMap
        googleMap.addCircle(circleOptions);

    }

    private void drawMarker(LatLng point) {
        // Creating an instance of MarkerOptions
        MarkerOptions markerOptions = new MarkerOptions();

        // Setting latitude and longitude for the marker
        markerOptions.position(point);

        // Adding InfoWindow title
        markerOptions.title("Location Coordinates");

        // Adding InfoWindow contents
        markerOptions.snippet(Double.toString(point.latitude) + "," + Double.toString(point.longitude));

        // Adding marker on the Google Map
        googleMap.addMarker(markerOptions);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        // Enabling MyLocation Layer of Google Map
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "You have to accept to enjoy all app's services!", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, SmartMaps.MY_PERMISSION_ACCESS_FINE_LOCATION);
        } else {
            googleMap.setMyLocationEnabled(true);
        }

        // Getting stored zoom level if exists else return 0
        String zoom = sharedPreferences.getString("zoom", "0");

        // If locations are already saved
        if (locationCount != 0) {

            String lat = "";
            String lng = "";

            // Iterating through all the locations stored
            for (int i = 0; i < locationCount; i++) {

                // Getting the latitude of the i-th location
                lat = sharedPreferences.getString("lat" + i, "0");

                // Getting the longitude of the i-th location
                lng = sharedPreferences.getString("lng" + i, "0");

                // Drawing marker on the map
                drawMarker(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)));

                // Drawing circle on the map
                drawCircle(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), SmartMaps.VOICE_RADIUS);
                drawCircle(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng)), SmartMaps.BEEP_RADIUS);
            }

            // Moving CameraPosition to last clicked position
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(Double.parseDouble(lat), Double.parseDouble(lng))));

            // Setting the zoom level in the map on last position  is clicked
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(Float.parseFloat(zoom)));
        } else {
            setUpMap();
        }

        googleMap.setOnMapClickListener(new OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                // Incrementing location count
                locationCount++;

                // Drawing marker on the map
                drawMarker(point);

                // Drawing circle on the map
                drawCircle(point, SmartMaps.VOICE_RADIUS);
                drawCircle(point, SmartMaps.BEEP_RADIUS);

                Intent proximityBeepIntent = getProximityIntent(point, SmartMaps.BEEP_RADIUS);
                // Creating a pending intent which will be invoked by LocationManager when the specified region is
                // entered or exited
                pendingBeepIntent = PendingIntent.getActivity(getBaseContext(), 0, proximityBeepIntent, 0);

                Intent proximityVoiceIntent = getProximityIntent(point, SmartMaps.VOICE_RADIUS);
                pendingVoiceIntent = PendingIntent.getActivity(getBaseContext(), 0, proximityVoiceIntent, 0);


                // Setting proximity alert
                // The pending intent will be invoked when the device enters or exits the region 20 meters
                // away from the marked point
                // The -1 indicates that, the monitor will not be expired
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "You have to accept to enjoy all app's services!", Toast.LENGTH_LONG).show();
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, SmartMaps.MY_PERMISSION_ACCESS_COARSE_LOCATION);
                } else {
                    locationManager.addProximityAlert(point.latitude, point.longitude, 20, -1, pendingBeepIntent);
                    locationManager.addProximityAlert(point.latitude, point.longitude, 100, -1, pendingVoiceIntent);
                }

                /** Opening the editor object to write data to sharedPreferences */
                SharedPreferences.Editor editor = sharedPreferences.edit();

                // Storing the latitude for the i-th location
                editor.putString("lat" + Integer.toString((locationCount - 1)), Double.toString(point.latitude));

                // Storing the longitude for the i-th location
                editor.putString("lng" + Integer.toString((locationCount - 1)), Double.toString(point.longitude));

                // Storing the count of locations or marker count
                editor.putInt("locationCount", locationCount);

                /** Storing the zoom level to the shared preferences */
                editor.putString("zoom", Float.toString(googleMap.getCameraPosition().zoom));

                /** Saving the values stored in the shared preferences */
                editor.commit();

                Toast.makeText(getBaseContext(), "Proximity Alert is added", Toast.LENGTH_SHORT).show();

            }
        });

        googleMap.setOnMapLongClickListener(new OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
                Intent proximityIntent = new Intent("com.android.activity.proximity");

                //Adding Intent Flag
                proximityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                pendingIntent = PendingIntent.getActivity(getBaseContext(), 0, proximityIntent, 0);

                // Removing the proximity alert
                locationManager.removeProximityAlert(pendingIntent);

                // Removing the marker and circle from the Google Map
                googleMap.clear();

                // Opening the editor object to delete data from sharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();

                // Clearing the editor
                editor.clear();

                // Committing the changes
                editor.commit();

                Toast.makeText(getBaseContext(), "Proximity Alert is removed", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Function to view current location by default on the map if there is no pre entered marker
     */
    private void setUpMap() {

        // Get LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Create a criteria object to retrieve provider
        Criteria criteria = new Criteria();

        // Get the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Get Current Location
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        Location myLocation = locationManager.getLastKnownLocation(provider);

        //set map type
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        // Get latitude of the current location
        double latitude = myLocation.getLatitude();

        // Get longitude of the current location
        double longitude = myLocation.getLongitude();

        // Create a LatLng object for the current location
        LatLng latLng = new LatLng(latitude, longitude);

        // Show the current location in Google Map
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        // Zoom in the Google Map
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
        googleMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("You are here!"));
    }

    /**
     * Common method to return the intent required for setting up location proximity alert.
     * @param point
     * @param type
     * @return
     */
    private Intent getProximityIntent(LatLng point, int type) {
        // This intent will call the activity ProximityActivity
        Intent proximityIntent = new Intent("com.android.activity.proximity");

        // Passing latitude to the PendingActivity
        proximityIntent.putExtra("lat", point.latitude);

        // Passing longitude to the PendingActivity
        proximityIntent.putExtra("lng", point.longitude);

        // Passing the distance to recoginize the type of proximity alert 20 or 100
        proximityIntent.putExtra("distance", type);

        //Adding Intent Flag
        proximityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return proximityIntent;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case SmartMaps.MY_PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        googleMap.setMyLocationEnabled(true);
                    } catch (SecurityException ex) {

                    }
                }
                break;

            case SmartMaps.MY_PERMISSION_ACCESS_COARSE_LOCATION:
                break;

            default:
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocationStatus.getInstance(this).RemoveLocationUpdates();
    }
}
