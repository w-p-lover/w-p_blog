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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * 3. 获取当前天气（适配和风天气接口 + FastJSON2）
     */
    @Override
    public WeatherData.NowWeather getWeatherNow(String cityId) {
        String url = hefengBaseUrl + "v7/weather/now";
        Map<String, String> params = new HashMap<>();
        params.put("key", hefengApiKey);
        params.put("location", cityId);
        params.put("lang", "zh-hans");
        params.put("unit", "m");

        String result = HttpUtil.get(url, params);
        JSONObject json = JSON.parseObject(result);

        // 1. 先判断接口是否返回成功（和风天气code=200表示成功）
        if (!"200".equals(json.getString("code"))) {
            throw new RuntimeException("获取天气失败：" + json.getString("code"));
        }
        JSONObject now = json.getJSONObject("now");
        if (now == null) {
            throw new RuntimeException("天气数据为空");
        }

        WeatherData.NowWeather weatherNow = new WeatherData.NowWeather();

        // 3. 字段映射（严格对应和风天气的JSON字段）
        weatherNow.setTemp(now.getString("temp"));
        weatherNow.setText(now.getString("text"));
        weatherNow.setObsTime(now.getString("obsTime"));
        weatherNow.setHumidity(now.getString("humidity"));
        weatherNow.setWindDir(now.getString("windDir"));
        weatherNow.setWindSpeed(now.getString("windSpeed"));
        weatherNow.setIcon("weather/" + now.getString("icon") + "-fill.svg");

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
        String url = hefengBaseUrl + "v7/weather/" + days + "d";
        Map<String, String> params = new HashMap<>();
        params.put("key", hefengApiKey);
        params.put("location", cityId);
        params.put("language", "zh-hans");
        params.put("unit", "m");

        String result = HttpUtil.get(url, params);
        JSONObject firstResult = JSON.parseObject(result);
        JSONArray daily = firstResult.getJSONArray("daily"); // 改用optJSONArray更安全
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

            // 基础字段（修正字段映射，使用optString避免字段不存在抛异常）
            forecast.setDate(day.getString("fxDate")); // 日期：对应接口的fxDate

            forecast.setTempMin(day.getString("tempMin"));    // 最低温：对应tempMin
            forecast.setTempMax(day.getString("tempMax"));    // 最高温：对应tempMax
            forecast.setText(day.getString("textDay"));      // 白天天气文本：对应textDay

            String codeDay = day.getString("iconDay");
            forecast.setIcon("weather/" + codeDay + "-fill.svg");      // 白天天气图标：对应iconDay

            // 补充未使用的字段解析（修正字段映射）
            forecast.setTextNight(day.getString("textNight")); // 夜间天气文本：对应textNight
            forecast.setCodeNight(day.getString("iconNight"));      // 夜间天气代码：对应iconNight
            forecast.setPrecip(day.getString("precip"));        // 降水概率：对应precip
            forecast.setWindDirection(day.getString("windDirDay")); // 风向（白天）：对应windDirDay
            forecast.setWindSpeed(day.getString("windSpeedDay"));   // 风速（白天，km/h）：对应windSpeedDay
            forecast.setWindScale(day.getString("windScaleDay"));   // 风力等级（白天）：对应windScaleDay
            forecast.setHumidity(day.getString("humidity"));        // 湿度（%）：对应humidity

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
        // 参数校验：避免无效请求
        if (cityId == null || cityId.trim().isEmpty() || types == null || types.trim().isEmpty()) {
            log.warn("无效的请求参数：cityId={}, types={}", cityId, types);
            return Collections.emptyList();
        }

        // 调整URL路径（根据接口实际路径修改，示例为v7/indices/daily，需与实际接口一致）
        String url = hefengBaseUrl + "v7/indices/"  + "1d";
        Map<String, String> params = new HashMap<>();
        params.put("key", hefengApiKey);
        params.put("location", cityId);
        params.put("language", "zh-hans");
        params.put("type", types);

        String result = HttpUtil.get(url, params);
        JSONObject firstResult = JSON.parseObject(result);
        if (firstResult == null) {
            log.warn("未获取到城市[{}]的生活指数数据", cityId);
            return Collections.emptyList();
        }

        // 从接口返回中获取daily数组（生活指数数据）
        JSONArray daily = firstResult.getJSONArray("daily");
        if (daily == null) {
            log.warn("生活指数daily数组为空：cityId={}", cityId);
            return Collections.emptyList();
        }

        List<WeatherData.LifeIndex> indices = new ArrayList<>();
        log.debug("开始解析生活指数数据，共{}条", daily.size());

        for (Object obj : daily) {
            if (!(obj instanceof JSONObject)) {
                log.warn("跳过非JSON对象的生活指数元素：{}", obj);
                continue;
            }
            JSONObject indexObj = (JSONObject) obj;
            try {
                if (!indexObj.containsKey("type") || !indexObj.containsKey("text")) {
                    log.warn("生活指数元素缺少核心字段（type/text）：{}", indexObj);
                    continue;
                }

                String type = indexObj.getString("type");
                // 过滤出types中包含的类型
                WeatherData.LifeIndex index = new WeatherData.LifeIndex();

                index.setType(type);
                index.setName(indexObj.containsKey("name") ? indexObj.getString("name") : ""); // 名称（如"运动指数"）
                index.setCategory(indexObj.containsKey("category") ? indexObj.getString("category") : ""); // 分类（如"较不宜"）
                index.setText(indexObj.getString("text")); // 详细描述
                index.setLevel(indexObj.containsKey("level") ? indexObj.getString("level") : ""); // 等级（如"3"）
                index.setDate(indexObj.containsKey("date") ? indexObj.getString("date") : ""); // 日期

                indices.add(index);
            } catch (JSONException e) {
                log.error("解析生活指数元素失败：{}，异常：{}", indexObj, e.getMessage());
                // 单个元素解析失败不影响整体，继续处理下一个
                continue;
            }
        }

        log.debug("生活指数解析完成，共获取{}条有效数据", indices.size());
        return indices;
    }

    @Override
    public WeatherData.Sun getSunTime(String cityId) {
        if (cityId == null || cityId.trim().isEmpty()) {
            log.warn("无效的城市ID：{}", cityId);
            return null;
        }

        String url = hefengBaseUrl + "v7/astronomy/sun";
        Map<String, String> params = new HashMap<>();
        params.put("key", hefengApiKey);
        params.put("location", cityId);
        params.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

        String result = HttpUtil.get(url, params);
        JSONObject firstResult = JSON.parseObject(result);
        if (firstResult == null) {
            log.warn("未获取到城市[{}]的日出日落数据", cityId);
            return null;
        }

        try {
            WeatherData.Sun sun = new WeatherData.Sun();

            // 解析日出日落时间（接口返回为直接字段，非数组）
            String sunrise = firstResult.getString("sunrise");
            String sunset = firstResult.getString("sunset");

            String date = sunrise.substring(0, 10);
            String riseDate = sunrise.substring(11, 16);
            String setDate = sunset.substring(11, 16);
            sun.setDate(date);
            sun.setSunrise(riseDate); // 保留完整时间（含时区）
            sun.setSunset(setDate);   // 保留完整时间（含时区）
            return sun;
        } catch (JSONException e) {
            log.error("解析日出日落数据失败：{}", e.getMessage());
            return null;
        }
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