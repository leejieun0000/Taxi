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
            {"해운대", "광안리", "18분", "6.3km", "35.1587", "129.1604", "35.1531", "129.1185"},
            {"부산 롯데월드", "아쿠아리움", "20분", "7.9km", "35.2310", "129.0832", "35.2325", "129.0840"},
            {"서면역", "부산역", "14분", "5.3km", "35.1575", "129.0591", "35.1151", "129.0421"}
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

            double startLat = Double.parseDouble(call[4]);
            double startLng = Double.parseDouble(call[5]);
            double endLat = Double.parseDouble(call[6]);
            double endLng = Double.parseDouble(call[7]);

            // ✅ Intent에 값 넣기
            Intent intent = new Intent(CallAlertActivity.this, NavigationActivity.class);
            intent.putExtra("startName", startName);
            intent.putExtra("endName", endName);
            intent.putExtra("duration", duration);
            intent.putExtra("startLat", startLat);
            intent.putExtra("startLng", startLng);
            intent.putExtra("endLat", endLat);
            intent.putExtra("endLng", endLng);

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
