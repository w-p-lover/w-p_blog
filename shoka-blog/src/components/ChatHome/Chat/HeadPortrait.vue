<template>
  <div class="head-portrait">
    <img :src="imgUrl" alt="头像" />
  </div>
</template>

<script>
import defaultImage from '@/assets/img/head_portrait.jpg';

export default {
  name: 'HeadPortrait',
  props: {
    imgUrl: {
      type: String,
      default: defaultImage,
    },
  },
};
</script>

<style lang="scss" scoped>
.head-portrait {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: 1px solid rgba(238, 242, 236, 0.68);
  position: relative;
  background: rgba(255, 255, 255, 0.05);
  box-shadow: 0 10px 22px rgba(0, 0, 0, 0.22);

  &::before {
    content: '';
    width: 12px;
    height: 12px;
    z-index: 1;
    display: block;
    border-radius: 50%;
    background-color: #9fd3a7;
    border: 2px solid #20242e;
    position: absolute;
    right: 1px;
    bottom: 2px;
  }

  img {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    box-sizing: border-box;
    position: absolute;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    vertical-align: middle;
  }
}
</style>
