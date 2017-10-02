package com.aserrapica.dropalert;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

public class GMailSender extends Activity {
    private Session session;


    public GMailSender() {
        super();

    }

    //Questo metodo effettua la connessione SMTP ai server google e attraverso la librearia JavaMail invia la mail
    public SMTPTransport connectToSmtp(String host, int port, String userEmail,
                                       String oauthToken, boolean debug) throws Exception {

        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.sasl.enable", "false");

        session = Session.getInstance(props);
        session.setDebug(debug);

        final URLName unusedUrlName = null;
        SMTPTransport transport = new SMTPTransport(session, unusedUrlName);
        final String emptyPassword = null;


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);


        transport.connect(host, port, userEmail, emptyPassword);

        byte[] response = String.format("user=%s\1auth=Bearer %s\1\1",
                userEmail, oauthToken).getBytes();
        response = BASE64EncoderStream.encode(response);

        transport.issueCommand("AUTH XOAUTH2 " + new String(response), 235);

        return transport;
    }

    //Questo metodo prende in ingresso tutti i parametri necessari all'invio della mail e procede a richiamare la funzione predisposta
    public synchronized void sendMail(final String subject, final String body, final String user,
                                        final String oauthToken, final String recipients) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {

                    SMTPTransport smtpTransport = connectToSmtp("smtp.gmail.com", 587,
                            user, oauthToken, true);

                    MimeMessage message = new MimeMessage(session);
                    DataHandler handler = new DataHandler(new ByteArrayDataSource(
                            body.getBytes(), "text/plain"));
                    message.setSender(new InternetAddress(user));
                    message.setSubject(subject);
                    message.setDataHandler(handler);
                    message.setRecipient(Message.RecipientType.TO,
                            new InternetAddress(recipients));
                    smtpTransport.sendMessage(message, message.getAllRecipients());

                } catch (Exception e) {
                    Log.d("test", e.getMessage(), e);
                }

                return null;
            }

        };
        task.execute();
    }

}