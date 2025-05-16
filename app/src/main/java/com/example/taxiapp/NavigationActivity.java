package com.example.taxiapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.util.List;

public class NavigationActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private TMapView tMapView;
    private TMapPoint startPoint;
    private TMapPoint endPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // ✅ 인텐트에서 전달받은 값 받기
        Intent intent = getIntent();
        double startLat = intent.getDoubleExtra("startLat", 0);
        double startLng = intent.getDoubleExtra("startLng", 0);
        double endLat = intent.getDoubleExtra("endLat", 0);
        double endLng = intent.getDoubleExtra("endLng", 0);
        String startName = intent.getStringExtra("startName");
        String endName = intent.getStringExtra("endName");
        int duration = intent.getIntExtra("duration", -1);

        startPoint = new TMapPoint(startLat, startLng);
        endPoint = new TMapPoint(endLat, endLng);

        TextView header = findViewById(R.id.map_header_text);
        header.setText(String.format("📍 %s → %s (예상 %d분)", startName, endName, duration));

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
        tMapData.findPathDataWithType(
                TMapData.TMapPathType.CAR_PATH,
                startPoint,
                endPoint,
                new TMapData.FindPathDataListenerCallback() {
                    @Override
                    public void onFindPathData(TMapPolyLine polyline) {
                        polyline.setLineWidth(15);
                        polyline.setLineColor(0xFF1E3A8A);
                        tMapView.addTMapPath(polyline);

                        // 출발 마커
                        TMapMarkerItem startMarker = new TMapMarkerItem();
                        startMarker.setTMapPoint(startPoint);
                        startMarker.setName("출발지");
                        //startMarker.setCalloutTitle("출발");
                        //Bitmap rawStart = BitmapFactory.decodeResource(getResources(), R.drawable.marker_start);
                        //Bitmap resizedStart = Bitmap.createScaledBitmap(rawStart, 64, 64, false);
                        //startMarker.setIcon(resizedStart);
                        startMarker.setCanShowCallout(true);
                        startMarker.setAutoCalloutVisible(true);
                        //tMapView.addMarkerItem("start", startMarker);

                        // 도착 마커
                        TMapMarkerItem endMarker = new TMapMarkerItem();
                        endMarker.setTMapPoint(endPoint);
                        endMarker.setName("도착지");
                        //endMarker.setCalloutTitle("도착");
                        //Bitmap rawEnd = BitmapFactory.decodeResource(getResources(), R.drawable.marker_end);
                        //Bitmap resizedEnd = Bitmap.createScaledBitmap(rawEnd, 64, 64, false);
                        //endMarker.setIcon(resizedEnd);
                        endMarker.setCanShowCallout(true);
                        endMarker.setAutoCalloutVisible(true);
                        //tMapView.addMarkerItem("end", endMarker);

                        // 경로 중간으로 중심 이동
                        List<TMapPoint> points = polyline.getLinePoint();
                        if (points != null && !points.isEmpty()) {
                            TMapPoint center = points.get(points.size() / 2);
                            runOnUiThread(() -> {
                                tMapView.setCenterPoint(center.getLongitude(), center.getLatitude());
                                tMapView.setZoomLevel(12);
                            });
                        }
                    }
                }
        );
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
