package com.example.acceltest;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private SpeechRecognizer speechRecognizer;
    boolean listening = false;
    AudioManager audioManager;
    TextView activity;
    TextView command;

    // Activity Recognizer
    private final static String TAG = "MainActivity";

    // TODO: Review check for devices with Android 10 (29+).
    private boolean runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    private boolean activityTrackingEnabled;

    private List<ActivityTransition> activityTransitionList;

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

        // Massive amount for activity recognition
        activityTransitionList = new ArrayList<>();
        activityTrackingEnabled = false;

        activity = (TextView) findViewById(R.id.activity);
        command = (TextView) findViewById(R.id.command);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
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


    private static String toActivityString(int activity) {
        switch (activity) {
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.WALKING:
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

                    if (event.getActivityType() == DetectedActivity.WALKING) {
                        activity.setText(R.string.running);


                        RecognitionListener recognitionListener = new RecognitionListener() {
                            @Override
                            public void onReadyForSpeech(Bundle bundle) {

                            }

                            @Override
                            public void onBeginningOfSpeech() {

                            }

                            @Override
                            public void onRmsChanged(float v) {

                            }

                            @Override
                            public void onBufferReceived(byte[] bytes) {

                            }

                            @Override
                            public void onEndOfSpeech() {

                            }

                            @Override
                            public void onError(int i) {

                            }

                            @Override
                            public void onResults(Bundle bundle) {
                                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                                command.setText(data.get(0));
                                callCommand(data.get(0));
                            }

                            @Override
                            public void onPartialResults(Bundle bundle) {
                            }

                            @Override
                            public void onEvent(int i, Bundle bundle) {
                            }
                        };

                        speechRecognizer.setRecognitionListener(recognitionListener);
                        Intent listenIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        speechRecognizer.startListening(listenIntent);
                        listening = true;
                    }
                    else {
                        activity.setText(R.string.still);
                        if(listening) {
                            speechRecognizer.stopListening();
                            listening = false;
                        }
                    }
                }
            }
        }
    }

    // Checks that the input string is a valid number 0-10
    private boolean isValidNumber(String input){
        HashMap<String, String> nums = new HashMap<String, String>();
        nums.put("zero", "0");
        nums.put("one", "1");
        nums.put("two", "2");
        nums.put("three", "3");
        nums.put("four", "4");
        nums.put("five", "5");
        nums.put("six", "6");
        nums.put("seven", "7");
        nums.put("eight", "8");
        nums.put("nine", "9");
        nums.put("ten", "10");

        if(nums.containsKey(input)) {
            input = nums.get(input);
        }
        try {
            int num = Integer.parseInt(input);
            if (num >= 0 && num <= 10) {
                return true;
            }
            return false;
        }
        catch(Exception e) {
            return false;
        }

    }

    // Checks the format of a command intended to change the volume
    private boolean checkVolumeCommand(String command) {
        String[] components = command.split(" ");
        // Check the format "Volume X"
        // X can only be a number 0-10.
        // "Volume X" will set the volume of the phone to x/10 % of the max volume
        if(components.length != 2) {
            return false;
        }
        if(!components[0].toLowerCase().equals("volume") || !isValidNumber(components[1])) {
            return false;
        }
        return true;
    }

    private double parseVolumeCommand(String command) {
        HashMap<String, Double> nums = new HashMap<String, Double>();
        nums.put("zero", 0.0);
        nums.put("one", .1);
        nums.put("two", .2);
        nums.put("three", .3);
        nums.put("four", .4);
        nums.put("five", .5);
        nums.put("six", .6);
        nums.put("seven", .7);
        nums.put("eight", .8);
        nums.put("nine", .9);
        nums.put("ten", 1.0);

        String strNum = command.split(" ")[1];
        if (nums.containsKey(strNum)) {
            return nums.get(strNum);
        }
        return Integer.parseInt(strNum) / 10.0;
    }

    private void runVolumeCommand(String command) {
        System.out.println("run volume command");
        audioManager = (AudioManager) MainActivity.this.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        double percent = parseVolumeCommand(command);
        int newVolume = (int) (maxVolume * percent);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
    }

    private boolean checkCallCommand(String command) {
        String[] components = command.split(" ");
        if(!components[0].toLowerCase().equals("call")) {
            return false;
        }
        // Check if person is in contacts
        return true;
    }

    private void runCallCommand(String command) {
        String[] parts = command.split(" ");
        String target_name = parts[1];
        System.out.println(target_name);
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        String[] projection = new String[] {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER};
        Cursor cursor = MainActivity.this.getContentResolver().query(uri, projection, null, null, null);

        int idxName = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int idxNumber = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

        String contactName = "";
        String contactNumber = "";

        if(cursor.moveToFirst()) {
            do {
                contactName = cursor.getString(idxName);
                contactNumber = cursor.getString(idxNumber);

                if (contactName.equals(target_name)){
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        System.out.println(contactName);
        System.out.println(contactNumber);
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        String uriString = "tel:";
        if (contactNumber.length() == 0) {
            return;
        }

        uriString = uriString.concat(contactNumber);
        System.out.println(uriString);
        callIntent.setData(Uri.parse(uriString));

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(callIntent);
    }


    private void callCommand(String command) {
        if(checkVolumeCommand(command)) {
            runVolumeCommand(command);
        }
        else if(checkCallCommand(command)) {
            runCallCommand(command);
        }
    }




}

