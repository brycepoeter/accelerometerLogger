package com.example.acceltest.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.acceltest.R;

import java.util.ArrayList;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;


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
            }
        });


        return root;
    }
}