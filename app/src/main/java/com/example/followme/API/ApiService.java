package com.example.followme.API;


import com.example.followme.Model.TripPoint;
import com.example.followme.Model.User;
import com.example.followme.Model.UserCredentials;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {
    @PUT("UserAccounts/VerifyUserCredentials")
    Call<User> verifyUserCredentials(@Body User user);

    @POST("UserAccounts/CreateUserAccount")
    Call<User> createUser(@Body User user);
    @GET("Datapoints/TripExists/{trip_id}")
    Call<String> tripExists(@Path("trip_id") String tripId);

    @GET("Datapoints/GetTrip/{trip_id}")
    Call<List<TripPoint>> getTrip(@Path("trip_id") String tripId);

    @POST("Datapoints/AddTripPoint")
    Call<TripPoint> addTripPoint(@Body TripPoint tripPoint);

    @GET("Datapoints/GetLastLocation/{trip_id}")
    Call<TripPoint> getLastLocation(@Path("trip_id") String tripId);

}
