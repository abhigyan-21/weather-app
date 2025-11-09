package com.example.weatherapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    TextView tvLocation, tvTempC, tvTempF, tvSunrise, tvSunset, tvExtra, tvDate;
    ImageView weatherIcon;
    
    // 5-day forecast views
    TextView[] forecastDays = new TextView[5];
    ImageView[] forecastIcons = new ImageView[5];
    TextView[] forecastTemps = new TextView[5];

    private final String API_KEY = "8c7b79640020ae4aa75c87b5eae0637e";
    private String city = "";
    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupClickListeners();
        processIntent();
    }

    private void initializeViews() {
        tvLocation = findViewById(R.id.tv_location);
        tvTempC = findViewById(R.id.tv_temp_c);
        tvTempF = findViewById(R.id.tv_temp_f);
        tvSunrise = findViewById(R.id.tv_sunrise);
        tvSunset = findViewById(R.id.tv_sunset);
        tvExtra = findViewById(R.id.tv_extra);
        weatherIcon = findViewById(R.id.weather_icon);
        tvDate = findViewById(R.id.tv_date);
        
        // Initialize forecast views
        forecastDays[0] = findViewById(R.id.forecast_day_1);
        forecastDays[1] = findViewById(R.id.forecast_day_2);
        forecastDays[2] = findViewById(R.id.forecast_day_3);
        forecastDays[3] = findViewById(R.id.forecast_day_4);
        forecastDays[4] = findViewById(R.id.forecast_day_5);
        
        forecastIcons[0] = findViewById(R.id.forecast_icon_1);
        forecastIcons[1] = findViewById(R.id.forecast_icon_2);
        forecastIcons[2] = findViewById(R.id.forecast_icon_3);
        forecastIcons[3] = findViewById(R.id.forecast_icon_4);
        forecastIcons[4] = findViewById(R.id.forecast_icon_5);
        
        forecastTemps[0] = findViewById(R.id.forecast_temp_1);
        forecastTemps[1] = findViewById(R.id.forecast_temp_2);
        forecastTemps[2] = findViewById(R.id.forecast_temp_3);
        forecastTemps[3] = findViewById(R.id.forecast_temp_4);
        forecastTemps[4] = findViewById(R.id.forecast_temp_5);

        String currentDate = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(new Date());
        tvDate.setText(currentDate);
        
        // Set forecast day names (today + next 4 days)
        setForecastDayNames();
    }
    
    private void setForecastDayNames() {
        String[] weekDays = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_WEEK) - 1; // Calendar.SUNDAY starts at 1
        
        for (int i = 0; i < 5; i++) {
            int dayIndex = (today + i) % 7;
            forecastDays[i].setText(weekDays[dayIndex]);
        }
    }

    private void setupClickListeners() {
        findViewById(R.id.burgerIcon).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LocationsActivity.class);
            startActivity(intent);
        });
    }

    private void processIntent() {
        if (getIntent() != null && getIntent().hasExtra("city")) {
            city = getIntent().getStringExtra("city");
            if (city != null && !city.isEmpty()) {
                fetchWeatherData();
                fetchWeatherForecast();
            } else {
                showPlaceholder();
            }
        } else {
            showPlaceholder();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    private void fetchWeatherData() {
        executor.execute(() -> {
            try {
                String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, API_KEY);
                Request request = new Request.Builder().url(url).build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response " + response);
                    }

                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);

                    double tempC = json.getJSONObject("main").getDouble("temp");
                    double tempF = (tempC * 9 / 5) + 32;

                    JSONObject sys = json.getJSONObject("sys");
                    String sunrise = formatTime(sys.getLong("sunrise"));
                    String sunset = formatTime(sys.getLong("sunset"));

                    int humidity = json.getJSONObject("main").getInt("humidity");
                    double windSpeed = json.getJSONObject("wind").getDouble("speed");
                    String condition = json.getJSONArray("weather").getJSONObject(0).getString("main");
                    String iconCode = json.getJSONArray("weather").getJSONObject(0).getString("icon");

                    WeatherModel weather = new WeatherModel(tempC, tempF, sunrise, sunset, humidity, windSpeed, condition, iconCode);
                    CityModel cityModel = new CityModel(city, weather);

                    mainHandler.post(() -> updateUI(cityModel));
                }
            } catch (UnknownHostException e) {
                mainHandler.post(() -> {
                    showPlaceholder();
                    Toast.makeText(MainActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                });
            } catch (JSONException e) {
                mainHandler.post(() -> {
                    showPlaceholder();
                    Toast.makeText(MainActivity.this, "Error parsing weather data", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    showPlaceholder();
                    Toast.makeText(MainActivity.this, "Error fetching weather: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchWeatherForecast() {
        executor.execute(() -> {
            try {
                String url = String.format("https://api.openweathermap.org/data/2.5/forecast?q=%s&appid=%s&units=metric", city, API_KEY);
                Request request = new Request.Builder().url(url).build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response " + response);
                    }

                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);
                    JSONArray list = json.getJSONArray("list");
                    
                    // Process forecast data for next 5 days
                    String currentDate = "";
                    int dayIndex = 0;
                    
                    for (int i = 0; i < list.length() && dayIndex < 5; i++) {
                        JSONObject item = list.getJSONObject(i);
                        
                        // Extract date (excluding time)
                        String itemDate = item.getString("dt_txt").split(" ")[0];
                        
                        // If we've moved to a new day
                        if (!itemDate.equals(currentDate)) {
                            currentDate = itemDate;
                            
                            // Skip today (first entry) as we already show current temp
                            if (dayIndex > 0 || i > 0) {
                                double temp = item.getJSONObject("main").getDouble("temp");
                                String iconCode = item.getJSONArray("weather").getJSONObject(0).getString("icon");
                                
                                int iconResource = getWeatherIconResource(iconCode);
                                
                                final int index = dayIndex;
                                final double finalTemp = temp;
                                final int finalIconResource = iconResource;
                                
                                mainHandler.post(() -> {
                                    forecastTemps[index].setText(String.format(Locale.US, "%.0f°", finalTemp));
                                    forecastIcons[index].setImageResource(finalIconResource);
                                });
                                
                                dayIndex++;
                            }
                        }
                    }
                }
            } catch (UnknownHostException e) {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "No internet connection", Toast.LENGTH_SHORT).show();
                });
            } catch (JSONException e) {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Error parsing forecast data", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                mainHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Forecast unavailable", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateUI(CityModel cityModel) {
        WeatherModel weather = cityModel.getWeather();
        tvLocation.setText(cityModel.getCityName());
        tvTempC.setText(String.format(Locale.US, "%.1f°C", weather.getTempC()));
        tvTempF.setText(String.format(Locale.US, "%.1f°F", weather.getTempF()));
        tvSunrise.setText(weather.getSunrise());
        tvSunset.setText(weather.getSunset());
        tvExtra.setText(String.format(Locale.US, "Humidity: %d%%   Wind: %.1f mph",
                weather.getHumidity(), weather.getWindSpeed()));

        // Update weather icon based on condition code
        int iconResource = getWeatherIconResource(weather.getIconCode());
        weatherIcon.setImageResource(iconResource);
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(timestamp * 1000));
    }

    private int getWeatherIconResource(String iconCode) {
        // Map OpenWeatherMap icon codes to local drawable resources
        switch (iconCode) {
            case "01d":
                return R.drawable.ic_sunny;
            case "02d":
            case "02n":
                return R.drawable.ic_partlycloudy;
            case "03d":
            case "03n":
            case "04d":
            case "04n":
                return R.drawable.ic_cloudy;
            case "09d":
            case "09n":
            case "10d":
            case "10n":
                return R.drawable.ic_rainy;
            case "11d":
            case "11n":
                return R.drawable.ic_storm;
            case "13d":
            case "13n":
                return R.drawable.ic_snow;
            case "50d":
            case "50n":
                return R.drawable.ic_foggy;
            default:
                return R.drawable.ic_sunny;
        }
    }

    private void showPlaceholder() {
        tvLocation.setText("Choose Location");
        tvTempC.setText("--°C");
        tvTempF.setText("--°F");
        tvSunrise.setText("--:--");
        tvSunset.setText("--:--");
        tvExtra.setText("Humidity: --%   Wind: -- mph");
        
        // Reset forecast views
        for (int i = 0; i < 5; i++) {
            forecastTemps[i].setText("--°");
        }
        
        weatherIcon.setImageResource(R.drawable.ic_sunny);
    }
}
