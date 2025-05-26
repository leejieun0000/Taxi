package com.example.taxiapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CallAlertActivity extends AppCompatActivity {

    private final String[][] callList = {
            //현재 위치는 울산광역시청 별관 앞  (35.53833, 129.31139)
            {"울산공업고등학교", "울산문화예술회관", "3분", "867m", "4100원", "35.542948", "129.31650", "35.542928", "129.327312", "8분", "1.9km"},
            {"굿모닝병원", "롯데백화점 울산점", "6분", "1.4km", "4000원", "35.535069", "129.319915", "35.538581", "129.338172", "6분", "1.9km"},
            {"농협 우정지점", "메가박스", "7분", "1.9km", "4200원", "35.554932", "129.309627", "35.554367", "129.321095", "6분", "2km"}

    };

    private int currentCallIndex = 0;

    private TextView departureValue;
    private TextView destinationValue;
    private TextView timeValue;

    private Button btnDecline;
    private Button btnAccept;
    private ImageButton btnBackHome;
    private TextView pickupInfoValue;  // 울산시청 → 출발지 시간 및 거리




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_alert);

        departureValue = findViewById(R.id.text_departure_value);
        destinationValue = findViewById(R.id.text_destination_value);
        timeValue = findViewById(R.id.text_time_value);
        btnDecline = findViewById(R.id.btn_decline);
        btnAccept = findViewById(R.id.btn_accept);
        btnBackHome = findViewById(R.id.btn_back_home);
        pickupInfoValue = findViewById(R.id.text_pickup_info_value);


        updateCallInfo(); // 첫 호출 정보 표시

        btnDecline.setOnClickListener(v -> {
            currentCallIndex++;
            if (currentCallIndex < callList.length) {
                updateCallInfo();
            } else {
                Toast.makeText(this, "모든 호출을 확인했습니다.", Toast.LENGTH_SHORT).show();
                finish(); // 끝나면 종료
            }
        });

        btnAccept.setOnClickListener(v -> {
            String[] call = callList[currentCallIndex];

            // 📌 각 항목 꺼내기
            String startName = call[0];
            String endName = call[1];
            String durationText = call[2];
            int duration = Integer.parseInt(durationText.replace("분", ""));
            String distanceText = call[3];
            String fareText = call[4]; // 💰 요금 정보 (예: "6800원")


            double startLat = Double.parseDouble(call[5]);
            double startLng = Double.parseDouble(call[6]);
            double endLat = Double.parseDouble(call[7]);
            double endLng = Double.parseDouble(call[8]);
            String toStartDuration = call[9];
            String toStartDistance = call[10];

            // ✅ Intent에 값 넣기
            Intent intent = new Intent(CallAlertActivity.this, NavigationActivity.class);
            intent.putExtra("startName", startName);
            intent.putExtra("endName", endName);
            intent.putExtra("duration", duration);
            intent.putExtra("distanceText", distanceText);
            intent.putExtra("fareText", fareText);

            intent.putExtra("startLat", startLat);
            intent.putExtra("startLng", startLng);
            intent.putExtra("endLat", endLat);
            intent.putExtra("endLng", endLng);
            intent.putExtra("toStartDuration", toStartDuration);
            intent.putExtra("toStartDistance", toStartDistance);

            // ✅ 울산광역시청 좌표 추가
            intent.putExtra("cityHallLat", 35.53833);
            intent.putExtra("cityHallLng", 129.31139);

            Toast.makeText(this, "내비게이션으로 이동합니다!", Toast.LENGTH_SHORT).show();

            startActivity(intent);
            finish();
        });


        btnBackHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
    }

    private void updateCallInfo() {
        String[] call = callList[currentCallIndex];

        departureValue.setText(call[0]);
        destinationValue.setText(call[1]);

        // 🔶 출발지 → 도착지까지 예상 시간 + 거리 + 요금
        String formatted = call[2] + " / " + call[3] + " / " + call[4];
        timeValue.setText(formatted);

        // 🔷출발지까지 시간 + 거리
        String pickupInfo = "출발지까지 " + call[9] + " / " + call[10];
        pickupInfoValue.setText(pickupInfo);
    }

}
