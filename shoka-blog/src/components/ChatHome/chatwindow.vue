<template>
  <div class="chat-window">
    <div class="top">
      <div class="head-pic">
        <HeadPortrait :imgUrl="friendInfo[2]"/>
      </div>
      <div class="info-detail">
        <div class="name">{{ shortName }}</div>
        <div class="detail">{{ shortDetail }}</div>
      </div>
      <div class="other-fun">
        <el-row type="flex" justify="space-between" align="middle">
          <!-- 视频按钮 -->
          <el-col :span="4">
            <span class="iconfont icon-shipin" @click="video"></span>
          </el-col>

          <!-- 电话按钮 -->
          <el-col :span="4">
            <span class="iconfont icon-gf-telephone" @click="telephone"></span>
          </el-col>

          <!-- 文件上传按钮 -->
          <el-col :span="4">
            <el-upload
                accept="doc/*"
                multiple
                action="http://localhost:8080/chat/upload?type=doc"
                :before-upload="beforeUpload"
                :on-success="sendFile"
                :show-file-list="false">
              <span class="iconfont icon-wenjian"></span>
            </el-upload>
          </el-col>

          <!-- 图片上传按钮 -->
          <el-col :span="4">
            <el-upload
                accept="image/*"
                multiple
                action="http://localhost:8080/chat/upload?type=img"
                :before-upload="beforeUpload"
                :on-success="sendImg"
                :show-file-list="false">
              <span class="iconfont icon-tupian"></span>
            </el-upload>
          </el-col>
        </el-row>
      </div>
    </div>
    <div class="botoom">
      <div class="chat-content" ref="chatContent">
        <div class="chat-wrapper" v-for="item in chatList" :key="item.id">
          <!-- 聊天内容 -->
          <div class="chat-friend" v-if="item.senderId != friendInfo[0]">
            <!-- 文字消息 -->
            <div class="chat-text" v-if="item.messageType === 'text'" v-html=item.content></div>
            <!-- 图片消息 -->
            <div class="chat-img" v-if="item.messageType === 'image'">
              <img :src="item.content" alt="表情"/>
              <!-- <el-image  :src="item.content" :preview-src-list="srcImgList"/>-->
            </div>
            <div class="chat-img" v-if="item.chatType == 2">
              <div class="word-file">
                <FileCard
                    :fileType="item.extend.fileType"
                    :file="item.content"
                ></FileCard>
              </div>
            </div>
            <div class="info-time">
              <img :src="friendInfo[2]" alt=""/>
              <span>{{ item.senderName }}</span>
              <span>{{ formatDateTime(item.createTime) }}</span>
            </div>
          </div>

          <div class="chat-me" v-else>
            <!-- 文字消息 -->
            <div class="chat-text" v-if="item.messageType === 'text'" v-html=item.content></div>
            <!-- 图片消息 -->
            <div class="chat-img" v-if="item.messageType == 'image'">
              <img :src="item.content" alt="表情"/>
              <!--              <el-image  :src="item.content" :preview-src-list="srcImgList"/>-->
            </div>
            <div class="chat-img" v-if="item.messageType == 2">
              <div class="word-file">
                <FileCard
                    :fileType="item.extend.fileType"
                    :file="item.content"
                ></FileCard>
              </div>
            </div>
            <div class="info-time">
              <img :src="friendInfo[5]" alt=""/>
              <span>{{ item.senderName }}</span>
              <span>{{ formatDateTime(item.createTime) }}</span>
            </div>
          </div>

        </div>
      </div>
      <!-- 输入框 -->
      <div class="chatInputs">
        <div class="emoji_box emoji">
          <Emoji emoji-ico="red" @add-emoji="handleEmoji"></Emoji>
        </div>
        <input v-model="inputMsg" class="inputs" @keyup.enter="sendText"/>
        <div class="send box_input" @click="sendText">
          <img src="../../assets/img/emoji/rocket.png" alt=""/>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {ref, reactive, onMounted} from "vue";
import HeadPortrait from "@/components/ChatHome/Chat/HeadPortrait.vue";
import FileCard from "@/components/ChatHome/Chat/FileCard.vue";
import {getChatMessage} from "@/api/chat/index.ts";
import WebSocketService from "@/api/chat/config.ts"
import {formatDateTime} from "@/utils/date.ts";
import emojiList from "@/utils/emoji";
import * as imageConversion from 'image-conversion';

