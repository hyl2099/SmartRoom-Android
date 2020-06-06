package com.upm.smartroom.postData;

import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.GET;

public interface TemperatureRESTAPIService {
    @GET("/temperature/all")
    Call<List<SpringTemperature>> readTemperatureAll();


    //在这个方法上, 有个@POST 的annotation,这是用来标识当这个方法执行的时候要发送POST 请求出去.
    // @Post annotation 的参数值, 是请求的地址, 这里是/posts. 所以, 请求的全路径将会是http://192.168.1.55:8080/posts.
    // FormUrlEncoded是用来标识这个请求的MIME 类型(一个用来标识HTTP 请求或响应的内容格式的HTTP 头) 需要设置成application/x-www-form-urlencoded,
    // 并且请求的字段和字段值需要在进行URL 编码之前先进行UTF-8 编码处理.
    // @Field("key") 里面的参数值需要和API 期望的参数名相匹配.
    // Retrofit 使用String.valueOf(Object) 将值转换成字符串, 然后对这些字符串进行URL 编码处理. null 值则忽略.
    @POST("/temperature/save")
    @FormUrlEncoded
    Call<SpringTemperature> saveTemp(@Field("temperatureIndoor") Float temperatureIndoor,
                        @Field("humidityIndoor") Float humidityIndoor,
                        @Field("temperatureOutdoor") Float temperatureOutdoor,
                        @Field("humidityOutdoor") Float humidityOutdoor,
                        @Field("time") Date time
                        );

    // 以下方法不是像上面那样每个字段都单独指定. 这个对象将使用在创建Retrofit 实例时指定的Converter 进行序列化.
    // 不过这个只能用于在进行POST 或者PUT 请求的时候.
//    @FormUrlEncoded
    @POST("/temperature/save")

    Call<SpringTemperature> saveTemperature(@Body SpringTemperature t);
}

