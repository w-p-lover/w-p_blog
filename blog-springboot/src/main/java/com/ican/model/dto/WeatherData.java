package com.ican.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class WeatherData {

    private CityInfo city;

    // 当前天气信息
    private NowWeather now;

    // 未来3天预报
    private List<Forecast> forecasts;

    // 生活指数（如穿衣建议、日出日落）
    private List<LifeIndex> lifeIndices;


    // 内部类：城市信息
    @Data
    public static class CityInfo {
        private String id;    // 城市唯一标识（用于查询天气）
        private String name;  // 城市名（如"北京"）
        private String adm2;  // 所属省份（如"北京市"）
        private String adm1;  // 所属国家（如"中国"）
    }

    // 内部类：当前天气
    @Data
    public static class NowWeather {
        private String temp;      // 实时温度（如"25"）
        private String text;      // 天气状况（如"晴"）
        private String obsTime;   // 观测时间（如"2023-10-01 14:30"）
        private String humidity;  // 湿度（如"60%"）
        private String windDir;   // 风向（如"东北风"）
        private String windSpeed; // 风速（如"3km/h"）
        private String icon;      // 天气图标URL
    }

    // 内部类：未来预报
    @Data
    public static class Forecast {
        private String date;          // 日期（如"2023-10-01"）
        private String tempMin;       // 最低温（如"18"）
        private String tempMax;       // 最高温（如"28"）
        private String text;          // 白天天气状况（如"多云"）
        private String icon;          // 白天天气图标URL

        // 新增：夜间天气信息
        private String textNight;     // 夜间天气状况（如"中雨"）
        private String codeNight;     // 夜间天气代码（如"14"，用于夜间图标）

        // 新增：降水相关
        private String rainfall;      // 降雨量（单位：mm，如"7.47"）
        private String precip;        // 降水概率（如"0.76"）

        // 新增：风力风向相关
        private String windDirection; // 风向（如"西南"）
        private String windSpeed;     // 风速（单位：km/h，如"8.4"）
        private String windScale;     // 风力等级（如"2"）

        // 新增：湿度
        private String humidity;      // 湿度（单位：%，如"96"）

    }

    // 内部类：生活指数
    @Data
    public static class LifeIndex {
        private String type;      // 指数类型（如"1"=运动指数，"2"=洗车指数）
        private String text;      // 指数详细描述（如"天气较好，但考虑天气寒冷..."）
        private String name;      // 指数名称（如"运动指数"、"洗车指数"）
        private String category;  // 指数分类（如"较不宜"）
        private String level;     // 指数等级（如"3"）
        private String date;      // 指数对应的日期（如"2021-12-16"）
    }

    @Data
    public static class Sun {
        private String date;          // 日期（如"2023-10-01"）
        private String sunrise;       // 最低温（如"18"）
        private String sunset;       // 最高温（如"28"）


    }
}