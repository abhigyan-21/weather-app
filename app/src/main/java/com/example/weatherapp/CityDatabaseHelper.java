package com.example.weatherapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class CityDatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "cities.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "city_table";
    public static final String COL_CITY = "city_name";
    public static final String COL_TEMPERATURE = "temperature";

    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            COL_CITY + " TEXT PRIMARY KEY, " +
            COL_TEMPERATURE + " TEXT)";

    public CityDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
