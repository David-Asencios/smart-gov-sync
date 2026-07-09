package com.example.tarea16.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tarea16.MainActivity;
import com.example.tarea16.api.ApiClient;
import com.example.tarea16.api.LoginRequest;
import com.example.tarea16.api.LoginResponse;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.databinding.ActivityLoginBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private TokenManager tokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tokenManager = new TokenManager(this);
        if (tokenManager.tieneToken()) {
            abrirMain();
            return;
        }
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String username = binding.txtUsuario.getText().toString().trim();
        String password = binding.txtPassword.getText().toString();
        ApiClient.getService().login(new LoginRequest(username, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().token != null) {
                    tokenManager.guardarToken(response.body().token);
                    abrirMain();
                } else {
                    Toast.makeText(LoginActivity.this, "Credenciales invalidas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "No se pudo conectar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void abrirMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
