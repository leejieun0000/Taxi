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
            //현재 위치는 부산시청임
            {"시청역 1번 출구", "온천천시민공원", "16분", "3.3km", "35.1798", "129.0766", "35.1923", "129.0880", "3분", "497m"},
            {"이마트 연제점", "해운대 해수욕장", "33분", "8.2km", "35.1865", "129.0797", "35.1587", "129.1604", "3분", "497m" },
            {"부산은행 거제동지점", "부산교육대학교", "9분", "1.7km", "35.187203", "129.078647", "35.1881", "129.0799", "3분", "497m"}
    };

    private int currentCallIndex = 0;

    private TextView departureValue;
    private TextView destinationValue;
    private TextView timeValue;

    private Button btnDecline;
    private Button btnAccept;
    private ImageButton btnBackHome;



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

            double startLat = Double.parseDouble(call[4]);
            double startLng = Double.parseDouble(call[5]);
            double endLat = Double.parseDouble(call[6]);
            double endLng = Double.parseDouble(call[7]);
            String toStartDuration = call[8];
            String toStartDistance = call[9];

            // ✅ Intent에 값 넣기
            Intent intent = new Intent(CallAlertActivity.this, NavigationActivity.class);
            intent.putExtra("startName", startName);
            intent.putExtra("endName", endName);
            intent.putExtra("duration", duration);
            intent.putExtra("distanceText", distanceText);
            intent.putExtra("startLat", startLat);
            intent.putExtra("startLng", startLng);
            intent.putExtra("endLat", endLat);
            intent.putExtra("endLng", endLng);
            intent.putExtra("toStartDuration", toStartDuration);
            intent.putExtra("toStartDistance", toStartDistance);

            // ✅ 부산시청 좌표 추가
            intent.putExtra("cityHallLat", 35.179554);
            intent.putExtra("cityHallLng", 129.075642);

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
        timeValue.setText(call[2]);
    }
}
