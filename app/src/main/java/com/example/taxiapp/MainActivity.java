package com.example.taxiapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnCallReceive;
    private Button btnRecommendSpot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // layout 파일: activity_main.xml

        btnCallReceive = findViewById(R.id.btnCallReceive);
        btnRecommendSpot = findViewById(R.id.btnRecommendSpot);

        btnCallReceive.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CallAlertActivity.class);
            startActivity(intent);
        });

        btnRecommendSpot.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            intent.putExtra("mode", "WAIT");  // 대기 장소 추천 모드
            startActivity(intent);
        });
    }
}
