package com.example.taxiapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.skt.Tmap.TMapData;
import com.skt.Tmap.TMapMarkerItem;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapPolyLine;
import com.skt.Tmap.TMapView;

import java.util.ArrayList;
import java.util.List;

public class NavigationActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private TMapView tMapView;
    private TMapPoint startPoint, endPoint, cityHallPoint;

    private TextView headerTextView;

    private Button btnNavi; // 클래스 위쪽에 선언

    private String startName, endName, toStartDuration, toStartDistance, distanceText;
    private int duration;

    private void updateHeaderText(String from, String to, String duration, String distance) {
        headerTextView.setText(String.format("📍 %s → %s (예상 %s, %s)", from, to, duration, distance));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // ① 인텐트에서 정보 받아오기
        Intent intent = getIntent();
        double startLat = intent.getDoubleExtra("startLat", 0);
        double startLng = intent.getDoubleExtra("startLng", 0);
        double endLat = intent.getDoubleExtra("endLat", 0);
        double endLng = intent.getDoubleExtra("endLng", 0);
        double cityHallLat = intent.getDoubleExtra("cityHallLat", 35.179554);
        double cityHallLng = intent.getDoubleExtra("cityHallLng", 129.075642);
        startName = intent.getStringExtra("startName");
        endName = intent.getStringExtra("endName");
        duration = intent.getIntExtra("duration", -1);
        distanceText = intent.getStringExtra("distanceText");  // 새로 추가한 부분
        toStartDuration = intent.getStringExtra("toStartDuration");
        toStartDistance = intent.getStringExtra("toStartDistance");




        startPoint = new TMapPoint(startLat, startLng);
        endPoint = new TMapPoint(endLat, endLng);
        cityHallPoint = new TMapPoint(cityHallLat, cityHallLng);

        headerTextView = findViewById(R.id.map_header_text);

// 🚩 처음에는 부산시청 → 출발지 표시
        updateHeaderText("부산시청", startName, toStartDuration, toStartDistance);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_CODE);
        } else {
            initTMap();
        }

        btnNavi = findViewById(R.id.btn_start_navi);
        btnNavi.setText("안내 시작");
        btnNavi.setOnClickListener(v -> showRoute2());

    }



    private void initTMap() {
        FrameLayout mapContainer = findViewById(R.id.map_container);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("3AVBuNVIlpv01yJMOIr68HuKEoaqyAH6aMpxIInh");

        tMapView.setIconVisibility(true);
        tMapView.setTrackingMode(false);
        tMapView.setSightVisible(false);
        tMapView.setCenterPoint(startPoint.getLongitude(), startPoint.getLatitude());

        mapContainer.addView(tMapView);

        // 지도 초기화 후 0.5초 뒤 경로 표시
        new android.os.Handler().postDelayed(this::showRoute, 500);
    }

    private void showRoute() {
        if (tMapView == null) return;

        TMapData tMapData = new TMapData();

// 🚗 1. 부산시청 → 출발지 (초록색)
        tMapData.findPathDataWithType(
                TMapData.TMapPathType.CAR_PATH,
                cityHallPoint,
                startPoint,
                polyline1 -> {
                    runOnUiThread(() -> {
                        polyline1.setLineWidth(10);
                        polyline1.setLineColor(0xFF10B981);  // 초록색
                        tMapView.addTMapPath(polyline1);
                    });
                    // 마커 추가
                    //addMarkers();

                    // 중심 자동 조정
                    List<TMapPoint> allPoints = new ArrayList<>(polyline1.getLinePoint());
                    adjustMapZoom(allPoints);

                }
        );

    }

    private void showRoute2() {
        if (tMapView == null) return;
        TMapData tMapData = new TMapData();
        // 🚗 2. 출발지 → 목적지 (파란색) ← 반드시 내부에서 실행
        tMapData.findPathDataWithType(
                TMapData.TMapPathType.CAR_PATH,
                startPoint,
                endPoint,
                polyline2 -> {
                    runOnUiThread(() -> {
                        polyline2.setLineWidth(10);
                        polyline2.setLineColor(0xFF1E3A8A);  // 파란색
                        tMapView.addTMapPath(polyline2);

                        // 마커 추가
                        //addMarkers();

                        // 중심 자동 조정
                        List<TMapPoint> allPoints = new ArrayList<>(polyline2.getLinePoint());
                        adjustMapZoom(allPoints);

                        updateHeaderText(startName, endName, duration+"분", distanceText);

                        btnNavi.setVisibility(View.GONE);  // 버튼 숨기기

                    });
                }
        );
    }


    private void adjustMapZoom(List<TMapPoint> allPoints) {
        if (allPoints == null || allPoints.isEmpty()) return;

        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;

        for (TMapPoint p : allPoints) {
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
        }

        double centerLat = (minLat + maxLat) / 2.0;
        double centerLon = (minLon + maxLon) / 2.0;
        double latDiff = maxLat - minLat;
        double lonDiff = maxLon - minLon;
        double maxDiff = Math.max(latDiff, lonDiff);

        int zoom;
        if (maxDiff < 0.01) zoom = 17;
        else if (maxDiff < 0.03) zoom = 16;
        else if (maxDiff < 0.08) zoom = 14;
        else if (maxDiff < 0.15) zoom = 13;
        else zoom = 12;

        tMapView.setCenterPoint(centerLon, centerLat);
        tMapView.setZoomLevel(zoom);
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
