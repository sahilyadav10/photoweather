package com.sahilyadav10.photoweather;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    EditText searchView;
    TextView temperatureTextView, feelsLikeTextView, windTextView;
    TextView locationView, statusTextView, updatedAtTextView, sunriseTextView, sunsetTextView, moonriseTextView, moonilluminationTextView;
    ProgressDialog progressView;
    SharedPreferences sharedPreferences;
    public static final String USER_PREF = "USER_PREF" ;
    public static final String KEY_NAME = "CITY_NAME";
    public static final String API_KEY = "Enter Your Key Here";
    public static final String WEATHER_URL = "https://api.weatherapi.com/v1/current.json?key=" + API_KEY + "&q=";
    public static final String ASTRONOMY_URL = "https://api.weatherapi.com/v1/astronomy.json?key=" + API_KEY + "&q=";

    private static class APICall {
        String weatherAddress;
        String astronomyAddress;

        APICall(String weatherAddress, String astronomyAddress) {
            this.weatherAddress = weatherAddress;
            this.astronomyAddress = astronomyAddress;
        }
    }

    private static class AppData {
        JSONObject weatherData;
        JSONObject astronomyData;

        AppData(JSONObject weatherData, JSONObject astronomyData) {
            this.weatherData = weatherData;
            this.astronomyData = astronomyData;
        }
    }

    ConnectivityManager cm;
    NetworkInfo activeNetwork;
    boolean isConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("PreviousData", Context.MODE_PRIVATE);

        searchView = findViewById(R.id.searchView);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        locationView = findViewById(R.id.locationView);
        feelsLikeTextView = findViewById(R.id.feelsLike);
        windTextView = findViewById(R.id.wind);
        statusTextView = findViewById(R.id.status);
        updatedAtTextView = findViewById(R.id.updated_at);
        sunriseTextView = findViewById(R.id.sunrise);
        sunsetTextView = findViewById(R.id.sunset);
        moonriseTextView = findViewById(R.id.moonrise);
        moonilluminationTextView = findViewById(R.id.illumination);

        Context context = getApplication().getApplicationContext();
        cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);


        //check if connected to internet
        activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        //make api call if connected to internet
        if(isConnected) {
            StringBuilder str = new StringBuilder();
            if (sharedPreferences.contains(KEY_NAME)) {
                String city = sharedPreferences.getString(KEY_NAME, "");
                APICall params = new APICall(WEATHER_URL + city,
                        ASTRONOMY_URL + city);
                new JsonTask().execute(params);
            }
        }


        //listen for enter button action on the keyboard
        searchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId != 0 || event.getAction() == KeyEvent.ACTION_DOWN) {
                    //hide keyboard after enter pressed
                    InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(v.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);


                    String cityName = searchView.getText().toString();

                    //Save city name in shared preferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(KEY_NAME, cityName);
                    editor.apply();

                    //Check if connected to internet
                    activeNetwork = cm.getActiveNetworkInfo();
                    isConnected = activeNetwork != null &&
                            activeNetwork.isConnectedOrConnecting();
                    //make api call if connected to api
                    if(isConnected) {
                        APICall params = new APICall("https://api.weatherapi.com/v1/current.json?key=6991f675ef9346d1b73130001202411&q=" + cityName,
                                "https://api.weatherapi.com/v1/astronomy.json?key=6991f675ef9346d1b73130001202411&q=" + cityName);
                        new JsonTask().execute(params);
                    }
                    //show toast if not connected to internet
                    else{
                        Toast.makeText(MainActivity.this, "No Internet Connection!",
                                Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private class JsonTask extends AsyncTask<APICall, String, AppData> {

        protected void onPreExecute() {
            super.onPreExecute();

            progressView = new ProgressDialog(MainActivity.this);
            progressView.setMessage("Please wait");
            progressView.setCancelable(false);
            progressView.show();
        }

        protected AppData doInBackground(APICall... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                URL url;
                InputStream stream;
                BufferedReader streamReader;

                // GET weather data
                url = new URL(params[0].weatherAddress);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                stream = connection.getInputStream();
                streamReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);

                JSONObject weatherJSON = new JSONObject(responseStrBuilder.toString());



                //GET astronomy data
                url = new URL(params[0].astronomyAddress);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                stream = connection.getInputStream();
                streamReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                responseStrBuilder = new StringBuilder();

                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);
                JSONObject astronomyJSON = new JSONObject(responseStrBuilder.toString());

                //returns the json object
                AppData appdata = new AppData(weatherJSON, astronomyJSON);
                return appdata;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(AppData result) {
            if (progressView.isShowing()){
                progressView.dismiss();
            }
            renderWeather(result.weatherData);
            renderAstronomy(result.astronomyData);
        }
    }

    private void renderWeather(JSONObject json){
        try {
            locationView.setText(json.getJSONObject("location").getString("name") + ", " +
                    json.getJSONObject("location").getString("country"));

            DateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
            long milliSec = Long.parseLong(json.getJSONObject("location").getString("localtime_epoch"));
            milliSec *= 1000;
            Date date = new Date(milliSec);
            updatedAtTextView.setText(formatter.format(date).toString());


            temperatureTextView.setText(json.getJSONObject("current").getString("temp_c") + "\u2103");
            windTextView.setText("Wind Speed: " + json.getJSONObject("current").getString("wind_kph") + "kph");
            feelsLikeTextView.setText("Feels Like: " + json.getJSONObject("current").getString("feelslike_c") + "\u2103");
            statusTextView.setText(json.getJSONObject("current").getJSONObject("condition").getString("text"));


        }catch(Exception e){
            Log.e("PhotoWeather", "One or more fields not found in the JSON data");
        }
    }

    private void renderAstronomy(JSONObject json){
        try {
            sunriseTextView.setText(json.getJSONObject("astronomy").getJSONObject("astro").getString("sunrise"));
            sunsetTextView.setText(json.getJSONObject("astronomy").getJSONObject("astro").getString("sunset"));
            moonriseTextView.setText(json.getJSONObject("astronomy").getJSONObject("astro").getString("moonrise"));
            moonilluminationTextView.setText(json.getJSONObject("astronomy").getJSONObject("astro").getString("moon_illumination"));

        }catch(Exception e){
            Log.e("PhotoWeather", "One or more fields not found in the JSON data");
        }
    }

}
