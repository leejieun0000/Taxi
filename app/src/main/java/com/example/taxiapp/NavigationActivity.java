package com.example.taxiapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NavigationActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private static final String BASE_URL = "https://fastapi-server-kleo.onrender.com";
    private static final String TAG = "NavigationActivity";

    private TMapView tMapView;
    private TMapPoint startPoint, endPoint, cityHallPoint;
    private TextView headerTextView;
    private Button btnNavi, btnStop;
    private LinearLayout arrivalOverlay;
    private TextView arrivalMessage;
    private Button arrivalActionButton;

    private String startName, endName, toStartDuration, toStartDistance, distanceText, fareText;
    private int duration;
    private double cityHallLat, cityHallLng;

    private Thread movementThread = null;
    private boolean isMoving = false;

    private HeatmapService heatmapService;
    private final List<String> heatmapKeys = new ArrayList<>();

    private boolean isAutoCentering = true;  // 자동차 중심 따라가기 여부

    private void setupMapTouchListener() {
        tMapView.setOnTouchListener((v, event) -> {
            isAutoCentering = false;  // 손으로 움직였을 때는 추적 중지
            return false;
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // --- Intent 에서 받은 데이터 파싱 ---
        Intent intent = getIntent();
        double startLat = intent.getDoubleExtra("startLat", 0);
        double startLng = intent.getDoubleExtra("startLng", 0);
        double endLat = intent.getDoubleExtra("endLat", 0);
        double endLng = intent.getDoubleExtra("endLng", 0);
        cityHallLat = intent.getDoubleExtra("cityHallLat", 35.5395);
        cityHallLng = intent.getDoubleExtra("cityHallLng", 129.311252);
        startName = intent.getStringExtra("startName");
        endName = intent.getStringExtra("endName");
        duration = intent.getIntExtra("duration", -1);
        distanceText = intent.getStringExtra("distanceText");
        toStartDuration = intent.getStringExtra("toStartDuration");
        toStartDistance = intent.getStringExtra("toStartDistance");
        fareText = intent.getStringExtra("fareText");

        startPoint = new TMapPoint(startLat, startLng);
        endPoint   = new TMapPoint(endLat, endLng);
        cityHallPoint = new TMapPoint(cityHallLat, cityHallLng);

        // --- 뷰 바인딩 및 초기 텍스트 설정 ---
        headerTextView = findViewById(R.id.map_header_text);
        updateHeaderText("울산광역시청", startName, toStartDuration, toStartDistance);

        arrivalOverlay       = findViewById(R.id.arrival_overlay);
        arrivalMessage       = findViewById(R.id.arrival_message);
        arrivalActionButton  = findViewById(R.id.arrival_action_button);
        arrivalOverlay.setVisibility(View.GONE);  // 처음엔 숨김

        // --- Retrofit + HeatmapService 초기화 ---
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        heatmapService = retrofit.create(HeatmapService.class);

        // --- 위치 권한 체크 및 맵 초기화 ---
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    REQUEST_PERMISSIONS_CODE);
        } else {
            initTMapBase(true);  // 권한 있으면 바로 맵 + heatmap 마커 로드
        }

        ImageButton gpsBtn = findViewById(R.id.btn_gps);
        gpsBtn.setOnClickListener(v -> {
            isAutoCentering = true;  // 다시 자동차 중심으로
            TMapMarkerItem movingMarker = tMapView.getMarkerItemFromID("moving");
            if (movingMarker != null) {
                TMapPoint current = movingMarker.getTMapPoint();
                tMapView.setCenterPoint(current.getLongitude(), current.getLatitude());
            }
        });


        // --- 네비게이션 시작 버튼 ---
        btnNavi = findViewById(R.id.btn_start_navi);
        btnNavi.setText("목적지로 안내");
        btnNavi.setOnClickListener(v -> {
            arrivalOverlay.setVisibility(View.GONE);
            initTMapBase(false);
        });

        // --- 네비게이션 중지 버튼 ---
        btnStop = findViewById(R.id.btn_stop_navi);
        btnStop.setVisibility(View.GONE);
        btnStop.setOnClickListener(v -> {
            stopMovement();
            Intent main = new Intent(NavigationActivity.this, MainActivity.class);
            main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            finish();
        });
    }

    private void initTMapBase(boolean toPickup) {
        stopMovement();

        FrameLayout mapContainer = findViewById(R.id.map_container);
        mapContainer.removeAllViews();

        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("3AVBuNVIlpv01yJMOIr68HuKEoaqyAH6aMpxIInh");
        tMapView.setIconVisibility(true);
        tMapView.setTrackingMode(false);
        tMapView.setSightVisible(false);
        tMapView.setCenterPoint(startPoint.getLongitude(), startPoint.getLatitude());
        tMapView.setZoomLevel(19);
        mapContainer.addView(tMapView);
        setupMapTouchListener(); // 👈 이 줄 추가

        ImageButton zoomIn = findViewById(R.id.btn_zoom_in);
        ImageButton zoomOut = findViewById(R.id.btn_zoom_out);
        zoomIn.setOnClickListener(v -> tMapView.MapZoomIn());
        zoomOut.setOnClickListener(v -> tMapView.MapZoomOut());

        // 0.5초 지연 후 경로 표시
        new android.os.Handler().postDelayed(() -> {
            if (toPickup) showRouteFromCityHall();
            else showRouteToDestination();
        }, 500);

        // heatmap 마커 로드
        fetchAndShowMarkers();
    }

    private void showRouteFromCityHall() {
        TMapData data = new TMapData();
        data.findPathDataWithType(TMapData.TMapPathType.CAR_PATH, cityHallPoint, startPoint, poly -> {
            runOnUiThread(() -> {
                poly.setLineWidth(30);
                poly.setLineColor(0xFF10B981);
                tMapView.addTMapPath(poly);
                simulateMovement(poly.getLinePoint(), true);
            });
        });
    }

    private void showRouteToDestination() {
        TMapData data = new TMapData();
        data.findPathDataWithType(TMapData.TMapPathType.CAR_PATH, startPoint, endPoint, poly -> {
            runOnUiThread(() -> {
                updateHeaderText(startName, endName, duration + "분", distanceText);
                btnNavi.setVisibility(View.GONE);
                btnStop.setVisibility(View.VISIBLE);
                poly.setLineWidth(30);
                poly.setLineColor(0xFF1E3A8A);
                tMapView.addTMapPath(poly);
                simulateMovement(poly.getLinePoint(), false);
            });
        });
    }

    private void simulateMovement(List<TMapPoint> pathPoints, boolean toPickup) {
        isMoving = true;
        movementThread = new Thread(() -> {
            Bitmap taxiIcon = BitmapFactory.decodeResource(getResources(), R.drawable.navitaxi);
            Bitmap scaled   = Bitmap.createScaledBitmap(taxiIcon, 100, 100, true);

            try {
                Thread.sleep(3000);  // 시작 전 준비 시간
            } catch (InterruptedException e) {
                return;
            }

            for (int i = 0; i < pathPoints.size() && isMoving; i++) {
                final TMapPoint pt = pathPoints.get(i);
                runOnUiThread(() -> {
                    tMapView.removeMarkerItem("moving");
                    TMapMarkerItem m = new TMapMarkerItem();
                    m.setTMapPoint(pt);
                    m.setIcon(scaled);
                    tMapView.addMarkerItem("moving", m);
                    if (isAutoCentering) {
                        tMapView.setCenterPoint(pt.getLongitude(), pt.getLatitude());
                    }

                });
                try {
                    Thread.sleep(i < 5 ? 1000 : 500);
                } catch (InterruptedException ignored) { return; }
            }

            runOnUiThread(() -> {
                arrivalOverlay.setVisibility(View.VISIBLE);
                if (toPickup) {
                    arrivalMessage.setText("출발지에 도착했습니다. 손님을 태워주세요!");
                    arrivalActionButton.setText("목적지로 안내");
                    arrivalActionButton.setOnClickListener(v -> {
                        arrivalOverlay.setVisibility(View.GONE);
                        initTMapBase(false);
                    });
                } else {
                    arrivalMessage.setText("목적지에 도착했습니다!");
                    arrivalActionButton.setText("안내 종료");
                    arrivalActionButton.setOnClickListener(v -> {
                        stopMovement();
                        Intent main = new Intent(NavigationActivity.this, MainActivity.class);
                        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(main);
                        finish();
                    });
                }
            });
        });
        movementThread.start();
    }

    private void stopMovement() {
        isMoving = false;
        if (movementThread != null && movementThread.isAlive()) {
            movementThread.interrupt();
        }
    }

    // NavigationActivity.java

    private void fetchAndShowMarkers() {
        if (heatmapService == null) {
            Log.e(TAG, "heatmapService is null! Retrofit 초기화 확인");
            return;
        }

        heatmapService.getHeatmap().enqueue(new Callback<HeatmapResponse>() {
            @Override
            public void onResponse(Call<HeatmapResponse> call, Response<HeatmapResponse> resp) {
                if (!resp.isSuccessful() || resp.body() == null) {
                    Log.e(TAG, "HeatmapResponse error: " + resp.code());
                    return;
                }
                List<Prediction> list = resp.body().getPredictions();

                // 기존 heatmap 마커 제거
                for (String key : heatmapKeys) {
                    tMapView.removeMarkerItem(key);
                }
                heatmapKeys.clear();

                // 모든 예측 위치에 빨간색 마커만 표시
                int idx = 0;
                for (Prediction p : list) {
                    TMapMarkerItem m = new TMapMarkerItem();
                    m.setTMapPoint(new TMapPoint(p.getLat(), p.getLon()));
                    m.setVisible(TMapMarkerItem.VISIBLE);
                    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.marker_red);
                    m.setIcon(icon);
                    m.setPosition(0.5f, 1.0f);

                    String key = "heat_" + (++idx);
                    tMapView.addMarkerItem(key, m);
                    heatmapKeys.add(key);
                    Log.d(TAG, "addHeatmapMarker key=" + key + " at " + p.getLat() + "," + p.getLon());
                }
            }

            @Override
            public void onFailure(Call<HeatmapResponse> call, Throwable t) {
                Log.e(TAG, "fetchAndShowMarkers onFailure: " + t.getMessage());
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms, @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == REQUEST_PERMISSIONS_CODE) {
            boolean ok = true;
            for (int r : results) if (r != PackageManager.PERMISSION_GRANTED) { ok = false; break; }
            if (ok) initTMapBase(true);
        }
    }

    private void updateHeaderText(String from, String to, String dur, String dist) {
        headerTextView.setText(String.format("📍 %s → %s (예상 %s, %s)", from, to, dur, dist));
    }
}
