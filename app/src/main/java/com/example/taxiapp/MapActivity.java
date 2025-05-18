package com.example.taxiapp;

import java.util.List;
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

    private static final int REQUEST_PERMISSIONS_CODE = 1;
    private static final String BASE_URL = "http://10.0.2.2:8000/";
    private TMapView tMapView;
    private String mode;
    private HeatmapService heatmapService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

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
        tMapView.setTrackingMode(false);
        tMapView.setSightVisible(true);
        tMapView.setCenterPoint(129.075642, 35.179554);

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
                Log.d("HeatmapTest", "▶ onResponse() HTTP 코드=" + response.code());

                if (!response.isSuccessful() || response.body() == null) {
                    Log.w("HeatmapTest", "   응답 실패 or body null. isSuccessful="
                            + response.isSuccessful()
                            + ", body=" + response.body());
                    return;
                }

                HeatmapResponse body = response.body();
                List<Prediction> list = body.getPredictions();
                Log.d("HeatmapTest", "▶ 예측 개수=" + list.size());

                // 기존 마커 전부 제거
                tMapView.removeAllMarkerItem();
                Log.d("HeatmapTest", "   removeAllMarkerItem() 호출됨");

                int idx = 0;
                for (Prediction p : list) {
                    double lat = p.getLat();
                    double lon = p.getLon();
                    Log.d("HeatmapTest", String.format(
                            "▶ [%d] 좌표=(%.6f, %.6f), demand=%d",
                            ++idx, lat, lon, p.getDemand()));

                    // 1) 마커 아이템 생성
                    TMapMarkerItem marker = new TMapMarkerItem();
                    Log.d("HeatmapTest", "   TMapMarkerItem 생성됨");

                    // 2) 위치 설정
                    marker.setTMapPoint(new TMapPoint(lat, lon));
                    marker.setName("대기 장소 " + idx);
                    marker.setVisible(TMapMarkerItem.VISIBLE);
                    Log.d("HeatmapTest", "   setTMapPoint(), setName(), setVisible() 완료");

                    // 3) 아이콘 디코딩
                    Bitmap icon = BitmapFactory.decodeResource(
                            getResources(),
                            R.drawable.marker_red  // 실제 사용 중인 리소스 이름
                    );
                    if (icon == null) {
                        Log.e("HeatmapTest", "   BitmapFactory.decodeResource() 가 null 반환!");
                    } else {
                        Log.d("HeatmapTest", "   아이콘 디코딩 완료 (w="
                                + icon.getWidth() + ", h=" + icon.getHeight() + ")");
                    }

                    // 4) 아이콘 설정
                    marker.setIcon(icon);
                    Log.d("HeatmapTest", "   marker.setIcon() 호출됨");

                    // 5) 맵에 추가
                    String key = "marker" + idx;
                    tMapView.addMarkerItem(key, marker);
                    Log.d("HeatmapTest", "   addMarkerItem() 호출됨, key=" + key);
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
