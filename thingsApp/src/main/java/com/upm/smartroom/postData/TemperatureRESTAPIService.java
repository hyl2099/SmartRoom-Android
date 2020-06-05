package com.upm.smartroom.postData;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;

public interface TemperatureRESTAPIService {
    @POST("/temperature/save")
    Call addTemperature(@Body RequestBody temperature);

    @GET("/temperature/all")
    Call readTemperatureAll();
}
