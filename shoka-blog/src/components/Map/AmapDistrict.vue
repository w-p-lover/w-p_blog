<template>
  <div class="district-map-container">
    <!-- 地图容器 -->
    <div id="mapContainer" class="map-container"></div>

    <!-- 控制面板 -->
    <div class="control-panel">
      <h4>选择省份</h4>
      <select
          v-model="selectedAdcode"
          @change="handleAdcodeChange"
          class="select-control"
      >
        <option value="-1">选择省份</option>
        <option
            v-for="item in adcodeList"
            :key="item.adcode"
            :value="item.adcode"
        >
          {{ item.name }}
        </option>
      </select>

      <h4>选择层级</h4>
      <select
          v-model="selectedDepth"
          @change="handleDepthChange"
          class="select-control"
      >
        <option value="0">0 - 显示省级</option>
        <option value="1">1 - 显示市级</option>
        <option value="2">2 - 显示区/县级</option>
      </select>
    </div>
  </div>
</template>

<script>
// 独立的颜色映射对象，避免使用Vue实例的this
const colorMap = {};

// 独立的颜色生成函数，不依赖Vue实例
function getColorByAdcode(adcode) {
  if (!colorMap[adcode]) {
    // 生成蓝色系颜色
    const gb = Math.random() * 155 + 50;
    colorMap[adcode] = `rgb(${gb}, ${gb}, 255)`;
  }
  return colorMap[adcode];
}

export default {
  name: 'DistrictMap',
  props: {
    weatherTitle: {
      type: String,
      default: '' // 或者一个默认值，比如 "默认天气"
    }
  },
  data() {
    return {
      // 地图实例
      map: null,
      // 行政区图层实例
      districtLayer: null,
      // 选中的省份编码
      selectedAdcode: '110000', // 默认河北省
      // 选中的层级
      selectedDepth: '2', // 默认显示区县级
      // 省份列表数据
      adcodeList: [],
    };
  },
  mounted() {
    // 初始化地图
    this.initMap();
    // 加载行政区编码数据
    this.loadAdcodeData();
  },
  beforeUnmount() {
    // 组件销毁时清除地图实例
    if (this.map) {
      this.map.destroy();
    }
  },
  // TIP 配合 activated() 和 deactivated() 生命周期，保证切换页面回来时地图能重新初始化。
  // TIP 关键：离开页面销毁地图，防止白屏, 容器重新显示，强制刷新尺寸
  activated() {
    if (!this.map) {
      this.initMap();
    } else {
      this.$nextTick(() => {
        this.map.resize();
      });
    }
  },
  deactivated() {
    if (this.map) {
      this.map.destroy();
      this.map = null;
      this.districtLayer = null;
    }
  },
  methods: {
    // 初始化地图
    initMap() {
      // 确保高德地图API已加载
      if (window.AMap) {
        this.map = new window.AMap.Map('mapContainer', {
          zoom: 4.5,
          center: [116.412427, 39.303573], // 北京坐标
          pitch: 0,
          viewMode: '3D'
        });

        // 地图加载完成后初始化行政区图层
        this.map.on('complete', () => {
          // 修改版权信息
          const copyrightEl = document.querySelector('.amap-mcode');
          if (copyrightEl) {
            copyrightEl.innerHTML = '- GS(2021)6375号、GS(2021)648号';
          }

          // 初始化行政区图层
          this.initDistrictLayer(this.selectedAdcode, this.selectedDepth);
        });
      } else {
        // 如果API未加载，尝试重试
        setTimeout(() => this.initMap(), 300);
      }
    },

    loadAdcodeData() {
      if (window.adcodes) {
        this.adcodeList = window.adcodes;
        // 传递了 weatherTitle，则匹配省份并更新地图
        if (this.weatherTitle) {
          const match = this.adcodeList.find(item => item.name.includes(this.weatherTitle));
          if (match) {
            this.selectedAdcode = match.adcode;
            this.initDistrictLayer(this.selectedAdcode, this.selectedDepth);
            console.log(`地图初始化到省份: ${match.name} (${match.adcode})`);
          } else {
            console.warn(`未找到匹配的省份: ${this.weatherTitle}`);
          }
        }

      } else {
        setTimeout(() => this.loadAdcodeData(), 300);
      }
    },

    // 初始化行政区图层
    initDistrictLayer(adcode, depth) {
      if (this.districtLayer && this.districtLayer.adcode === adcode && this.districtLayer.depth === depth) return;
      // 清除已有图层
      if (this.districtLayer) {
        this.districtLayer.setMap(null);
      }

      // 创建新的行政区图层
      this.districtLayer = new window.AMap.DistrictLayer.Province({
        zIndex: 12,
        adcode: [adcode],
        depth: parseInt(depth),
        styles: {
          // 使用独立函数，避免引用Vue实例
          'fill': (properties) => getColorByAdcode(properties.adcode),
          'province-stroke': 'cornflowerblue',
          'city-stroke': 'white',
          'county-stroke': 'rgba(255,255,255,0.5)'
        }
      });

      // 添加图层到地图
      this.districtLayer.setMap(this.map);
    },

    // 处理省份选择变化
    handleAdcodeChange() {
      if (this.selectedAdcode !== '-1') {
        this.initDistrictLayer(this.selectedAdcode, this.selectedDepth);
      }
    },

    // 处理层级选择变化
    handleDepthChange() {
      this.initDistrictLayer(this.selectedAdcode, this.selectedDepth);
    }
  }
};
</script>

<style scoped>
.district-map-container {
  position: relative;
  width: 61%;
  padding-top: 50px;
  padding-left: 20px;
  height: 76vh;
}

.map-container {
  width: 100%;
  height: 100%;
}

.control-panel {
  position: absolute;
  top: 80px;
  right: 10px;
  background-color: white;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  z-index: 100;
}

h4 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 14px;
}

.select-control {
  width: 100%;
  height: 28px;
  margin-bottom: 15px;
  padding: 0 5px;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>
