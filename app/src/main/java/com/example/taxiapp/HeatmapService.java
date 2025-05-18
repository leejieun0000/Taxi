package com.example.taxiapp;

import retrofit2.Call;
import retrofit2.http.GET;

public interface HeatmapService {
    @GET("heatmap")
    Call<HeatmapResponse> getHeatmap();
}
