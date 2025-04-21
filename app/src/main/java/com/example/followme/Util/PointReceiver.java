package com.example.followme.Util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.followme.activity.FollowTripActivity;
import com.example.followme.activity.TripLeadActivity;
import com.google.android.gms.maps.model.LatLng;

public class PointReceiver extends BroadcastReceiver {

    private static final String TAG = "PointReceiver";

    private final TripLeadActivity mainActivity;
    private int counter = 0;

    public PointReceiver(TripLeadActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent);

        if (intent == null || intent.getAction() == null)
            return;

        if (!intent.getAction().equals("com.example.broadcast.MY_BROADCAST"))
            return;

        counter++;

        double lat = intent.getDoubleExtra("LATITUDE", 0);
        double lon = intent.getDoubleExtra("LONGITUDE", 0);
        float bearing = intent.getFloatExtra("BEARING", 0);

        mainActivity.updateLocationfromservice(counter, new LatLng(lat, lon), bearing);

    }
}