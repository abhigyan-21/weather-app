package com.example.weatherapp;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LocationsActivity extends AppCompatActivity {

    LinearLayout locationList;
    CityDatabaseHelper dbHelper;
    String API_KEY = "8c7b79640020ae4aa75c87b5eae0637e";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locations);

        dbHelper = new CityDatabaseHelper(this);
        locationList = findViewById(R.id.location_list);

        Toolbar toolbar = findViewById(R.id.toolbar_locations);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ImageView addLocationBtn = findViewById(R.id.add_location);
        addLocationBtn.setOnClickListener(v -> showCityInputDialog());

        loadSavedCities();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(LocationsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedCities();
    }

    private void showCityInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter City");

        final EditText input = new EditText(this);
        input.setHint("e.g., Mumbai");
        builder.setView(input);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String city = input.getText().toString().trim();
            if (!city.isEmpty()) {
                fetchAndSaveWeather(city);
            } else {
                Toast.makeText(this, "Please enter a city", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void fetchAndSaveWeather(String cityName) {
        new AsyncTask<String, Void, CityModel>() {
            @Override
            protected CityModel doInBackground(String... cities) {
                try {
                    String urlString = "https://api.openweathermap.org/data/2.5/weather?q=" + cities[0] + "&units=metric&appid=" + API_KEY;
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    StringBuilder result = new StringBuilder();
                    int data = reader.read();
                    while (data != -1) {
                        result.append((char) data);
                        data = reader.read();
                    }

                    JSONObject json = new JSONObject(result.toString());
                    double tempC = json.getJSONObject("main").getDouble("temp");
                    double tempF = (tempC * 9 / 5) + 32;
                    int humidity = json.getJSONObject("main").getInt("humidity");
                    double wind = json.getJSONObject("wind").getDouble("speed");

                    long sunriseTimestamp = json.getJSONObject("sys").getLong("sunrise");
                    long sunsetTimestamp = json.getJSONObject("sys").getLong("sunset");
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getDefault());
                    String sunrise = sdf.format(new Date(sunriseTimestamp * 1000));
                    String sunset = sdf.format(new Date(sunsetTimestamp * 1000));

                    String condition = json.getJSONArray("weather").getJSONObject(0).getString("main");
                    String iconCode = json.getJSONArray("weather").getJSONObject(0).getString("icon");

                    WeatherModel weather = new WeatherModel(
                            tempC, tempF, sunrise, sunset, humidity, wind, condition, iconCode
                    );
                    return new CityModel(cities[0], weather);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(CityModel cityModel) {
                if (cityModel != null) {
                    saveToDB(cityModel);
                    loadSavedCities();
                } else {
                    Toast.makeText(LocationsActivity.this, "Failed to fetch weather", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(cityName);
    }

    private void saveToDB(CityModel model) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(CityDatabaseHelper.COL_CITY, model.getCityName());
        values.put(CityDatabaseHelper.COL_TEMPERATURE, model.getWeather().getTempC() + "°C");

        db.insert(CityDatabaseHelper.TABLE_NAME, null, values);
        db.close();
    }

    private void loadSavedCities() {
        locationList.removeAllViews();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(CityDatabaseHelper.TABLE_NAME, null, null, null, null, null, null);

        if (cursor.getCount() == 0) {
            TextView emptyMessage = new TextView(this);
            emptyMessage.setText("No cities added yet.\nTap the + button to add a location.");
            emptyMessage.setTextColor(getResources().getColor(android.R.color.white));
            emptyMessage.setTextSize(18);
            emptyMessage.setGravity(android.view.Gravity.CENTER);
            emptyMessage.setPadding(32, 80, 32, 80);
            locationList.addView(emptyMessage);
        } else {
            while (cursor.moveToNext()) {
                String city = cursor.getString(cursor.getColumnIndex(CityDatabaseHelper.COL_CITY));
                String temp = cursor.getString(cursor.getColumnIndex(CityDatabaseHelper.COL_TEMPERATURE));
                double tempValue = Double.parseDouble(temp.replace("°C", ""));
                double tempF = (tempValue * 9 / 5) + 32;

                WeatherModel dummyWeather = new WeatherModel(
                        tempValue, tempF, "--:--", "--:--", 0, 0.0, "Clear", "01d"
                );
                addCard(new CityModel(city, dummyWeather));
            }
        }
        cursor.close();
        db.close();
    }

    private void addCard(CityModel model) {
        // Create main container with padding and margins
        LinearLayout locationCard = new LinearLayout(this);
        locationCard.setOrientation(LinearLayout.HORIZONTAL);
        locationCard.setPadding(32, 24, 16, 24); // Reduced right padding for delete button
        locationCard.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Set margins
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) locationCard.getLayoutParams();
        params.setMargins(0, 0, 0, 16);
        locationCard.setLayoutParams(params);
        
        // Set background
        locationCard.setBackground(getResources().getDrawable(R.drawable.location_item_bg));

        // Create a container for the clickable content (city name and temperature)
        LinearLayout contentContainer = new LinearLayout(this);
        contentContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0, 
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));
        contentContainer.setOrientation(LinearLayout.HORIZONTAL);
        
        // City name
        TextView cityName = new TextView(this);
        cityName.setText(model.getCityName());
        cityName.setTextColor(getResources().getColor(android.R.color.white));
        cityName.setTextSize(20);
        cityName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        contentContainer.addView(cityName);

        // Temperature
        TextView tempView = new TextView(this);
        tempView.setText(String.format(Locale.getDefault(), "%.1f°C", model.getWeather().getTempC()));
        tempView.setTextColor(getResources().getColor(android.R.color.white));
        tempView.setTextSize(20);
        contentContainer.addView(tempView);
        
        // Set click listener on content area
        contentContainer.setOnClickListener(v -> {
            Intent intent = new Intent(LocationsActivity.this, MainActivity.class);
            intent.putExtra("city", model.getCityName());
            startActivity(intent);
        });
        
        locationCard.addView(contentContainer);
        
        // Delete button
        ImageView deleteButton = new ImageView(this);
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete);
        deleteButton.setColorFilter(getResources().getColor(android.R.color.white));
        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        deleteButton.setPadding(padding, padding, padding, padding);
        deleteButton.setOnClickListener(v -> {
            // Show confirmation dialog
            new AlertDialog.Builder(this)
                    .setTitle("Delete Location")
                    .setMessage("Are you sure you want to delete " + model.getCityName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Delete from database and refresh UI
                        deleteLocation(model.getCityName());
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
        locationCard.addView(deleteButton);

        locationList.addView(locationCard);
    }
    
    private void deleteLocation(String cityName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String whereClause = CityDatabaseHelper.COL_CITY + "=?";
        String[] whereArgs = {cityName};
        
        db.delete(CityDatabaseHelper.TABLE_NAME, whereClause, whereArgs);
        db.close();
        
        // Refresh the list
        loadSavedCities();
        
        Toast.makeText(this, cityName + " removed", Toast.LENGTH_SHORT).show();
    }
}
