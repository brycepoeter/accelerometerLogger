package com.example.acceltest.ui.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.acceltest.MainActivity;
import com.example.acceltest.R;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private Button recordRun;
    private Button recordOther;
    private Button stopRecord;
    AlertDialog.Builder adBuilder;
    AlertDialog alertDialog;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        adBuilder = new AlertDialog.Builder(this.getContext());
        alertDialog = adBuilder.create();

        recordRun = root.findViewById(R.id.recordRun);
        recordOther = root.findViewById(R.id.recordOther);
        stopRecord = root.findViewById(R.id.stopRecord);

        recordRun.setOnClickListener(view -> {
            alertDialog.setTitle("Recording Running Data");
            Log.d("RECORDING", "running");
            alertDialog.show();
            ((MainActivity) getActivity()).startCapturing("run");
        });

        recordOther.setOnClickListener(view -> {
            alertDialog.setTitle("Recording Non Running Data");
            Log.d("RECORDING", "other");
            alertDialog.show();
            ((MainActivity) getActivity()).startCapturing("other");
        });

        stopRecord.setOnClickListener(view -> {
            ((MainActivity) getActivity()).stopCapturing();
            alertDialog.setTitle("Stopped Recording Data");
            alertDialog.show();
        });

        return root;
    }
}