package com.example.miniweather

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

class MyPagerAdapter(private val context: Context) : PagerAdapter() {
    private val views = arrayOf(
        LayoutInflater.from(context).inflate(R.layout.next_week_weather1, null),
        LayoutInflater.from(context).inflate(R.layout.next_week_weather2, null)
    )

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        // 获取指定位置的 View
        val view = views[position]
        // 将 View 添加到容器中
        container.addView(view)
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        // 从容器中移除指定位置的 View
        container.removeView(obj as View)
    }

    override fun getCount(): Int {
        // 返回页面的数量
        return views.size
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        // 判断 View 是否为指定的 Object
        return view == obj
    }
}