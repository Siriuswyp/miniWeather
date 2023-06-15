package com.example.miniweather

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.Image
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.viewpager.widget.ViewPager
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.w3c.dom.Text
import java.io.IOException
import java.time.LocalDateTime
import java.util.Date


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val prefs = getPreferences(Context.MODE_PRIVATE)
        val isSaved = prefs.getBoolean("is_saved", false)
        println(isSaved)
        println(prefs.getString("bg_saved", ""))

        val viewPager = findViewById<ViewPager>(R.id.next_week_weather)
        val pagerAdapter = MyPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        val indicator1 =  findViewById<ImageView>(R.id.page_indicator_1)
        val indicator2 =  findViewById<ImageView>(R.id.page_indicator_2)

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                // 不需要实现
            }

            override fun onPageSelected(position: Int) {
                // 根据当前页面更新小圆点的显示状态
                indicator1.setImageResource(if (position == 0) R.drawable.page_indicator_focused else R.drawable.page_indicator_unfocused)
                indicator2.setImageResource(if (position == 0) R.drawable.page_indicator_unfocused else R.drawable.page_indicator_focused)
            }

            override fun onPageScrollStateChanged(state: Int) {
                // 不需要实现
            }
        })

        // 获取SharedPreferences对象
        val preferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

        // 读取数据
        val cityname = preferences.getString("cityname", "北京")
        val citycode = preferences.getString("citycode", "101010100")

        if (citycode != null && cityname != null) {
            update_main_title(cityname)
            update_main(citycode)
        }

        Log.d("abcd",cityname.toString())
        Log.d("abcd",citycode.toString())

        val someActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            //处理返回结果
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val resultcode = data?.getStringExtra("data_return")
                val resultname = data?.getStringExtra("data_return_name")
                Log.d("return","返回的城市代码：${resultcode} ; 返回的城市名字：${resultname}")
                // 处理返回结果
                //获取网络数据，并更新
                if (resultcode != null && resultname != null) {
                    update_main_title(resultname)
                    update_main(resultcode)
                }
            }
        }

        val city_manager = findViewById<ImageView>(R.id.title_city_manager)
        city_manager.setOnClickListener {
            val intent = Intent(this, SelectCity::class.java)
//            startActivity(intent)
            someActivityResultLauncher.launch(intent)
        }


        val title_update = findViewById<ImageView>(R.id.title_update_btn)
        title_update.setOnClickListener {
            val updatecode = preferences.getString("citycode", "101010100")
            if (updatecode != null) {
                update_main(updatecode)
            }
        }
    }


    fun update_main_weather(citycode:String) {
        // 使用Thread类创建新线程
        val thread = Thread {
            val client = OkHttpClient()
            val url = "https://devapi.qweather.com/v7/weather/now?location="+citycode+
                    "&key=cb88fcc9f9ee4d8c86dbeade241f8b9c"
            val request = Request.Builder().url(url).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 处理请求失败的情况
                    Log.e("now", "请求失败：${e.message}")
                }

                @SuppressLint("SetTextI18n")
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call, response: Response) {
                    // 处理请求成功的情况
                    val jsonString = response.body?.string()
                    Log.i("now", "请求成功：$jsonString")

                    data class nowVo(
                        var temp: String,//气温
                        var text: String,//天气
                        var windDir: String,//风向
                        var windScale: String,//风力
                        var humidity: String, //湿度
                        var icon:String, //天气代码
                    )
                    data class ResponseVo(
                        var code: Int,
                        var fxLink: String,
                        var updateTime: String,
                        var now: nowVo
                    )

                    val gson = Gson()
                    val weather = gson.fromJson(jsonString, ResponseVo::class.java)
                    Log.i("now", "请求成功：${weather.now.text}")

                    val city_text = findViewById<TextView>(R.id.climate)
                    val city_image = findViewById<ImageView>(R.id.weather_img)
                    val city_temp = findViewById<TextView>(R.id.temperature)
                    val city_wind = findViewById<TextView>(R.id.wind)
                    val city_hum = findViewById<TextView>(R.id.humidity)
                    val city_time = findViewById<TextView>(R.id.time)//发布时间
                    val city_weekday = findViewById<TextView>(R.id.week_today)

                    // 可以在UI线程中更新UI
                    runOnUiThread {
                        // 更新UI
                        city_text.text = weather.now.text
                        city_temp.text = "${weather.now.temp}℃"
                        city_wind.text = "${weather.now.windDir} ${weather.now.windScale}级"
                        city_hum.text = "湿度:${weather.now.humidity}%"
                        city_time.text = "今天${weather.updateTime.substring(11,16)}发布"

                        when(LocalDateTime.now().dayOfWeek.value){
                            1 -> city_weekday.text = "星期一"
                            2 -> city_weekday.text = "星期二"
                            3 -> city_weekday.text = "星期三"
                            4 -> city_weekday.text = "星期四"
                            5 -> city_weekday.text = "星期五"
                            6 -> city_weekday.text = "星期六"
                            7 -> city_weekday.text = "星期日"
                        }

                        when(weather.now.icon.toInt()){
                            100,150 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_qing)
                            101,102,103,151,152,153 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_duoyun)
                            104 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_yin)
                            300,301,350,351 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_zhenyu)
                            302,303 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_leizhenyu)
                            304 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_leizhenyubingbao)
                            305,309 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_xiaoyu)
                            306,313,314,399 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_zhongyu)
                            307,308,315 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_dayu)
                            310,316 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_baoyu)
                            311,317 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_dabaoyu)
                            312,318 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_tedabaoyu)
                            400 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_xiaoxue)
                            401 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_zhongxue)
                            402 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_daxue)
                            403 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_baoxue)
                            404,405,406,456 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_yujiaxue)
                            407,457 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_zhenxue)
                            500,501,502,509,510,511,512,513,514,515 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_wu)
                            503,504,507,508 -> city_image.setImageResource(
                                R.mipmap.biz_plugin_weather_shachenbao)
                        }

                    }


                }
            })
        }
        thread.start()
    }

    fun update_main_air(citycode:String){
        // 使用Thread类创建新线程
        val thread_air = Thread {
            val client = OkHttpClient()
            val url = "https://devapi.qweather.com/v7/air/now?location="+citycode+
                    "&key=cb88fcc9f9ee4d8c86dbeade241f8b9c"
            val request = Request.Builder().url(url).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 处理请求失败的情况
                    Log.e("air", "请求失败：${e.message}")
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call, response: Response) {
                    // 处理请求成功的情况
                    val jsonString = response.body?.string()
                    Log.i("air", "请求成功：$jsonString")

                    data class nowAir(
                        var category: String, //污染等级
                        var pm2p5: String, //pm2.5
                        var aqi: String, //空气质量指数
                    )
                    data class ResponseAir(
                        var code: Int,
                        var fxLink: String,
                        var now: nowAir
                    )

                    val gson = Gson()
                    val air = gson.fromJson(jsonString, ResponseAir::class.java)
                    Log.i("air", "请求成功：${air.now.category}")

                    val city_pm_quality = findViewById<TextView>(R.id.pm2_5_quality)
                    val city_pm2p5 = findViewById<TextView>(R.id.pm_data)
                    val pm2p5_image = findViewById<ImageView>(R.id.pm2_5_img)

                    // 可以在UI线程中更新UI
                    runOnUiThread {
                        // 更新UI
                        city_pm_quality.text = air.now.category
                        city_pm2p5.text = air.now.pm2p5

                        when(air.now.aqi.toInt()){
                            in 0..50 -> pm2p5_image.setImageResource(
                                R.mipmap.biz_plugin_weather_0_50)
                            in 51..100 -> pm2p5_image.setImageResource(
                                R.mipmap.biz_plugin_weather_51_100)
                            in 101..150 -> pm2p5_image.setImageResource(
                                R.mipmap.biz_plugin_weather_101_150)
                            in 151..200 -> pm2p5_image.setImageResource(
                                R.mipmap.biz_plugin_weather_151_200)
                            else -> pm2p5_image.setImageResource(
                                R.mipmap.biz_plugin_weather_201_300)
                        }
                    }
                }
            })
        }
        thread_air.start()
    }

    fun update_next_air(citycode:String){
        // 使用Thread类创建新线程
        val thread_air = Thread {
            val client = OkHttpClient()
            val url = "https://devapi.qweather.com/v7/air/5d?location="+citycode+
                    "&key=cb88fcc9f9ee4d8c86dbeade241f8b9c"
            val request = Request.Builder().url(url).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 处理请求失败的情况
                    Log.e("nextair", "请求失败：${e.message}")
                }

                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call, response: Response) {
                    // 处理请求成功的情况
                    val jsonString = response.body?.string()
                    Log.i("nextair", "请求成功：$jsonString")

                    data class nextAir(
                        var category: String, //污染等级
                        var aqi: String, //空气质量指数
                    )
                    data class ResponseAir(
                        var code: Int,
                        var fxLink: String,
                        var updateTime: String,
                        var daily: List<nextAir>
                    )

                    val gson = Gson()
                    val air = gson.fromJson(jsonString, ResponseAir::class.java)
                    Log.i("nextair", "请求成功：${air.updateTime}")

                    val city_img = listOf(findViewById<ImageView>(R.id.img1),
                        findViewById<ImageView>(R.id.img2),
                        findViewById<ImageView>(R.id.img3),
                        findViewById<ImageView>(R.id.img4),
                        findViewById<ImageView>(R.id.img5),
                        findViewById<ImageView>(R.id.img6))

                    // 可以在UI线程中更新UI
                    runOnUiThread {
                        // 更新UI
                        for(i in 1..4){
                            when(air.daily[i].aqi.toInt()){
                                in 0..50 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_0_50)
                                in 51..100 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_51_100)
                                in 101..150 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_101_150)
                                in 151..200 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_151_200)
                                else -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_201_300)
                            }
                        }
                        when(air.daily[4].aqi.toInt()){
                            in 0..50 -> {
                                city_img[4].setImageResource(
                                    R.mipmap.biz_plugin_weather_0_50
                                )
                                city_img[5].setImageResource(
                                    R.mipmap.biz_plugin_weather_0_50
                                )
                            }
                            in 51..100 -> {
                                city_img[4].setImageResource(
                                    R.mipmap.biz_plugin_weather_51_100)
                                city_img[5].setImageResource(
                                    R.mipmap.biz_plugin_weather_51_100)
                            }
                            in 101..150 -> {
                                city_img[4].setImageResource(
                                    R.mipmap.biz_plugin_weather_101_150)
                                city_img[5].setImageResource(
                                    R.mipmap.biz_plugin_weather_101_150)
                            }
                            in 151..200 -> {
                                city_img[4].setImageResource(
                                    R.mipmap.biz_plugin_weather_151_200)
                                city_img[5].setImageResource(
                                    R.mipmap.biz_plugin_weather_151_200)
                            }
                            else -> {
                                city_img[4].setImageResource(
                                    R.mipmap.biz_plugin_weather_201_300)
                                city_img[5].setImageResource(
                                    R.mipmap.biz_plugin_weather_201_300)
                            }
                        }
                    }
                }
            })
        }
        thread_air.start()
    }

    fun update_next_week_weather(citycode:String) {
        // 使用Thread类创建新线程
        val thread = Thread {
            val client = OkHttpClient()
            val url = "https://devapi.qweather.com/v7/weather/7d?location="+citycode+
                    "&key=cb88fcc9f9ee4d8c86dbeade241f8b9c"
            val request = Request.Builder().url(url).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 处理请求失败的情况
                    Log.e("nextweek", "请求失败：${e.message}")
                }

                @SuppressLint("SetTextI18n")
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call, response: Response) {
                    // 处理请求成功的情况
                    val jsonString = response.body?.string()
                    Log.i("nextweek", "请求成功：$jsonString")

                    data class nextVo(
                        var tempMax: String,//气温
                        var tempMin: String,//气温
                        var textDay: String,//天气
                        var windDirDay: String,//风向
                        var windScaleDay: String,//风力
                        var iconDay:String, //天气代码
                    )
                    data class ResponseVo(
                        var code: Int,
                        var fxLink: String,
                        var updateTime: String,
                        var daily: List<nextVo>,
                    )

                    val gson = Gson()
                    val weather = gson.fromJson(jsonString, ResponseVo::class.java)
                    Log.i("nextweek", "请求成功：${weather.updateTime}")

                    val city_climate = listOf(
                        findViewById<TextView>(R.id.climate1),
                        findViewById<TextView>(R.id.climate2),
                        findViewById<TextView>(R.id.climate3),
                        findViewById<TextView>(R.id.climate4),
                        findViewById<TextView>(R.id.climate5),
                        findViewById<TextView>(R.id.climate6)
                    )

                    val city_temp = listOf(
                        findViewById<TextView>(R.id.temperature1),
                        findViewById<TextView>(R.id.temperature2),
                        findViewById<TextView>(R.id.temperature3),
                        findViewById<TextView>(R.id.temperature4),
                        findViewById<TextView>(R.id.temperature5),
                        findViewById<TextView>(R.id.temperature6)
                    )

                    val city_wind = listOf(
                        findViewById<TextView>(R.id.wind1),
                        findViewById<TextView>(R.id.wind2),
                        findViewById<TextView>(R.id.wind3),
                        findViewById<TextView>(R.id.wind4),
                        findViewById<TextView>(R.id.wind5),
                        findViewById<TextView>(R.id.wind6)
                    )


                    val city_img = listOf(
                        findViewById<ImageView>(R.id.img1),
                        findViewById<ImageView>(R.id.img2),
                        findViewById<ImageView>(R.id.img3),
                        findViewById<ImageView>(R.id.img4),
                        findViewById<ImageView>(R.id.img5),
                        findViewById<ImageView>(R.id.img6))

                    val weekday_list = listOf("星期一", "星期二", "星期三", "星期四",
                        "星期五", "星期六", "星期日")
                    var today = LocalDateTime.now().dayOfWeek.value - 1

                    val city_weekday = listOf(
                        findViewById<TextView>(R.id.date1),
                        findViewById<TextView>(R.id.date2),
                        findViewById<TextView>(R.id.date3),
                        findViewById<TextView>(R.id.date4),
                        findViewById<TextView>(R.id.date5),
                        findViewById<TextView>(R.id.date6)
                    )

                    // 可以在UI线程中更新UI
                    runOnUiThread {
                        // 更新UI
                        for(i in 1..6){
                            city_climate[i-1].text = weather.daily[i].textDay
                            city_temp[i-1].text = "${weather.daily[i].tempMin}℃~" +
                                    "${weather.daily[i].tempMax}℃"
                            city_wind[i-1].text = "${weather.daily[i].windDirDay} " +
                                    "${weather.daily[i].windScaleDay}级"
                            city_weekday[i-1].text = weekday_list[++today%7]


                            when(weather.daily[i].iconDay.toInt()){
                                100,150 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_qing)
                                101,102,103,151,152,153 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_duoyun)
                                104 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_yin)
                                300,301,350,351 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_zhenyu)
                                302,303 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_leizhenyu)
                                304 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_leizhenyubingbao)
                                305,309 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_xiaoyu)
                                306,313,314,399 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_zhongyu)
                                307,308,315 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_dayu)
                                310,316 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_baoyu)
                                311,317 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_dabaoyu)
                                312,318 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_tedabaoyu)
                                400 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_xiaoxue)
                                401 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_zhongxue)
                                402 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_daxue)
                                403 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_baoxue)
                                404,405,406,456 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_yujiaxue)
                                407,457 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_zhenxue)
                                500,501,502,509,510,511,512,513,514,515 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_wu)
                                503,504,507,508 -> city_img[i-1].setImageResource(
                                    R.mipmap.biz_plugin_weather_shachenbao)
                            }
                        }
                    }

                }
            })
        }
        thread.start()
    }

    fun update_main(citycode: String){
        val mUpdateBtn =findViewById<ImageView>(R.id.title_update_btn);
        val mProgressBar = findViewById<ProgressBar>(R.id.title_update_progress);

        mUpdateBtn.setVisibility(View.INVISIBLE);
        mProgressBar.setVisibility(View.VISIBLE);
        update_main_air(citycode)
        update_main_weather(citycode)
        update_next_week_weather(citycode)

        Thread {
            Thread.sleep(1000)//模拟更新时间
            runOnUiThread {
                // 在主线程中更新 UI
                mUpdateBtn.setVisibility(View.VISIBLE);
                mProgressBar.setVisibility(View.GONE);
            }
        }.start()
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("restart","重启")

        // 获取SharedPreferences对象
        val preferences = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)

        // 读取数据
        val cityname = preferences.getString("cityname", "北京")
        val citycode = preferences.getString("citycode", "101010100")

        if (citycode != null && cityname != null) {
            update_main_title(cityname)
            update_main(citycode)
        }
    }

    fun update_main_title(cityname:String) {
        val city_title = findViewById<TextView>(R.id.title_city_name)
        val city_name = findViewById<TextView>(R.id.city)

        city_title.text = cityname+"天气"
        city_name.text = cityname
    }

}