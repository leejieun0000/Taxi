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
            {"해운대", "광안리", "20분"},
            {"부산 롯데월드", "아쿠아리움", "7분"},
            {"서면역", "부산역", "15분"}
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

            Toast.makeText(this, "내비게이션으로 이동합니다!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CallAlertActivity.this, MapActivity.class);

            intent.putExtra("mode", "CALL");
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
