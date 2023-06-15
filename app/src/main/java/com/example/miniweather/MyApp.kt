package com.example.miniweather

import android.app.Application
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化全局变量、初始化第三方库、初始化数据库等应用程序级别的操作
        Log.d("abc", "MyApp onCreate")
        copyDatabaseFile()
    }
    private fun copyDatabaseFile() {
        try {
            // 检查目标文件是否已经存在
            val outputFile = File(getDatabasePath("my_database.db").path)
            if (outputFile.exists()) {
                Log.d("abc", "Database file already exists. Skipping copy operation.")
                return
            }

            // 复制数据库文件
            val inputStream = assets.open("city.db")
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(1024)
            var length: Int

            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Log.d("abc", "Database file copied successfully.")

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}