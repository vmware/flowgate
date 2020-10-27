package com.example.display;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;


public class DisplayMessageActivity extends AppCompatActivity {

    static String token;
    static JSONObject data;
    static String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);

        // Get the Intent that started this activity and extract the string
        //Intent intent = getIntent();
        //String deviceName = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        deviceName = "testServer";


        Thread t1 = new Thread(getToken);
        t1.start();
        try {
            t1.join();
        }
        catch(InterruptedException e) {
            token = "Error: Cannot get token\n";
        }

        Thread t2 = new Thread(getData);
        t2.start();
        try {
            t2.join();
        }
        catch(InterruptedException e) {
            try {
                data = new JSONObject("{'Error': 'Cannot get data'}");
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }

        String dataStr;
        if (data == null)
            dataStr = "Error: obtained data is null.";
        else
            dataStr = data.toString();
        String result = "token: " + token +"\n\n" + "device data: " + dataStr;
        //String result = "device data: " + dataStr;

        // Capture the layout's TextView and set the string as its text
        TextView textView = findViewById(R.id.textView);
        textView.setText(result);
    }

    Runnable getToken = new Runnable() {
        @Override
        public void run() {
            // https request
            flowgateClient fc = new flowgateClient("10.11.16.36", "API", "QWxv_3arJ70gl");
            token = fc.getAuthToken();
        }
    };

    Runnable getData = new Runnable() {
        @Override
        public void run() {
            // https request
            flowgateClient fc = new flowgateClient("10.11.16.36", "API", "QWxv_3arJ70gl");
            data = fc.getAssetByName(deviceName);
        }
    };
}