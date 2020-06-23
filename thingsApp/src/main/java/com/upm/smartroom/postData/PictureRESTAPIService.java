package com.upm.smartroom.postData;


import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface PictureRESTAPIService {
    //此处如果后台接受的是Picture类对象，可以用一个Map把对象传过去。不过我已经单独写了个只接受from-data类型的API

    @Multipart
    @POST("pictures/addPhoto")
    Call<SpringPicture> addPhoto(@Part MultipartBody.Part file,
                             @Part("owner") RequestBody owner,
                             @Part("remark") RequestBody remark
                             );


    @GET("/pictures/all")
    Call<SpringPicture> readPictureAll();
}
