package com.ican.controller;

import com.ican.model.dto.WeatherData;
import com.ican.model.vo.Result;
import com.ican.service.WeatherService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

// 允许跨域（根据前端地址调整origin）
@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/weather") // 接口基础路径，与前端BASE_API对应
public class WeatherController {

    @Resource
    private WeatherService weatherService;

    /**
     * 1. IP定位获取当前城市（前端初始化用）
     */
    @GetMapping("/location")
    public Result<WeatherData.CityInfo> getLocationByIp() {
        try {
            WeatherData.CityInfo city = weatherService.getLocationByIp();
            return Result.success(city);
        } catch (Exception e) {
            return Result.fail("IP定位失败：" + e.getMessage());
        }
    }

    /**
     * 2. 城市搜索（前端切换城市时用）
     * @param keyword 城市名关键词（如"北京"、"杭州"）
     */
    @GetMapping("/search")
    public Result<List<WeatherData.CityInfo>> searchCity(@RequestParam String keyword) {
        try {
            List<WeatherData.CityInfo> cities = weatherService.searchCity(keyword);
            return Result.success(cities);
        } catch (Exception e) {
            return Result.fail("城市搜索失败：" + e.getMessage());
        }
    }

    /**
     * 3. 获取当前天气（实时温度、天气状况等）
     * @param cityId 城市ID（从定位或搜索接口获取）
     */
    @GetMapping("/now")
    public Result<WeatherData.NowWeather> getWeatherNow(@RequestParam String cityId) {
        try {
            WeatherData.NowWeather now = weatherService.getWeatherNow(cityId);
            return Result.success(now);
        } catch (Exception e) {
            return Result.fail("获取实时天气失败：" + e.getMessage());
        }
    }

    /**
     * 4. 获取未来3天预报
     * @param cityId 城市ID
     */
    @GetMapping("/forecast")
    public Result<List<WeatherData.Forecast>> getForecast(@RequestParam String cityId) {
        try {
            List<WeatherData.Forecast> forecasts = weatherService.getForecast(cityId, 3); // 3天
            return Result.success(forecasts);
        } catch (Exception e) {
            return Result.fail("获取预报失败：" + e.getMessage());
        }
    }

    /**
     * 5. 获取生活指数（穿衣建议、日出日落等）
     * @param cityId 城市ID
     * @param types 指数类型（如"3,7"表示穿衣指数和日出日落）
     */
    @GetMapping("/life")
    public Result<List<WeatherData.LifeIndex>> getLifeIndices(
            @RequestParam String cityId,
            @RequestParam String types) {
        try {
            List<WeatherData.LifeIndex> indices = weatherService.getLifeIndices(cityId, types);
            return Result.success(indices);
        } catch (Exception e) {
            return Result.fail("获取生活指数失败：" + e.getMessage());
        }
    }


    /**
     * 6. 获取日出日落
     * @param cityId 城市ID
     */
    @GetMapping("/Sun")
    public Result<WeatherData.Sun> getSunTime(
            @RequestParam String cityId) {
        try {
            WeatherData.Sun indices = weatherService.getSunTime(cityId);
            return Result.success(indices);
        } catch (Exception e) {
            return Result.fail("获取生活指数失败：" + e.getMessage());
        }
    }
}