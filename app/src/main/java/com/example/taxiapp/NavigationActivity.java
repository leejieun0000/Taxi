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
import android.widget.ImageButton;
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
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.example.taxiapp.HeatmapService;
import com.example.taxiapp.HeatmapResponse;
import com.example.taxiapp.Prediction;

public class NavigationActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private TMapView tMapView;
    private TMapPoint startPoint, endPoint, cityHallPoint;

    private TextView headerTextView;
    private Button btnNavi;
    private Button btnStop;

    private String startName, endName, toStartDuration, toStartDistance, distanceText, fareText;
    private int duration;
    private double cityHallLat, cityHallLng;

    private Thread movementThread = null;
    private boolean isMoving = false;

    private LinearLayout arrivalOverlay;
    private TextView arrivalMessage;
    private Button arrivalActionButton;

    private static final String BASE_URL = "http://10.0.2.2:8000/";
    private static final String TAG      = "NavigationActivity";
    private HeatmapService   heatmapService;
    private final List<String> heatmapKeys = new ArrayList<>();
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
        cityHallLat = intent.getDoubleExtra("cityHallLat", 35.1798);
        cityHallLng = intent.getDoubleExtra("cityHallLng", 129.076);
        startName = intent.getStringExtra("startName");
        endName = intent.getStringExtra("endName");
        duration = intent.getIntExtra("duration", -1);
        distanceText = intent.getStringExtra("distanceText");
        toStartDuration = intent.getStringExtra("toStartDuration");
        toStartDistance = intent.getStringExtra("toStartDistance");
        fareText = intent.getStringExtra("fareText");


        startPoint = new TMapPoint(startLat, startLng);
        endPoint = new TMapPoint(endLat, endLng);
        cityHallPoint = new TMapPoint(cityHallLat, cityHallLng);

        headerTextView = findViewById(R.id.map_header_text);
        updateHeaderText("시청역 5번 출구", startName, toStartDuration, toStartDistance);

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
            initTMapBase(true);  // 처음 진입: 시청역 5번 출구 → 출발지 경로
        }

        btnNavi = findViewById(R.id.btn_start_navi);
        btnNavi.setText("목적지로 안내");
        btnNavi.setOnClickListener(v -> {
            // 👉 도착 메시지가 떠 있으면 숨기기
            if (arrivalOverlay != null) {
                arrivalOverlay.setVisibility(View.GONE);
            }

            // 👉 안내 시작: 출발지 → 도착지
            initTMapBase(false);
        });


        btnStop = findViewById(R.id.btn_stop_navi); // 레이아웃에 미리 추가된 버튼 연결
        btnStop.setVisibility(View.GONE); // 처음엔 숨김
        btnStop.setOnClickListener(v -> {
            stopMovement();  // 🔴 마커 스레드 종료
            Intent intent2 = new Intent(NavigationActivity.this, MainActivity.class);
            intent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  // 스택 초기화
            startActivity(intent2);
            finish(); // 현재 화면 종료
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        heatmapService = retrofit.create(HeatmapService.class);
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
        tMapView.setZoomLevel(19);

        ImageButton zoomIn = findViewById(R.id.btn_zoom_in);
        ImageButton zoomOut = findViewById(R.id.btn_zoom_out);

        zoomIn.setOnClickListener(v -> tMapView.MapZoomIn());
        zoomOut.setOnClickListener(v -> tMapView.MapZoomOut());


        if (shouldShowRoute) {
            new android.os.Handler().postDelayed(this::showRoute, 500);
        } else {
            new android.os.Handler().postDelayed(this::showRoute2, 500);
        }

        fetchAndShowMarkers();
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

            try {
                // 🔹 1. 시작 전 2초 대기
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                return;
            }

            for (int i = 0; i < pathPoints.size(); i++) {
                if (!isMoving || tMapView == null) return;

                final TMapPoint point = pathPoints.get(i);
                runOnUiThread(() -> {
                    if (tMapView == null) return;
                    TMapMarkerItem movingMarker = new TMapMarkerItem();
                    movingMarker.setTMapPoint(point);
                    movingMarker.setIcon(scaled);
                    movingMarker.setName("택시 이동 중");

                    tMapView.removeMarkerItem("moving");
                    tMapView.addMarkerItem("moving", movingMarker);

                    tMapView.setCenterPoint(point.getLongitude(), point.getLatitude());
                    //tMapView.setZoomLevel(19);
                });

                try {
                    // 🔹 2. 처음 몇 지점(예: 5개)은 천천히 이동
                    if (i < 5) {
                        Thread.sleep(1000); // 느리게
                    } else {
                        Thread.sleep(500); // 원래 속도
                    }
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
    private void fetchAndShowMarkers() {
        heatmapService.getHeatmap().enqueue(new Callback<HeatmapResponse>() {
            @Override
            public void onResponse(Call<HeatmapResponse> call, Response<HeatmapResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "HeatmapResponse error: " + response.code());
                    return;
                }
                List<Prediction> list = response.body().getPredictions();
                // 기존 마커 클리어
                for (String key : heatmapKeys) {
                    tMapView.removeMarkerItem(key);
                }
                heatmapKeys.clear();

                // 1) demand 값만 뽑아 내림차순 정렬
                List<Integer> demands = new ArrayList<>();
                for (Prediction p : list) {
                    demands.add(p.getDemand());
                }
                Collections.sort(demands, Collections.reverseOrder());
                int size = demands.size();
                int redCount    = (int) Math.ceil(size * 0.3f);
                int orangeCount = (int) Math.ceil(size * 0.6f);
                int redThreshold    = demands.get(Math.min(redCount - 1, size - 1));
                int orangeThreshold = demands.get(Math.min(orangeCount - 1, size - 1));

                // 2) 마커 추가
                int idx = 0;
                for (Prediction p : list) {
                    double lat = p.getLat();
                    double lon = p.getLon();
                    int demand = p.getDemand();

                    // 1) demand에 따라 아이콘 선택
                    Bitmap icon;
                    if (demand >= redThreshold) {
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.marker_red);
                    } else if (demand >= orangeThreshold) {
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.marker_orange);
                    } else {
                        // 하위 40%는 건너뜀
                        continue;
                    }

                    // 2) 리소스가 null인지 확인
                    if (icon == null) {
                        Log.e(TAG, "Marker icon decode failed for demand=" + demand);
                        continue;
                    }

                    // 3) 마커 키 생성 및 설정
                    String key = "heatmap_" + (idx++);
                    TMapMarkerItem marker = new TMapMarkerItem();
                    marker.setTMapPoint(new TMapPoint(lat, lon));
                    marker.setIcon(icon);
                    marker.setPosition(0.5f, 1.0f);
                    marker.setVisible(TMapMarkerItem.VISIBLE);

                    // 4) 맵에 추가 및 키 저장
                    tMapView.addMarkerItem(key, marker);
                    heatmapKeys.add(key);
                }
            }

            @Override
            public void onFailure(Call<HeatmapResponse> call, Throwable t) {
                Log.e(TAG, "fetchAndShowMarkers onFailure: " + t.getMessage());
            }
        });
    }

}
