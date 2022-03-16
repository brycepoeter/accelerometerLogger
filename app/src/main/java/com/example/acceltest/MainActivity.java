package com.example.acceltest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.acceltest.ui.notifications.LogFragment;
import com.example.acceltest.ui.notifications.NotificationsFragment;
import com.example.acceltest.ui.notifications.NotificationsViewModel;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    // Activity Recognizer
    private final static String TAG = "MainActivity";

    // TODO: Review check for devices with Android 10 (29+).
    private boolean runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    private boolean activityTrackingEnabled;
    public LogFragment mLogFragment;

    private List<ActivityTransition> activityTransitionList;
    NotificationsFragment notificationsFragment;
    NotificationsViewModel notificationsViewModel;

    // Action fired when transitions are triggered.
    private final String TRANSITIONS_RECEIVER_ACTION =
            "APP_ID" + "TRANSITIONS_RECEIVER_ACTION";

    private PendingIntent mActivityTransitionsPendingIntent;
    private TransitionsReceiver mTransitionsReceiver;
    String printToScreenMessage = "Hasn't been set yet";


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

        // Massive amount for activity recognition
        activityTransitionList = new ArrayList<>();
        activityTrackingEnabled = false;

        // TODO: Add activity transitions to track.
//
//        mLogFragment =
//                (LogFragment) getSupportFragmentManager().findFragmentById(R.id.log_fragment);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.navigation_notifications, new NotificationsFragment(), "NOTIFICATIONS_FRAGMENT");
        fragmentTransaction.commit();
        notificationsFragment = (NotificationsFragment) getSupportFragmentManager().findFragmentByTag("NOTIFICATIONS_FRAGMENT");

        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.RUNNING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        mActivityTransitionsPendingIntent =
                PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

        mTransitionsReceiver = new TransitionsReceiver();
        enableActivityTransitions();

    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mTransitionsReceiver, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));
    }

    @Override
    protected void onPause() {
        if (activityTrackingEnabled) {
            disableActivityTransitions();
        }
        super.onPause();
    }


    @Override
    protected void onStop() {
        unregisterReceiver(mTransitionsReceiver);

        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(listener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Start activity recognition if the permission was approved.
        if (activityRecognitionPermissionApproved() && !activityTrackingEnabled) {
            enableActivityTransitions();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void enableActivityTransitions() {

        Log.d(TAG, "enableActivityTransitions()");
        ActivityTransitionRequest request = new ActivityTransitionRequest(activityTransitionList);

        // Register for Transitions Updates.
        Task<Void> task = ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent);

        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        activityTrackingEnabled = true;
                        printToScreen("Transitions Api was successfully registered.");

                    }
                });

        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        printToScreen("Transitions Api could NOT be registered: " + e);
                        Log.e(TAG, "Transitions Api could NOT be registered: " + e);

                    }
                });
    }


    private void disableActivityTransitions() {

        Log.d(TAG, "disableActivityTransitions()");

        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mActivityTransitionsPendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        activityTrackingEnabled = false;
                        printToScreen("Transitions successfully unregistered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        printToScreen("Transitions could not be unregistered: " + e);
                        Log.e(TAG,"Transitions could not be unregistered: " + e);
                    }
                });
    }

    private boolean activityRecognitionPermissionApproved() {

        if (runningQOrLater) {

            return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
            );
        } else {
            return true;
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

    private static String toActivityString(int activity) {
        switch (activity) {
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.RUNNING:
                return "RUNNING";
            default:
                return "UNKNOWN";
        }
    }

    private static String toTransitionType(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "ENTER";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "EXIT";
            default:
                return "UNKNOWN";
        }
    }

    private void printToScreen(@NonNull String message) {
//        LogView logView = mLogFragment.getLogView();
//        logView.print(message);
        printToScreenMessage = message;
        Log.d(TAG, message);
    }

    public String getPrintMessage() {
        return this.printToScreenMessage;
    }

    public class TransitionsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "onReceive(): " + intent);

            if (!TextUtils.equals(TRANSITIONS_RECEIVER_ACTION, intent.getAction())) {

                printToScreen("Received an unsupported action in TransitionsReceiver: action = " +
                        intent.getAction());
                return;
            }

            if (ActivityTransitionResult.hasResult(intent)) {

                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);

                for (ActivityTransitionEvent event : result.getTransitionEvents()) {

                    String info = "Transition: " + toActivityString(event.getActivityType()) +
                            " (" + toTransitionType(event.getTransitionType()) + ")" + "   " +
                            new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());

                    printToScreen(info);
                }
            }
        }
    }


}