export default {
  computed: {
    shortName() {
      return this.friendInfo[4].length > 4 ? this.friendInfo[4].slice(0, 4) + '...' : this.friendInfo[4];
    },
    shortDetail() {
      return this.friendInfo[3].length > 6 ? this.friendInfo[3].slice(0, 6) + '...' : this.friendInfo[3];
    }
  },
  methods: {
    formatDateTime,
    handleEmoji(emoji) {
      this.inputMsg += emoji;
    }
  },
  components: {HeadPortrait, FileCard},
  props: {
    friendInfo: {
      type: Array,
      required: true,
    },
  },

  setup(props) {
    //在script定义的变量赋值是没有被template获取的
    let chatList = reactive([]); // 使用 reactive 定义聊天列表
    const inputMsg = ref(""); // 输入框绑定值
    const srcImgList = reactive([]); // 图片列表
    const chatContent = ref(null); // 聊天内容 DOM 引用
    const webSocketService = new WebSocketService(); // WebSocket 服务实例

    const getFriendChatMsg = async () => {
      try {
        const {data} = await getChatMessage(props.friendInfo); // 调用接口获取数据
        chatList.splice(0, chatList.length, ...data); // 更新 chatList 数据
        srcImgList.splice(0, srcImgList.length); // 清空 srcImgList
        scrollBottom(); // 滚动到底部
      } catch (error) {
        console.error("Failed to fetch chat messages:", error);
      }
    };

    const onMessageReceived = (message) => {
      if (message.senderId == props.friendInfo[1]) {
        // 接收到。消息不是自己发的，添加到聊天列表
        console.log("接收消息");
        chatList.push(message);
        scrollBottom(); // 滚动到底部
      }
    };

    const scrollBottom = () => {
      //保证dom更新后调用，避免异步渲染导致的渲染不到位
      nextTick(() => {
        if (chatContent.value) {
          // 直接将 scrollTop 设置为 scrollHeight，确保滚动到最底部
          chatContent.value.scrollTop = chatContent.value.scrollHeight;
        }
      });
    };

    const sendText = () => {
      if (inputMsg.value) {
        inputMsg.value = inputMsg.value.replace(/\[.+?\]/g, (str) => {
          return (
              "<img src= '" +
              emojiList[str] +
              "' width='21' height='21' style='margin: 0 1px;vertical-align: text-bottom'/>"
          );
        });
        const message = {
          id: chatList.length + 1,
          content: inputMsg.value,
          messageType: 'text', // 文字消息
          senderName: props.friendInfo[6],
          name: props.friendInfo[4],
          createTime: new Date(),
          senderId: props.friendInfo[0], // 当前用户 ID
          receiveId: props.friendInfo[1],
          senderAvatar: props.friendInfo[2],
        };
        //发送的。在这里添加到聊天列表
        chatList.push(message);
        webSocketService.sendMessage(message);
        inputMsg.value = "";
        scrollBottom();
      }
    };

    // 发送图片
    const sendImg = async (response) => {
      console.log(response);
      const message = {
        id: chatList.length + 1,
        content: response, // 使用返回的 URL
        messageType: 'image', // 图片消息
        senderName: props.friendInfo[6],
        name: props.friendInfo[4],
        createTime: new Date(),
        senderId: props.friendInfo[0],
        receiveId: props.friendInfo[1],
        senderAvatar: props.friendInfo[2],
      };
      chatList.push(message); // 将消息推入聊天列表
      webSocketService.sendMessage(message); // 发送 WebSocket 消息
      scrollBottom(); // 滚动到底部
    };

    // 发送文件
    const sendFile = async (response) => {
      const message = {
        id: chatList.length + 1,
        content: response.url, // 文件链接
        messageType: 'file', // 文件消息
        senderName: props.friendInfo[6],
        name: props.friendInfo[4],
        createTime: new Date(),
        senderId: props.friendInfo[0],
        receiveId: props.friendInfo[1],
        senderAvatar: props.friendInfo[2],
        chatType: 2, // 文件类型
        extend: {fileType: file.type},
        msg: response.url,
      };
      chatList.push(message); // 将文件消息推入聊天列表
      webSocketService.sendMessage(message); // 发送 WebSocket 消息
      scrollBottom(); // 滚动到底部
    };
    const beforeUpload = (rawFile) => {
      return new Promise(resolve => {
        if (rawFile.size / 1024 < 200) {
          console.log(rawFile.type);
          resolve(rawFile);
        }
        // 压缩到200KB,这里的200就是要压缩的大小,可自定义
        imageConversion
            .compressAccurately(rawFile, 200)
            .then(res => {
              resolve(res);
            });
      });
    };

    watch(
        () => props.friendInfo[1],
        (newFriendInfo, oldFriendInfo) => {
          if (newFriendInfo !== oldFriendInfo) {
            getFriendChatMsg(); // 重新获取聊天信息
          }
        },
        {immediate: true} // 在组件加载时立即执行一次
    );

    onMounted(() => {
      webSocketService.connect(onMessageReceived);
      getFriendChatMsg();
    });

    onUnmounted(() => {
      webSocketService.disconnect();
    });

    return {
      chatList,
      inputMsg,
      chatContent,
      srcImgList,
      sendText,
      sendImg,
      sendFile,
      beforeUpload
    };
  },
};
</script>

<style scoped>
@import url('@/assets/fonts/iconfont.css');

