package com.example.display;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;
import org.json.*;

import javax.net.ssl.*;

public class flowgateClient {

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

    // read `response` from `connection` after 200 OK
    private StringBuilder readResponse(HttpURLConnection connection) throws IOException {
        InputStreamReader isr = new InputStreamReader(connection.getInputStream());
        BufferedReader br = new BufferedReader(isr);
        StringBuilder response = new StringBuilder();
        String responseLine;
        while((responseLine = br.readLine()) != null){
            response.append(responseLine.trim());
        }
        return response;
    }

    // @RequiresApi(api = Build.VERSION_CODES.KITKAT) throws IOException, JSONException
    public String getAuthToken() {
        try{
            /*
             * Existed token not expired
             */
            if(this.token != null){
                int expiresTime = Integer.parseInt(token.getString("expires_in"));
                long currentTime = System.currentTimeMillis(); // unix time in milliseconds
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
            HttpURLConnection tokenHttpCon = (HttpURLConnection)(tokenUrl.openConnection());
            tokenHttpCon.setRequestMethod("POST");
            tokenHttpCon.setDoOutput(true);
            tokenHttpCon.setRequestProperty("Content-Type", "application/json");
            tokenHttpCon.setRequestProperty("Accept", "application/json");

            // send authentication info
            String jsonUser = "{\"userName\": \""+(this.userName)+"\", "
                    + "\"password\": \""+this.password+"\"}";
            byte[] inputUser = jsonUser.getBytes(); // StandardCharsets.UTF_8
            OutputStream os = tokenHttpCon.getOutputStream();
            os.write(inputUser, 0, inputUser.length);

            // receive and set this.token and return string if success
            tokenHttpCon.connect();
            int responseStatus = tokenHttpCon.getResponseCode();
            if(responseStatus == 200){
                StringBuilder response = this.readResponse(tokenHttpCon);
                this.token = new JSONObject(response.toString());
                return token.getString("access_token");
            }

            return null;
        }
        catch (IOException e){
            Log.w("flowgateClient", "getAuthToken: IO exception when asking for token");
            return null;
        }
        catch (JSONException e){
            Log.w("flowgateClient", "getAuthToken: JSON exception when asking for token");
            return null;
        }
    }

    /*
     * `type` is to identify which API is used; `identifier` is to identify the device
     * REQUIRES:
     * "this.host + urlPart" should be a valid address in the backend
     * EFFECTS:
     * return required asset info
     */
    private JSONObject getAssetInfo(String type, String urlPart, String identifier){
        try{
            /*
             * Get token
             */
            String curToken = getAuthToken();
            if(curToken == null || curToken.isEmpty()){
                Log.w("flowgateClient", "getAssetByName: no available token");
                return null;
            }

            /*
             * Set up connection
             */
            String urlString = "https://" + this.host + urlPart + "/";
            URL url = new URL(urlString);
            HttpURLConnection httpCon = (HttpURLConnection)(url.openConnection());
            httpCon.setRequestMethod("GET");
            httpCon.setDoOutput(true);
            httpCon.setRequestProperty("Content-Type", "application/json");
            httpCon.setRequestProperty("Authorization", "Bearer " + curToken);
            httpCon.setRequestProperty("Accept", "application/json");

            Log.i("flowgateClient", "getAssetBy " + type + ": query device: " + identifier);
            httpCon.connect();
            int responseStatus = httpCon.getResponseCode();
            if(responseStatus == 200){
                StringBuilder response = readResponse(httpCon);
                return new JSONObject(response.toString());
            }
            else{
                Log.w("flowgateClient", "getAssetInfo: response code not 200 (" +
                        responseStatus + ")");
                return null;
            }
        }
        catch (IOException e){
            Log.w("flowgateClient", "getAssetInfo: IO exception when asking for token");
            return null;
        }
        catch (JSONException e){
            Log.w("flowgateClient", "getAssetInfo: JSON exception when asking for token");
            return null;
        }
    }

    public JSONObject getAssetByName(String name){
        return getAssetInfo("name", this.assetString + "name/" + name, name);
    }

    public JSONObject getAssetById(String id){
        return getAssetInfo("id", this.assetString + id, id);
    }
}
