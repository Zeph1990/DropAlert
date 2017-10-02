package com.aserrapica.dropalert;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexs on 30/06/2016.
 */
public class DbHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "daRegistro";
    private static final String TABLE_REG = "registro";
    private static final String KEY_DATE = "date";
    private static final String KEY_TIME = "time";
    private static final String KEY_LOC = "loc";

    public DbHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //Crea la tabella nel Database
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE =
                "CREATE TABLE " + TABLE_REG + "(" + KEY_DATE + " TEXT," + KEY_TIME + " TEXT," + KEY_LOC + " TEXT)";
        System.out.println(CREATE_TABLE);
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REG);
        onCreate(db);
    }

    //Aggiunge una nuova riga alla tabella.
    public void addEvent(Registro r) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_DATE, String.valueOf(r.getData()));
        values.put(KEY_TIME, String.valueOf(r.getTime()));
        if (r.getLinkloc() != null && !r.getLinkloc().equals(""))
            values.put(KEY_LOC, r.getLinkloc());

        db.insert(TABLE_REG, null, values);
    }

    //Restituisce tutti gli eventi del registro
    public List<Registro> getAllEvents() {
        List<Registro> registro = new ArrayList<Registro>();
        String selectQuery = "SELECT * FROM " + TABLE_REG;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToLast()) {
            do {
                Registro r = new Registro();
                r.setData(cursor.getString(0));
                r.setTime(cursor.getString(1));
                if (cursor.getString(2) != null && !cursor.getString(2).equals(""))
                    r.setLinkloc(cursor.getString(2));
                registro.add(r);
            } while (cursor.moveToPrevious());
        }
        return registro;
    }

}

