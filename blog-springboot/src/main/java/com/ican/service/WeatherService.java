package com.ican.service;

import com.ican.model.dto.WeatherData;

import java.util.List;

public interface WeatherService {
    // IP定位获取城市
    WeatherData.CityInfo getLocationByIp();

    // 搜索城市
    List<WeatherData.CityInfo> searchCity(String keyword);

    // 获取当前天气
    WeatherData.NowWeather getWeatherNow(String cityId);

    // 获取未来n天预报
    List<WeatherData.Forecast> getForecast(String cityId, int days);

    // 获取生活指数
    List<WeatherData.LifeIndex> getLifeIndices(String cityId, String types);

    WeatherData.Sun getSunTime(String cityId);
}