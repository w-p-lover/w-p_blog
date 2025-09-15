<template>
  <div class="nav">
    <div class="nav-menu-wrapper">
      <ul class="menu-list">
        <li
            v-for="(item, index) in menuList"
            :key="index"
            :class="{ activeNav: index === current }"
            @click="changeMenu(index)"
        >
          <div class="block"></div>
          <span class=" icon iconfont" :class="item"></span>
        </li>
      </ul>
    </div>
    <div class="own-pic">
      <HeadPortrait :imgUrl="imgUrl"/>
    </div>
  </div>
</template>

<script>
import HeadPortrait from './HeadPortrait.vue';
import useStore from "@/store";
import defaultImage from "@/assets/img/head_portrait.jpg"
const {user, app} = useStore();
export default {
  components: {
    HeadPortrait,
  },
  data() {
    return {
      menuList: [
        "icon-xinxi",
        "icon-shipin",
        "icon-shu",
        "icon-shandian",
        "icon-shezhi",
      ],
      current: 0,
      //TODO 逻辑实现，缓存有时候会出小毛病，有时间再说
      imgUrl: user.id? user.avatar : defaultImage, // 确保路径正确
    };
  },

  methods: {
    changeMenu(index) {
      this.current = index;
      if (index === 0) {
        this.$router.push({name: 'ChatHome'});
      } else {
        this.showMessage('该功能还没有开发哦，敬请期待一下吧~🥳');
      }
    },
    showMessage(message) {
      this.$message(message);
    },
  },
};
</script>

<style lang="scss" scoped>

@import url('@/assets/fonts/iconfont.css');

.iconfont {
  font-family: "iconfont",serif !important;
  font-style: normal;
  font-size: 25px;
  vertical-align: middle;
  color: rgb(117, 120, 137);
  transition: .3s;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.nav {
  width: 100%;
  height: 88vh;
  position: relative;
  border-radius: 20px 0 0 20px;

  .nav-menu-wrapper {
    position: absolute;
    top: 40%;
    transform: translate(0, -50%);

    .menu-list {
      margin-left: 10px;

      li {
        margin: 40px 0 0 30px;
        list-style: none;
        cursor: pointer;
        position: relative;

        .block {
          background-color: rgb(29, 144, 245);
          position: absolute;
          left: -40px;
          width: 6px;
          height: 25px;
          transition: 0.5s;
          border-top-right-radius: 4px;
          border-bottom-right-radius: 4px;
          opacity: 0;
        }

        &:hover {
          span {
            color: rgb(29, 144, 245);
          }

          .block {
            opacity: 1;
          }
        }
      }
    }
  }

  .own-pic {
    position: absolute;
    bottom: 10%;
    margin-left: 25px;
  }
}

.activeNav {
  span {
    color: rgb(29, 144, 245);
  }

  .block {
    opacity: 1 !important;
  }
}
</style>
