package com.example.followme.API;

import android.util.Log;

import com.example.followme.Model.TripPoint;
import com.example.followme.Model.User;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private final ApiService apiService;

    public UserRepository() {
        apiService = RetrofitClient.getApiService();
    }

    // Callback Interface
    public interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String error);
    }

    public interface TripCallback {
        void onSuccess(boolean exists);
        void onFailure(String error);
    }

    public void verifyUser(String username, String password, AuthCallback callback) {
        User user = new User(username, password);
        apiService.verifyUserCredentials(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResult()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("Invalid credentials");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void createUser(String firstName, String lastName, String username, String password, String email, AuthCallback callback) {
        User newUser = new User(firstName, lastName, username, password, email);
        apiService.createUser(newUser).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onFailure("User creation failed");
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public void checkTripExists(String tripId, TripCallback callback) {
        apiService.tripExists(tripId).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean exists = response.body().trim().equalsIgnoreCase("true");
                    callback.onSuccess(exists);
                } else {
                    callback.onFailure("false");
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public interface GetTripCallback {
        void onSuccess(List<TripPoint> tripPoints);
        void onFailure(String error);
    }

    public void getTrip(String tripId, GetTripCallback callback) {
        apiService.getTrip(tripId).enqueue(new Callback<List<TripPoint>>() {
            @Override
            public void onResponse(Call<List<TripPoint>> call, Response<List<TripPoint>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else if (response.code() == 404) {
                    callback.onFailure("Trip not found");
                } else {
                    callback.onFailure("Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<TripPoint>> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

    public interface AddTripPointCallback {
        void onSuccess(TripPoint tripPoint);
        void onFailure(String error);
    }

    public void addTripPoint(String tripId, double latitude, double longitude, String userName, AddTripPointCallback callback) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String currentDateTime = LocalDateTime.now().format(formatter);

        TripPoint tripPoint = new TripPoint();
        tripPoint.setTripId(tripId);
        tripPoint.setLatitude(latitude);
        tripPoint.setLongitude(longitude);
        tripPoint.setDatetime(currentDateTime);
        tripPoint.setUserName(userName);

        apiService.addTripPoint(tripPoint).enqueue(new Callback<TripPoint>() {
            @Override
            public void onResponse(Call<TripPoint> call, Response<TripPoint> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else if(response.code() == 400) {
                    callback.onFailure("Bad Request: Invalid or missing trip point data.");
                } else {
                    callback.onFailure("Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<TripPoint> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }
    public interface GetLastLocationCallback {
        void onSuccess(TripPoint tripPoint);
        void onFailure(String error);
    }
    public void getLastLocation(String tripId, GetLastLocationCallback callback) {
        apiService.getLastLocation(tripId).enqueue(new Callback<TripPoint>() {
            @Override
            public void onResponse(Call<TripPoint> call, Response<TripPoint> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else if (response.code() == 404) {
                    callback.onFailure("Trip not found");
                } else {
                    callback.onFailure("Error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<TripPoint> call, Throwable t) {
                callback.onFailure(t.getMessage());
            }
        });
    }

}
