package com.example.followme.ViewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.followme.API.UserRepository;
import com.example.followme.Model.User;

public class UserViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();

    public UserViewModel() {
        userRepository = new UserRepository();
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // 🔹 Verify User Login
    public void verifyUser(String userName, String password) {
        isLoading.postValue(true);
        userRepository.verifyUser(userName, password, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.postValue(false);
                userLiveData.postValue(user);
            }

            @Override
            public void onFailure(String error) {
                isLoading.postValue(false);
                errorLiveData.postValue(error);
            }
        });
    }

    public void createUser(String firstName, String lastName, String userName, String password, String email) {
        isLoading.postValue(true);
        userRepository.createUser(firstName, lastName, userName, password, email, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.postValue(false);
                userLiveData.postValue(user);
            }

            @Override
            public void onFailure(String error) {
                isLoading.postValue(false);
                errorLiveData.postValue(error);
            }
        });
    }
}
