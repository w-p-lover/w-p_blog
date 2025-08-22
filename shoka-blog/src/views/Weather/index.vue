<template>
  <div class="parent-container">
    <el-card class="weather-card" shadow="hover">
      <!-- Card header: 左侧图标/温度  右侧 城市/操作/展开箭头 -->
      <div class="card-header">
        <div class="left">
          <img v-if="weatherIcon" :src="weatherIcon" alt="icon" class="main-icon"/>
          <div class="temp-block">
            <div class="temp-title">{{ currentWeather.temp || '--' }}°C</div>
            <div class="text" v-if="currentWeather.text">{{ currentWeather.text }}</div>
          </div>
        </div>

        <div class="right">
          <div class="city-row">
            <el-icon class="loc-icon">
              <Location/>
            </el-icon>
            <span class="city-name">{{ currentCity }}</span>

            <el-button
                class="city-btn"
                size="mini"
                type="text"
                @click.stop="showCitySelector = true"
                title="切换城市"
            >
              <el-icon>
                <Search/>
              </el-icon>
            </el-button>

            <el-badge v-if="reminderSettings.sunsetAlarm" class="alarm-badge" value="已设"/>
            <el-icon
                class="toggle-icon"
                :class="{ open: isExpanded }"
                @click.stop="toggleExpanded"
                title="展开/收起"
            >
              <ArrowDown/>
            </el-icon>
          </div>

          <div class="meta-row">
            <div class="obs-time" v-if="currentWeather.obsTime">
              {{ formatTime(currentWeather.obsTime) }} 更新
            </div>

            <div class="sunset-block" v-if="sunsetTime">
              <el-icon class="bell-icon">
                <Bell/>
              </el-icon>
              <span class="sunset-text">日落 {{ sunsetTime }}</span>
              <span class="countdown" v-if="sunsetCountdown">{{ sunsetCountdown }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- brief dressing tips and small status -->
      <div class="brief-row">
        <el-badge type="info" :value="dressingTips || '暂无建议'"></el-badge>
        <div class="track-btns">
          <el-button size="small" @click="refresh" :loading="isLoading">刷新</el-button>
          <el-button size="small" type="text" @click="openTrackList">追踪记录</el-button>
        </div>
      </div>

      <!-- 展开详情（点击箭头展开） -->
      <transition name="fade-slide">
        <div v-if="isExpanded" class="detail-area">
          <el-divider/>
          <el-row gutter={16} class="detail-top">
            <el-col :xs="24" :sm="24" :md="12">
              <el-card class="sub-card">
                <div class="sub-title">今后 3 天</div>
                <el-row :gutter="8" class="forecast-row">
                  <el-col :span="8" v-for="(day, i) in forecast" :key="i" class="forecast-col">
                    <div class="forecast-day">{{ formatDate(day.date) }}</div>
                    <img v-if="day.icon" :src="day.icon" class="forecast-icon"/>
                    <div class="forecast-temp">{{ day.tempMin }}° ~ {{ day.tempMax }}°</div>
                    <div class="forecast-text">{{ day.text }}</div>
                  </el-col>
                </el-row>
              </el-card>
            </el-col>

            <el-col :xs="24" :sm="24" :md="12">
              <el-card class="sub-card">
                <div class="sub-title">天气详情</div>
                <el-row class="detail-info" :gutter="12">
                  <el-col :span="18">湿度：{{ currentWeather.humidity || '--' }}%</el-col>
                  <el-col :span="18">风级：{{ currentWeather.windScale || '--' }} hPa</el-col>
                  <el-col :span="18">风向：{{ currentWeather.windDirection || '--' }}</el-col>
                  <el-col :span="18">风速：{{ currentWeather.windSpeed || '--' }} km/h</el-col>
                </el-row>
              </el-card>
            </el-col>
          </el-row>

          <el-divider/>

          <el-card class="sub-card reminder-card">
            <div class="sub-title">提醒设置</div>
            <el-form :model="reminderSettings" size="small" label-width="80px">
              <el-row :gutter="12">
                <el-col :span="12">
                  <el-form-item label="日落闹钟">
                    <el-switch
                        v-model="reminderSettings.sunsetAlarm"
                        @change="setSunsetAlarm"
                        active-text="开启"
                        inactive-text="关闭"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="天气预警">
                    <el-switch v-model="reminderSettings.warning" @change="onWarningChange"/>
                  </el-form-item>
                </el-col>
              </el-row>

              <div class="reminder-note" v-if="reminderSettings.sunsetAlarm && sunsetTime">
                下次日落提醒：{{ nextSunsetDisplay }}
              </div>
            </el-form>
          </el-card>
        </div>
      </transition>

      <!-- 城市选择对话框 -->
      <el-dialog title="切换城市" v-model="showCitySelector" width="400px" :close-on-click-modal="false">
        <el-autocomplete
            v-model="cityInput"
            :fetch-suggestions="fetchCities"
            placeholder="输入城市名，回车或选择"
            @select="selectCity"
            @keyup.enter.native="onEnterCity"
            clearable
        >
          <template #prefix>
            <el-icon>
              <Search/>
            </el-icon>
          </template>
        </el-autocomplete>

        <div class="dialog-foot" style="margin-top:12px;">
          <el-button type="primary" @click="onEnterCity" :loading="isLoading">查询</el-button>
          <el-button @click="showCitySelector = false">取消</el-button>
        </div>
      </el-dialog>

      <!-- 错误提示显著放在卡片右上（红点 + tooltip） -->
      <el-tooltip v-if="errorMsg" effect="dark" :content="errorMsg" placement="top">
        <div class="error-indicator"/>
      </el-tooltip>

      <!-- 自定义加载遮罩（更直观） -->
      <div v-if="isLoading" class="loading-overlay">
        <div class="spinner"/>
        <div class="loading-text">加载中...</div>
      </div>
    </el-card>
    <AmapDistrict
        :current-city="defaultCity"
        :amap-key="AmapKey"
        map-width="500px"
        map-height="600px"
        @map-loaded="handleMapLoaded"
    />
  </div>
</template>

<script setup>
import {ref, onMounted, onUnmounted, computed} from 'vue';
import axios from 'axios';
import {
  Location,
  ArrowDown,
  Bell,
  Search
} from '@element-plus/icons-vue';
import {ElMessage} from 'element-plus';
import AmapDistrict from "@/components/Map/AmapDistrict.vue";
const defaultCity = ref('北京');
const AmapKey = ref('4c7c8ed3e9e6be656d909bec63c0bd12');
const BASE_API = 'http://localhost:8800/api/weather';

// 状态
const currentCity = ref('定位中...');
const currentWeather = ref({
  temp: '',
  text: '',
  obsTime: '',
  humidity: '',
  windDirection: '',
  windScale: '',
  windSpeed: '',
});
const forecast = ref([]);
const weatherIcon = ref('');
const dressingTips = ref('');
const sunsetTime = ref(''); // "HH:mm" 格式
const isExpanded = ref(false);
const showCitySelector = ref(false);
const cityInput = ref('');
const errorMsg = ref('');
const isLoading = ref(false);
const cityListCache = ref([]);

// 提醒设置（保存在 localStorage）
const reminderSettings = ref({
  sunsetAlarm: JSON.parse(localStorage.getItem('reminder.sunsetAlarm') || 'true'),
  warning: JSON.parse(localStorage.getItem('reminder.warning') || 'true')
});

// 计时器引用
let sunsetTimer = null;
let countdownTimer = null;

// 初始化：尝试定位并拉取天气
onMounted(async () => {
  isLoading.value = true;
  try {
    const {data} = await axios.get(`${BASE_API}/location`);
    if (data && data.success && data.data) {
      const city = data.data;
      currentCity.value = city.name;
      await fetchWeatherData(city.id);
    } else {
      // 兜底北京
      currentCity.value = '北京';
      await fetchWeatherData('101010100');
    }
  } catch (err) {
    handleError('初始化失败', err);
    currentCity.value = '北京';
    await fetchWeatherData('101010100');
  } finally {
    isLoading.value = false;
  }
});

// 清理
onUnmounted(() => {
  if (sunsetTimer) window.clearTimeout(sunsetTimer);
  if (countdownTimer) window.clearInterval(countdownTimer);
});

// 刷新
const refresh = async () => {
  // 如果已有城市追踪记录，尽量用cityId，否则用兜底
  const track = JSON.parse(localStorage.getItem('weatherTrack') || '[]');
  const last = track.length ? track[track.length - 1] : null;
  const cityId = last ? last.cityId : '101010100';
  await fetchWeatherData(cityId);
};

const handleMapLoaded = () => {
  console.log('地图加载完成');
};

// 展开/收起（只由箭头触发）
const toggleExpanded = () => {
  isExpanded.value = !isExpanded.value;
};

// 拉取天气数据（并行请求）
const fetchWeatherData = async (cityId) => {
  isLoading.value = true;
  errorMsg.value = '';
  try {
    const [nowRes, forecastRes, lifeRes, sunRes] = await Promise.all([
      axios.get(`${BASE_API}/now`, {params: {cityId}}),
      axios.get(`${BASE_API}/forecast`, {params: {cityId}}), // 期待未来多天数组
      axios.get(`${BASE_API}/life`, {params: {cityId, types: '3,5,1'}}), // 3: 穿衣
      axios.get(`${BASE_API}/Sun`, {params: {cityId}}),
    ]);

    // 兼容后端返回结构
    const now = nowRes?.data?.data || {};
    weatherIcon.value = now.icon || '';

    // 未来预报
    //TODO 这部分得定义风速风向等等
    const fk = forecastRes?.data?.data || [];
    const todayForecast = fk[0] || {};
    currentWeather.value = {
      temp: now.temp ?? '',       // 实时温度
      text: now.text ?? '',       // 实时天气状况
      obsTime: now.obsTime ?? '', // 实时观测时间
      humidity: todayForecast.humidity ?? '',  // 当天湿度（来自预报）
      windDirection: todayForecast.windDirection ?? '',  // 当天风向
      windSpeed: todayForecast.windSpeed ?? '',          // 当天风速
      windScale: todayForecast.windScale ?? ''            // 当天风级
    };
    console.log('currentWeather:', currentWeather.value);

    forecast.value = fk.slice(0, 3).map(d => ({
      date: d.date,
      tempMin: d.tempMin,
      tempMax: d.tempMax,
      icon: d.icon,
      text: d.text
    }));

    dressingTips.value = '';
    sunsetTime.value = '';
    (lifeRes?.data?.data || []).forEach(item => {
      if (item.type === '3' || item.type === 3) dressingTips.value = item.text;

    });
    console.log('sunsetTime:', sunRes);
    sunsetTime.value = sunRes.data.data.sunset;
    // 如果已开启日落提醒，则安排通知
    if (reminderSettings.value.sunsetAlarm && sunsetTime.value) {
      setSunsetNotification();
    }

    // 存储追踪
    saveWeatherToTrack(cityId);
  } catch (err) {
    handleError('天气数据获取失败', err);
  } finally {
    isLoading.value = false;
  }
};

// 城市自动补全：fetch-suggestions 接口
const fetchCities = async (queryString, cb) => {
  if (!queryString) {
    cb([]);
    return;
  }
  isLoading.value = true;
  try {
    const {data} = await axios.get(`${BASE_API}/search`, {
      params: {keyword: queryString.trim()}
    });
    const list = (data?.data || []).map(it => ({
      value: it.adm2 ? `${it.name} (${it.adm2})` : it.name,
      id: it.id,
      name: it.name,
      adm2: it.adm2 || ''
    }));
    cityListCache.value = list;
    cb(list);
  } catch (err) {
    handleError('城市搜索失败', err);
    cb([]);
  } finally {
    isLoading.value = false;
  }
};

// 回车确认城市
const onEnterCity = async () => {
  if (!cityInput.value) return;
  // 尝试匹配缓存
  const found = cityListCache.value.find(c => c.value === cityInput.value || c.name === cityInput.value);
  if (found) {
    selectCity(found);
    return;
  }
  // 如果没有缓存匹配，则直接调用后端搜索并选择第一项
  isLoading.value = true;
  try {
    const {data} = await axios.get(`${BASE_API}/search`, {params: {keyword: cityInput.value.trim()}});
    const arr = data?.data || [];
    if (arr.length > 0) {
      selectCity({id: arr[0].id, name: arr[0].name, adm2: arr[0].adm2});
    } else {
      ElMessage.info('未找到匹配城市');
    }
  } catch (err) {
    handleError('城市查询失败', err);
  } finally {
    isLoading.value = false;
  }
};

// 选择城市
const selectCity = async (city) => {
  currentCity.value = city.name || city.value || currentCity.value;
  showCitySelector.value = false;
  cityInput.value = '';
  await fetchWeatherData(city.id || cityIdFallback(city));
};

// helper：若传入对象不是预期结构，尝试获取 id
const cityIdFallback = (city) => {
  return city.id || city.cityId || (city.value && city.value.id) || '101010100';
};

// 设置日落闹钟开关
const setSunsetAlarm = (checked) => {
  localStorage.setItem('reminder.sunsetAlarm', JSON.stringify(checked));
  if (checked && sunsetTime.value) {
    setSunsetNotification();
    ElMessage.success(`已开启日落提醒（${sunsetTime.value}）`);
  } else {
    // 关闭
    if (sunsetTimer) {
      clearTimeout(sunsetTimer);
      sunsetTimer = null;
    }
    if (countdownTimer) {
      clearInterval(countdownTimer);
      countdownTimer = null;
    }
    ElMessage.info('已关闭日落提醒');
  }
};

// 天气预警开关（占位）
const onWarningChange = (v) => {
  localStorage.setItem('reminder.warning', JSON.stringify(v));
  ElMessage.info(v ? '已开启天气预警' : '已关闭天气预警');
};

// 通知权限与安排
const setSunsetNotification = () => {
  if (!('Notification' in window)) {
    handleError('浏览器不支持通知功能', null);
    return;
  }

  if (Notification.permission === 'granted') {
    scheduleSunsetNotification();
  } else if (Notification.permission !== 'denied') {
    Notification.requestPermission().then(permission => {
      if (permission === 'granted') {
        scheduleSunsetNotification();
      } else {
        ElMessage.warning('需要开启通知权限才能使用日落提醒功能');
      }
    });
  } else {
    ElMessage.warning('通知权限已被拒绝，请在浏览器设置中开启');
  }
};

// 安排日落通知（并维护倒计时）
const scheduleSunsetNotification = () => {
  if (!sunsetTime.value) return;

  // 清理之前的
  if (sunsetTimer) clearTimeout(sunsetTimer);
  if (countdownTimer) clearInterval(countdownTimer);

  const [hours, minutes] = sunsetTime.value.split(':').map(Number);
  const now = new Date();
  let sunset = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hours, minutes, 0);
  let delay = sunset.getTime() - now.getTime();

  // 若已过则安排到明天
  if (delay <= 0) {
    sunset.setDate(sunset.getDate() + 1);
    delay = sunset.getTime() - now.getTime();
  }

  // 安排倒计时更新（每秒更新）
  updateCountdown(sunset);
  countdownTimer = setInterval(() => updateCountdown(sunset), 1000);

  // 安排通知（使用 setTimeout）
  sunsetTimer = setTimeout(() => {
    try {
      new Notification('日落提醒', {
        body: `现在是 ${sunsetTime.value}，适合外出散步～ 当前天气：${currentWeather.value.text || '暂无'}`,
        icon: weatherIcon.value || undefined
      });
    } catch (e) {
      console.error('通知发送失败：', e);
    }
    // 通知后安排第二天的提醒
    scheduleSunsetNotification();
  }, Math.max(0, delay));
};

