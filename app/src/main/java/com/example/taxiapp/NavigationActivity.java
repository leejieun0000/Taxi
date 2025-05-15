package com.example.taxiapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
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

import org.w3c.dom.Document;

import java.util.List;

public class NavigationActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private TMapView tMapView;
    private TMapPoint startPoint = new TMapPoint(35.179554, 129.075642); // 부산시청
    private TMapPoint endPoint = new TMapPoint(35.164082, 129.064961); // 부산역

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);


        TextView header = findViewById(R.id.map_header_text);
        header.setText("📍 부산시청 → 부산역 (길안내 모드)");

        Button btnNavi = findViewById(R.id.btn_start_navi);
        btnNavi.setText("안내 시작");

        btnNavi.setOnClickListener(v -> showRoute());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_CODE);
        } else {
            initTMap();
        }
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
    }



    private void showRoute() {
        if (tMapView == null) return;

        TMapData tMapData = new TMapData();
        tMapData.findPathData(startPoint, endPoint, new TMapData.FindPathDataListenerCallback() {
            @Override
            public void onFindPathData(TMapPolyLine polyline) {
                polyline.setLineWidth(15); // 선 두께 조절
                polyline.setLineColor(0xFF1E3A8A); // 진한 파란색 (indigo-900 계열)
                tMapView.addTMapPath(polyline);

                // 출발지 마커
                TMapMarkerItem startMarker = new TMapMarkerItem();
                startMarker.setTMapPoint(startPoint);
                startMarker.setName("출발지");
                startMarker.setCanShowCallout(true);
                startMarker.setAutoCalloutVisible(true);
                tMapView.addMarkerItem("start", startMarker);

                // 도착지 마커
                TMapMarkerItem endMarker = new TMapMarkerItem();
                endMarker.setTMapPoint(endPoint);
                endMarker.setName("도착지");
                endMarker.setCanShowCallout(true);
                endMarker.setAutoCalloutVisible(true);
                tMapView.addMarkerItem("end", endMarker);

                // 중심 이동
                List<TMapPoint> points = polyline.getLinePoint();
                if (points != null && !points.isEmpty()) {
                    TMapPoint center = points.get(points.size() / 2);
                    runOnUiThread(() -> {
                        TextView routeInfo = findViewById(R.id.route_info);
                        double distance = polyline.getDistance();
                        routeInfo.setText(String.format("총 거리: %.1f km", distance / 1000.0));

                        tMapView.setCenterPoint(center.getLongitude(), center.getLatitude());
                        tMapView.setZoomLevel(12);
                    });
                }
            }
        });
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
