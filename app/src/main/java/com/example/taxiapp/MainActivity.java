package com.example.taxiapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.skt.Tmap.TMapView;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private TMapView tMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // 우리가 만든 레이아웃

        // 위치 권한 체크 및 요청
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_PERMISSIONS_CODE);
        } else {
            initTMap();
        }
    }

    // TMapView 초기화
    private void initTMap() {
        FrameLayout mapContainer = findViewById(R.id.map_container);

        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("3AVBuNVIlpv01yJMOIr68HuKEoaqyAH6aMpxIInh");

        // 지도 설정 (옵션)
        tMapView.setZoomLevel(15);
        tMapView.setIconVisibility(true); // 내 위치 아이콘 표시
        tMapView.setTrackingMode(true);   // 지도 따라 움직임
        tMapView.setSightVisible(true);   // 시야 각도 표시

        mapContainer.addView(tMapView);
    }

    // 권한 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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
            } else {
                // 권한 거부된 경우 처리 (예: 안내 메시지)
            }
        }
    }
}