// 更新倒计时展示（computed 使用）
const sunsetCountdown = ref('');
const updateCountdown = (targetDate) => {
  const now = new Date();
  let diff = Math.max(0, Math.floor((targetDate.getTime() - now.getTime()) / 1000));
  const h = String(Math.floor(diff / 3600)).padStart(2, '0');
  const m = String(Math.floor((diff % 3600) / 60)).padStart(2, '0');
  const s = String(diff % 60).padStart(2, '0');
  sunsetCountdown.value = `${h}:${m}:${s}`;
};

// 下次日落显示字符串
const nextSunsetDisplay = computed(() => {
  if (!sunsetTime.value) return '--';
  // 估算是今天还是明天
  const [hh, mm] = sunsetTime.value.split(':').map(Number);
  const now = new Date();
  let candidate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hh, mm, 0);
  if (candidate <= now) candidate.setDate(candidate.getDate() + 1);
  return `${candidate.getMonth() + 1}-${candidate.getDate()} ${sunsetTime.value}`;
});

// 存储天气记录到 localStorage（只保留最近 10 条）
const saveWeatherToTrack = (cityId) => {
  try {
    const trackData = JSON.parse(localStorage.getItem('weatherTrack') || '[]');
    const existingIndex = trackData.findIndex(item => item.cityId === cityId);
    const weatherRecord = {
      cityId,
      cityName: currentCity.value,
      weather: currentWeather.value.text,
      temp: currentWeather.value.temp,
      updateTime: new Date().toISOString()
    };
    console.log('保存天气记录：', weatherRecord);
    if (existingIndex > -1) {
      trackData[existingIndex] = weatherRecord;
    } else {
      if (trackData.length >= 10) trackData.shift();
      trackData.push(weatherRecord);
    }

    localStorage.setItem('weatherTrack', JSON.stringify(trackData));
  } catch (err) {
    console.error('存储天气记录失败：', err);
  }
};

