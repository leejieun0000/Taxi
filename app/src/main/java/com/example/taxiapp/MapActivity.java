package com.example.taxiapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.skt.Tmap.TMapView;
import android.widget.ImageButton;

public class MapActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private TMapView tMapView;
    private String mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);  // layout 파일: activity_map.xml

        mode = getIntent().getStringExtra("mode");
        TextView header = findViewById(R.id.map_header_text);
        header.setText("CALL".equals(mode) ? "콜 수신 모드: 수요 지역 안내 중" : "대기 장소 추천 모드");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    REQUEST_PERMISSIONS_CODE
            );
        } else {
            initTMap();
        }
    }

    private void initTMap() {
        FrameLayout mapContainer = findViewById(R.id.map_container);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("3AVBuNVIlpv01yJMOIr68HuKEoaqyAH6aMpxIInh");

        tMapView.setZoomLevel(15);
        tMapView.setIconVisibility(true);
        tMapView.setTrackingMode(true);
        tMapView.setSightVisible(true);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                tMapView.setCenterPoint(location.getLongitude(), location.getLatitude());
            } else {
                tMapView.setCenterPoint(129.075642, 35.179554); // 부산 시청
            }
        }

        mapContainer.addView(tMapView);

        ImageButton zoomIn = findViewById(R.id.btn_zoom_in);
        ImageButton zoomOut = findViewById(R.id.btn_zoom_out);

        zoomIn.setOnClickListener(v -> tMapView.MapZoomIn());
        zoomOut.setOnClickListener(v -> tMapView.MapZoomOut());

        ImageButton backHome = findViewById(R.id.btn_back_home);
        backHome.setOnClickListener(v -> finish());  // 현재 액티비티 종료 → 이전(MainActivity)로

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                initTMap();
            }
        }
    }
}
