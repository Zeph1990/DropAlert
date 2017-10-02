package com.aserrapica.dropalert;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends Activity {
    private static final int SMS_PERM = 1111;
    private static final int LOC_PERM = 11111;
    private static final int GPICK = 110011;
    private static final int CAL = 101010;
    private static final int DIALOG_ERROR_ID = 111;

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Creo i due tab principali
        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();
        TabHost.TabSpec spec1 = tabHost.newTabSpec("Impostazioni");
        spec1.setContent(R.id.tab1);
        spec1.setIndicator("Impostazioni");
        TabHost.TabSpec spec2 = tabHost.newTabSpec("Registro Eventi");
        spec2.setContent(R.id.tab2);
        spec2.setIndicator("Registro Eventi");
        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        //Creo registro degli eventi
        updatelist(null);
        //Leggo le preferenze dalla memoria e aggiorno le preferenze
        getPref();
    }

    //Controlla il numero per l'invio dell'sms e se si tratta di un numero valido lo salva
    public void savenum(View view) {
        final EditText ednumber = (EditText) findViewById(R.id.smsnum);
        if (ednumber.getText() != null && !ednumber.getText().toString().trim().equals("")) {
            if (PhoneNumberUtils.isGlobalPhoneNumber(ednumber.getText().toString())) {
                updatePref(ednumber);
                Toast.makeText(this, "Numero salvato correttamente!", Toast.LENGTH_LONG).show();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                ednumber.clearFocus();
            } else
                Toast.makeText(this, "Numero non corretto!", Toast.LENGTH_LONG).show();

        } else {
            updatePref(ednumber);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            ednumber.clearFocus();
            Toast.makeText(this, "Numero eliminato!", Toast.LENGTH_LONG).show();
            CheckBox ck = (CheckBox) findViewById(R.id.checkNum);
            CheckBox ck2 = (CheckBox) findViewById(R.id.checkNumLoc);
            ck2.setChecked(false);
            ck.setChecked(false);
            showHide(ck);
        }

    }

    //Controlla l'indirizzo email inserito per l'invio delle notifiche mail e se corretto lo salva
    public void savemail(View view) {
        final EditText edmail = (EditText) findViewById(R.id.mailaddr);
        if (edmail.getText() != null && !edmail.getText().toString().trim().equals("")) {
            if (isValidEmail(edmail.getText().toString())) {
                updatePref(edmail);
                Toast.makeText(this, "Email salvata correttamente!", Toast.LENGTH_LONG).show();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                edmail.clearFocus();
            } else
                Toast.makeText(this, "Email non corretta!", Toast.LENGTH_LONG).show();
        } else {
            updatePref(edmail);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            edmail.clearFocus();
            Toast.makeText(this, "Email eliminata!", Toast.LENGTH_LONG).show();
            CheckBox ck = (CheckBox) findViewById(R.id.checkMail);
            CheckBox ck2 = (CheckBox) findViewById(R.id.checkMailLoc);
            ck2.setChecked(false);
            ck.setChecked(false);
            showHide(ck);
        }

    }

    //Aggiorna il registro degli eventi interrogando il db
    public void updatelist(@Nullable View view) {
        ListView lv = (ListView) findViewById(R.id.listView1);
        lv.removeAllViewsInLayout();
        DbHandler db = new DbHandler(this);
        ArrayList<Registro> r = (ArrayList<Registro>) db.getAllEvents();
        ArrayList<HashMap<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < r.size(); i++) {
            Registro p = r.get(i);
            HashMap<String, Object> regMap = new HashMap<>();
            regMap.put("data", p.getData());
            regMap.put("ora", p.getTime());
            if (p.getLinkloc() != null && !p.getLinkloc().equals(""))
                regMap.put("loc", p.getLinkloc());
            else
                regMap.put("loc", "Non Disponibile");

            data.add(regMap);
        }
        String[] from = {"data", "ora", "loc"};
        int[] to = {R.id.datatext, R.id.oratext, R.id.loclinktext};
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                data,
                R.layout.listelement,
                from,
                to);


        ((ListView) findViewById(R.id.listView1)).setAdapter(adapter);
        db.close();
    }

    //Creazione e gestione della AlertDialog
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog dialog;
        switch (id) {
            case DIALOG_ERROR_ID:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Avviso");
                builder.setMessage("Connessione ad internet assente. E' necessario essere connessi per configurare le notifiche mail.");
                builder.setCancelable(true);
                builder.setNegativeButton("OK", null);
                dialog = builder.create();
                break;

            default:
                dialog = null;
        }
        return dialog;
    }

    //Metodo per il controllo della presenza dei Google Play Services
    public boolean isGooglePlayServicesAvailable(Activity activity) {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(activity);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 2404).show();
            }
            return false;
        }
        return true;
    }

    //Metodo per mostrare o nascondere delle view collegate a diverse funzionalità
    public void showHide(View view) {
        RelativeLayout lay = null;
        final String MY_PREFERENCES = "daPref";
        SharedPreferences prefs = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        boolean flag = false;
        switch (view.getId()) {
            case R.id.checkMail:
                lay = (RelativeLayout) findViewById(R.id.lemail);
                String token = prefs.getString("gToken", "no");
                flag = true;
                if (((CheckBox) view).isChecked() && token.equals("no")) {
                    if (isOnline() && isGooglePlayServicesAvailable(this)) {
                        Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null,
                                new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, "Seleziona l'account dal quale inviare le notifiche email", null, null, null);
                        startActivityForResult(googlePicker, GPICK);

                    } else {
                        ((CheckBox) view).setChecked(false);
                        showDialog(DIALOG_ERROR_ID);
                        return;
                    }
                }
                break;

            case R.id.checkNum:
                lay = (RelativeLayout) findViewById(R.id.lnum);
                if (((CheckBox) view).isChecked()) {

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.SEND_SMS},
                                SMS_PERM);
                    }
                }
                break;
            case R.id.checkSound:
                lay = (RelativeLayout) findViewById(R.id.lsound);
                break;
        }
        TextView tv = (TextView) findViewById(R.id.tvmail);
        View v = findViewById(R.id.ltvmail);
        if (!((CheckBox) view).isChecked()) {
            lay.setVisibility(View.GONE);
            if (flag) {
                tv.setVisibility(View.GONE);
                v.setVisibility(View.GONE);
            }

        } else {

            lay.setVisibility(View.VISIBLE);

            if (flag) {
                tv.setVisibility(View.VISIBLE);
                v.setVisibility(View.VISIBLE);
            }
        }

    }

    //Quando viene cliccata una checkbox o lo switch principale questa funzione si occupa di aggiornare le SharedPreferences
    //e di richiamare altre funzioni di supporto.
    public void updatePref(View view) {
        final String MY_PREFERENCES = "daPref";
        SharedPreferences prefs = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String typecheck = "";
        switch (view.getId()) {
            case R.id.checkSound:
                typecheck = "soundCheck";
                showHide(view);
                break;
            case R.id.checkMailLoc:
                typecheck = "mailLocCheck";
                break;
            case R.id.checkNumLoc:
                typecheck = "numLocCheck";
                break;
            case R.id.checkAuto:
                typecheck = "autoCheck";
                break;
            case R.id.checkMail:
                typecheck = "mailCheck";
                showHide(view);
                break;
            case R.id.checkNum:
                typecheck = "numCheck";
                showHide(view);
                break;
            case R.id.checkForceSound:
                typecheck = "forceSoundCheck";
                break;
            case R.id.checkRetard:
                typecheck = "retardCheck";
                break;
            case R.id.switch1:
                typecheck = "dasCheck";
                String check = prefs.getString("cal", "no");
                if (check.equals("no")) {
                    launchCalibration(view);
                    return;
                } else {
                    daswitch();
                }

                break;
            case R.id.mailaddr:
                if (((EditText) view).getText() == null || ((EditText) view).getText().toString().trim().equals("")) {
                    editor.putString("mailaddr", "no");
                    editor.putString("mailCheck", "no");
                    editor.putString("mailLocCheck", "no");
                    editor.commit();
                } else {
                    editor.putString("mailaddr", ((EditText) view).getText().toString().toLowerCase().trim());
                    editor.putString("mailCheck", "yes");
                    editor.commit();
                }
                break;
            case R.id.smsnum:
                if (((EditText) view).getText() == null || ((EditText) view).getText().toString().trim().equals("")) {
                    editor.putString("smsnum", "no");
                    editor.putString("numCheck", "no");
                    editor.putString("mailNumCheck", "no");
                    editor.commit();
                } else {
                    editor.putString("smsnum", ((EditText) view).getText().toString().toLowerCase().trim());
                    editor.putString("numCheck", "yes");
                    editor.commit();
                }
                break;

        }


        if (typecheck.equals("dasCheck") && prefs.getString("cal", "no").equals("yes")) {
            Intent daservice = new Intent(getBaseContext(), DropAlertService.class);

            if (((Switch) view).isChecked()) {
                editor.putString(typecheck, "yes");
                if (!DropAlertService.getStato()) {
                    startService(daservice);
                    Toast.makeText(this, "DropAlert Service avviato!", Toast.LENGTH_LONG).show();
                }
            } else {
                editor.putString(typecheck, "no");
                if (DropAlertService.getStato()) {
                    stopService(daservice);
                    Toast.makeText(this, "DropAlert Service interrotto!", Toast.LENGTH_LONG).show();
                }
            }
        } else if (!typecheck.equals("") && !typecheck.equals("dasCheck")) {
            if (((CheckBox) view).isChecked()) {

                editor.putString(typecheck, "yes");
            } else
                editor.putString(typecheck, "no");
        }

        editor.commit();

        if (typecheck != null && typecheck.equals("numLocCheck") || typecheck.equals("mailLocCheck")) {
            if (((CheckBox) view).isChecked()) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            LOC_PERM);
                }
            }
        }
    }

    //Gestisce la risposta dell'utente alla rcihiesta dei permessi
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        final String MY_PREFERENCES = "daPref";
        SharedPreferences prefs = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        switch (requestCode) {
            case SMS_PERM:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    editor.putString("smsperm", "yes");
                    editor.commit();
                } else {
                    CheckBox ck = (CheckBox) findViewById(R.id.checkNum);
                    ck.setChecked(false);
                    RelativeLayout ly = (RelativeLayout) findViewById(R.id.lnum);
                    ly.setVisibility(View.GONE);
                    editor.putString("numCheck", "no");
                    editor.commit();
                }
                break;
            case LOC_PERM:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    editor.putString("locperm", "yes");
                    editor.commit();
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                LOC_PERM);
                    }

                } else {
                    CheckBox ck = (CheckBox) findViewById(R.id.checkMailLoc);
                    CheckBox ck2 = (CheckBox) findViewById(R.id.checkNumLoc);
                    ck.setChecked(false);
                    ck2.setChecked(false);
                    editor.putString("numLocCheck", "no");
                    editor.putString("numMailCheck", "no");
                    editor.commit();
                }
                break;

        }
    }

    //Metodo per avviare la procedura di calibrazione dello smartphone
    public void launchCalibration(View view) {
        Switch sw = (Switch) findViewById(R.id.switch1);
        sw.setChecked(false);
        Intent daservice = new Intent(getBaseContext(), DropAlertService.class);
        stopService(daservice);
        Toast.makeText(this, "DropAlert Service interrotto!", Toast.LENGTH_LONG).show();
        Intent i = new Intent(this, CalibrationActivity.class);
        startActivityForResult(i, CAL);
    }

    //Metodo per cancellare il registro degli eventi
    public void deletelist(View view) {
        deleteDatabase("daRegistro");
        updatelist(null);
    }

    //Questo metodo gestisce il ritorno dalle activity di accesso all'account Google e di calibrazione
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        final String MY_PREFERENCES = "daPref";
        SharedPreferences prefs = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();
        if (requestCode == GPICK && resultCode == RESULT_OK) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            editor.putString("gName", accountName);
            editor.commit();
            String accountType = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
            final Account acc = new Account(accountName, accountType);

            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    String token = "no";
                    try {
                        token = GoogleAuthUtil.getToken(MainActivity.this, acc, "oauth2:https://mail.google.com/");

                    } catch (IOException transientEx) {
                        // Network or server error, try later
                        Log.e("ERRORE", transientEx.toString());
                    } catch (UserRecoverableAuthException e) {
                        // Recover (with e.getIntent())
                        Log.e("ERRORE", e.toString());
                        Intent recover = e.getIntent();
                        startActivityForResult(recover, 110011);
                    } catch (GoogleAuthException authEx) {
                        // The call is not ever expected to succeed
                        // assuming you have already verified that
                        // Google Play services is installed.
                        Log.e("ERROE", authEx.toString());
                    }
                    editor.putString("gToken", token);
                    editor.commit();
                    return null;
                }


            };

            task.execute();

        } else if (requestCode == GPICK && resultCode == RESULT_CANCELED) {
            CheckBox ck = (CheckBox) findViewById(R.id.checkMail);
            ck.setChecked(false);
            editor.putString("mailCheck", "no");
            editor.commit();
            RelativeLayout ly = (RelativeLayout) findViewById(R.id.lemail);
            ly.setVisibility(View.GONE);
        }
        if (requestCode == CAL) {
            if (prefs.getString("cal", "no").equals("no"))
                daswitch();

            editor.putString("cal", "yes");
            editor.putString("dasCheck", "yes");
            editor.commit();
            Switch sw = (Switch) findViewById(R.id.switch1);
            sw.setChecked(true);

            Intent daservice = new Intent(getBaseContext(), DropAlertService.class);
            startService(daservice);
        }
    }

    //Questo metodo controlla se lo smartphone è online
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    //Questo metodo recupera tutte le informazioni dalle SharedPreferences e reimposta le view in base alle scelte fatte dall'utente
    public void getPref() {
        final String MY_PREFERENCES = "daPref";
        SharedPreferences prefs = getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Switch sw = (Switch) findViewById(R.id.switch1);
        String textData = prefs.getString("dasCheck", "no");
        Intent daservice = new Intent(getBaseContext(), DropAlertService.class);

        if (textData.equals("no")) {
            sw.setChecked(false);
            if (DropAlertService.getStato()) {
                stopService(daservice);
                Toast.makeText(this, "DropAlert Service interrotto!", Toast.LENGTH_LONG).show();
            }
            daswitch();
        } else {
            if (!DropAlertService.getStato()) {
                startService(daservice);
                Toast.makeText(this, "DropAlert Service avviato!", Toast.LENGTH_LONG).show();
            }
        }

        CheckBox ck = (CheckBox) findViewById(R.id.checkSound);
        textData = prefs.getString("soundCheck", "no");
        if (textData.equals("yes"))
            ck.setChecked(true);
        else
            ck.setChecked(false);
        showHide(ck);

        ck = (CheckBox) findViewById(R.id.checkForceSound);
        textData = prefs.getString("forceSoundCheck", "no");
        if (textData.equals("yes"))
            ck.setChecked(true);
        else
            ck.setChecked(false);

        EditText ed = (EditText) findViewById(R.id.mailaddr);
        textData = prefs.getString("mailaddr", "no");
        if (!textData.equals("no"))
            ed.setText(textData);
        else {
            editor.putString("mailCheck", "no");
            editor.commit();
        }

        ck = (CheckBox) findViewById(R.id.checkMail);
        textData = prefs.getString("mailCheck", "no");
        if (textData.equals("yes"))
            ck.setChecked(true);
        else
            ck.setChecked(false);

        showHide(ck);

        ck = (CheckBox) findViewById(R.id.checkMailLoc);
        textData = prefs.getString("mailLocCheck", "no");
        if (textData.equals("yes"))
            ck.setChecked(true);
        else
            ck.setChecked(false);

        ed = (EditText) findViewById(R.id.smsnum);
        textData = prefs.getString("smsnum", "no");
        if (!textData.equals("no"))
            ed.setText(textData);
        else {
            editor.putString("numCheck", "no");
            editor.commit();
        }
        ck = (CheckBox) findViewById(R.id.checkNum);
        textData = prefs.getString("numCheck", "no");
        if (textData.equals("yes"))
            ck.setChecked(true);
        else
            ck.setChecked(false);
        showHide(ck);

        ck = (CheckBox) findViewById(R.id.checkNumLoc);
        textData = prefs.getString("numLocCheck", "no");
        if (textData.equals("yes"))
            ck.setChecked(true);
        else
            ck.setChecked(false);

        ck = (CheckBox) findViewById(R.id.checkAuto);
        textData = prefs.getString("autoCheck", "no");
        if (textData.equals("yes"))
            ck.setChecked(true);
        else
            ck.setChecked(false);

        ck = (CheckBox) findViewById(R.id.checkRetard);
        textData = prefs.getString("retardCheck", "no");
        if (textData.equals("yes"))
            ck.setChecked(true);
        else
            ck.setChecked(false);

    }

    //Questo metodo si occupa di abilitare/disabilitare tutte le view dipendenti dal servizio DropAlert
    public void daswitch() {

        RelativeLayout layout = (RelativeLayout) findViewById(R.id.ly);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child.getClass() == layout.getClass()) {
                for (int k = 0; k < ((RelativeLayout) child).getChildCount(); k++) {
                    View childchild = ((RelativeLayout) child).getChildAt(k);
                    if (childchild.isEnabled()) {
                        childchild.setEnabled(false);
                        if (childchild.getId() == R.id.tvcal) {
                            ((TextView) childchild).setTextColor(getResources().getColor(android.R.color.darker_gray));
                        }
                    } else {
                        childchild.setEnabled(true);
                        if (childchild.getId() == R.id.tvcal) {
                            ((TextView) childchild).setTextColor(Color.BLACK);
                        }
                    }
                }
            } else {
                if (child.isEnabled()) {
                    child.setEnabled(false);
                    if (child.getId() == R.id.tvcal) {
                        ((TextView) child).setTextColor(getResources().getColor(android.R.color.darker_gray));
                    }
                    if (child.getId() == R.id.tvmail) {
                        ((TextView) child).setTextColor(getResources().getColor(android.R.color.darker_gray));
                    }
                } else {
                    child.setEnabled(true);
                    if (child.getId() == R.id.tvcal) {
                        ((TextView) child).setTextColor(Color.BLACK);
                    }
                    if (child.getId() == R.id.tvmail) {
                        ((TextView) child).setTextColor(Color.BLACK);
                    }
                }
            }
        }
    }

    //Questo metodo permette all'utente di scegliere un nuovo account Google dal quale inviare le mail
    public void changeAccount(View view) {
        if (isOnline() && isGooglePlayServicesAvailable(this)) {
            Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null,
                    new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false, "Seleziona l'account dal quale inviare le notifiche email", null, null, null);
            startActivityForResult(googlePicker, GPICK);
        } else {
            showDialog(DIALOG_ERROR_ID);

        }
    }

    //ELIMINAAAAAAAAA
    public void fakealarm(View view) {
        Intent dialogIntent = new Intent(this, AlarmActivity.class);
        startActivity(dialogIntent);

    }
}



