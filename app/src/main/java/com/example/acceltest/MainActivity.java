package com.example.acceltest;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private SensorManager sensorManager;
    private SpeechRecognizer speechRecognizer;
    float xValue;
    float yValue;
    float zValue;
    float timestamp;
    FirebaseFirestore db;
    Integer accuracy;
    Boolean postingActivated;
    Boolean capturingActivated;
    List<Map<String, Float>> dataToPost;
    String dataLabel;
    long capturingStartTime;
    long capturingEndTime;

    float speed;
    float vx = 0;
    float vy = 0;
    float vz = 0;
    boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set initial values
        postingActivated = false;
        capturingActivated = false;
        dataToPost = new ArrayList<>();

        // Set up accelerometer sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Set up Cloud Firestore
        db = FirebaseFirestore.getInstance();


        // Set up nav
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        final NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(listener);
        }
    }

    public void startPosting() {
        postingActivated = true;
    }

    public void stopPosting() {
        postingActivated = false;
        dataToPost = new ArrayList<>();
    }

    public void startCapturing(String label) {
        capturingStartTime = System.currentTimeMillis();
        dataLabel = label;
        capturingActivated = true;
    }

    public void stopCapturing() {
        capturingEndTime = System.currentTimeMillis();
        capturingActivated = false;
        startPosting();
        postData(dataLabel);
        stopPosting();
    }

    public void postData(String collection) {
        if (postingActivated) {

            // Save capture end time
            Map<String, String> endTime = new HashMap<>();
            endTime.put("endTime", String.valueOf(capturingEndTime));
            db.collection(collection)
                    .document(String.valueOf(capturingStartTime))
                    .collection("endTime")
                    .add(endTime)
                    .addOnSuccessListener(documentReference -> Log.d("ADDED END TIME", documentReference.getId()))
                    .addOnFailureListener(e -> Log.d("FAILED ADD END TIME", e.getMessage()));

            // Save each accelerometer reading under capture start time
            dataToPost.forEach((input) -> db.collection(collection)
                .document(String.valueOf(capturingStartTime))
                .collection("data")
                .add(input)
                    .addOnSuccessListener(documentReference -> Log.d("ADDED DATA", documentReference.getId()))
                    .addOnFailureListener(e -> Log.d("FAILED ADD DATA", e.getMessage())));
        }
    }

    private void captureData() {
        if (capturingActivated) {
            if (xValue > 10 || yValue > 10 || zValue > 10) {
                Map<String, Float> input = new HashMap<>();
                input.put("time", timestamp);
                input.put("accuracy", Float.valueOf(accuracy));
                input.put("x", xValue);
                input.put("y", yValue);
                input.put("z", zValue);
                dataToPost.add(input);
                Log.d("DATA_POST_LENGTH:", String.valueOf(dataToPost.size()));
            }
        }
    }

    private void updateSpeed(SensorEvent event) {
        float dT = (event.timestamp - timestamp) / 1000000;
        vx += xValue * dT;
        vy += yValue * dT;
        vz += yValue * dT;
        speed = (float) Math.sqrt(vx*vx + vy*vy + vz*vz);
        if(speed > 2.2) {
            running = true;
        }
        else {
            running  = false;
        }
    }

    private SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            xValue = Math.abs(event.values[0]);
            yValue = Math.abs(event.values[1]);
            zValue = Math.abs(event.values[2]);
            accuracy = event.accuracy;
            timestamp = event.timestamp;
            if (capturingActivated) {
                captureData();
            }
            updateSpeed(event);
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int passedAccuracy) {
            accuracy = passedAccuracy;
        }
    };

}

