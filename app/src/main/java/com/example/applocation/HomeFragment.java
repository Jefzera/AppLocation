package com.example.applocation;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.journeyapps.barcodescanner.CaptureActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private MapView mapView;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;
    private TextView tvLocation;
    private Button btnScanQRCode;

    private static final double DEFAULT_LATITUDE = -23.482875; // 23°28'58.5"S
    private static final double DEFAULT_LONGITUDE = -52.693250; // 52°41'35.9"W

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);


        mapView = view.findViewById(R.id.map);
        tvLocation = view.findViewById(R.id.tv_location);
        btnScanQRCode = view.findViewById(R.id.btn_scan_qr_code);

        Configuration.getInstance().load(getContext(), android.preference.PreferenceManager.getDefaultSharedPreferences(getContext()));
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);


        showLocationOnMap(new Location("dummyprovider") {{
            setLatitude(DEFAULT_LATITUDE);
            setLongitude(DEFAULT_LONGITUDE);
        }});


        btnScanQRCode.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CaptureActivity.class);
            startActivityForResult(intent, 0);
        });

        return view;
    }

    private void showLocationOnMap(Location location) {
        currentLocation = location;
        mapView.getController().setZoom(18);
        mapView.getController().setCenter(new org.osmdroid.util.GeoPoint(location.getLatitude(), location.getLongitude()));

        addMarkerAtLocation(location);
        updateLocationText(location);
    }

    private void updateLocationText(Location location) {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getLocality();
                String state = address.getAdminArea();
                if (city != null && state != null) {
                    tvLocation.setText(String.format("%s, %s", city, state));
                } else {
                    tvLocation.setText("Não encontramos sua localização!");
                }
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Erro na localização: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addMarkerAtLocation(Location location) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new org.osmdroid.util.GeoPoint(location.getLatitude(), location.getLongitude()));
        marker.setTitle("Você está aqui");
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(getContext(), "Permissão de localização recusada!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        fusedLocationClient.getLastLocation().addOnSuccessListener(getActivity(), location -> {
            if (location != null) {
                showLocationOnMap(location);
            } else {
                Toast.makeText(getContext(), "Não foi possível obter a sua localização.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Erro ao encontrar sua localização! " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