.iconfont {
  font-family: "iconfont" !important;
  font-style: normal;
  font-size: 25px;
  vertical-align: middle;
  color: rgb(117, 120, 137);
  transition: .3s;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.chat-window {
  position: relative;
  height: 100%;
  width: 100%;
  background-color: rgb(39, 42, 55);

  .top {
    margin-bottom: 40px;
    display: flex; /* 使用 Flexbox 布局 */
    align-items: center; /* 垂直居中对齐 */

    &::after {
      content: "";
      display: block;
      clear: both;
    }

    .head-pic {
      float: left;
    }

    .info-detail {
      float: left;
      margin: 5px 20px 0;

      .name {
        font-size: 16px;
        font-weight: 600;
        color: #fff;
      }

      .detail {
        color: #9e9e9e;
        font-size: 12px;
        margin-top: -5.5px;
      }
    }

    .other-fun {
      float: right;
      margin-top: 20px;
      display: table-column;

      span {
        margin-left: 30px;
        cursor: pointer;
      }

      input {
        display: none;
      }
    }
  }

  .botoom {
    margin-left: 15px;
    width: 97.5%;
    height: 66vh;
    background-color: rgb(56, 60, 75);
    border-radius: 20px;
    padding: 20px;
    box-sizing: border-box;
    position: relative;

    .chat-content {
      width: 100%;
      height: 85%;
      overflow-y: auto;
      padding: 20px;
      box-sizing: border-box;

      &::-webkit-scrollbar {
        width: 0; /* Safari,Chrome 隐藏滚动条 */
        height: 0; /* Safari,Chrome 隐藏滚动条 */
        display: none; /* 移动端、pad 上Safari，Chrome，隐藏滚动条 */
      }

      .chat-wrapper {
        position: relative;
        word-break: break-all;

        .chat-friend {
          width: 100%;
          float: left;
          margin-bottom: 20px;
          display: flex;
          flex-direction: column;
          justify-content: flex-start;
          align-items: flex-start;

          .chat-text {
            max-width: 90%;
            padding: 20px;
            border-radius: 20px 20px 20px 5px;
            background-color: rgb(82, 85, 97);
            color: #fff;

            &:hover {
              background-color: rgb(68, 69, 78);
            }
          }

          .chat-img {
            img {
              max-width: 300px;
              max-height: 200px;
              border-radius: 10px;
            }
          }

          .info-time {
            margin: 10px 0;
            color: #fff;
            font-size: 14px;

            img {
              width: 30px;
              height: 30px;
              border-radius: 50%;
              vertical-align: middle;
              margin-right: 10px;
            }

            span:last-child {
              color: rgb(101, 104, 115);
              margin-left: 10px;
              vertical-align: middle;
            }
          }
        }

        .chat-me {
          width: 100%;
          float: right;
          margin-bottom: 20px;
          position: relative;
          display: flex;
          flex-direction: column;
          justify-content: flex-end;
          align-items: flex-end;

          .chat-text {
            float: right;
            max-width: 90%;
            padding: 20px;
            border-radius: 20px 20px 5px 20px;
            background-color: rgb(29, 144, 245);
            color: #fff;

            &:hover {
              background-color: rgb(26, 129, 219);
            }
          }

          .chat-img {
            img {
              max-width: 300px;
              max-height: 200px;
              border-radius: 10px;
            }
          }

          .info-time {
            margin: 10px 0;
            color: #fff;
            font-size: 14px;
            display: flex;
            justify-content: flex-end;

            img {
              width: 30px;
              height: 30px;
              border-radius: 50%;
              vertical-align: middle;
              margin-left: 10px;
            }

            span {
              line-height: 30px;
            }

            span:last-child {
              color: rgb(101, 104, 115);
              margin-left: 10px;
              vertical-align: middle;
            }
          }
        }
      }
    }

    .chatInputs {
      width: 90%;
      position: absolute;
      bottom: 0;
      margin: 3%;
      display: flex;

      .emoji_box {
        width: 24.5px;
        height: 25.5px;
        background-color: rgb(66, 70, 86);
        border-radius: 15px;
        border: 1px solid rgb(80, 85, 103);
      }

      .box_input {
        width: 50px;
        height: 50px;
        background-color: rgb(66, 70, 86);
        border-radius: 15px;
        border: 1px solid rgb(80, 85, 103);
        position: relative;
        cursor: pointer;

        img {
          width: 30px;
          height: 30px;
          position: absolute;
          left: 50%;
          top: 50%;
          transform: translate(-50%, -50%);
        }
      }

      .emoji {
        transition: 0.3s;

        &:hover {
          background-color: rgb(46, 49, 61);
          border: 1px solid rgb(71, 73, 82);
        }
      }

      .inputs {
        width: 90%;
        height: 50px;
        background-color: rgb(66, 70, 86);
        border-radius: 15px;
        border: 2px solid rgb(34, 135, 225);
        padding: 10px;
        box-sizing: border-box;
        transition: 0.2s;
        font-size: 20px;
        color: #fff;
        font-weight: 100;
        margin: 0 20px;

        &:focus {
          outline: none;
        }
      }

      .send {
        background-color: rgb(29, 144, 245);
        border: 0;
        transition: 0.3s;
        box-shadow: 0px 0px 5px 0px rgba(0, 136, 255);

        &:hover {
          box-shadow: 0px 0px 10px 0px rgba(0, 136, 255);
        }
      }
    }
  }

  @media (max-width: 900px) {
    .info-detail .detail {
      display: none;
    }
  }

}
</style>
