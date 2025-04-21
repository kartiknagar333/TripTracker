package com.example.followme.activity;

import static android.view.View.GONE;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.followme.API.UserRepository;
import com.example.followme.Model.TripPoint;
import com.example.followme.R;
import com.example.followme.Util.LocationService;
import com.example.followme.Util.NetWorkCheckingOntime;
import com.example.followme.Util.NetworkChecker;
import com.example.followme.Util.PointReceiver;
import com.example.followme.databinding.ActivityTripLeadBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class TripLeadActivity extends FragmentActivity implements OnMapReadyCallback {
    private ActivityTripLeadBinding binding;
    private boolean trylocation = false,isEarlier = true;
    private AlertDialog Dialog;
    private UserRepository userRepository;
    private PointReceiver pointReceiver;
    private Intent locationServiceIntent;
    private Runnable elapsedTimeRunnable;
    private static final String PREFS_NAME = "UserPrefs";
    private SharedPreferences spf;
    private GoogleMap mMap;
    private static final int LOCATION_REQUEST = 111;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Polyline llHistoryPolyline;
    private final ArrayList<LatLng> latLonHistory = new ArrayList<>();
    private Marker carMarker;
    private String lasttime;
    public static int screenHeight;
    private double lastLatitude = 0.0, lastLongitude = 0.0;
    public static int screenWidth;
    private final float zoomDefault = 15.0f;
    private ObjectAnimator objectAnimator1;
    private ObjectAnimator objectAnimator2;
    private  int status = 0;
    private AnimatorSet animatorSet;
    private float totalDistance = 0f;
    private String tripid;
    private boolean isfirstAddtrippoint = true;
    private Handler handler = new Handler();
    private long seconds = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTripLeadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        tripid = getIntent().getStringExtra("tripID");
        objectAnimator1 =
                ObjectAnimator.ofFloat(binding.centeranimator, "alpha", 1.0f, 0.25f);
        objectAnimator2 =
                ObjectAnimator.ofFloat(binding.brodimg, "alpha", 1.0f, 0.25f);




        userRepository = new UserRepository();
        spf = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (!isLocationEnabled()) {
            trylocation = true;
            DisplayDialogLocationMessage("Follow Me - " + "Cannot StartTrip!");
        } else {
            if(NetworkChecker.hasNetworkConnection(this)){
                NetWorkCheckingOntime.getNetwork(this,true);
                userRepository.checkTripExists(tripid, new UserRepository.TripCallback() {
                    @Override
                    public void onSuccess(boolean exists) {
                        if(exists) {
                            ErrorDialog("Follow Me - Duplicate Trip Id", "The Trip ID '" + tripid + "' already exists.",1);
                        }else{
                            getStartTrip();
                        }
                    }
                    @Override
                    public void onFailure(String error) {
                        ErrorDialog("Follow Me - Error", error,1);
                    }
                });

            }else{
                ErrorDialog("Follow Me - No Network", "No network connection - cannot verify Trip Id now\n\nStopping the trip now.", 1);
            }
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if(status!= 2){
                    moveTaskToBack(true);
                }else{
                   finish();
                }
            }
        });

        binding.playpause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(status == 0){
                    status = 1;
                    binding.playpause.setImageResource(R.drawable.play);
                }else{
                    status = 0;
                    binding.playpause.setImageResource(R.drawable.pause);
                }
            }
        });

        binding.stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocations(v);
                status = 2;
                binding.playpause.setImageResource(R.drawable.play);
            }
        });
        binding.tvtripid.setText("Trip ID: " + tripid);


        binding.sharebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareTrip(spf.getString("username","").toString());
            }
        });


    }

    private void getStartTrip(){
        setanimation();
        startLocationService();
        getScreenDimensions();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        binding.playpause.setImageResource(R.drawable.pause);
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
    public void getNetworkChange(boolean flag){
        if(status != 2){
            if(flag){
                if(Dialog!= null && Dialog.isShowing()){
                    Dialog.dismiss();
                }
                if(!objectAnimator2.isRunning()){
                    objectAnimator2.start();
                }
                binding.nonetworktv.setVisibility(GONE);

            }else{
                if(objectAnimator2.isRunning()){
                    objectAnimator2.cancel();
                }
                binding.nonetworktv.setVisibility(View.VISIBLE);
                if(isEarlier){
                    ErrorDialog("Follow Me - No Network","No network connetion - cannot get earlier trip data now\n\nStoppig the trip now.",1);
                }
            }
        }
    }

    private void setanimation() {
        objectAnimator1.setDuration(750);
        objectAnimator1.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator1.setRepeatMode(ObjectAnimator.REVERSE);

        objectAnimator2.setDuration(750);
        objectAnimator2.setRepeatCount(ObjectAnimator.INFINITE);
        objectAnimator2.setRepeatMode(ObjectAnimator.REVERSE);

       objectAnimator1.start();
    }

    @SuppressLint("MissingPermission")
    private void updateInitialLocation(GoogleMap googleMap) {
        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastLocation != null) {
            LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomDefault));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0f));
            updateLocation(lastLocation);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomDefault));
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        setupLocationListener();
        updateInitialLocation(googleMap);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null && locationListener != null)
            locationManager.removeUpdates(locationListener);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();
        if (locationManager != null && locationListener != null)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 10, locationListener);
    }

    private void getScreenDimensions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
    }

    private void startLocationService() {
        if (isLocationEnabled()) {
            pointReceiver = new PointReceiver(this);

            ContextCompat.registerReceiver(this,
                    pointReceiver,
                    new IntentFilter("com.example.broadcast.MY_BROADCAST"),
                    ContextCompat.RECEIVER_EXPORTED);
        }
        locationServiceIntent = new Intent(this, LocationService.class);

        ContextCompat.startForegroundService(this, locationServiceIntent);

    }

    @SuppressLint("MissingPermission")
    private void setupLocationListener() {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new MyLocListener(this);
        if (locationManager != null)
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 15, locationListener);
    }

    public void updateLocation(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        objectAnimator1.cancel();
        if(status == 0){
            if(!objectAnimator2.isRunning()){
                objectAnimator2.start();
            }

            userRepository.addTripPoint(tripid, latLng.latitude, latLng.longitude, spf.getString("username",""), new UserRepository.AddTripPointCallback() {
                @Override
                public void onSuccess(TripPoint tripPoint) {
                    runOnUiThread(() -> {
                        binding.nonetworktv.setVisibility(GONE);
                        if(isfirstAddtrippoint){
                            lastLatitude = tripPoint.getLatitude();
                            lastLongitude = tripPoint.getLongitude();
                            lasttime = tripPoint.getDatetime();
                            binding.tvtriptime.setText("Trip Start: " + tripPoint.showDatetime());
                            isfirstAddtrippoint = false;
                            startElapsedTimeUpdater(LocalDateTime.parse(tripPoint.getDatetime()));
                        }
                        if(tripPoint.getLatitude() != 0 && tripPoint.getLongitude() != 0){
                            Location newLocation = new Location("current");
                            newLocation.setLatitude(tripPoint.getLatitude());
                            newLocation.setLongitude(tripPoint.getLongitude());
                            updateTotalDistance(newLocation);
                        }
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        binding.nonetworktv.setVisibility(GONE);
                    });
                }
            });

        }

    }

    private float getRadius() {
        float z = mMap.getCameraPosition().zoom;
        return 15f * z - 130f;
    }
    public void updateLocationfromservice(int counter, LatLng latLng, float bearing) {
        if(status == 0 || status == 1) {
            latLonHistory.add(latLng); // Add the LL to our location history

            if (llHistoryPolyline != null) {
                llHistoryPolyline.remove(); // Remove old polyline
            }

            if (latLonHistory.size() == 1) { // First update
                mMap.addMarker(new MarkerOptions().alpha(0.5f).position(latLng).title("My Origin")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomDefault));
                binding.centeranimator.setVisibility(GONE);
                isEarlier = false;
                return;
            }

            if (latLonHistory.size() > 1) { // Second (or more) update
                PolylineOptions polylineOptions = new PolylineOptions();

                for (LatLng ll : latLonHistory) {
                    polylineOptions.add(ll);
                }
                llHistoryPolyline = mMap.addPolyline(polylineOptions);
                llHistoryPolyline.setEndCap(new RoundCap());
                llHistoryPolyline.setWidth(12);
                llHistoryPolyline.setColor(Color.BLUE);

                float r = getRadius();
                if (r > 0) {
                    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
                    Bitmap resized = Bitmap.createScaledBitmap(icon, (int) r, (int) r, false);
                    BitmapDescriptor iconBitmap = BitmapDescriptorFactory.fromBitmap(resized);

                    MarkerOptions options = new MarkerOptions();
                    options.position(latLng);
                    options.icon(iconBitmap);
                    options.rotation(bearing);

                    if (carMarker != null) {
                        carMarker.remove();
                    }

                    carMarker = mMap.addMarker(options);
                }
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
        //calculateTotalDistance();

    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        return false;
    }

    private void ErrorDialog(String title, String message,int flag) {
        View customView = getLayoutInflater().inflate(R.layout.dialogbox_error, null);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setView(customView)
                .setCancelable(false)
                .setBackground(ContextCompat.getDrawable(this, R.drawable.borderdialogbox));

        Dialog = dialogBuilder.create();
        Dialog.show();

        TextView messageview = customView.findViewById(R.id.message);
        TextView titleview = customView.findViewById(R.id.title);
        Button btn_ok = customView.findViewById(R.id.btn_ok);

        titleview.setText(title);
        messageview.setText(message);

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog.dismiss();
                if(flag == 1){
                    finish();
                }
            }
        });

    }
    private void DisplayDialogLocationMessage(String title){
        View customView = getLayoutInflater().inflate(R.layout.dialogbox_error, null);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setView(customView)
                .setCancelable(false)
                .setBackground(ContextCompat.getDrawable(this, R.drawable.borderdialogbox));

        Dialog = dialogBuilder.create();
        Dialog.show();

        TextView messageview = customView.findViewById(R.id.message);
        TextView titleview = customView.findViewById(R.id.title);
        Button btn_ok = customView.findViewById(R.id.btn_ok);
        btn_ok.setText("Go To Location Settings");
        Button btn_cancel = customView.findViewById(R.id.btn_cancel);
        btn_cancel.setVisibility(View.VISIBLE);

        titleview.setText(title);
        messageview.setText("GPS is not enable. please enable GPS by turning on \"Use location\" in the location settings to continue.");

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLocationSettings();
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TripLeadActivity.this, "Cannot StartTrip", Toast.LENGTH_SHORT).show();
                Dialog.dismiss();
                finish();
            }
        });

    }

    private void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    protected void onRestart() {
        super.onRestart();
        if(trylocation){
            if(isLocationEnabled()){
                Dialog.dismiss();
                trylocation = false;
                startLocationService();
            }
        }
    }
    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void calculateTotalDistance() {
        float distance = 0.0f;
        if (latLonHistory.size() < 2) {
            binding.tvtripdist.setText(String.format("%.2f", distance / 1000) + " km");
        }
        for (int i = 1; i < latLonHistory.size(); i++) {
            LatLng previous = latLonHistory.get(i - 1);
            LatLng current = latLonHistory.get(i);
            float[] results = new float[1];
            Location.distanceBetween(previous.latitude, previous.longitude,
                    current.latitude, current.longitude, results);
            distance += results[0];
        }
        binding.tvtripdist.setText("Distance: "  +String.format("%.2f", distance / 1000) + " km");
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

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void getElpasedTime(String newtime){
        LocalDateTime start = LocalDateTime.parse(lasttime);
        LocalDateTime end = LocalDateTime.parse(newtime);

        Duration duration = Duration.between(start, end).minusSeconds(5);

        long totalSeconds = duration.getSeconds();

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;


        binding.tvtripelpsed.setText("Elapsed: " + String.format("%02dh %02dm %02ds", hours, minutes, seconds));

    }
    public void stopLocations(View v) {
        status = 2;
        userRepository.addTripPoint(tripid, 0.0, 0.0, spf.getString("username",""), new UserRepository.AddTripPointCallback() {
            @Override
            public void onSuccess(TripPoint tripPoint) {
                runOnUiThread(() -> {
                    handler.removeCallbacks(elapsedTimeRunnable);
                    binding.nonetworktv.setVisibility(GONE);
                    Toast.makeText(TripLeadActivity.this, "Trip Ended", Toast.LENGTH_SHORT).show();
                    objectAnimator2.cancel();
                    getElpasedTime(tripPoint.getDatetime());
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    binding.nonetworktv.setVisibility(GONE);
                    binding.nonetworktv.setText(binding.nonetworktv.getText() + "\n" + error);
                });
            }
        });
        if (pointReceiver != null) {
            unregisterReceiver(pointReceiver);
        }
        if(locationServiceIntent!=null) {
            stopService(locationServiceIntent);
            binding.stop.setEnabled(false);
            binding.playpause.setEnabled(false);
            binding.playpause.setImageResource(R.drawable.play);
        }

    }

    private void shareTrip(String message){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        String shareContent = "Follow Me Trip Id: " + tripid + "\n" +
                message + " has shared a \"Follow Me\" Trip ID with you.";

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Follow Me Trip Id: " + tripid);

        startActivity(Intent.createChooser(shareIntent, "Share via"));


    }


}