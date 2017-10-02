package com.aserrapica.dropalert;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by alexs on 23/06/2016.
 */
public class AlarmActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    final String MY_PREFERENCES = "daPref";

    MediaPlayer mp;
    int trvol = 0;
    boolean ok = true;

    GoogleApiClient mGoogleApiClient;
    String SENT = "SMS_SENT";
    TextView ret;
    TextView sms;
    String smsmsg="";
    TextView mailm;
    SharedPreferences prefs;
    String addr;
    String msg = "Attenzione questo messaggio e' stato generato automaticamente dall'applicazione DropAlert. E' stata rilevata una caduta.";
    String obj = "[DropAlert Service]Caduta Rilevata";
    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    Date date = new Date();
    DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    DbHandler db = new DbHandler(this);
    private BroadcastReceiver smsreceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        prefs = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        addr = prefs.getString("mailaddr", "no");
        ret = (TextView) findViewById(R.id.ret);
        sms = (TextView) findViewById(R.id.numm);
        mailm = (TextView) findViewById(R.id.mailm);

       //Predispongo un receiver per osservare l'esito dell'invio dell'SMS
        smsreceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                sms.setVisibility(View.VISIBLE);

                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        sms.setText("SMS inviato!");
                        break;

                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:

                        sms.setText("Invio SMS fallito. Errore non specificato.");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:

                        sms.setText("Invio SMS fallito. Nessun servizio.");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:

                        sms.setText("Invio SMS fallito. PDU assente.");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:

                        sms.setText("Invio SMS fallito. Nessun servizio.");
                        break;

                }
            }
        };
        registerReceiver(smsreceiver, new IntentFilter(SENT));
        //Preparo l'oggetto per eventuali accessi alla posizione
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        //Salvo l'evento nel DB se non è necessaria la localizzazione
        if ((prefs.getString("mailLocCheck", "no").equals("no") || prefs.getString("mailCheck", "no").equals("no")) && (prefs.getString("numLocCheck", "no").equals("no") || prefs.getString("numCheck", "no").equals("no"))) {
            db.addEvent(new Registro(dateFormat.format(date), timeFormat.format(date)));
            startAlarms();
        }
        else {
            if (prefs.getString("mailLocCheck", "no").equals("yes") || prefs.getString("numLocCheck", "no").equals("yes")) {
                mGoogleApiClient.connect();
            } else
                startAlarms();
        }
    }

    //Interrompe l'allarme sonoro andando a ripristinare i precedenti volumi se necessario
    public void stop(View view) {
        ok = false;
        DropAlertService.setAllarme(false);
        String sound = prefs.getString("soundCheck", "no");
        String forcesound = prefs.getString("forceSoundCheck", "no");
        if (sound.equals("yes")) {
            if (forcesound.equals("yes")) {
                AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, trvol, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
            mp.stop();
        }
        finish();
    }

    //Avvia l'allarme sonoro, controlla se è necessario posticipare l'invio delle notifiche e lancia la funzione per l'invio delle notifiche
    public void startAlarms() {
        String sound = prefs.getString("soundCheck", "no");
        String forcesound = prefs.getString("forceSoundCheck", "no");
        if (sound.equals("yes")) {
            if (forcesound.equals("yes")) {
                AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                int maxVolumeMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                trvol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolumeMusic, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            }
            mp = MediaPlayer.create(this, R.raw.allarme);
            mp.setLooping(true);
            mp.start();
        }
        String retard = prefs.getString("retardCheck", "no");

        if (retard.equals("yes")) {
            ret.setVisibility(View.VISIBLE);

            ret.setText("L'invio di SMS e EMAIL sarà ritardato di 15 sec.");

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (ok)
                        sendEmailSMS();
                }
            }, 15000);

        } else {
            sendEmailSMS();
        }

    }

    //Questo metodo controlla che lo smartphone sia effettivamente online
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    //Questo metodo si occupa di inviare SMS e email
    public void sendEmailSMS() {
        String mail = prefs.getString("mailCheck", "no");
        if (mail.equals("yes")) {
            if (prefs.getString("mailLocCheck", "no").equals("yes") && addr!=null && !addr.equals("") && !addr.equals("no")) {
                sendlocemail();
            } else if(addr!=null && !addr.equals("") && !addr.equals("no")){
                GMailSender gm = new GMailSender();
                gm.sendMail(obj, msg, prefs.getString("gName", "no"), prefs.getString("gToken", "no"), addr);
                mailm.setText("Email inviata.");
                mailm.setVisibility(View.VISIBLE);
            }
        }

        msg = "Attenzione questo messaggio e' stato generato automaticamente dall'applicazione DropAlert. E' stata rilevata una caduta.";
        String num = prefs.getString("numCheck", "no");
        String smsnum= prefs.getString("smsnum", "no");
        if (num.equals("yes")) {
            if (prefs.getString("numLocCheck", "no").equals("yes") && !smsnum.equals("no")) {
                    sendlocsms();
            } else {
                try {
                    if( smsnum!=null && !smsnum.equals("") && !smsnum.equals("no") ){
                        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
                        SmsManager smsManager = SmsManager.getDefault();
                        smsManager.sendTextMessage(smsnum, null, msg, sentPI, null);
                    }

                } catch (Exception ex) {
                    Toast.makeText(getApplicationContext(),
                            ex.getMessage().toString(), Toast.LENGTH_LONG)
                            .show();
                    ex.printStackTrace();
                }
            }
        }
    }

    //Questo metodo gestisce l'invio delle mail o degli sms quando è necessario effettuare la localizzazione. Inoltre salva l'evento nel registro aggiugendo la localizzazione
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("CIAO");

        }
        System.out.println("CIAOCIAO");
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        msg = msg + " Link GMaps alla posizione dell'evento( o ultima posizione nota):  http://maps.google.com/?q=" + String.valueOf(mLastLocation.getLatitude()) + "," + String.valueOf(mLastLocation.getLongitude());
        smsmsg = "DropAlert. Rilevata una caduta. Link GMaps alla posizione dell'evento(o ultima nota): http://maps.google.com/?q=" + String.valueOf(mLastLocation.getLatitude()) + "," + String.valueOf(mLastLocation.getLongitude());
        String link = "http://maps.google.com/?q=" + String.valueOf(mLastLocation.getLatitude()) + "," + String.valueOf(mLastLocation.getLongitude());
        db.addEvent(new Registro(dateFormat.format(date), timeFormat.format(date), link));
        startAlarms();
    }

    public void sendlocemail() {
        if (isOnline()) {
            GMailSender gm = new GMailSender();
            gm.sendMail(obj, msg, prefs.getString("gName", "no"), prefs.getString("gToken", "no"), addr);
            mailm.setVisibility(View.VISIBLE);
            mailm.setText("Email inviata!");

        } else {

            mailm.setText("Impossibile inviare l'email. Connessione assente.");
            mailm.setVisibility(View.VISIBLE);
        }
    }
    public void sendlocsms() {
        try {
            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(prefs.getString("smsnum", "no"), null, smsmsg, sentPI, null);
        } catch (Exception ex) {

        }
    }
    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("CIAO2");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        System.out.println("CIAO3");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsreceiver);
    }
}

