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
export default {
  name: 'DistrictMap',
  data() {
    return {
      // 地图实例
      map: null,
      // 行政区图层实例
      districtLayer: null,
      // 选中的省份编码
      selectedAdcode: '130000', // 默认河北省
      // 选中的层级
      selectedDepth: '2', // 默认显示区县级
      // 省份列表数据
      adcodeList: [],
      // 存储颜色映射，确保同一区域颜色不变
      colorMap: {}
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

    // 加载行政区编码数据
    loadAdcodeData() {
      // 这里假设adcodes.js已通过script标签引入，暴露了window.adcodes变量
      if (window.adcodes) {
        this.adcodeList = window.adcodes;
      } else {
        // 如果数据未加载，尝试重试
        setTimeout(() => this.loadAdcodeData(), 300);
      }
    },

    // 初始化行政区图层
    initDistrictLayer(adcode, depth) {
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
          'fill': (properties) => this.getColorByAdcode(properties.adcode),
          'province-stroke': 'cornflowerblue',
          'city-stroke': 'white',
          'county-stroke': 'rgba(255,255,255,0.5)'
        }
      });

      // 添加图层到地图
      this.districtLayer.setMap(this.map);
    },

    // 根据adcode获取颜色
    getColorByAdcode(adcode) {
      if (!this.colorMap[adcode]) {
        // 生成蓝色系颜色
        const gb = Math.random() * 155 + 50;
        this.colorMap[adcode] = `rgb(${gb}, ${gb}, 255)`;
      }
      return this.colorMap[adcode];
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
.district-map-container[data-v-98a4431c] {
  position: relative;
  width: 71%;
  padding-top: 75px;
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
