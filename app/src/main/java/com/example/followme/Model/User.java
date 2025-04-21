package com.example.followme.Model;


import java.io.Serializable;

public class User implements Serializable {
    private String firstName;
    private String lastName;
    private String userName;
    private String password;
    private String email;
    private boolean result;

    public User(String firstName, String lastName, String userName, String password, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.password = password;
        this.email = email;
    }

    public User(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getUserName() { return userName; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public boolean getResult() { return result; }
}

