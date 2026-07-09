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

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private double latitud;
    private double longitud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        latitud = getIntent().getDoubleExtra("latitud", -12.0464);
        longitud = getIntent().getDoubleExtra("longitud", -77.0428);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        LatLng posicion = new LatLng(latitud, longitud);
        googleMap.addMarker(new MarkerOptions().position(posicion).title("Derivacion"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 16f));
    }
}
