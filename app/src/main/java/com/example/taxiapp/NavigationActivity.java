package com.example.taxiapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
    private TMapPoint startPoint, endPoint, cityHallPoint;

    private TextView headerTextView;
    private Button btnNavi;
    private Button btnStop;

    private String startName, endName, toStartDuration, toStartDistance, distanceText;
    private int duration;
    private double cityHallLat, cityHallLng;

    private Thread movementThread = null;
    private boolean isMoving = false;

    private LinearLayout arrivalOverlay;
    private TextView arrivalMessage;
    private Button arrivalActionButton;


    private void updateHeaderText(String from, String to, String duration, String distance) {
        headerTextView.setText(String.format("📍 %s → %s (예상 %s, %s)", from, to, duration, distance));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        Intent intent = getIntent();
        double startLat = intent.getDoubleExtra("startLat", 0);
        double startLng = intent.getDoubleExtra("startLng", 0);
        double endLat = intent.getDoubleExtra("endLat", 0);
        double endLng = intent.getDoubleExtra("endLng", 0);
        cityHallLat = intent.getDoubleExtra("cityHallLat", 35.179554);
        cityHallLng = intent.getDoubleExtra("cityHallLng", 129.075642);
        startName = intent.getStringExtra("startName");
        endName = intent.getStringExtra("endName");
        duration = intent.getIntExtra("duration", -1);
        distanceText = intent.getStringExtra("distanceText");
        toStartDuration = intent.getStringExtra("toStartDuration");
        toStartDistance = intent.getStringExtra("toStartDistance");

        startPoint = new TMapPoint(startLat, startLng);
        endPoint = new TMapPoint(endLat, endLng);
        cityHallPoint = new TMapPoint(cityHallLat, cityHallLng);

        headerTextView = findViewById(R.id.map_header_text);
        updateHeaderText("부산시청", startName, toStartDuration, toStartDistance);

        arrivalOverlay = findViewById(R.id.arrival_overlay);
        arrivalMessage = findViewById(R.id.arrival_message);
        arrivalActionButton = findViewById(R.id.arrival_action_button);
        arrivalOverlay.setVisibility(View.GONE); // 처음에는 숨김


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_CODE);
        } else {
            initTMapBase(true);  // 처음 진입: 부산시청 → 출발지 경로
        }

        btnNavi = findViewById(R.id.btn_start_navi);
        btnNavi.setText("목적지로 안내");
        btnNavi.setOnClickListener(v -> initTMapBase(false));  // 안내 시작: 출발지 → 도착지

        btnStop = findViewById(R.id.btn_stop_navi); // 레이아웃에 미리 추가된 버튼 연결
        btnStop.setVisibility(View.GONE); // 처음엔 숨김
        btnStop.setOnClickListener(v -> {
            stopMovement();  // 🔴 마커 스레드 종료
            Intent intent2 = new Intent(NavigationActivity.this, MainActivity.class);
            intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  // 스택 초기화
            startActivity(intent2);
            finish(); // 현재 화면 종료
        });
    }

    private void initTMapBase(boolean shouldShowRoute) {
        stopMovement();  // 이전 스레드 정지
        FrameLayout mapContainer = findViewById(R.id.map_container);
        mapContainer.removeAllViews();

        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("3AVBuNVIlpv01yJMOIr68HuKEoaqyAH6aMpxIInh");

        tMapView.setIconVisibility(true);
        tMapView.setTrackingMode(false);
        tMapView.setSightVisible(false);
        tMapView.setCenterPoint(startPoint.getLongitude(), startPoint.getLatitude());

        mapContainer.addView(tMapView);

        if (shouldShowRoute) {
            new android.os.Handler().postDelayed(this::showRoute, 500);
        } else {
            new android.os.Handler().postDelayed(this::showRoute2, 500);
        }
    }

    private void stopMovement() {
        isMoving = false;
        if (movementThread != null && movementThread.isAlive()) {
            movementThread.interrupt();
        }
    }

    private void simulateMovement(List<TMapPoint> pathPoints, boolean isToPickup) {
        isMoving = true;
        movementThread = new Thread(() -> {
            Bitmap original = BitmapFactory.decodeResource(getResources(), R.drawable.navitaxi);
            Bitmap scaled = Bitmap.createScaledBitmap(original, 100, 100, true);

            for (TMapPoint point : pathPoints) {
                if (!isMoving || tMapView == null) return;

                runOnUiThread(() -> {
                    if (tMapView == null) return;
                    TMapMarkerItem movingMarker = new TMapMarkerItem();
                    movingMarker.setTMapPoint(point);
                    movingMarker.setIcon(scaled);
                    movingMarker.setName("택시 이동 중");

                    tMapView.removeMarkerItem("moving");
                    tMapView.addMarkerItem("moving", movingMarker);

                    tMapView.setCenterPoint(point.getLongitude(), point.getLatitude());
                    tMapView.setZoomLevel(19);
                });

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }

            runOnUiThread(() -> {
                if (isToPickup) {
                    arrivalMessage.setText("출발지에 도착했습니다. 손님을 태워주세요!");
                    arrivalActionButton.setText("목적지로 안내");
                    arrivalActionButton.setOnClickListener(v -> {
                        arrivalOverlay.setVisibility(View.GONE);
                        initTMapBase(false); // 목적지 안내 시작
                    });
                } else {
                    arrivalMessage.setText("목적지에 도착했습니다!");
                    arrivalActionButton.setText("안내 종료");
                    arrivalActionButton.setOnClickListener(v -> {
                        stopMovement();
                        Intent intent = new Intent(NavigationActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    });
                }
                arrivalOverlay.setVisibility(View.VISIBLE);
            });

        });
        movementThread.start();
    }

    private void showRoute() {
        if (tMapView == null) return;

        TMapData tMapData = new TMapData();
        tMapData.findPathDataWithType(
                TMapData.TMapPathType.CAR_PATH,
                cityHallPoint,
                startPoint,
                polyline -> runOnUiThread(() -> {
                    polyline.setLineWidth(30);
                    polyline.setLineColor(0xFF10B981);
                    tMapView.addTMapPath(polyline);

                    TextView directionBanner = findViewById(R.id.text_direction_banner);
                    directionBanner.setText("➡ 300m 직진 후 우회전");

                    tMapView.setTrackingMode(false);
                    tMapView.setSightVisible(false);
                    tMapView.setCompassMode(false);
                    tMapView.setZoomLevel(18);

                    simulateMovement(polyline.getLinePoint(), true);
                })
        );
    }

    private void showRoute2() {
        if (tMapView == null) return;

        TMapData tMapData = new TMapData();
        tMapData.findPathDataWithType(
                TMapData.TMapPathType.CAR_PATH,
                startPoint,
                endPoint,
                polyline2 -> runOnUiThread(() -> {
                    updateHeaderText(startName, endName, duration + "분", distanceText);
                    btnNavi.setVisibility(View.GONE);
                    btnStop.setVisibility(View.VISIBLE);  // 안내종료 버튼 보임 ✅

                    polyline2.setLineWidth(30);
                    polyline2.setLineColor(0xFF1E3A8A);
                    tMapView.addTMapPath(polyline2);

                    TextView directionBanner = findViewById(R.id.text_direction_banner);
                    directionBanner.setText("➡ 300m 직진 후 우회전");

                    tMapView.setTrackingMode(false);
                    tMapView.setSightVisible(false);
                    tMapView.setCompassMode(false);
                    tMapView.setZoomLevel(18);

                    simulateMovement(polyline2.getLinePoint(), false);
                })
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
                initTMapBase(true);
            }
        }
    }
}
