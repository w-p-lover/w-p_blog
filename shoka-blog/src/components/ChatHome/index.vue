<template>
  <div class="chatHome">
    <div class="chatLeft">
      <div class="title">
        <h1>辞书</h1>
      </div>
      <div class="conversation-search">
        <svg-icon icon-class="search" size="1rem"></svg-icon>
        <input v-model="searchKeyword" type="text" placeholder="搜索会话"/>
      </div>
      <div class="online-person">
        <span class="onlin-text">聊天列表 · {{ filteredPersonList.length }}</span>
        <div class="person-cards-wrapper">
          <div
              class="personList"
              v-for="personInfo in filteredPersonList"
              :key="personInfo.id"
              @click="clickPerson(personInfo)"
          >
            <PersonCard
                :personInfo="personInfo"
                :pcCurrent="pcCurrent"
            ></PersonCard>
          </div>
          <div class="empty-list" v-if="!filteredPersonList.length">没有找到匹配会话</div>
        </div>
      </div>
    </div>
    <div class="chatRight">
      <div v-if="showChatWindow">
        <ChatWindow
            :friendInfo="chatWindowInfo"
            @personCardSort="personCardSort"
            @conversationUpdate="updateConversation"
        ></ChatWindow>
      </div>
      <div class="showIcon" v-else>
        <span class="iconfont icon-snapchat"></span>
        <h2>选择一段对话</h2>
        <p>让消息在夜色里安静落座。</p>
      </div>
    </div>
  </div>
</template>

<script>
import PersonCard from "@/components/ChatHome/Chat/PersonCard.vue";
import ChatWindow from "./ChatWindow.vue";
import {getFriendList} from "@/api/chat/index.ts";
import useStore from "@/store";
import {filterConversations, updateConversationState} from "@/components/ChatHome/chatModel";
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
      searchKeyword: "",
      showChatWindow: false,
      chatWindowInfo: [],
    };
  },
  computed: {
    filteredPersonList() {
      return filterConversations(this.personList, this.searchKeyword);
    },
  },
  mounted() {
      getFriendList(user.id).then(({data}) => {
        this.personList = data.map((item) => ({
          ...item,
          lastMsg: item.lastMsg || item.detail || "还没有消息",
          lastTime: item.lastTime || "",
          unreadCount: item.unreadCount || 0,
        }));
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
      this.pcCurrent = info.id;
      info.unreadCount = 0;
    },
    updateConversation(payload) {
      this.personList = updateConversationState(this.personList, {
        ...payload,
        activeConversationId: this.pcCurrent,
      });
    },
    personCardSort(id) {
      if (!this.personList.length || id === this.personList[0].id) {
        return;
      }
        let nowPersonInfo;
        for (let i = 0; i < this.personList.length; i++) {
          if (this.personList[i].id == id) {
            nowPersonInfo = this.personList[i];
            this.personList.splice(i, 1);
            break;
          }
        }
      if (nowPersonInfo) {
        this.personList.unshift(nowPersonInfo);
      }
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
  color: rgb(117,120,137);
  transition: .3s;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.chatHome {
  display: flex;
  height: 100%;
  min-height: 0;
  color: #f2f0ea;

  .chatLeft {
    width: 292px;
    flex-shrink: 0;
    position: relative;
    z-index: 10;
    padding: 28px 18px 22px;
    box-sizing: border-box;
    border-right: 1px solid rgba(255, 255, 255, 0.06);
    background: rgba(18, 22, 30, 0.42);

    .title {
      color: #f2f0ea;
      padding-left: 2px;
      margin-top: 0;

      h1 {
        margin: 0;
        font-size: 28px;
        line-height: 1.1;
        font-weight: 700;
        letter-spacing: 0;
      }
    }

    .online-person {
      margin-top: 24px;

      .onlin-text {
        display: block;
        padding-left: 2px;
        color: rgba(220, 224, 218, 0.58);
        font-size: 13px;
        font-weight: 600;
      }

      .person-cards-wrapper {
        margin-top: 14px;
        max-height: calc(90vh - 190px);
        overflow-y: auto;
        box-sizing: border-box;
        padding-right: 4px;

        &::-webkit-scrollbar {
          width: 5px;
          background-color: transparent;
        }

        &::-webkit-scrollbar-thumb {
          background-color: rgba(210, 218, 211, 0.22);
          border-radius: 10px;
          transition: background-color 0.3s ease;
        }

        &::-webkit-scrollbar-thumb:hover {
          background-color: rgba(210, 218, 211, 0.34);
        }

        &::-webkit-scrollbar-track {
          background-color: transparent;
        }
      }

      .empty-list {
        padding: 26px 8px;
        color: rgba(220, 224, 218, 0.42);
        font-size: 13px;
        text-align: center;
      }

    }
  }

  .conversation-search {
    height: 40px;
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 24px;
    padding: 0 12px;
    border-radius: 10px;
    color: rgba(220, 224, 218, 0.52);
    background: rgba(255, 255, 255, 0.045);
    border: 1px solid rgba(255, 255, 255, 0.07);

    input {
      width: 100%;
      min-width: 0;
      color: #f2f0ea;
      font-size: 13px;
      background: transparent;
      border: 0;
      outline: none;

      &::placeholder {
        color: rgba(220, 224, 218, 0.36);
      }
    }

    &:focus-within {
      border-color: rgba(187, 205, 190, 0.34);
      box-shadow: 0 0 0 3px rgba(161, 181, 164, 0.09);
    }
  }

  .chatRight {
    flex: 1;
    min-width: 0;
    padding: 18px;

    > div {
      width: 100%;
      height: 100%;
    }

    .showIcon {
      height: 100%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      border-radius: 18px;
      background:
        linear-gradient(145deg, rgba(255, 255, 255, 0.045), rgba(255, 255, 255, 0.018)),
        rgba(20, 24, 31, 0.36);
      border: 1px solid rgba(255, 255, 255, 0.06);

      .icon-snapchat {
        font-size: 132px;
        color: rgba(214, 224, 216, 0.15);
      }

      h2 {
        margin: 18px 0 8px;
        color: rgba(242, 240, 234, 0.88);
        font-size: 22px;
        font-weight: 650;
      }

      p {
        margin: 0;
        color: rgba(220, 224, 218, 0.5);
        font-size: 14px;
      }
    }
  }
}

@media (max-width: 900px) {
  .chatHome {
    .chatLeft {
      width: 230px;
      padding: 22px 12px;
    }

    .chatRight {
      padding: 12px;
    }
  }
}

@media (max-width: 760px) {
  .chatHome {
    .chatLeft {
      width: 0;
      padding: 0;
      overflow: hidden;
      border-right: 0;
    }
  }
}


</style>