// 打开追踪记录（简单 demo，若需更复杂可弹窗展示）
const openTrackList = () => {
  const track = JSON.parse(localStorage.getItem('weatherTrack') || '[]');
  if (!track.length) {
    ElMessage.info('暂无追踪记录');
    return;
  }
  // 简单展示最新记录
  const last = track[track.length - 1];
  ElMessage.info(`上次：${last.cityName} ${last.temp}° ${last.weather}（${new Date(last.updateTime).toLocaleString()}）`);
};

// 错误处理
const handleError = (message, error) => {
  console.error(message, error);
  errorMsg.value = message;
  ElMessage.error(message);

  // 3 秒后隐藏
  setTimeout(() => {
    errorMsg.value = '';
  }, 3000);
};

// 格式化时间与日期
const formatTime = (timeStr) => {
  if (!timeStr) return '';
  const d = new Date(timeStr);
  if (isNaN(d.getTime())) return timeStr; // 若不是标准 ISO，直接返回
  return d.toLocaleTimeString('zh-CN', {hour: '2-digit', minute: '2-digit'});
};

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  return `${d.getMonth() + 1}-${d.getDate()} ${weekDays[d.getDay()]}`;
};

// 帮助：如果 city 对象结构不一致，尝试拿 name
const cityNameSafe = (city) => city?.name || city?.value || '未知城市';
</script>

