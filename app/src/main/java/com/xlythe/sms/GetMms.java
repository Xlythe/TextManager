package com.xlythe.sms;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Niko on 6/23/15.
 */
public class GetMms extends AsyncTask<String, String, String> {

    @Override
    protected String doInBackground(String... params) {
        // Grab data from server
        String result = "[]";
        try {
            URL url = new URL(params[0]);

            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();

            InputStreamReader isr = new InputStreamReader(in);

            StringBuilder builder = new StringBuilder();

            int data = isr.read();
            while(data != -1) {
                char current = (char) data;
                data = isr.read();
                builder.append(current);
            }
            result = builder.toString();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        Log.d("get mms",""+result);
        return result;
    }

    @SuppressLint("NewApi")
    public void executeAsync() {
        if(android.os.Build.VERSION.SDK_INT < 11) {
            execute();
        }
        else {
            executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}