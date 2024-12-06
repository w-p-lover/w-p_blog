<template>
  <div class="chatHome">
    <div class="chatLeft">
      <div class="title">
        <h1>大猫聊天室</h1>
      </div>
      <div class="online-person">
        <span class="onlin-text">聊天列表</span>
        <div class="person-cards-wrapper">
          <div
              class="personList"
              v-for="personInfo in personList"
              :key="personInfo.id"
              @click="clickPerson(personInfo)"
          >
            <PersonCard
                :personInfo="personInfo"
                :pcCurrent="pcCurrent"
            ></PersonCard>
          </div>
        </div>
      </div>
    </div>
    <div class="chatRight">
      <div v-if="showChatWindow">
        <ChatWindow
            :friendInfo="chatWindowInfo"
            @personCardSort="personCardSort"
        ></ChatWindow>
      </div>
      <div class="showIcon" v-else>
        <span class="iconfont icon-snapchat"></span>
      </div>
    </div>
  </div>
</template>

<script>
import PersonCard from "@/components/ChatHome/Chat/PersonCard.vue";
import ChatWindow from "./chatwindow.vue";
import {getFriendList} from "@/api/chat/index.ts";
import useStore from "@/store";
const {user, app} = useStore();

export default {
  name: "ChatHome",  // 给组件命名
  components: {
    PersonCard,
    ChatWindow,
  },
  data() {
    return {
      currentUserUid:"",
      pcCurrent: "",
      personList: [],
      showChatWindow: false,
      chatWindowInfo: [],
    };
  },
  mounted() {
      getFriendList(user.id).then(({data}) => {
        this.personList = data;
      });
  },
  methods: {
    clickPerson(info) {
      this.showChatWindow = true;
      this.personInfo = info;
      //TODO 封住成一个单独的类实现，有空搞
      this.chatWindowInfo[0] = user.id
      this.chatWindowInfo[1] = info.id;
      this.chatWindowInfo[2] = info.headImg;
      this.chatWindowInfo[3] = info.detail;
      this.chatWindowInfo[4] = info.name;
      this.chatWindowInfo[5] = user.avatar;
      this.chatWindowInfo[6] = user.nickname
      console.log(this.chatWindowInfo);
      this.pcCurrent = info.id;
    },
    personCardSort(id) {
      if (id !== this.personList[0].id) {
        let nowPersonInfo;
        for (let i = 0; i < this.personList.length; i++) {
          if (this.personList[i].id == id) {
            nowPersonInfo = this.personList[i];
            this.personList.splice(i, 1);
            break;
          }
        }
        this.personList.unshift(nowPersonInfo);
      }
    },
  },
};
</script>

<style lang="scss" scoped>

@import url('@/assets/fonts/iconfont.css');
.iconfont {
  font-family: "iconfont" !important;
  font-style: normal;
  font-size: 25px;
  vertical-align: middle;
  color: rgb(117,120,137);
  transition: .3s;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.chatHome {
  display: flex;
  height: 80vh; /* 确保页面填充全屏 */

  .chatLeft {
    width: 280px; /* 固定宽度 */
    flex-shrink: 0; /* 防止被压缩 */
    position: relative; /* 保证 chatLeft 可以显示在前面 */
    z-index: 10; /* 确保 chatLeft 不会被 chatRight 遮挡 */

    .title {
      color: #fff;
      padding-left: 10px;
      margin-top: 20px;
    }

    .online-person {
      margin-top: 60px;

      .onlin-text {
        padding-left: 10px;
        color: rgb(176, 178, 189);
        font-size: 16px;
      }

      .person-cards-wrapper {
        padding-left: 10px;
        max-height: 550px; /* 根据页面调整最大高度 */
        overflow-y: auto; /* 启用垂直滚动条 */
        box-sizing: border-box;

        &::-webkit-scrollbar {
          width: 6px; /* 滚动条宽度 */
          background-color: #f1f1f1; /* 背景颜色 */
        }

        &::-webkit-scrollbar-thumb {
          background-color: #aaa; /* 滚动条颜色 */
          border-radius: 10px; /* 滚动条圆角 */
          transition: background-color 0.3s ease;
        }

        &::-webkit-scrollbar-thumb:hover {
          background-color: #888; /* 鼠标悬停时颜色 */
        }

        &::-webkit-scrollbar-track {
          background-color: #eaeaea; /* 滚动条轨道颜色 */
        }
      }

    }
  }

  .chatRight {
    flex: 1;
    padding-right: 30px;
    .showIcon {
      position: absolute;
      top: calc(50% - 150px); /*垂直居中 */
      left: calc(50% - 50px); /*水平居中 */
      .icon-snapchat {
        width: 300px;
        height: 300px;
        font-size: 300px;
        // color: rgb(28, 30, 44);
      }
    }
  }
}


</style>
