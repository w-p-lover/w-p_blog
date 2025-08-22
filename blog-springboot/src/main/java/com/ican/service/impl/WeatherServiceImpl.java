package com.ican.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.ican.model.dto.WeatherData;
import com.ican.service.WeatherService;
import com.ican.utils.HttpUtil;
import com.ican.utils.IpUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static cn.dev33.satoken.SaManager.log;

@Service
public class WeatherServiceImpl implements WeatherService {

    // 从配置文件读取心知天气密钥（application.yml中配置）
    @Value("${seniverse.api-key}")
    private String apiKey;

    @Value("${seniverse.base-url}")
    private String baseUrl;

    // 从配置文件读取心知天气密钥（application.yml中配置）
    @Value("${hefeng.api-key}")
    private String hefengApiKey;

    @Value("${hefeng.base-url}")
    private String hefengBaseUrl;

    // 日志实例（类级别声明）
    private static final Logger log = LoggerFactory.getLogger(WeatherServiceImpl.class);
    /**
     * 1. IP定位获取城市（适配FastJSON2）
     */
    @Override
    public WeatherData.CityInfo getLocationByIp() {

        // 1. 获取客户端真实IP地址
        String clientIp = getClientIp();
        if (clientIp == null || clientIp.isEmpty()) {
            throw new RuntimeException("无法获取客户端IP地址");
        }
        String ipSource = IpUtils.getIpSource(clientIp);
        ipSource  = ipSource.isEmpty() ? "北京" : ipSource;

        String url = hefengBaseUrl + "geo/v2/city/lookup";
        Map<String, String> params = new HashMap<>();
        params.put("key", hefengApiKey);
        params.put("location", ipSource);
        params.put("lang", "zh-hans");

        String result = HttpUtil.get(url, params);
        JSONObject json = JSON.parseObject(result);
        JSONArray locations = json.getJSONArray("location"); // 获取数组

        if (locations.isEmpty()) {
            throw new RuntimeException("未获取到定位信息");
        }

        JSONObject loc = locations.getJSONObject(0); // 获取第一个城市
        WeatherData.CityInfo city = new WeatherData.CityInfo();
        city.setId(loc.getString("id")); // 获取字符串字段
        city.setName(loc.getString("name"));
        city.setAdm2(loc.getString("adm2"));
        return city;
    }


    /**
     * 2. 搜索城市
     */
    @Override
    public List<WeatherData.CityInfo> searchCity(String keyword) {
        String url = hefengBaseUrl + "geo/v2/city/lookup";
        Map<String, String> params = new HashMap<>();
        params.put("key", hefengApiKey);
        params.put("location", keyword);
        params.put("lang", "zh-hans");

        String result = HttpUtil.get(url, params);
        JSONObject json = JSONObject.parseObject(result);
        JSONArray locations = json.getJSONArray("location");

        List<WeatherData.CityInfo> cities = new ArrayList<>();
        for (Object obj : locations) {
            JSONObject loc = (JSONObject) obj;
            WeatherData.CityInfo city = new WeatherData.CityInfo();
            city.setId(loc.getString("id"));
            city.setName(loc.getString("name"));
            city.setAdm2(loc.getString("adm2"));
            cities.add(city);
        }
        return cities;
    }


    /**
     * 3. 获取当前天气（适配FastJSON2）
     */
    @Override
    public WeatherData.NowWeather getWeatherNow(String cityId) {
        String url = baseUrl + "/weather/now.json";
        Map<String, String> params = new HashMap<>();
        params.put("key", apiKey);
        params.put("location", cityId);
        params.put("language", "zh-Hans");
        params.put("unit", "c");

        String result = HttpUtil.get(url, params);
        JSONObject json = JSON.parseObject(result);
        // 链式获取嵌套字段：results[0].now
        JSONObject now = json.getJSONArray("results")
                .getJSONObject(0)
                .getJSONObject("now");

        WeatherData.NowWeather weatherNow = new WeatherData.NowWeather();
        weatherNow.setTemp(now.getString("temperature"));
        weatherNow.setText(now.getString("text"));
        // 获取最后更新时间（results[0].last_update）
        weatherNow.setObsTime(json.getJSONArray("results")
                .getJSONObject(0)
                .getString("last_update"));
        weatherNow.setHumidity(now.getString("humidity"));
        weatherNow.setWindDir(now.getString("wind_direction"));
        weatherNow.setWindSpeed(now.getString("wind_speed"));
        weatherNow.setIcon("weather/" + now.getString("code") + "@1x.png");
        return weatherNow;
    }


