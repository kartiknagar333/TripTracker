package com.example.followme.Util;


import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.TransportInfo;

import androidx.annotation.NonNull;

import com.example.followme.activity.FollowTripActivity;
import com.example.followme.activity.TripLeadActivity;

public class NetWorkCheckingOntime {

    public static boolean lastCheckHadNetwork = true;

    public static void getNetwork(Activity activity, boolean flag) {
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        connectivityManager.registerNetworkCallback(
                builder.build(),
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        if(flag) {
                            if (activity instanceof TripLeadActivity) {
                                activity.runOnUiThread(() -> ((TripLeadActivity) activity).getNetworkChange(true));
                            }
                        }else{
                            if (activity instanceof FollowTripActivity) {
                                activity.runOnUiThread(() -> ((FollowTripActivity) activity).getNetworkChange(true));
                            }
                        }

                    }
                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        if(flag) {
                            if (activity instanceof TripLeadActivity) {
                                activity.runOnUiThread(() -> ((TripLeadActivity) activity).getNetworkChange(false));
                            }
                        }else{
                            if (activity instanceof FollowTripActivity) {
                                activity.runOnUiThread(() -> ((FollowTripActivity) activity).getNetworkChange(false));
                            }
                        }
                    }
                }
        );
    }

}
