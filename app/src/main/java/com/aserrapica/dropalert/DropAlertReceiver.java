package com.aserrapica.dropalert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Created by alexs on 22/06/2016.
 */
public class DropAlertReceiver extends BroadcastReceiver {

    //Gestisce l'avvio dell'appliczione all'avvio di Android
    @Override
    public void onReceive(Context context, Intent intent) {

        Intent myIntent = new Intent(context, DropAlertService.class);
        final String MY_PREFERENCES = "daPref";
        SharedPreferences prefs = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
        String textData = prefs.getString("autoCheck", "no");
        String textData2 = prefs.getString("dasCheck", "no");
        if (textData.equals("yes") && textData2.equals("yes"))
            context.startService(myIntent);

    }
}
