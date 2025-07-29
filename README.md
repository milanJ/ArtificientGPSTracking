Artificient GPS Tracking & Data Collection App
==================

An app that collects real-time GPS data. The app simulates trip recording functionality and stores the location history locally. This can be used for scenarios such as delivery tracking in the
Background.

## How to build the app

1. Clone this branch
    ```
    git clone git@github.com:milanJ/BragiMovieDb.git --branch main
    ```
2. Open the cloned repository in Android Studio.
3. Create a `local.properties` file the project's root directory.
4. Add a valid Google Maps API key to the `local.properties` file:
   ```
   MAPS_API_KEY=your_google_maps_api_key
   ```
5. In terminal run `./gradlew assemble` task.
6. After the task finishes running, release and debug APK files will be located in the `/app/build/outputs/apk` directory.

## General architecture

The app is based on MVVM architecture. It was built using Hilt, Coroutines, Material Design and Jetpack Compose.
It is separated into 7 modules: `app`, `core-data`, `core-database`, `core-testing`, `core-ui`, `feature-settings`, `feature-tracking`, `feature-trip-history` and `test-app`.
