package com.example.taxiapp;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.skt.Tmap.TMapView;
import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapMarkerItem;

import com.example.taxiapp.HeatmapService;
import com.example.taxiapp.HeatmapResponse;
import com.example.taxiapp.Prediction;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = "MapActivity";
    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private static final String BASE_URL = "https://fastapi-server-kleo.onrender.com";
    private TMapView tMapView;
    private String mode;
    private HeatmapService heatmapService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mode = getIntent().getStringExtra("mode");
        TextView header = findViewById(R.id.map_header_text);
        header.setText("CALL".equals(mode) ? "콜 수신 : 수요 지역 안내 중" : "대기 장소 추천");

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
        tMapView.setTrackingMode(false);
        tMapView.setSightVisible(true);
        tMapView.setCenterPoint(129.311348, 35.538485);

        mapContainer.addView(tMapView);

        // Retrofit 인스턴스 생성
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        heatmapService = retrofit.create(HeatmapService.class);

        // 대기 장소 추천 모드일 때만 마커 로드
        if ("WAIT".equals(mode)) {
            fetchAndShowMarkers();
        }

        ImageButton zoomIn = findViewById(R.id.btn_zoom_in);
        ImageButton zoomOut = findViewById(R.id.btn_zoom_out);
        ImageButton backHome = findViewById(R.id.btn_back_home);

        zoomIn.setOnClickListener(v -> tMapView.MapZoomIn());
        zoomOut.setOnClickListener(v -> tMapView.MapZoomOut());
        backHome.setOnClickListener(v -> finish());
    }

    private void fetchAndShowMarkers() {
        Log.d("HeatmapTest", "▶ fetchAndShowMarkers() 호출, mode=" + mode);
        heatmapService.getHeatmap().enqueue(new Callback<HeatmapResponse>() {
            @Override
            public void onResponse(Call<HeatmapResponse> call, Response<HeatmapResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "HeatmapResponse error: " + response.code());
                    return;
                }
                List<Prediction> list = response.body().getPredictions();
                Log.d(TAG, "▶ 예측 개수=" + list.size());

                // 기존 마커 전부 제거
                tMapView.removeAllMarkerItem();
                Log.d(TAG, "   removeAllMarkerItem() 호출됨");

                // 1) 수요량만 추출해 내림차순 정렬 → 상위 30% 빨강, 다음 30% 주황 임계값 구하기
                List<Integer> demands = new ArrayList<>();
                for (Prediction pTemp : list) {
                    demands.add(pTemp.getDemand());
                }
                Collections.sort(demands, Collections.reverseOrder());
                int size = demands.size();
                int redCount    = (int) Math.ceil(size * 0.3f);
                int orangeCount = (int) Math.ceil(size * 0.6f);
                final int redThreshold    = demands.get(Math.min(redCount - 1, size - 1));
                final int orangeThreshold = demands.get(Math.min(orangeCount - 1, size - 1));
                Log.d(TAG, "   redThreshold=" + redThreshold + ", orangeThreshold=" + orangeThreshold);

                // 2) 마커 생성 및 아이콘 분기
                int idx = 0;
                for (Prediction p : list) {
                    double lat    = p.getLat();
                    double lon    = p.getLon();
                    int    demand = p.getDemand();
                    Log.d(TAG, String.format("▶ [%d] 좌표=(%.6f, %.6f), demand=%d",
                            ++idx, lat, lon, demand));

                    // 위치 설정
                    TMapMarkerItem marker = new TMapMarkerItem();
                    marker.setTMapPoint(new TMapPoint(lat, lon));
                    marker.setVisible(TMapMarkerItem.VISIBLE);

                    // 수요량에 따른 아이콘 분기
                    Bitmap icon;
                    if (demand >= redThreshold) {
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.marker_red);
                    } else if (demand >= orangeThreshold) {
                        icon = BitmapFactory.decodeResource(getResources(), R.drawable.marker_orange);
                    } else {
                        // 하위 40%는 표시하지 않음
                        continue;
                    }

                    if (icon == null) {
                        Log.e(TAG, "   BitmapFactory.decodeResource() returned null");
                        continue;
                    }

                    // 마커에 아이콘 설정 및 추가
                    marker.setIcon(icon);
                    marker.setPosition(0.5f, 1.0f);
                    tMapView.addMarkerItem("marker_" + idx, marker);
                    Log.d(TAG, "   addMarkerItem() key=marker_" + idx);
                }
            }
            @Override
            public void onFailure(Call<HeatmapResponse> call, Throwable t) {
                Log.e("HeatmapTest", "▶ onFailure(): " + t.getMessage(), t);
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
