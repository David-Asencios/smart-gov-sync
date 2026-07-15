package com.example.tarea16.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tarea16.MainActivity;
import com.example.tarea16.R;
import com.example.tarea16.api.ApiClient;
import com.example.tarea16.api.LoginRequest;
import com.example.tarea16.api.LoginResponse;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.databinding.ActivityLoginBinding;
import com.example.tarea16.security.RoleManager;

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
        if (tokenManager.sesionValida()) {
            abrirMain();
            return;
        }
        tokenManager.limpiar();
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String username = binding.txtUsuario.getText() == null ? "" : binding.txtUsuario.getText().toString().trim();
        String password = binding.txtPassword.getText() == null ? "" : binding.txtPassword.getText().toString();
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.login_required_fields, Toast.LENGTH_SHORT).show();
            return;
        }
        mostrarCarga(true);
        ApiClient.getService().login(new LoginRequest(username, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                mostrarCarga(false);
                LoginResponse body = response.body();
                if (response.isSuccessful() && body != null && body.token != null
                        && RoleManager.isAllowed(body.rol) && tokenManager.guardarSesion(body)) {
                    abrirMain();
                } else {
                    tokenManager.limpiar();
                    Toast.makeText(LoginActivity.this, R.string.login_invalid, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable error) {
                mostrarCarga(false);
                Toast.makeText(LoginActivity.this, R.string.login_connection_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarCarga(boolean loading) {
        binding.progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!loading);
        binding.txtUsuario.setEnabled(!loading);
        binding.txtPassword.setEnabled(!loading);
    }

    private void abrirMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
