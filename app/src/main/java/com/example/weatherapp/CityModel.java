package com.example.weatherapp;

import java.util.Locale;

public class CityModel {
    private final String cityName;
    private final WeatherModel weather;

    public CityModel(String cityName, WeatherModel weather) {
        this.cityName = cityName;
        this.weather = weather;
    }

    public String getCityName() { 
        return cityName; 
    }

    public WeatherModel getWeather() { 
        return weather; 
    }
    
    public String getTemperature() {
        return String.format(Locale.US, "%.1f°C", weather.getTempC());
    }
}
