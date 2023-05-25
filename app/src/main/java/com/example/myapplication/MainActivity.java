package com.example.myapplication;

import static android.Manifest.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView cityTextView;
    private TextView temperatureTextView;
    private TextView descriptionTextView;

    private ImageView searchBtn;

    private EditText searchText;

    private ImageView icon;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private int LOCATION_REQUEST_CODE = 1;
    private double lat = 0;
    private double lon = 0;
    private ListView listView;
    SimpleAdapter adapter;

    private ArrayList<Map<String, Object>> forecastDataList;

    public void cacheLocation(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cityTextView = findViewById(R.id.cityTextView);
        temperatureTextView = findViewById(R.id.temperatureTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        searchText = findViewById(R.id.searchText);
        searchBtn = findViewById(R.id.searchBtn);
        icon = findViewById(R.id.icon);
        listView = findViewById(R.id.mobile_list);

        forecastDataList = new ArrayList<>();
        adapter = new SimpleAdapter(this, forecastDataList, R.layout.listitem,
                new String[] { "date", "Temp" },
                new int[] { R.id.dateTextView, R.id.tempTextView });
        listView.setAdapter(adapter);

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = searchText.getText().toString();
                if (!TextUtils.isEmpty(query)) {
                    String weatherApiUrl = "https://api.weatherapi.com/v1/forecast.json?key=d950dfdb852448bdb24181716232305&q="
                            + query + "&days=10&aqi=no&alerts=no";
                    Weather weatherTask = new Weather(cityTextView, temperatureTextView, descriptionTextView,
                            MainActivity.this, icon, listView);
                    weatherTask.execute(weatherApiUrl);
                    searchText.setText("");
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a city name", Toast.LENGTH_SHORT).show();
                }
            }
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        MainActivity self = this;
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                if (lat != self.lat && lon != self.lon) {
                    cacheLocation(lat, lon);
                    Toast.makeText(MainActivity.this, "New location: " + lat + ", " + lon, Toast.LENGTH_SHORT)
                            .show();
                    // get city name
                    Geocoder geocoder = new Geocoder(getApplicationContext());
                    try {
                        String city = geocoder.getFromLocation(lat, lon, 1).get(0).getLocality();

                        cityTextView.setText(city);
                        GetWeather(city);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                        permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {
                    permission.ACCESS_FINE_LOCATION,
                    permission.ACCESS_COARSE_LOCATION
            }, this.LOCATION_REQUEST_CODE);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this,
                        permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this,
                                permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else {
                Toast.makeText(this, "No access to location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void GetWeather(String cityName) {
        String weatherApiUrl = "https://api.weatherapi.com/v1/forecast.json?key=d950dfdb852448bdb24181716232305&q="
                + cityName + "&days=10&aqi=no&alerts=no";
        Weather weatherTask = new Weather(cityTextView, temperatureTextView, descriptionTextView,
                getApplicationContext(), icon, listView);
        weatherTask.execute(weatherApiUrl);
    }
}

class Weather extends AsyncTask<String, Void, String> {

    private TextView cityTextView;
    private TextView temperatureTextView;
    private TextView descriptionTextView;
    private Context context;
    private ImageView image;
    private SimpleAdapter adapter;
    private ListView listView;

    private List<Map<String, Object>> forecastDataList = new ArrayList<>();

    public Weather(TextView cityTextView, TextView temperatureTextView, TextView descriptionTextView, Context context,
            ImageView image, ListView listView) {
        this.cityTextView = cityTextView;
        this.temperatureTextView = temperatureTextView;
        this.descriptionTextView = descriptionTextView;
        this.context = context;
        this.image = image;
        this.listView = listView;
        this.adapter = new SimpleAdapter(context, forecastDataList, R.layout.listitem,
                new String[] { "date", "Temp" },
                new int[] { R.id.dateTextView, R.id.tempTextView });
        this.listView.setAdapter(adapter);
    }

    protected String doInBackground(String... strings) {
        String url = strings[0];
        try {
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(String result) {

        if (result != null) {
            try {
                JSONObject jsonResponse = new JSONObject(result);

                JSONObject location = jsonResponse.getJSONObject("location");
                String cityName = location.getString("name");
                String countryName = location.getString("country");

                JSONObject current = jsonResponse.getJSONObject("current");
                double temperatureC = current.getDouble("temp_c");
                String conditionText = current.getJSONObject("condition").getString("text");

                String iconUrl = jsonResponse.getJSONObject("current").getJSONObject("condition").getString("icon");
                new DownloadImageTask(image).execute(iconUrl);

                JSONArray forecastdayArray = jsonResponse.getJSONObject("forecast").getJSONArray("forecastday");

                forecastDataList.clear();

                for (int i = 0; i < forecastdayArray.length(); i++) {
                    JSONObject forecastdayObject = forecastdayArray.getJSONObject(i);

                    String date = forecastdayObject.getString("date");
                    double temp = forecastdayObject.getJSONObject("day").getDouble("maxtemp_c");

                    Map<String, Object> forecastData = new HashMap<>();
                    forecastData.put("date", date);
                    forecastData.put("Temp", temp + "°C");

                    forecastDataList.add(forecastData);
                }

                adapter.notifyDataSetChanged();

                cityTextView.setText(cityName + ", " + countryName);
                temperatureTextView.setText(temperatureC + "°C");
                descriptionTextView.setText(conditionText);
            } catch (JSONException e) {
                e.printStackTrace();

            }
        } else {
            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show();
        }
    }
}
