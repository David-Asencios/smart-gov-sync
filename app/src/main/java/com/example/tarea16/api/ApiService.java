package com.example.tarea16.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("sincronizacion")
    Call<Map<String, Object>> sincronizacion(@Header("Authorization") String token, @Query("desde") long desde);

    @POST("sync-data")
    Call<Map<String, Object>> syncData(@Header("Authorization") String token, @Body SyncRequest request);
}
