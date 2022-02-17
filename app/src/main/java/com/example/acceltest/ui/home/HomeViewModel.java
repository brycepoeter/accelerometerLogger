package com.example.acceltest.ui.home;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.acceltest.MainActivity;

import static androidx.core.content.ContextCompat.getSystemService;

public class HomeViewModel extends ViewModel {

    private MutableLiveData<String> mText;
    private MutableLiveData<Boolean> sending;
    private SensorManager sensorManager;


    public HomeViewModel() {
        sending = new MutableLiveData<>();
//        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void postRun() {

    }



//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (sensorManager != null) {
//            sensorManager.unregisterListener(listener);
//        }
//    }
//    private SensorEventListener listener = new SensorEventListener() {
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            float xValue = Math.abs(event.values[0]);
//            float yValue = Math.abs(event.values[1]);
//            float zValue = Math.abs(event.values[2]);
//            if (xValue > 5 || yValue > 5 || zValue > 5) {
//
//                String xString = new StringBuilder(String.valueOf(xValue)).toString();
//            }
//        }
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        }
//    };

    public LiveData<String> getText() {
        return mText;
    }

    public LiveData<Boolean> getSending() {return sending;}

}