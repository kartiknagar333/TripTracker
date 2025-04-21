# TripTracker (Follow Me)

A real-time trip sharing Android application that allows Trip Leaders to share their journey and Trip Followers to track it live on a map.

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Endpoints](#api-endpoints)
- [Project Structure](#project-structure)
- [Download APK for Testing](#download-apk-for-testing)
- [Screenshots](#screenshots)


## Features
- **User Authentication**: Register and log in via Follow Me API.
- **Trip Management**: Create or auto-generate a unique Trip ID.
- **Real-Time Tracking**: Share location updates in the background.
- **Map Visualization**: Display trip path as a polyline and current location with a car icon oriented by bearing.
- **Trip Details**: Show total distance traveled, elapsed time, and start time.
- **Pause/Resume Sharing**: Temporarily pause location updates without ending the trip.
- **Post-Trip Access**: Retrieve and review trip data after completion.
- **MVVM Architecture**: Leverage ViewModel and LiveData for UI and data handling.

## Tech Stack
- **Language**: Java
- **Android SDK**: minSdk 29, targetSdk 35
- **UI**: AndroidX AppCompat, Material Components, ConstraintLayout
- **Networking**: Retrofit 2, Gson Converter
- **Mapping & Location**: Google Play Services Maps & Location
- **Architecture**: Android Architecture Components (ViewModel, LiveData)
- **Build System**: Gradle Kotlin DSL

## Prerequisites
- Android Studio Flamingo (or newer)
- Android device or emulator with Google Play services
- Google Maps API Key
- Internet connectivity for API calls

## Installation
1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/TripTracker.git
   cd TripTracker
   ```
2. **Open in Android Studio**:
   - Select **Open an existing project** and navigate to the cloned folder.
3. **Sync and Build**:
   - Click **Sync Project** with Gradle files.
   - Build the project (Build → Make Project).

## Configuration
1. **API Base URL**:
   - Open `RetrofitClient.java` (in `app/src/main/java/com/example/followme/API/`).
   - Replace `private static final String BASE_URL = "http://....com/api/";` with Your Base URL
     ```java
     private static final String BASE_URL = "http://...com/api/";
     ```
2. **Google Maps API Key**:
   - In `app/google_maps_api.xml`, set your `MAPS_API_KEY`.
   - Or add to `local.properties`:
     ```properties
     MAPS_API_KEY=YOUR_KEY_HERE
     ```

## Usage
1. **Launch the App** on your device/emulator.
2. **Register or Log In** as a Trip Leader.
3. **Start a Trip**:
   - Enter or generate a Trip ID.
   - Begin moving to send location updates.
4. **Share Trip ID** with followers via Share Intent.
5. **Follow a Trip**:
   - In the main screen, tap **Follow Trip** and enter the shared Trip ID.
   - Watch the Trip Leader’s path update live.
6. **End/Review Trip**:
   - End sharing from Trip Leader screen.
   - Followers can still view completed trip path.

## API Endpoints
Base URL: `http://...com/api/`

| Endpoint                                    | Method | Description                                           |
|---------------------------------------------|--------|-------------------------------------------------------|
| `/UserAccounts/VerifyUserCredentials`       | PUT    | Verify login credentials                             |
| `/UserAccounts/CreateUserAccount`           | POST   | Register a new user                                  |
| `/Datapoints/TripExists/{trip_id}`          | GET    | Check if Trip ID exists                              |
| `/Datapoints/GetTrip/{trip_id}`             | GET    | Retrieve all trip points                             |
| `/Datapoints/GetLastLocation/{trip_id}`     | GET    | Retrieve the most recent trip point                  |
| `/Datapoints/AddTripPoint`                  | POST   | Submit a new trip point (latitude, longitude, time)  |

## Project Structure
```
TripTracker/
├── app/
│   ├── src/main/java/com/example/followme/
│   │   ├── API/                # Retrofit client, service interface, repository
│   │   ├── Model/              # Data models (User, TripPoint)
│   │   ├── ViewModel/          # Android ViewModel classes
│   │   ├── ui/                 # Activities: Main, TripLead, TripFollower
│   └── src/main/res/           # Layouts, drawables, values
├── build.gradle.kts
└── gradle/
```

## Download APK for Testing
You can download the APK file for testing from the release below:
- [Download v1.0 Testing APK](https://github.com/kartiknagar333/TripTracker/releases/tag/v1.0)
<br>  

## Screenshots
- **MainActivity**
<div align="center">
  <img src="https://github.com/user-attachments/assets/974461d2-14a6-4a22-8377-f301184a583b" alt="Home" width="336" height="748"/>
</div>
<br>

- **Login - Registration Dialog**
<div align="center">
  <img src="https://github.com/user-attachments/assets/66330c74-c8e3-4523-b358-8032632d5979" alt="Home" width="336" height="748" hspace = "10px"/>
  <img src="https://github.com/user-attachments/assets/2e70e60b-de75-47da-8a8a-f343b051431e" alt="Home" width="336" height="748"/>
</div>
<br>

- **StartTrip**
<div align="center">
  <img src="https://github.com/user-attachments/assets/64761347-98e6-4210-bb0c-c99d697ef87a" alt="Home" width="336" height="748" hspace = "10px"/>
   <img src="https://github.com/user-attachments/assets/42cde04d-7b22-4646-9d26-07b2808470be" alt="Home" width="336" height="748"/>
</div>
<br>

- **FollowTrip**
<div align="center">
  <img src="https://github.com/user-attachments/assets/e800d7f7-959a-4936-b071-9362f5b5afc8" alt="Home" width="336" height="748" hspace = "10px"/>
  <img src="https://github.com/user-attachments/assets/bd823621-52d1-405e-b0d2-e5936b1fa599" alt="Home" width="336" height="748" >
</div>
<br>

