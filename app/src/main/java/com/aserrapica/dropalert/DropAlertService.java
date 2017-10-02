package com.aserrapica.dropalert;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by alexs on 22/06/2016.
 */
public class DropAlertService extends Service implements SensorEventListener {
    private static boolean stato = false;
    private static boolean allarme = false;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private double lastAcc = 0.0f;
    private double acceleration = 0.0f;
    private double soglia = 0.0f;

    static boolean getStato() {
        return stato;
    }

    public static void setStato(boolean s) {
        stato = s;
    }

    public boolean isAllarme() {
        return allarme;
    }

    public static void setAllarme(boolean b) {
        allarme = b;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        setStato(false);

    }

    @Override
    public IBinder onBind(Intent intent) {
        // Used only in case of bound services.
        return null;
    }

    //Metodo richiamato all'avvio del service. Lancia la notifica imposta il service come foreground e registra il listener sull'accelerometro
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setStato(true);
        final String MY_PREFERENCES = "daPref";
        SharedPreferences prefs = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        soglia = prefs.getFloat("calval", (float) (2 * 9.81));
        System.out.println("SOGLIA LETTA: " + soglia);
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent pintent = PendingIntent.getActivity(this, 0, resultIntent, 0);
        NotificationManager nm = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification noti = new NotificationCompat.Builder(this)
                .setContentTitle("Servizio DropAlert")
                .setContentText("Il servizio è attivo e sta monitorando lo smartphone")
                .setSmallIcon(R.drawable.ic_stat_dropalert)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.notico))

                .setContentIntent(pintent)
                .build();

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        startForeground(101, noti);


        return 0;
    }

    //Questo metodo monitora l'accelerometro riportandone i valori aggiornati

    @Override
    public void onSensorChanged(SensorEvent event) {

        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];

        lastAcc = acceleration;
        acceleration = Math.sqrt(x * x + y * y + z * z);
        double diff = acceleration - lastAcc;
        if (diff >= soglia) {
            if (!isAllarme()) {
                setAllarme(true);
                Intent dialogIntent = new Intent(this, AlarmActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(dialogIntent);
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}





