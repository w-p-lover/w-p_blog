<template>
  <div class="imgs">
    <ul>
      <li class="item" v-for="(image, index) in imageList" :key="index" :style="{
        'background-image': 'url(' + image + ')'
      }">
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">

const imageList = [
  "\n" +
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/img/202410311034088.jpg",
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/IMG_20240508_190550.jpg",
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/IMG_20240508_174939.jpg",
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/IMG_20240508_173624.jpg",
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/IMG_20240420_190212.jpg",
  "https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/img202410311029127.jpg",
  "\n"
]
</script>

<style lang="scss" scoped>
@import "@/assets/styles/mixin.scss";

.imgs {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  z-index: -9;
  background-color: #363636;
  overflow: hidden;

  .item {
    @include absolute;
    width: 100%;
    height: 100%;
    background: no-repeat 50% 50% / cover;
    opacity: 0;
    animation: imageAnimation 38s linear infinite 0s;
    backface-visibility: hidden;
    transform-style: preserve-3d;

    &:nth-child(2) {
      animation-delay: 6s;
    }

    &:nth-child(3) {
      animation-delay: 12s;
    }

    &:nth-child(4) {
      animation-delay: 18s;
    }

    &:nth-child(5) {
      animation-delay: 24s;
    }

    &:nth-child(6) {
      animation-delay: 30s;
    }
  }

  &::before {
    content: '';
    display: block;
    position: absolute;
    inset: 0;
    background:
      linear-gradient(155deg, rgba(5, 12, 28, 0.42), rgba(8, 18, 38, 0.2) 42%, rgba(5, 12, 24, 0.44)),
      radial-gradient(circle at 50% 38%, rgba(255, 255, 255, 0.08), transparent 34%);
    transition: all .2s ease-in-out 0s;
  }
}

@keyframes imageAnimation {
  0% {
    opacity: 0;
    transform: scale(1);
    animation-timing-function: ease-in;
  }

  2% {
    opacity: 1;
  }

  10% {
    opacity: 1;
    transform: scale(1.03);
    animation-timing-function: ease-out;
  }

  19% {
    opacity: 1;
    transform: scale(1.075);
  }

  26% {
    opacity: 0;
    transform: scale(1.085);
  }

  100% {
    opacity: 0;
    transform: scale(1.085);
  }
}
</style>
