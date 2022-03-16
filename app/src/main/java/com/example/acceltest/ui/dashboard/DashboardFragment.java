package com.example.acceltest.ui.dashboard;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.acceltest.MainActivity;
import com.example.acceltest.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    AudioManager audioManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        // Set up microphone listening
        boolean isSpeechEnabled = SpeechRecognizer.isRecognitionAvailable(this.getContext());
        Log.d("Speech available ", String.valueOf(isSpeechEnabled));
        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this.getContext());
        RecognitionListener recognitionListener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
                Log.d("onReadyForSpeech ", String.valueOf(isSpeechEnabled));
                dashboardViewModel.setText(bundle.toString());
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d("onBeginningOfSpeech ", String.valueOf(isSpeechEnabled));
                dashboardViewModel.setText("onBeginning");
            }

            @Override
            public void onRmsChanged(float v) {
                Log.d("onRmsChanged ", String.valueOf(isSpeechEnabled));
                dashboardViewModel.setText("rmsChanged");
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
                Log.d("onBufferReceived ", String.valueOf(isSpeechEnabled));
            }

            @Override
            public void onEndOfSpeech() {
                Log.d("onEndOfSpeech ", String.valueOf(isSpeechEnabled));
            }

            @Override
            public void onError(int i) {
                Log.d("onError ", String.valueOf(i));
            }

            @Override
            public void onResults(Bundle bundle) {
                Log.d("onResults ", String.valueOf(isSpeechEnabled));
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                dashboardViewModel.setText(data.get(0));
            }

            @Override
            public void onPartialResults(Bundle bundle) {
                Log.d("onPartialResults ", String.valueOf(isSpeechEnabled));
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                dashboardViewModel.setText(data.get(0));
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
                Log.d("onEvent ", String.valueOf(isSpeechEnabled));
                dashboardViewModel.setText(bundle.toString());
            }
        };
        speechRecognizer.setRecognitionListener(recognitionListener);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizer.startListening(intent);

        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        final TextView textView = root.findViewById(R.id.text_dashboard);
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
                callCommand(s);
            }
        });


        return root;
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
        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
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
        Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);

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

        if (ActivityCompat.checkSelfPermission(this.getContext(),
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