package com.example.weatherapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.example.weatherapp.R
import com.example.weatherapp.databinding.ListItemBinding
import com.squareup.picasso.Picasso


class WeatherAdapter(val listener: Listener?) : ListAdapter<WeatherModel, WeatherAdapter.WeatherViewHolder>(Comporator()) {

    class WeatherViewHolder(view: View, val listener: Listener?) : RecyclerView.ViewHolder(view) {
        val binding = ListItemBinding.bind(view)
        var itemTemp: WeatherModel? = null

        init {
            itemView.setOnClickListener{
                itemTemp?.let { it1 -> listener?.onClick(it1) }
            }
        }

        fun bind(item: WeatherModel) = with(binding) {
            itemTemp = item
            val time = item.time.split(" ")
            var temp = item.currentTemp

            temp = if (temp.isNotEmpty()) temp.split(".")[0] + "°C"
            else "${item.maxTemp.split(".")[0]}°C/${item.minTemp.split(".")[0]}°C"

            val str = String(item.condition.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)

            if (time.size == 1) tvDate.text = time[0]
            else tvDate.text = time[1]

            tvCondition.text = str
            tvTemp.text = temp
            Picasso.get().load("https:${item.imageUrl}").into(im)
        }

    }

    class Comporator : DiffUtil.ItemCallback<WeatherModel>() {
        override fun areItemsTheSame(oldItem: WeatherModel, newItem: WeatherModel): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: WeatherModel, newItem: WeatherModel): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return WeatherViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    interface Listener{
        fun onClick(item: WeatherModel)
    }

}