<style scoped>
.weather-card {
  width: 610px;
  margin-left: 75px;
  margin-top: 75px;
  position: relative;
  overflow: visible;
  border-radius: 12px;
  padding: 12px;
  background: linear-gradient(180deg, rgba(255, 254, 254, 0.86), rgba(220, 212, 212, 0.65));
}

/* header */
.card-header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.left {
  display: flex;
  align-items: center;
  gap: 40px;
  flex: 1;
}

.main-icon {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  background-color: rgba(0, 0, 0, 0.03);
  padding: 6px;
  object-fit: contain;
}

.temp-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 110px;
}

.temp-title {
  font-size: 20px;
  color: #202020;
  font-weight: bold;
}

.text {
  font-size: 0.95rem;
  color: #666;
  padding-bottom: 10px;
}

/* right section */
.right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  min-width: 150px;
}

.city-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.loc-icon {
  color: #409EFF;
}

.city-name {
  font-weight: 600;
  margin-right: 6px;
}

.city-btn {
  margin-left: 6px;
}

.alarm-badge {
  margin-left: 6px;
}

.toggle-icon {
  cursor: pointer;
  transition: transform 0.25s ease;
  color: #909399;
}

.toggle-icon.open {
  transform: rotate(180deg);
  color: #409EFF;
}

