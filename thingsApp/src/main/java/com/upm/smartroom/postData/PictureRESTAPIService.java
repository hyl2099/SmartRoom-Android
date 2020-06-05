package com.upm.smartroom.postData;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface PictureRESTAPIService {
    @POST("/pictures/addfile")
    Call addPicture(@Body RequestBody picture);

    @GET("/pictures/all")
    Call readPictureAll();
}
