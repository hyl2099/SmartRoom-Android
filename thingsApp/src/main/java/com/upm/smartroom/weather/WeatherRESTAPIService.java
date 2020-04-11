package com.upm.smartroom.weather;

import com.upm.smartroom.weather.Weather;
import retrofit2.Call;
import retrofit2.http.GET;

public interface WeatherRESTAPIService {
    //https://api.openweathermap.org/data/2.5/weather?id=2172797&APPID=c9c811de2bf865411aae739065ff94e5&lat=40&lon=-3.6
    @GET("data/2.5/weather?id=2172797&APPID=c9c811de2bf865411aae739065ff94e5&lat=40&lon=-3.6")
    Call<Weather> getTempByCityName();
}