    /**
     * 4. 获取未来n天预报
     */
    @Override
    public List<WeatherData.Forecast> getForecast(String cityId, int days) {
        // 增加参数校验，避免无效请求
        if (cityId == null || cityId.trim().isEmpty() || days < 1) {
            log.warn("无效的请求参数：cityId={}, days={}", cityId, days);
            return Collections.emptyList();
        }

        String url = baseUrl + "/weather/daily.json";
        Map<String, String> params = new HashMap<>();
        params.put("key", apiKey);
        params.put("location", cityId);
        params.put("language", "zh-Hans");
        params.put("unit", "c");
        params.put("days", String.valueOf(days));

        String result = HttpUtil.get(url, params);
        JSONObject firstResult = parseFirstResult(result);
        if (firstResult == null) {
            log.warn("未获取到城市[{}]的预报数据", cityId);
            return Collections.emptyList();
        }

        // 安全获取daily数组（避免字段不存在导致的空指针）
        JSONArray daily = firstResult.getJSONArray("daily");
        return getForecasts(daily);
    }

    @NotNull
    private static List<WeatherData.Forecast> getForecasts(JSONArray daily) {
        List<WeatherData.Forecast> forecasts = new ArrayList<>();
        // 处理daily为null的情况
        if (daily == null) {
            log.warn("daily数组为空，返回空预报列表");
            return forecasts;
        }

        log.debug("开始解析daily数据，共{}条", daily.size());
        for (Object obj : daily) {
            // 跳过非JSON对象的元素，避免类型转换异常
            if (!(obj instanceof JSONObject)) {
                log.warn("跳过非JSON对象的元素：{}", obj);
                continue;
            }

            JSONObject day = (JSONObject) obj;
            WeatherData.Forecast forecast = new WeatherData.Forecast();

            // 基础字段（原代码已使用）
            forecast.setDate(day.getString("date")); // 日期，缺省值"未知日期"

            forecast.setTempMin(day.getString("low"));     // 最低温
            forecast.setTempMax(day.getString("high"));    // 最高温
            forecast.setText(day.getString("text_day")); // 白天天气文本

            String codeDay = day.getString("code_day");
            forecast.setIcon("weather/" + codeDay + "@1x.png");

            // 补充未使用的字段解析
            forecast.setTextNight(day.getString("text_night")); // 夜间天气文本
            forecast.setCodeNight(day.getString("code_night"));     // 夜间天气代码
            forecast.setRainfall(day.getString("rainfall"));       // 降雨量（mm）
            forecast.setPrecip(day.getString("precip"));           // 降水概率
            forecast.setWindDirection(day.getString("wind_direction")); // 风向
            forecast.setWindSpeed(day.getString("wind_speed"));     // 风速（km/h）
            forecast.setWindScale(day.getString("wind_scale"));     // 风力等级
            forecast.setHumidity(day.getString("humidity"));       // 湿度（%）

            forecasts.add(forecast);
        }

        log.debug("解析完成，共获取{}条有效预报数据", forecasts.size());
        return forecasts;
    }


