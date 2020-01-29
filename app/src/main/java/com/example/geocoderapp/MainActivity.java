package com.example.geocoderapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private int requestId = 0;
    private double traveled;

    private Thread loadingThread;
    private Geocoder geocoder;
    private DecimalFormat milesFormat = new DecimalFormat("####.###");
    private DecimalFormat latLongFormat = new DecimalFormat("#####.####");

    private Location location;
    private LocationManager locationManager;
    private TextView latitudeView;
    private TextView longitudeView;
    private TextView distanceView;
    private TextView addressView;
    private TextView addressView2;

    public void refresh(View view)
    {
        traveled = 0;
        geocoder = new Geocoder(this, Locale.getDefault());
        onLocationChanged(location);
    }

    public String repeat(String s, int n)
    {
        StringBuilder o = new StringBuilder(); for (int i = 0; i<n; i++) o.append(s); return o.toString();
    }

    public String fillStart(String s, String fragment, int ceiling)
    {
        int delta = s.length() - fragment.length();
        if (delta > ceiling)
            return fragment + s.substring(fragment.length());
        String out = null; int index = 0;
        while (index < s.length() - ceiling)
        {
            out = fragment.substring(0, index++) + s.substring(index);
        }
        return out;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    requestId++
            );
            return;
        }

        setContentView(R.layout.activity_main);

        latitudeView = findViewById(R.id.latitude);
        longitudeView = findViewById(R.id.longitude);
        distanceView = findViewById(R.id.distance);
        addressView = findViewById(R.id.address);
        addressView2 = findViewById(R.id.addressLine2);
        Button refreshButton = findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this::refresh);

        assert locationManager != null;
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1000, this);

        // treat this comment as a change please
        new Thread(() ->
        {
            int step = 0;
            List<String> lat = new ArrayList<>();
            for (int i=0;i<=24;i++)
                lat.add(fillStart(repeat(". ", i), "loading ", 4));

            List<String> lon = new ArrayList<>(lat);
            lon.add(0, lon.remove(lon.size() - 1));

            List<String> dist = new ArrayList<>(lon);
            dist.add(0, dist.remove(dist.size() - 1));

            while (location == null)
            {
                try {
                    step = step + 1 > lat.size() - 1 ? 0 : step + 1;
                    final int stepF = step;
                    runOnUiThread(() -> {
                        latitudeView.setText(lat.get(stepF));
                        longitudeView.setText(lon.get(stepF));
                        distanceView.setText(dist.get(stepF));
                    });
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            onLocationChanged(location);
        });
        loadingThread.start();

        refresh(null);
    }

    @Override
    @SuppressLint("SetTextI18n")
    public void onLocationChanged(Location location) {
        assert locationManager != null;
        assert location != null;

        if (locationManager == null) return;
        if (location == null) return;

        if (this.location != null)
            traveled += this.location.distanceTo(location);
        this.location = location;

        try
        {
            loadingThread.join();
        } catch (InterruptedException e) {
            Log.e("ERROR", Objects.requireNonNull(e.getMessage()));
        }

        //noinspection AndroidLintSetTextI18n
        this.latitudeView.setText("Latitude: " + latLongFormat.format(this.location.getLatitude()));
        //noinspection AndroidLintSetTextI18n
        this.longitudeView.setText("Longitude: " + latLongFormat.format(this.location.getLongitude()));
        //noinspection AndroidLintSetTextI18n
        distanceView.setText("Distance traveled: " + milesFormat.format(metersToMiles(traveled)) + " mi");
        try {
            Address address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1).get(0);
            addressView.setText(address.getAddressLine(address.getMaxAddressLineIndex()));;
            addressView2.setText(address.getAdminArea());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double metersToMiles(double m)
    {
        return m/1000 * 0.621371;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
