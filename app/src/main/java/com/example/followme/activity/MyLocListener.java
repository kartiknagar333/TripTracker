package com.example.followme.activity;

import android.location.Location;
import android.location.LocationListener;
import android.util.Log;

import androidx.annotation.NonNull;

public class MyLocListener implements LocationListener {

    private final TripLeadActivity mapsActivity;


    private static final String TAG = "MyLocListener";

    public MyLocListener(TripLeadActivity mapsActivity) {
        this.mapsActivity = mapsActivity;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d(TAG, "onLocationChanged: " + location);
        mapsActivity.updateLocation(location);
    }
}
