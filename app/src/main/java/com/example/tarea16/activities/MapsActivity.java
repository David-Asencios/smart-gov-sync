package com.example.tarea16.activities;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.example.tarea16.R;
import com.example.tarea16.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private double latitud;
    private double longitud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        latitud = getIntent().getDoubleExtra("latitud", Double.NaN);
        longitud = getIntent().getDoubleExtra("longitud", Double.NaN);
        if (!Double.isFinite(latitud) || !Double.isFinite(longitud)
                || (latitud == 0d && longitud == 0d)) {
            android.widget.Toast.makeText(this, R.string.map_location_missing, android.widget.Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.toolbarMap.setNavigationOnClickListener(v -> finish());
        binding.txtCoordinates.setText(getString(
                R.string.map_coordinates,
                String.format(Locale.US, "%.6f", latitud),
                String.format(Locale.US, "%.6f", longitud)));
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng posicion = new LatLng(latitud, longitud);
        googleMap.addMarker(new MarkerOptions()
                .position(posicion)
                .title(getString(R.string.map_marker_title)));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 16f));
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }
}
