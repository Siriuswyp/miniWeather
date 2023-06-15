package com.example.miniweather

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MyDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "my_database.db"
        const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 创建数据库表格
        db.execSQL("CREATE TABLE my_table (id INTEGER PRIMARY KEY, name TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 更新数据库表格
        db.execSQL("DROP TABLE IF EXISTS my_table")
        onCreate(db)
    }

    @SuppressLint("Range")
    fun queryData(): List<String> {
        val data = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM city", null)

        if (cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndex("number"))
                data.add(name)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return data
    }

    @SuppressLint("Range")
    fun searchCitiesByName(name: String): List<City> {
        val COLUMN_CITY = "city"
        val COLUMN_CODE = "number"

        val db = readableDatabase
        val cursor = db.query("city", arrayOf(COLUMN_CITY, COLUMN_CODE), "$COLUMN_CITY LIKE ?", arrayOf("%$name%"), null, null, null)
        val result = mutableListOf<City>()
        while (cursor.moveToNext()) {
            val cityName = cursor.getString(cursor.getColumnIndex(COLUMN_CITY))
            val cityCode = cursor.getString(cursor.getColumnIndex(COLUMN_CODE))
            result.add(City(cityName, cityCode))
        }
        cursor.close()
        db.close()
        return result
    }
}