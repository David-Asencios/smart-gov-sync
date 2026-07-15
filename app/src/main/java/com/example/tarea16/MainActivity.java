package com.example.tarea16;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.tarea16.activities.LoginActivity;
import com.example.tarea16.api.TokenManager;
import com.example.tarea16.databinding.ActivityMainBinding;
import com.example.tarea16.security.RoleManager;
import com.example.tarea16.sync.SyncManager;
import com.google.android.material.navigation.NavigationView;
import com.example.tarea16.sync.SyncScheduler;

public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private TokenManager tokenManager;
    private String role;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tokenManager = new TokenManager(this);
        if (!tokenManager.sesionValida()) {
            cerrarSesion();
            return;
        }
        role = tokenManager.obtenerRol();
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        configurarCabecera(navigationView);
        configurarMenu(navigationView.getMenu());

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_expedientes, R.id.nav_documentos, R.id.nav_bandeja,
                R.id.nav_derivaciones, R.id.nav_archivo, R.id.nav_sincronizacion)
                .setOpenableLayout(drawer)
                .build();
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment == null) {
            throw new IllegalStateException("No se encontro el contenedor de navegacion");
        }
        navController = navHostFragment.getNavController();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                cerrarSesion();
                return true;
            }
            if (!RoleManager.canSeeDestination(role, item.getItemId())) {
                Toast.makeText(this, R.string.role_action_denied, Toast.LENGTH_SHORT).show();
                return false;
            }
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) drawer.close();
            return handled;
        });

        int home = RoleManager.homeDestination(role);
        if (navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != home) {
            navController.navigate(home);
        }
        navigationView.setCheckedItem(home);
        new SyncManager(this).sincronizar(null);
    }

    private void configurarCabecera(NavigationView navigationView) {
        View header = navigationView.getHeaderView(0);
        SyncScheduler.schedule(this);
        TextView user = header.findViewById(R.id.txtNavUsuario);
        TextView roleView = header.findViewById(R.id.txtNavRol);
        user.setText(tokenManager.obtenerNombreCompleto());
        roleView.setText(RoleManager.displayName(role) + " · "
                + tokenManager.obtenerNombreOficina());
    }

    private void configurarMenu(Menu menu) {
        int[] destinations = {
                R.id.nav_expedientes, R.id.nav_documentos, R.id.nav_bandeja,
                R.id.nav_derivaciones, R.id.nav_archivo, R.id.nav_sincronizacion
        };
        for (int destination : destinations) {
            menu.findItem(destination).setVisible(RoleManager.canSeeDestination(role, destination));
        }
    }

    private void cerrarSesion() {
        tokenManager.limpiar();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }
}
