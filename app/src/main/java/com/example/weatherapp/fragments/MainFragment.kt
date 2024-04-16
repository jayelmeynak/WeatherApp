package com.example.weatherapp.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.transition.Visibility
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherapp.API_KEY
import com.example.weatherapp.DialogManager
import com.example.weatherapp.MainViewModel
import com.example.weatherapp.R
import com.example.weatherapp.adapter.ViewPagerAdapter
import com.example.weatherapp.adapter.WeatherModel
import com.example.weatherapp.databinding.FragmentMainBinding
import com.example.weatherapp.isPermissionGranted
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject

class MainFragment : Fragment() {
    private val fList = listOf(
        HoursFragment.newInstance(),
        DaysFragment.newInstance()
    )
    private lateinit var binding: FragmentMainBinding
    private lateinit var pLauncher: ActivityResultLauncher<String>
    private val model: MainViewModel by activityViewModels()
    private lateinit var fLocationClient : FusedLocationProviderClient


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        init()
        updateCurrentCard()
    }

    override fun onResume() {
        super.onResume()
        checkLocation()
    }



    private fun init() = with(binding) {
        fLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val adapter = ViewPagerAdapter(activity as FragmentActivity, fList)
        vp.adapter = adapter
        val tList = listOf(
            getString(R.string.hours),
            getString(R.string.days)
        )
        TabLayoutMediator(tabLayout, vp) { tab, pos ->
            tab.text = tList[pos]
        }.attach()

        ibSync.setOnClickListener{
            checkLocation()
            tabLayout.selectTab(tabLayout.getTabAt(0))
        }
        ibSearch.setOnClickListener{
            DialogManager.searchByNameDialog(requireContext(), object:DialogManager.Listener{
                override fun onClick(name: String?) {
                    Log.d("MyLog", "Name: $name")
                    if(!name.isNullOrEmpty()) requestWeatherData(name)
                }
            })

            tabLayout.selectTab(tabLayout.getTabAt(0))
        }
    }

    private fun getLocation(){
        val ct = CancellationTokenSource()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, ct.token).addOnCompleteListener{
            requestWeatherData("${it.result.latitude},${it.result.longitude}")
        }
    }

    private fun checkLocation(){
        if(isLocationEnabled()){
            getLocation()
        }
        else{
            DialogManager.locationSettingDialog(requireContext(),object: DialogManager.Listener{
                override fun onClick(name: String?) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }

            })
        }
    }

    private fun isLocationEnabled(): Boolean{
        val lm = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun permissionListener() {// запуск registerFarActivityResult
        pLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            Toast.makeText(activity, "Permission is $it", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermission() { //проверка есть ли разрешение на геолокацию
        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestWeatherData(city: String) {// отправка запроса на сервер
        var url =
            "https://api.weatherapi.com/v1/forecast.json?key=$API_KEY&q=$city&days=7&aqi=no&alerts=no&lang=ru"
        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET, url, { result ->
                parseWeatherData(result)
                Log.d("MyLog", result)
            }, { error ->
                Log.d("MyLog", "Error $error")
            }
        )
        queue.add(request)
    }

    private fun updateCurrentCard() = with(binding) {// обновление данных в массиве
        model.liveDataCurrent.observe(viewLifecycleOwner) {
            tvData.text = it.time
            val city = String(it.city.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)

            tvCity.text = city
            val maxMinTemp = "${it.maxTemp.split(".")[0]}°C/${it.minTemp.split(".")[0]}°C"
            var temp = it.currentTemp

            if (temp.isNotEmpty()) {
                temp = temp.split(".")[0] + "°C"
                tvMaxMin.text = maxMinTemp
            }
            else {
                temp = maxMinTemp
                tvMaxMin.text = ""
            }

            tvCurrentTemp.text = temp
            val str = String(it.condition.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
            tvCondition.text = str
            Picasso.get().load("https:${it.imageUrl}").into(imWeather)
        }
    }

    private fun parseWeatherData(result: String) {//инициализация JSONObject и получение всех данных
        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentData(mainObject, list[0])
    }

    private fun parseCurrentData(mainObject: JSONObject, weatherItem: WeatherModel) {
        val item = WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current").getJSONObject("condition").getString("text"),
            mainObject.getJSONObject("current").getString("temp_c"),
            weatherItem.maxTemp, weatherItem.minTemp,
            mainObject.getJSONObject("current").getJSONObject("condition").getString("icon"),
            weatherItem.hours
        )
        model.liveDataCurrent.value = item
    }

    private fun parseDays(mainObject: JSONObject): List<WeatherModel> {
        val list = ArrayList<WeatherModel>()
        val daysArray = mainObject.getJSONObject("forecast").getJSONArray("forecastday")
        val name = mainObject.getJSONObject("location").getString("name")
        for (i in 0 until daysArray.length()) {
            val day = daysArray[i] as JSONObject
            val item = WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day").getJSONObject("condition").getString("text"),
                "",
                day.getJSONObject("day").getString("maxtemp_c"),
                day.getJSONObject("day").getString("mintemp_c"),
                day.getJSONObject("day").getJSONObject("condition").getString("icon"),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        model.liveDataList.value = list
        return list
    }

    companion object {

        @JvmStatic
        fun newInstance() = MainFragment()
    }
}