/* meta-row */
.meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 0.82rem;
  color: #888;
}

.sunset-block {
  display: flex;
  align-items: center;
  gap: 6px;
}

.bell-icon {
  color: #f56c6c;
}

/* brief row */
.brief-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  gap: 10px;
}

.track-btns {
  display: flex;
  gap: 8px;
}

/* detail area */
.detail-area {
  margin-top: 12px;
}

/* sub cards */
.sub-card {
  border-radius: 10px;
  box-shadow: none;
  background: rgba(209, 200, 197, 0.32);
}

.sub-title {
  font-weight: 600;
  margin-bottom: 8px;
  color: #333;
}

.forecast-row {
  justify-content: space-between;
}

.forecast-col {
  text-align: center;
}

.forecast-day {
  font-size: 0.8rem;
  color: #666;
}

.forecast-icon {
  width: 38px;
  height: 38px;
  margin: 6px auto;
  display: block;
}

.forecast-temp {
  font-size: 0.85rem;
}

.forecast-text {
  font-size: 0.75rem;
  color: #777;
}

/* reminder card */
.reminder-card .reminder-note {
  margin-top: 8px;
  font-size: 0.85rem;
  color: #555;
}

/* 错误提示红点 */
.error-indicator {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 14px;
  height: 14px;
  background-color: #f56c6c;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(245, 108, 108, 0.12);
}

/* 修改后 */
.parent-container {
  display: flex;
  gap: 20px;
  align-items: flex-start; /* 关键：子元素顶部对齐，不拉伸高度 */
  padding: 20px;
}

/* 加载遮罩 */
.loading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.7);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  z-index: 50;
}

.spinner {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-top-color: rgba(64, 158, 255, 0.9);
  animation: spin 1s linear infinite;
}

.loading-text {
  margin-top: 8px;
  color: #333;
  font-size: 0.9rem;
}

/* 动画 */
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

:deep(.el-col-md-12.is-guttered) {
  display: block;
  padding: 10px;
}

:deep(.el-row) {
  font-family: fantasy,sans-serif;
}

/* 响应式 */
@media (max-width: 420px) {
  .weather-card { width: 100%; }
  .main-icon { width: 48px; height: 48px; }
  .temp { font-size: 1.8rem; }
}
</style>
