package com.aserrapica.dropalert;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class CalibrationActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private double lastAcc = 0.0f;
    private double acceleration = 0.0f;
    private double diff = 0.0f;
    private double maxdiff = 0.0f;
    private int step = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_calibration);
        TextView txt = (TextView) findViewById(R.id.nonora);
        txt.setPaintFlags(txt.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        //Inizializzo il controllo sull'accelerometro
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void calibration(View view) {
        setContentView(R.layout.activity_calibrating);
        //Attivo il listener
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);

    }

    public void close(View view) {
        finish();
    }

    //Questo metodo gestisce i tre step della calibrazione e va in seguito a salvare la soglia calcolata
    public void step(View view) {
        final String MY_PREFERENCES = "daPref";
        SharedPreferences prefs = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        double calloc = prefs.getFloat("calloc", 0);
        calloc = calloc + maxdiff;
        maxdiff = 0;
        editor.putFloat("calloc", (float) calloc);
        editor.commit();
        TextView tv = (TextView) findViewById(R.id.counter);

        if (step == 3) {

            calloc = prefs.getFloat("calloc", 0);
            double calval = calloc / 3;
            calval = calval - calval / 10;
            editor.putFloat("calval", (float) calval);
            editor.putFloat("calloc", 0);
            editor.commit();
            setContentView(R.layout.activity_calibrationok);
        }
        step++;
        tv.setText(step + "/3");

    }

    //Questo metodo monitora l'accelerometro riportandone i valori aggiornati
    @Override
    public void onSensorChanged(SensorEvent event) {
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];

        lastAcc = acceleration;
        acceleration = Math.sqrt(x * x + y * y + z * z);
        diff = acceleration - lastAcc;
        if (diff > maxdiff) {
            maxdiff = diff;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);


    }
}