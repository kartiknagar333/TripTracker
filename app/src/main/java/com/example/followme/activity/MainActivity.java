package com.example.followme.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import androidx.appcompat.app.AppCompatActivity;
import com.example.followme.R;
import com.example.followme.Util.NetworkChecker;
import com.example.followme.ViewModel.UserViewModel;
import com.example.followme.databinding.ActivityMainBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private boolean keepOn = true;
    private final long minSplashTime = 1000;
    private long startTime;
    private int isValidate = 0;
    private static final String PREFS_NAME = "UserPrefs";
    private static final String USERNAME_SET_KEY = "usernames";
    private UserViewModel userViewModel;
    private AlertDialog Dialog;
    private boolean isLogin = true, triedScuccessfully = false,trylocation = false,isStartrip = true;
    private SharedPreferences spf;
    private SharedPreferences.Editor editor;
    private String pwd;
    private static final int LOCATION_REQUEST = 1001;
    private static final int BACKGROUND_LOCATION_REQUEST = 1002;

    private static final int NOTIFICATION_REQUEST = 1003;
    private static final int LOCATION_SETTINGS_REQUEST = 1004;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        startTime = System.currentTimeMillis();

        splashScreen.setKeepOnScreenCondition(new SplashScreen.KeepOnScreenCondition() {
            @Override
            public boolean shouldKeepOnScreen() {
                return keepOn || (System.currentTimeMillis() - startTime <= minSplashTime);
            }
        });

        new Handler().postDelayed(() -> keepOn = false, minSplashTime);
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        new Handler().postDelayed(() -> {
            View rootView = binding.getRoot();
            Animation exitAnim = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            rootView.startAnimation(exitAnim);
            setContentView(rootView);
        }, 1000);

        spf = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);


        binding.btnstarttrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isStartrip = true;
                if(spf.contains("username") || triedScuccessfully){
                    checkAppPermission();
                    return;
                }
                binding.btnstarttrip.setEnabled(false);
                DisplaySigninDialog(false);
            }
        });

        binding.btnfollowme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isStartrip = false;
                checkAppPermission();
            }
        });

        userViewModel = new UserViewModel();
        userViewModel.getUserLiveData().observe(this, user -> {
            if(isLogin){
                editor = spf.edit();
                editor.putString("username",user.getUserName());
                editor.putString("password", pwd);
                if(((CheckBox) Objects.requireNonNull(Dialog.findViewById(R.id.checksavecredetial))).isChecked()){
                    editor.putBoolean("rememberme",true);
                }else{
                    editor.putBoolean("rememberme",false);
                }
                editor.apply();
                Dialog.dismiss();
                binding.btnstarttrip.setEnabled(true);
                triedScuccessfully = true;
                checkAppPermission();
            }else{
                Dialog.dismiss();
                saveUsername(user.getUserName());
                DisplayErrorDialog(true,"Welcome "+ user.getFirstName() +" " + user.getLastName() +
                        "\n\n"+ "Your username is: " + user.getUserName() + "\nYour email is: " + user.getEmail() , "Follow Me - Registration Successful");
            }
        });
        userViewModel.getErrorLiveData().observe(this, error -> {
            if(isLogin) {
                Dialog.findViewById(R.id.btn_login).setEnabled(true);
                DisplayErrorDialog(false,error,"Follow Me - Login Failed");
            }else{
                Dialog.findViewById(R.id.btn_register).setEnabled(true);
                DisplayErrorDialog(false,error,"Follow Me - Registration Failed");
            }
        });


    }
    private void DisplaySigninDialog(boolean fromregister) {
        View customView = getLayoutInflater().inflate(R.layout.dialogbox_signin, null);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setView(customView)
                .setCancelable(false)
                .setBackground(ContextCompat.getDrawable(this, R.drawable.borderdialogbox));

        Dialog = dialogBuilder.create();
        Dialog.show();

        Button btn_register = customView.findViewById(R.id.btn_register);
        Button btn_login = customView.findViewById(R.id.btn_login);
        Button btn_cancel = customView.findViewById(R.id.btn_cancel);
        EditText usernameedit = customView.findViewById(R.id.usernameedit);
        EditText passswordedit = customView.findViewById(R.id.passswordedit);
        com.google.android.material.textfield.TextInputLayout usernamelayout = customView.findViewById(R.id.usernameLayout);
        com.google.android.material.textfield.TextInputLayout passwordlayout = customView.findViewById(R.id.passwordLayout);
        addTextChangedListener("username",usernameedit,usernamelayout);
        addTextChangedListener("password",passswordedit,passwordlayout);
        if(spf.getBoolean("rememberme",false) && !fromregister){
            usernameedit.setText(spf.getString("username",""));
            passswordedit.setText(spf.getString("password",""));
            ((CheckBox) Objects.requireNonNull(Dialog.findViewById(R.id.checksavecredetial))).setChecked(true);
        }

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog.dismiss();
                DisplayRegisterDialog();
            }
        });

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(NetworkChecker.hasNetworkConnection(MainActivity.this)){
                    isValidate = 0;
                    validation("username",usernameedit.getText().toString(),usernamelayout);
                    validation("password",passswordedit.getText().toString(),passwordlayout);
                    if (isValidate == 2) {
                        isLogin = true;
                        pwd = passswordedit.getText().toString();
                        btn_login.setEnabled(false);
                        userViewModel.verifyUser(usernameedit.getText().toString(),passswordedit.getText().toString());
                    }
                }else{
                    DisplayErrorDialog(false,"No network connection - cannot login now","Follow Me - No Network");
                }
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.btnstarttrip.setEnabled(true);
                Dialog.dismiss();
                checkAppPermission();
            }
        });
    }

    private void DisplayRegisterDialog() {
        View customView = getLayoutInflater().inflate(R.layout.dialogbox_signup, null);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setView(customView)
                .setCancelable(false)
                .setBackground(ContextCompat.getDrawable(this, R.drawable.borderdialogbox));

        Dialog = dialogBuilder.create();
        Dialog.show();

        Button btn_register = customView.findViewById(R.id.btn_register);
        Button btn_cancel = customView.findViewById(R.id.btn_cancel);
        EditText fsnameedit = customView.findViewById(R.id.fsnameedit);
        EditText lsnameedit = customView.findViewById(R.id.lsnameedit);
        EditText emailedit = customView.findViewById(R.id.emailedit);
        EditText usernameedit = customView.findViewById(R.id.usernameedit);
        EditText passswordedit = customView.findViewById(R.id.passswordedit);
        com.google.android.material.textfield.TextInputLayout emaillayout = customView.findViewById(R.id.emaillayout);
        com.google.android.material.textfield.TextInputLayout fsnamelayout = customView.findViewById(R.id.fsnameLayout);
        com.google.android.material.textfield.TextInputLayout lsnamelayout = customView.findViewById(R.id.lsnameLayout);
        com.google.android.material.textfield.TextInputLayout usernamelayout = customView.findViewById(R.id.usernameLayout);
        com.google.android.material.textfield.TextInputLayout passwordlayout = customView.findViewById(R.id.passwordLayout);

        addTextChangedListener("email",emailedit,emaillayout);
        addTextChangedListener("username",usernameedit,usernamelayout);
        addTextChangedListener("password",passswordedit,passwordlayout);
        addTextChangedListener("firstname",fsnameedit,fsnamelayout);
        addTextChangedListener("lastname",lsnameedit,lsnamelayout);


        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(NetworkChecker.hasNetworkConnection(MainActivity.this)){
                    isValidate = 0;
                    validation("firstname",fsnameedit.getText().toString(),fsnamelayout);
                    validation("lastname",lsnameedit.getText().toString(),lsnamelayout);
                    validation("email",emailedit.getText().toString(),emaillayout);
                    validation("username",usernameedit.getText().toString(),usernamelayout);
                    validation("password",passswordedit.getText().toString(),passwordlayout);
                    if (isValidate == 5) {
                        if (isUsernameUnique(usernameedit.getText().toString())) {
                            isLogin = false;
                            btn_register.setEnabled(false);
                            userViewModel.createUser(fsnameedit.getText().toString(),lsnameedit.getText().toString(),
                                    usernameedit.getText().toString(),passswordedit.getText().toString(),
                                    emailedit.getText().toString());

                        }else{
                            DisplayErrorDialog(false,"Username "+ usernameedit.getText().toString() +" already exists.",
                                    "Follow Me - Registration Failed");
                        }
                    }
                }else{
                    DisplayErrorDialog(false,"No network connection - cannot create user account now","Follow Me - No Network");
                }
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_cancel.setEnabled(false);
                Dialog.dismiss();
                DisplaySigninDialog(false);
            }
        });
    }

    private void addTextChangedListener(String type, EditText editText, com.google.android.material.textfield.TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validation(type,s.toString(),layout);
            }
        });
    }

    private void validation(String type, String value, com.google.android.material.textfield.TextInputLayout layout) {
        switch (type) {
            case "username":
                validateUsername(value, layout);
                break;
            case "password":
                validatePassword(value, layout);
                break;
            case "firstname":
                validateFirstName(value, layout);
                break;
            case "lastname":
                validateLastName(value, layout);
                break;
            case "email":
                validateEmail(value, layout);
                break;
            default:
                break;
        }

    }

    private void validateUsername(String value, com.google.android.material.textfield.TextInputLayout layout) {
        if (value.isEmpty()) {
            layout.setError("Username is required");
            isValidate = 0;
        } else if (value.length() < 8 || value.length() > 12) {
            layout.setError("Username must be between 8 and 12 characters");
            isValidate = 0;
        } else {
            layout.setError(null);
            isValidate++;
        }
    }

    private void validatePassword(String value, com.google.android.material.textfield.TextInputLayout layout) {
        if (value.isEmpty()) {
            layout.setError("Password is required");
            isValidate = 0;
        } else if (value.length() < 8 || value.length() > 12) {
            layout.setError("Password must be between 8 and 12 characters");
            isValidate = 0;
        } else {
            layout.setError(null);
            isValidate++;
        }
    }

    private void validateFirstName(String value, com.google.android.material.textfield.TextInputLayout layout) {
        if (value.isEmpty()) {
            layout.setError("First name is required");
            isValidate = 0;
        } else if (value.length() == 1 || value.length() > 100) {
            layout.setError("First name must be between 1 and 100 characters");
            isValidate = 0;
        } else {
            layout.setError(null);
            isValidate++;
        }
    }

    private void validateLastName(String value, com.google.android.material.textfield.TextInputLayout layout) {
        if (value.isEmpty()) {
            layout.setError("Last name is required");
            isValidate = 0;
        } else if (value.length() == 1 || value.length() > 100) {
            layout.setError("Last name must be between 1 and 100 characters");
            isValidate = 0;
        } else {
            layout.setError(null);
            isValidate++;
        }
    }

    private void validateEmail(String value, com.google.android.material.textfield.TextInputLayout layout) {
        if (value.isEmpty()) {
            layout.setError("Email is required");
            isValidate = 0;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            layout.setError("Invalid email address");
            isValidate = 0;
        } else {
            layout.setError(null);
            isValidate++;
        }
    }

    private void DisplayErrorDialog(boolean openLoginDialog,String message, String title) {
        View customView = getLayoutInflater().inflate(R.layout.dialogbox_error, null);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setView(customView)
                .setBackground(ContextCompat.getDrawable(this, R.drawable.borderdialogbox));

        final androidx.appcompat.app.AlertDialog dialog = dialogBuilder.create();
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
                if (openLoginDialog) {
                    DisplaySigninDialog(true);
                }
            }
        });
    }

    private boolean isUsernameUnique(String username) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        Set<String> usernames = prefs.getStringSet(USERNAME_SET_KEY, new HashSet<>());
        return !usernames.contains(username);
    }

    @SuppressLint("MutatingSharedPrefs")
    private void saveUsername(String username) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> usernames = prefs.getStringSet(USERNAME_SET_KEY, new HashSet<>());
        usernames.add(username);
        editor.putStringSet(USERNAME_SET_KEY, usernames);
        editor.apply();
    }

    private boolean checkAppPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    }, LOCATION_REQUEST);
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.POST_NOTIFICATIONS
                        }, NOTIFICATION_REQUEST);
                return false;
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {


            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    }, BACKGROUND_LOCATION_REQUEST);
            return false;
        }


        if (!isLocationEnabled()) {
            DisplayDialogLocationMessage("Follow Me - " + (isStartrip ? "Cannot StartTrip!" : "Cannot follow!"));
            return false;
        }else{
            GetTripID();
        }

        return true;
    }

    @SuppressLint("SetTextI18n")
    private void GetTripID(){
        View customView = getLayoutInflater().inflate(R.layout.dialogbox_tripidbox, null);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setView(customView)
                .setBackground(ContextCompat.getDrawable(this, R.drawable.borderdialogbox));

        Dialog = dialogBuilder.create();
        Dialog.show();
        Button btn_generate = customView.findViewById(R.id.btn_generate);
        Button btn_cancel = customView.findViewById(R.id.btn_cancel);
        Button btn_ok = customView.findViewById(R.id.btn_ok);
        TextView msg = customView.findViewById(R.id.message);
        EditText editid = customView.findViewById(R.id.editid);

        editid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                String filtered = input.toUpperCase().replaceAll("[^A-Z0-9-]", "");
                if (!input.equals(filtered)) {
                    editid.removeTextChangedListener(this);
                    editid.setText(filtered);
                    editid.setSelection(filtered.length());
                    editid.addTextChangedListener(this);
                }
            }
        });

        if (isStartrip) {
            btn_generate.setVisibility(View.VISIBLE);
        } else {
            msg.setText("Enter the Trip ID to follow:");
            btn_generate.setVisibility(View.GONE);
        }

        btn_generate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editid.setText(generateTripID());
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog.dismiss();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editid.getText().toString().isEmpty()){
                    Toast.makeText(MainActivity.this, "Please enter a trip ID", Toast.LENGTH_SHORT).show();
                }else{
                    Dialog.dismiss();
                    if(isStartrip){
                        Intent intent = new Intent(MainActivity.this, TripLeadActivity.class);
                        intent.putExtra("tripID",editid.getText().toString());
                        startActivity(intent);
                    }else{
                        Intent intent = new Intent(MainActivity.this, FollowTripActivity.class);
                        intent.putExtra("tripID",editid.getText().toString());
                        startActivity(intent);
                    }

                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST) {
            if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[0])) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAppPermission();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        checkAppPermission();
                    } else {
                        showSettingsDialog();
                    }
                }
            }
            return;
        }

        if (requestCode == NOTIFICATION_REQUEST) {
            if (permissions[0].equals(Manifest.permission.POST_NOTIFICATIONS)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAppPermission();
                    return;
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                        checkAppPermission();
                    } else {
                        openNotifictionPersmission();
                    }
                }
            }
        }

        if (requestCode == BACKGROUND_LOCATION_REQUEST) {
            if (Manifest.permission.ACCESS_BACKGROUND_LOCATION.equals(permissions[0])) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (!isLocationEnabled()) {
                        DisplayDialogLocationMessage("Follow Me - " + (isStartrip ? "Cannot StartTrip!" : "Cannot follow!"));
                    }else{
                        GetTripID();
                    }
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        if (!isLocationEnabled()) {
                            DisplayDialogLocationMessage("Follow Me - " + (isStartrip ? "Cannot StartTrip!" : "Cannot follow!"));
                        }else{
                            GetTripID();
                        }
                    } else {
                        showSettingsDialog();
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
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
                Dialog.dismiss();
            }
        });

    }

    private void showSettingsDialog() {
        View customView = getLayoutInflater().inflate(R.layout.dialogbox_error, null);

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setView(customView)
                .setCancelable(false)
                .setBackground(ContextCompat.getDrawable(this, R.drawable.borderdialogbox));

        final androidx.appcompat.app.AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        TextView messageview = customView.findViewById(R.id.message);
        TextView titleview = customView.findViewById(R.id.title);
        Button btn_ok = customView.findViewById(R.id.btn_ok);
        btn_ok.setText("Go To Settings");
        Button btn_cancel = customView.findViewById(R.id.btn_cancel);
        btn_cancel.setVisibility(View.VISIBLE);

        titleview.setText("Follow Me - Permission Required");
        messageview.setText("This app requires location permission. Please enable it in settings.");

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAppSettings();
            }
        });

    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void openNotifictionPersmission(){
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }
        return false;
    }

    private void openLocationSettings() {
        trylocation = true;
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    public static String generateTripID() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 5; i++) {
            int randomIndex = random.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }

        sb.append("-");

        for (int i = 0; i < 5; i++) {
            int randomIndex = random.nextInt(characters.length());
            sb.append(characters.charAt(randomIndex));
        }

        return sb.toString();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(trylocation){
            trylocation = false;
            if(isLocationEnabled()){
                Dialog.dismiss();
                if(spf.contains("tripID") && isStartrip){
                    Intent intent = new Intent(MainActivity.this, TripLeadActivity.class);
                    startActivity(intent);
                }else{
                    GetTripID();
                }
            }
        }
    }
}