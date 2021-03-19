package com.google.ar.sceneform.samples.augmentedimage.flowgate;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/*
 * This class is for sending *one* request to *one* url to get asset information and perform corresponding action
 * Similar functions as `displayAssetInfoOnScreen` can be written:
 * add arguments (e.g. location of barcode) &
 * define a new actor to determine what to do when receiving response (e.g. display with AR)
 */
public class flowgateClientWorker {
    String urlString;   // `urlString` is the string of url to be requested

    public flowgateClientWorker(String urlString){
        this.urlString = urlString;
    }

    /*
     * `context` is the class of MainActivity,
     * `receivedToken` is the received token used for authorization
     * `actor` is used to determine what to do when receiving response
     * EFFECTS: obtain asset info by sending GET request to `this.urlString`,
     *          and `actor` acts
     */
    public void getAssetInfo(Context context, String receivedToken, flowgateClientActor actor) {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, urlString, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            actor.act(response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // Log.d("flowgateClientWorker", "setAssetInfo: " + response.toString());
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error maybe throw exception/display message on screen
                        Log.w("flowgateClientWorker",
                                "setAssetInfo: response error - " + error.toString());
                    }
                })
        {
            @Override
            public Map<String, String> getHeaders() {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/json");
                params.put("Authorization", "Bearer " + receivedToken);
                params.put("Accept", "application/json");
                return params;
            }
        };
        MySingleton.getInstance(context).addToRequestQueue(jsonObjectRequest);
    }

    /*
     * EFFECTS: obtain asset info by sending GET request to `this.urlString`,
     *          and display received info on `textView`
     */
    public void displayAssetInfoOnScreen(Context context, String receivedToken, TextView textView){
        flowgateClientActor actor = new flowgateClientActor() {
            @Override
            public void act(JSONObject response) throws JSONException {
                String assetNumber = response.getString("assetNumber");
                String assetName = response.getString("assetName");
                String category = response.getString("category");
                String manufacturer = response.getString("manufacturer");
                String model = response.getString("model");
                String mountingSide = response.getString("mountingSide");

                String disp =
                        assetName  + "\n" +
                                "Asset Number: " + assetNumber + "\n" +
                                "Category: " + category  + "\n" +
                                "Manufacturer: " + manufacturer  + "\n" +
                                "Model: " + model  + "\n" +
                                "Mounting side: " + mountingSide;

                textView.setText(disp);
            }
        };
        getAssetInfo(context, receivedToken, actor);
    }

}
