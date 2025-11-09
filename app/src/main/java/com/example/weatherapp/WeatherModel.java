package com.example.weatherapp;

public class WeatherModel {
    private final double tempC;
    private final double tempF;
    private final String sunrise;
    private final String sunset;
    private final int humidity;
    private final double windSpeed;
    private final String condition;
    private final String iconCode;

    public WeatherModel(double tempC, double tempF, String sunrise, String sunset, 
                       int humidity, double windSpeed, String condition, String iconCode) {
        this.tempC = tempC;
        this.tempF = tempF;
        this.sunrise = sunrise;
        this.sunset = sunset;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.condition = condition;
        this.iconCode = iconCode;
    }

    public double getTempC() { return tempC; }
    public double getTempF() { return tempF; }
    public String getSunrise() { return sunrise; }
    public String getSunset() { return sunset; }
    public int getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }
    public String getCondition() { return condition; }
    public String getIconCode() { return iconCode; }
}