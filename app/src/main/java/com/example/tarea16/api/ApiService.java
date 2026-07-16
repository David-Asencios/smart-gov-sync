package com.example.tarea16.api;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import java.util.List;
import retrofit2.http.Query;
import okhttp3.ResponseBody;
import retrofit2.http.Streaming;

public interface ApiService {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("sincronizacion")
    Call<Map<String, Object>> sincronizacion(@Header("Authorization") String token, @Query("desde") long desde);

    @POST("sync-data")
    Call<Map<String, Object>> syncData(@Header("Authorization") String token, @Body SyncRequest request);

    @GET("usuarios")
    Call<List<UsuarioResponse>> usuarios(@Header("Authorization") String token);

    @POST("usuarios")
    Call<UsuarioResponse> crearUsuario(@Header("Authorization") String token, @Body UsuarioRequest request);

    @PUT("usuarios/{id}")
    Call<UsuarioResponse> actualizarUsuario(@Header("Authorization") String token, @Path("id") int id,
                                             @Body UsuarioRequest request);

    @Streaming
    @GET("documentos/{id}/adjunto")
    Call<ResponseBody> descargarAdjunto(@Header("Authorization") String token, @Path("id") long id);
}
