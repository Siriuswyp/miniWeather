package com.example.miniweather

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CityAdapter(context: Context, private val cities: List<City>) : ArrayAdapter<City>(context, android.R.layout.simple_list_item_1, cities) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)

        val city = cities[position]
        (view.findViewById(android.R.id.text1) as TextView).text = city.name

        return view
    }
}