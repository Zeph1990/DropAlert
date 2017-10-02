package com.aserrapica.dropalert;

/**
 * Created by alexs on 30/06/2016.
 */
//Questa classe è una classe di supporto per gli oggetti del registro degli eventi
public class Registro {
    private String data;
    private String time;
    private String linkloc;

    public Registro() {

    }

    public Registro(String d, String t, String l) {
        this.setData(d);
        this.setTime(t);
        this.setLinkloc(l);
    }

    public Registro(String d, String t) {
        this.setData(d);
        this.setTime(t);
    }


    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getLinkloc() {
        return linkloc;
    }

    public void setLinkloc(String linkloc) {
        this.linkloc = linkloc;
    }
}
