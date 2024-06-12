package com.google.ar.sceneform.samples.augmentedimage.flowgate;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class flowgateClient {
    private static final String TAG = "flowgateClient";

    private String host;
    private String userName;
    private String password;

    private final String assetString = "/apiservice/v1/assets/";

    private JSONObject token = null;

    public flowgateClient(String host, String userName, String password){
        this.host = host;
        this.userName = userName;
        this.password = password;
    }

    public void setHost(String newHost){
        this.host = newHost;
    }

    public void setUserName(String newUserName){
        this.userName = newUserName;
    }

    public void setPassword(String newPassword){
        this.password = newPassword;
    }

    TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
    };

    // Read `response` from `connection` after 200 OK
    private StringBuilder readResponse(HttpsURLConnection connection) throws IOException {
        InputStreamReader isr = new InputStreamReader(connection.getInputStream());
        BufferedReader br = new BufferedReader(isr);
        StringBuilder response = new StringBuilder();
        String responseLine;
        while((responseLine = br.readLine()) != null){
            response.append(responseLine.trim());
        }
        return response;
    }

    // Get token
    public String getAuthToken() {
        try{
            /*
             * Existed token not expired
             */
            if(this.token != null){
                long expiresTime = Long.parseLong(token.getString("expires_in"));
                long currentTime = System.currentTimeMillis(); // unix time in milliseconds
                Log.d("flowgateClient", expiresTime + " " + currentTime);
                if(expiresTime - currentTime > 600000){
                    return token.getString("access_token");
                }
            }

            /*
             * Acquire new token
             */
            // install all-trusting trust manager TODO because our server has no certificate
            try{
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            }
            catch (Exception e){}
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession sslSession) {
                    return true;
                }
            });

            // set up connection
            String authString = "/apiservice/v1/auth/token";
            URL tokenUrl = new URL("https://" + host + authString);
            HttpsURLConnection tokenHttpCon = (HttpsURLConnection)(tokenUrl.openConnection());
            tokenHttpCon.setRequestMethod("POST");
            tokenHttpCon.setDoOutput(true);
            tokenHttpCon.setRequestProperty("Content-Type", "application/json");
            tokenHttpCon.setRequestProperty("Accept", "application/json");

            Log.d(TAG, "connect");

            // send authentication info
            String jsonUser = "{\"userName\": \""+(this.userName)+"\", "
                    + "\"password\": \""+this.password+"\"}";
            byte[] inputUser = jsonUser.getBytes(); // StandardCharsets.UTF_8
            OutputStream os = tokenHttpCon.getOutputStream();
            os.write(inputUser, 0, inputUser.length);

            Log.d(TAG, "auth");

            // receive and set this.token and return string if success
            tokenHttpCon.connect();
            int responseStatus = tokenHttpCon.getResponseCode();
            if(responseStatus == 200){
                StringBuilder response = this.readResponse(tokenHttpCon);
                this.token = new JSONObject(response.toString());
                return token.getString("access_token");
            }


            Log.d(TAG, "return");

            return null;
        }
        catch (IOException e){
            Log.w("flowgateClient", "getAuthToken: IO exception when asking for token");
            e.printStackTrace();
            return null;
        }
        catch (JSONException e){
            Log.w("flowgateClient", "getAuthToken: JSON exception when asking for token");
            return null;
        }
    }

    // Let flowgateClientWorker get asset information from `urlString` and display on `textView`
    public void getAssetInfoOnScreen(Context context, String urlString, TextView textView) throws Exception {
        class NullTokenException extends Exception{
            public NullTokenException(){
                super("flowgateClient: NullTokenException in getAssetInfo");
            }
        }
        String receivedToken = getAuthToken();
        if(receivedToken == null){
            Log.w("flowgateClient", "getAssetInfo: null token");
            throw new NullTokenException();
        }

        flowgateClientWorker worker = new flowgateClientWorker(urlString);
        worker.displayAssetInfoOnScreen(context, receivedToken, textView);
    }

    // Get asset information by `name` and display on `textView`
    public void getAssetByNameOnScreen(Context context, String name, TextView textView){
        try {
            getAssetInfoOnScreen(context, "https://" + this.host + this.assetString + "name/" + name, textView);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // Get asset information by `id` and display on `textView`
    public void getAssetByIdOnScreen(Context context, String id, TextView textView){
        try {
            Log.d(TAG, "here");
            getAssetInfoOnScreen(context, "https://" + this.host + this.assetString + id, textView);
        }
        catch (Exception e){
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

}
