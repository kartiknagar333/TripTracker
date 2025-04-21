package com.example.followme.activity;


import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.followme.API.UserRepository;
import com.example.followme.Model.TripPoint;
import com.example.followme.R;
import com.example.followme.Util.NetWorkCheckingOntime;
import com.example.followme.Util.NetworkChecker;
import com.example.followme.databinding.ActivityFollowTripBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FollowTripActivity extends FragmentActivity implements OnMapReadyCallback {
    private ActivityFollowTripBinding binding;
    private UserRepository userRepository;
    private boolean isready = false;
    private androidx.appcompat.app.AlertDialog dialog;
    private Runnable elapsedTimeRunnable;
    private GoogleMap mMap;

    private Polyline llHistoryPolyline;
    public static int screenHeight;
    public static int screenWidth;
    private final float zoomDefault = 15.0f;
    private GoogleMap gMap;
    private boolean isdisplayed = false;
    private double lastLatitude = 0.0, lastLongitude = 0.0;
    private String lasttime;
    private float totalDistance = 0f;
    private Handler handler = new Handler();
    private Runnable locationRunnable;
    private static final long POLLING_INTERVAL = 3000;
    private String tripID;
    private boolean getReadylsLo = true;
    private List<LatLng> latLngList = new ArrayList<>();
    private Polyline currentPolyline;
    private Marker startMarker, endMarker;
    private ObjectAnimator objectAnimator2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFollowTripBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        NetWorkCheckingOntime.getNetwork(this,false);
        userRepository = new UserRepository();
        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            if(NetworkChecker.hasNetworkConnection(FollowTripActivity.this)){
                tripID = extras.getString("tripID");
                getTripexists(extras.getString("tripID"));
            }else{
                DisplayErrorDialog("Follow Me - No Netwok", "No network connection - cannot access trip data now\n\nCannot follow the trip now",true);
            }
        }else{
            DisplayErrorDialog("Follow Me - Error", "Trip ID is missing",true);
        }

        getScreenDimensions();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        objectAnimator2 =
                ObjectAnimator.ofFloat(binding.brodimg, "alpha", 1.0f, 0.25f);
        objectAnimator2.setDuration(750);
        objectAnimator2.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator2.setRepeatMode(ObjectAnimator.REVERSE);

        binding.pointbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gettargetme();
            }
        });
    }

    private void getlatsLocation(){
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                if(!getReadylsLo) {
                    handler.removeCallbacks(locationRunnable);
                    return;
                }
                userRepository.getLastLocation(tripID, new UserRepository.GetLastLocationCallback() {
                    @Override
                    public void onSuccess(TripPoint tripPoint) {
                        Location newLocation = new Location("current");
                        newLocation.setLatitude(tripPoint.getLatitude());
                        newLocation.setLongitude(tripPoint.getLongitude());

                        if(tripPoint.getLatitude() != 0 && tripPoint.getLongitude() != 0){
                            updateTotalDistance(newLocation);
                            lastLatitude = tripPoint.getLatitude();
                            lastLongitude = tripPoint.getLongitude();
                        }



                        updateMapWithNewPoint(tripPoint);

                    }
                    @Override
                    public void onFailure(String error) {
                        DisplayErrorDialog("Follow Me - Error", error, false);
                    }
                });
                handler.postDelayed(this, POLLING_INTERVAL);
            }
        };
        // Start the runnable immediately
        handler.post(locationRunnable);
    }


    private void getScreenDimensions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
    }
    private void getTripexists(String tripID){
        userRepository.checkTripExists(tripID, new UserRepository.TripCallback() {
            @Override
            public void onSuccess(boolean exists) {
                if (exists) {
                    if(NetworkChecker.hasNetworkConnection(FollowTripActivity.this)){
                        userRepository.getTrip(tripID, new UserRepository.GetTripCallback() {
                            @SuppressLint("SetTextI18n")
                            @Override
                            public void onSuccess(List<TripPoint> tripPoints) {
                                binding.tvtripid.setText("Trip ID: " + tripID);
                                binding.tvtriptime.setText("Trip Start: " + tripPoints.get(0).showDatetime());
                                displayTripPoints(tripPoints);
                            }
                            @Override
                            public void onFailure(String error) {
                                DisplayErrorDialog("Follow Me - Error", error,true);
                            }
                        });
                    }else{
                        DisplayErrorDialog("Follow Me - No Netwok", "No network connection - cannot access trip data now\n\nCannot follow the trip now",true);
                    }
                } else {
                    DisplayErrorDialog("Trip Not Found", "The Trip ID '" + tripID + "' was not found.",true);
                }
            }
            @Override
            public void onFailure(String error) {
                DisplayErrorDialog("Follow Me - Error", error,true);
            }
        });
    }

    public  void getNetworkChange(boolean isConnected) {
        if (isready){
            if(isConnected) {
                if(!objectAnimator2.isRunning()){
                    objectAnimator2.start();
                }
                binding.nonetworktv.setVisibility(View.GONE);
            }else{
                if(objectAnimator2.isRunning()){
                    objectAnimator2.cancel();
                }
                binding.nonetworktv.setVisibility(View.VISIBLE);
            }
        }
    }

    private void DisplayErrorDialog(String title,String message,boolean isfinish) {
        View customView = getLayoutInflater().inflate(R.layout.dialogbox_error, null);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setView(customView)
                .setCancelable(false)
                .setBackground(ContextCompat.getDrawable(this, R.drawable.borderdialogbox));

        dialog = dialogBuilder.create();
        dialog.show();

        Button btn_ok = customView.findViewById(R.id.btn_ok);
        TextView messageview = customView.findViewById(R.id.message);
        TextView titleview = customView.findViewById(R.id.title);

        messageview.setText(message);
        titleview.setText(title);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if(isfinish) finish();
            }
        });
    }

    private void displayTripPoints(List<TripPoint> tripPoints) {
        if (tripPoints == null || tripPoints.isEmpty()) return;

        // Clear any previous data
        latLngList.clear();
        mMap.clear();

        // Convert TripPoints to LatLng and populate the global list
        for (TripPoint point : tripPoints) {
            if (point.getLatitude() == 0) continue;
            latLngList.add(new LatLng(point.getLatitude(), point.getLongitude()));
            lastLatitude = point.getLatitude();
            lastLongitude = point.getLongitude();
        }
        if (latLngList.isEmpty()) return;

        // Draw the polyline for the trip
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(latLngList)
                .width(12)
                .color(Color.BLUE)
                .endCap(new RoundCap());
        currentPolyline = mMap.addPolyline(polylineOptions);

        // Add a marker at the start
        startMarker = mMap.addMarker(new MarkerOptions()
                .position(latLngList.get(0))
                .title("Start")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Prepare the car icon for the end marker
        Bitmap carBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.car);
        float iconSize = 100f;
        Bitmap resizedCarBitmap = Bitmap.createScaledBitmap(carBitmap, (int) iconSize, (int) iconSize, false);
        BitmapDescriptor carIcon = BitmapDescriptorFactory.fromBitmap(resizedCarBitmap);

        // Add the end marker (current location)
        MarkerOptions endMarkerOptions = new MarkerOptions()
                .position(latLngList.get(latLngList.size() - 1))
                .title("End")
                .icon(carIcon)
                .anchor(0.5f, 0.5f);
        if (latLngList.size() > 1) {
            float bearing = computeBearing(latLngList.get(latLngList.size() - 2), latLngList.get(latLngList.size() - 1));
            endMarkerOptions.rotation(bearing).flat(true);
        }
        endMarker = mMap.addMarker(endMarkerOptions);

        if (latLngList.size() == 1) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngList.get(0), 20f));
        } else {
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            for (LatLng latLng : latLngList) {
                boundsBuilder.include(latLng);
            }
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 100;
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
        lasttime = tripPoints.get(0).getDatetime();
        isready = true;
        if(tripPoints.get(tripPoints.size()-1).getLatitude() == 0 && tripPoints.get(tripPoints.size()-1).getLongitude() == 0){
            DisplayErrorDialog("Trip Ended","The trip has ended.",false);
            getReadylsLo = false;
            handler.removeCallbacks(locationRunnable);
            getElpasedTime(tripPoints.get(tripPoints.size()-1).getDatetime());
        }else{
            getlatsLocation();
            objectAnimator2.start();
            if(!isdisplayed){
                isdisplayed = true;
                startElapsedTimeUpdater(LocalDateTime.parse(tripPoints.get(0).getDatetime()));
            }
        }
        calculateTotalDistance(latLngList);
        gettargetme();
    }


    private void updateMapWithNewPoint(TripPoint newPoint) {
        if(newPoint.getLatitude() == 0 && newPoint.getLongitude() == 0){
            handler.removeCallbacks(elapsedTimeRunnable);
            DisplayErrorDialog("Trip Ended","The trip has ended.",false);
            getReadylsLo = false;
            handler.removeCallbacks(locationRunnable);
            objectAnimator2.cancel();
            getElpasedTime(newPoint.getDatetime());
            return;
        }

        if (newPoint == null || newPoint.getLatitude() == 0) return;

        LatLng newLatLng = new LatLng(newPoint.getLatitude(), newPoint.getLongitude());
        latLngList.add(newLatLng);

        // Update the polyline with the new point
        if (currentPolyline != null) {
            currentPolyline.setPoints(latLngList);
        } else {
            PolylineOptions polylineOptions = new PolylineOptions()
                    .addAll(latLngList)
                    .width(12)
                    .color(Color.BLUE)
                    .endCap(new RoundCap());
            currentPolyline = mMap.addPolyline(polylineOptions);
        }

        // Update the end marker's position and adjust its rotation based on bearing
        if (endMarker != null) {
            if (latLngList.size() > 1) {
                float bearing = computeBearing(latLngList.get(latLngList.size() - 2), newLatLng);
                endMarker.setRotation(bearing);
            }
            endMarker.setPosition(newLatLng);
        } else {
            // If not already created, create the marker
            Bitmap carBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.car);
            float iconSize = 100f;
            Bitmap resizedCarBitmap = Bitmap.createScaledBitmap(carBitmap, (int) iconSize, (int) iconSize, false);
            BitmapDescriptor carIcon = BitmapDescriptorFactory.fromBitmap(resizedCarBitmap);
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(newLatLng)
                    .title("End")
                    .icon(carIcon)
                    .anchor(0.5f, 0.5f);
            endMarker = mMap.addMarker(markerOptions);
        }

        // Optionally, animate the camera to the new location
       // mMap.animateCamera(CameraUpdateFactory.newLatLng(newLatLng));
    }



    private float computeBearing(LatLng from, LatLng to) {
        double fromLat = Math.toRadians(from.latitude);
        double fromLng = Math.toRadians(from.longitude);
        double toLat   = Math.toRadians(to.latitude);
        double toLng   = Math.toRadians(to.longitude);

        double dLng = toLng - fromLng;
        double y = Math.sin(dLng) * Math.cos(toLat);
        double x = Math.cos(fromLat) * Math.sin(toLat)
                - Math.sin(fromLat) * Math.cos(toLat) * Math.cos(dLng);
        double bearing = Math.toDegrees(Math.atan2(y, x));

        return (float) ((bearing + 360) % 360);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void calculateTotalDistance(List<LatLng> latLngList) {
        if (latLngList == null || latLngList.size() < 2) {
            return;
        }
        for (int i = 1; i < latLngList.size(); i++) {
            Location previousLocation = new Location("previous");
            previousLocation.setLatitude(latLngList.get(i - 1).latitude);
            previousLocation.setLongitude(latLngList.get(i - 1).longitude);

            Location currentLocation = new Location("current");
            currentLocation.setLatitude(latLngList.get(i).latitude);
            currentLocation.setLongitude(latLngList.get(i).longitude);

            totalDistance += previousLocation.distanceTo(currentLocation);
        }

        binding.tvtripdist.setText(String.format("Distance: %.2f", (totalDistance / 1000.0)) + " km");

    }
    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateTotalDistance(Location newLocation) {
        if (newLocation == null) return;
        Location previousLocation = new Location("previous");
        previousLocation.setLatitude(lastLatitude);
        previousLocation.setLongitude(lastLongitude);
        totalDistance += previousLocation.distanceTo(newLocation);
        binding.tvtripdist.setText(String.format("Distance: %.2f", (totalDistance / 1000.0)) + " km");
    }

    private void gettargetme (){
        if(isready){
            LatLng markerLocation = new LatLng(lastLatitude, lastLongitude);
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(markerLocation, 15.0f));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(markerLocation));
        }
    }

    private void startElapsedTimeUpdater(LocalDateTime tripStartTime) {
        elapsedTimeRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (tripStartTime != null) {
                        LocalDateTime now = LocalDateTime.now();
                        Duration elapsed = Duration.between(tripStartTime, now);
                        long totalSeconds = Math.max(0, elapsed.getSeconds());
                        long hours = totalSeconds / 3600;
                        long minutes = (totalSeconds % 3600) / 60;
                        long seconds = totalSeconds % 60;
                        binding.tvtripelpsed.setText("Elapsed: " + String.format("%02dh %02dm %02ds", hours, minutes, seconds));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Update every second
                handler.postDelayed(this, 1000);
            }
        };
        // Start the updater immediately
        handler.post(elapsedTimeRunnable);
    }


    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void getElpasedTime(String newtime){
        LocalDateTime start = LocalDateTime.parse(lasttime);
        LocalDateTime end = LocalDateTime.parse(newtime);

        Duration duration = Duration.between(start, end).minusSeconds(4);

        long totalSeconds = duration.getSeconds();

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;


        binding.tvtripelpsed.setText("Elapsed: " + String.format("%02dh %02dm %02ds", hours, minutes, seconds));

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomDefault));
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.resetMinMaxZoomPreference();
        gMap = googleMap;
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(getReadylsLo){
            handler.post(locationRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(getReadylsLo){
            handler.removeCallbacks(locationRunnable);
        }
    }
}