package com.example.taxiapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CallAlertActivity extends AppCompatActivity {

    private Button btnDecline;
    private Button btnAccept;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_alert);

        btnDecline = findViewById(R.id.btn_decline);
        btnAccept = findViewById(R.id.btn_accept);

        btnDecline.setOnClickListener(v -> {
            Toast.makeText(this, "호출을 거절했습니다.", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnAccept.setOnClickListener(v -> {
            Toast.makeText(this, "수락하고 내비게이션 시작!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CallAlertActivity.this, MapActivity.class);
            intent.putExtra("mode", "CALL");
            startActivity(intent);
            finish();
        });
    }
}
