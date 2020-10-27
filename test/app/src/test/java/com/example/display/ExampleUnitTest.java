package com.example.display;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void getAuthToken_isCorrect() {
        /*
           In order to test locally with JSONObject:
           `testOptions`, `testCompile 'org.json:json:20200518' is added to build.gradle(:app)`
         */
        flowgateClient fc = new flowgateClient("10.11.16.36", "admin", "Ar_InDataCenter_450");
        String s = fc.getAuthToken();
        System.out.println("TOKEN: " + s);
        // don't know where to find the output.. could only test by setting break points
    }

    @Test
    public void getAssetByName_isCorrect() throws JSONException {
        /*
           In order to test locally with JSONObject:
           `testOptions`, `testCompile 'org.json:json:20200518' is added to build.gradle(:app)`
         */
        flowgateClient fc = new flowgateClient("10.11.16.36", "API", "QWxv_3arJ70gl");
        String name = "testServer";
        JSONObject js = fc.getAssetByName(name);
        //System.out.println("TOKEN: " + js.getString("id"));
        System.out.println("data: " + js.toString());
    }
}