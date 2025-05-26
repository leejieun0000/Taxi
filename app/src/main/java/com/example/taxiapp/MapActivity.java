package com.example.taxiapp;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.skt.Tmap.TMapPoint;
import com.skt.Tmap.TMapView;
import com.skt.Tmap.TMapMarkerItem;

import com.example.taxiapp.HeatmapService;
import com.example.taxiapp.HeatmapResponse;
import com.example.taxiapp.Prediction;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = "MapActivity";

    // 울산광역시청 좌표
    private static final double CITY_HALL_LAT = 35.53833;
    private static final double CITY_HALL_LON = 129.31139;

    private static final String BASE_URL = "https://fastapi-server-kleo.onrender.com";

    private TMapView tMapView;
    private HeatmapService heatmapService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initTMap();

        // GPS 버튼 (항상 울산광역시청으로 중앙 이동)
        ImageButton gpsBtn = findViewById(R.id.btn_gps);
        gpsBtn.setOnClickListener(v -> {
            tMapView.setCenterPoint(CITY_HALL_LON, CITY_HALL_LAT);
        });

        // 줌 버튼
        ImageButton zoomIn = findViewById(R.id.btn_zoom_in);
        ImageButton zoomOut = findViewById(R.id.btn_zoom_out);
        zoomIn.setOnClickListener(v -> tMapView.MapZoomIn());
        zoomOut.setOnClickListener(v -> tMapView.MapZoomOut());
    }

    private void initTMap() {
        // 1) Map 뷰 세팅
        FrameLayout mapContainer = findViewById(R.id.map_container);
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey("3AVBuNVIlpv01yJMOIr68HuKEoaqyAH6aMpxIInh");

        // 2) “현재 위치” 표시(파란 점)
        tMapView.setLocationPoint(CITY_HALL_LON, CITY_HALL_LAT);
        tMapView.setIconVisibility(true);

        // 3) 지도 초기 중앙을 울산광역시청으로
        tMapView.setCenterPoint(CITY_HALL_LON, CITY_HALL_LAT);
        tMapView.setZoomLevel(15);

        mapContainer.addView(tMapView);

        // 4) Retrofit 초기화
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        heatmapService = retrofit.create(HeatmapService.class);

        // 5) 모드가 "WAIT"일 때만 heatmap 마커 로드
        String mode = getIntent().getStringExtra("mode");
        if ("WAIT".equals(mode)) {
            fetchAndShowMarkers();
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

                // 1) 기존 마커 전부 제거 (파란 점도 removeAllMarkerItem에 포함되므로, 다시 파란 점만 표시)
                tMapView.removeAllMarkerItem();

                // 2) 파란 점(현재 위치) 다시 표시
                tMapView.setLocationPoint(CITY_HALL_LON, CITY_HALL_LAT);
                tMapView.setIconVisibility(true);
                tMapView.setCenterPoint(CITY_HALL_LON, CITY_HALL_LAT);

                // 3) JSON 예측 데이터에서 모든 좌표에 빨간 마커 추가
                List<Prediction> list = response.body().getPredictions();
                int idx = 0;
                for (Prediction p : list) {
                    TMapMarkerItem marker = new TMapMarkerItem();
                    marker.setTMapPoint(new TMapPoint(p.getLat(), p.getLon()));
                    marker.setVisible(TMapMarkerItem.VISIBLE);
                    marker.setIcon(BitmapFactory.decodeResource(
                            getResources(), R.drawable.marker_red));
                    marker.setPosition(0.5f, 1.0f);
                    String key = "marker_" + (++idx);
                    tMapView.addMarkerItem(key, marker);
                }
            }

            @Override
            public void onFailure(Call<HeatmapResponse> call, Throwable t) {
                Log.e(TAG, "fetchAndShowMarkers onFailure: " + t.getMessage(), t);
            }
        });
    }
}
