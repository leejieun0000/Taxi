package com.example.taxiapp;

import java.util.List;

public class HeatmapResponse {
    private String status;
    private List<Prediction> predictions;

    // 필수: public getter
    public String getStatus() {
        return status;
    }

    public List<Prediction> getPredictions() {
        return predictions;
    }
}