    /**
     * 5. 获取生活指数
     */
    @Override
    public List<WeatherData.LifeIndex> getLifeIndices(String cityId, String types) {
        String url = baseUrl + "/life/suggestion.json";
        Map<String, String> params = new HashMap<>();
        params.put("key", apiKey);
        params.put("location", cityId); // 建议用参数cityId代替硬编码
        params.put("language", "zh-Hans");
        params.put("day", String.valueOf(1));

        //拨除最外层的result的包裹
        String result = HttpUtil.get(url, params);
        JSONObject firstResult = parseFirstResult(result);
        if (firstResult == null) {
            return Collections.emptyList();
        }

        // 解析真正的JSON返回结构
        JSONObject suggestions = firstResult.getJSONObject("suggestion");
        if (suggestions == null) {
            return Collections.emptyList();
        }

        List<WeatherData.LifeIndex> indices = new ArrayList<>();

        // 1. 穿衣指数（types包含"3"，对应dressing）
        if (types.contains("3")) {
            JSONObject dressingObj = suggestions.getJSONObject("dressing");
            if (dressingObj != null) { // 增加空判断
                WeatherData.LifeIndex dress = new WeatherData.LifeIndex();
                dress.setType("3");
                dress.setText(dressingObj.getString("brief"));
                indices.add(dress);
            }
        }

        // 2. 紫外线指数（假设types包含"7"对应uv，根据实际业务调整）
        if (types.contains("7")) {
            JSONObject uvObj = suggestions.getJSONObject("uv");
            if (uvObj != null) { // 增加空判断
                WeatherData.LifeIndex uv = new WeatherData.LifeIndex();
                uv.setType("7");
                uv.setText(uvObj.getString("brief")); // 如"中等"
                indices.add(uv);
            }
        }

        // 3. 可根据需要添加其他指数（如洗车、运动等）
        if (types.contains("1")) { // 假设"1"对应洗车指数
            JSONObject carWashObj = suggestions.getJSONObject("car_washing");
            if (carWashObj != null) {
                WeatherData.LifeIndex carWash = new WeatherData.LifeIndex();
                carWash.setType("1");
                carWash.setText(carWashObj.getString("brief"));
                indices.add(carWash);
            }
        }

        return indices;
    }

    @Override
    public WeatherData.Sun getSunTime(String cityId) {
        String url = baseUrl + "/geo/sun.json";
        Map<String, String> params = new HashMap<>();
        params.put("key", apiKey);
        params.put("location", cityId);
        params.put("days", String.valueOf(1));

        String result = HttpUtil.get(url, params);
        JSONObject firstResult = parseFirstResult(result);
        if (firstResult == null) {
            return null;
        }

        // 解析真正的JSON返回结构
        JSONArray sunTime = firstResult.getJSONArray("sun");
        if (sunTime == null) {
            return null;
        }
        JSONObject sunTimeObject = sunTime.getJSONObject(0);
        WeatherData.Sun sun = new WeatherData.Sun();
        sun.setDate(sunTimeObject.getString("date"));
        sun.setSunset(sunTimeObject.getString("sunset"));
        sun.setSunrise(sunTimeObject.getString("sunrise"));
        return sun;
    }


    /**
     * 通用解析接口返回的results数组第一个元素
     */
    protected JSONObject parseFirstResult(String result) {
        if (StringUtils.isEmpty(result)) {
            return null;
        }
        try {
            JSONObject rootObj = JSON.parseObject(result);
            JSONArray resultsArray = rootObj.getJSONArray("results");
            return (resultsArray != null && !resultsArray.isEmpty())
                    ? resultsArray.getJSONObject(0)
                    : null;
        } catch (JSONException e) {
            log.error("JSON解析异常: {}", result, e);
            return null;
        }
    }

    /**
     * 获取客户端真实IP（处理代理、负载均衡场景）
     */
    private String getClientIp() {
        // 从请求上下文获取HttpServletRequest
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null; // 非Web环境下返回null
        }
        HttpServletRequest request = attributes.getRequest();

        // 可能的IP请求头（按优先级排序）
        String[] ipHeaders = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For可能包含多个IP，取第一个（最原始的客户端IP）
                if (ip.contains(",")) {
                    return ip.split(",")[0].trim();
                }
                return ip.trim();
            }
        }

        // 若没有代理，直接获取远程地址
        return request.getRemoteAddr();
    }
}