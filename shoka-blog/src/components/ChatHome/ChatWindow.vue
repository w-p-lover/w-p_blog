<template>
  <div class="chat-window">
    <div class="top">
      <div class="head-pic">
        <HeadPortrait :imgUrl="friendInfo[2]"/>
      </div>
      <div class="info-detail">
        <div class="name">{{ shortName }}</div>
        <div class="detail">{{ connectionText }} · {{ shortDetail }}</div>
      </div>
      <div class="other-fun">
        <el-row type="flex" justify="space-between" align="middle">
          <!-- 视频按钮 -->
          <el-col :span="4">
            <span class="iconfont icon-shipin action-icon" title="视频" @click="video"></span>
          </el-col>

          <!-- 电话按钮 -->
          <el-col :span="4">
            <span class="iconfont icon-gf-telephone action-icon" title="电话" @click="telephone"></span>
          </el-col>

          <!-- 文件上传按钮 -->
          <el-col :span="4">
            <el-upload
                accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx,.pdf,.zip,.txt"
                multiple
                action="http://localhost:8080/chat/upload?type=file"
                :before-upload="beforeUploadFile"
                :on-success="sendFile"
                :on-error="handleUploadError"
                :show-file-list="false">
              <span class="iconfont icon-wenjian action-icon" title="发送文件"></span>
            </el-upload>
          </el-col>

          <!-- 图片上传按钮 -->
          <el-col :span="4">
            <el-upload
                accept="image/*"
                multiple
                action="http://localhost:8080/chat/upload?type=img"
                :before-upload="beforeUploadImage"
                :on-success="sendImg"
                :on-error="handleUploadError"
                :show-file-list="false">
              <span class="iconfont icon-tupian action-icon" title="发送图片"></span>
            </el-upload>
          </el-col>
        </el-row>
      </div>
    </div>
    <div class="botoom">
      <div class="chat-content" ref="chatContent">
        <div class="history-loader">
          <button
              v-if="historyHasMore"
              class="history-button"
              type="button"
              :disabled="historyLoading"
              @click="loadOlderMessages"
          >{{ historyLoading ? "加载中..." : "加载更早消息" }}</button>
          <span v-else-if="chatList.length" class="history-end">已到达最早消息</span>
          <span v-if="historyError" class="history-error">{{ historyError }}</span>
        </div>
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
            <div class="chat-img" v-if="item.messageType === 'file'">
              <div class="word-file">
                <FileCard
                    :fileType="item.fileInfo.fileType"
                    :file="item.content"
                    :fileName="item.fileInfo.fileName"
                    :fileSize="item.fileInfo.fileSize"
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
              <!--              <el-image  :src="item.content" :preview-src-list="srcImgList"/>-->
            </div>
            <div class="chat-img" v-if="item.messageType == 'file'">
              <div class="word-file">
                <FileCard
                    :fileType="item.fileInfo.fileType"
                    :file="item.content"
                    :fileName="item.fileInfo.fileName"
                    :fileSize="item.fileInfo.fileSize"
                ></FileCard>
              </div>
            </div>
            <div class="info-time">
              <img :src="friendInfo[5]" alt=""/>
              <span>{{ item.senderName }}</span>
              <span>{{ formatDateTime(item.createTime) }}</span>
              <button
                  v-if="item.clientStatus === 'failed'"
                  class="message-status retry"
                  type="button"
                  @click="retryMessage(item)"
              >重发</button>
              <span v-else-if="item.clientStatus === 'sending'" class="message-status">发送中</span>
            </div>
          </div>

        </div>
      </div>
      <!-- 输入框 -->
      <div class="chatInputs">
        <div class="emoji_box emoji">
          <Emoji emoji-ico="red" @add-emoji="handleEmoji"></Emoji>
        </div>
        <input v-model="inputMsg" class="inputs" placeholder="写点什么..." @keyup.enter="sendText"/>
        <div class="send box_input" :class="{ disabled: !inputMsg.trim() }" @click="sendText">
          <img src="../../assets/img/emoji/rocket.png" alt=""/>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {computed, nextTick, onMounted, onUnmounted, reactive, ref, watch} from "vue";
import HeadPortrait from "@/components/ChatHome/Chat/HeadPortrait.vue";
import FileCard from "@/components/ChatHome/Chat/FileCard.vue";
import {getChatMessagePage} from "@/api/chat/index.ts";
import WebSocketService from "@/api/chat/config.ts"
import {formatDateTime} from "@/utils/date.ts";
import emojiList from "@/utils/emoji";
import * as imageConversion from 'image-conversion';
import {
  createOutgoingMessage,
  formatFileSize,
  getFileTypeByMime,
  getMessagePreview,
  mergeOlderMessages,
  renderEmojiContent,
  shouldCompressUpload
} from "@/components/ChatHome/chatModel";

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
  emits: ["personCardSort", "conversationUpdate"],
  props: {
    friendInfo: {
      type: Array,
      required: true,
    },
  },

  setup(props, {emit}) {
    //在script定义的变量赋值是没有被template获取的
    let chatList = reactive([]); // 使用 reactive 定义聊天列表
    const fileName = ref("");
    const fileSize = ref("");
    const extension = ref("");
    const inputMsg = ref(""); // 输入框绑定值
    const srcImgList = reactive([]); // 图片列表
    const chatContent = ref(null); // 聊天内容 DOM 引用
    const webSocketService = new WebSocketService(); // WebSocket 服务实例
    const isConnected = ref(false);
    const uploadFileInfo = ref(null);
    const connectionText = computed(() => isConnected.value ? "在线" : "离线");
    const historyPage = ref(1);
    const historyPageSize = 20;
    const historyHasMore = ref(false);
    const historyLoading = ref(false);
    const historyError = ref("");

    const normalizeHistoryResponse = (data) => {
      return data?.records ? data : data?.data;
    };

    const normalizeHistoryMessage = (item) => ({
      ...item,
      id: item.id || item.messageId || `${item.senderId}-${item.createTime}-${item.content}`,
      receiveId: item.receiveId || item.receiverId,
      clientStatus: "sent",
    });

    const fetchHistoryPage = async (pageNum) => {
      const {data} = await getChatMessagePage({
        senderId: props.friendInfo[0],
        receiveId: props.friendInfo[1],
        pageNum,
        pageSize: historyPageSize,
      });
      const page = normalizeHistoryResponse(data);
      return {
        records: (page?.records || []).map(normalizeHistoryMessage),
        hasMore: Boolean(page?.hasMore),
      };
    };

    const getFriendChatMsg = async () => {
      historyLoading.value = true;
      historyError.value = "";
      try {
        historyPage.value = 1;
        const page = await fetchHistoryPage(historyPage.value);
        chatList.splice(0, chatList.length, ...page.records); // 更新 chatList 数据
        historyHasMore.value = page.hasMore;
        srcImgList.splice(0, srcImgList.length); // 清空 srcImgList
        scrollBottom(); // 滚动到底部
      } catch (error) {
        console.error("Failed to fetch chat messages:", error);
        historyError.value = "聊天记录加载失败";
      } finally {
        historyLoading.value = false;
      }
    };

    const loadOlderMessages = async () => {
      if (historyLoading.value || !historyHasMore.value) {
        return;
      }
      const container = chatContent.value;
      const previousScrollHeight = container?.scrollHeight || 0;
      const previousScrollTop = container?.scrollTop || 0;
      historyLoading.value = true;
      historyError.value = "";
      try {
        const nextPage = historyPage.value + 1;
        const page = await fetchHistoryPage(nextPage);
        const mergedMessages = mergeOlderMessages(chatList, page.records);
        chatList.splice(0, chatList.length, ...mergedMessages);
        historyPage.value = nextPage;
        historyHasMore.value = page.hasMore;
        nextTick(() => {
          if (chatContent.value) {
            chatContent.value.scrollTop = chatContent.value.scrollHeight - previousScrollHeight + previousScrollTop;
          }
        });
      } catch (error) {
        console.error("Failed to fetch older chat messages:", error);
        historyError.value = "更早消息加载失败";
      } finally {
        historyLoading.value = false;
      }
    };

    const onMessageReceived = (message) => {
      if (message.senderId == props.friendInfo[1]) {
        // 接收到。消息不是自己发的，添加到聊天列表
        const receivedMessage = {
          ...message,
          id: message.id || message.localId || `${message.senderId}-${message.createTime}-${message.content}`,
          clientStatus: "sent",
        };
        chatList.push(receivedMessage);
        emitConversationUpdate(receivedMessage);
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

    const markMessageStatus = (localId, status) => {
      const message = chatList.find((item) => item.localId === localId || item.id === localId);
      if (message) {
        message.clientStatus = status;
      }
    };

    const pushAndSendMessage = (message) => {
      chatList.push(message);
      const sent = webSocketService.sendMessage(message);
      markMessageStatus(message.localId, sent ? "sent" : "failed");
      emitConversationUpdate(message);
      scrollBottom();
      if (!sent) {
        window.$message?.warning("消息暂未发出，连接恢复后可重试");
      }
      return sent;
    };

    const emitConversationUpdate = (message) => {
      emit("personCardSort", props.friendInfo[1]);
      emit("conversationUpdate", {
        id: props.friendInfo[1],
        lastMsg: getMessagePreview(message),
        lastTime: message.createTime,
        clientStatus: message.clientStatus,
        incoming: message.senderId == props.friendInfo[1],
      });
    };

    const sendText = () => {
      const content = renderEmojiContent(inputMsg.value, emojiList);
      if (!content) {
        return;
      }
      const message = createOutgoingMessage({
        content,
        messageType: "text",
        senderName: props.friendInfo[6],
        receiverName: props.friendInfo[4],
        senderId: props.friendInfo[0],
        receiveId: props.friendInfo[1],
        senderAvatar: props.friendInfo[5],
      });
      pushAndSendMessage(message);
      inputMsg.value = "";
    };

    // 发送图片
    const sendImg = async (response) => {
      if (!response) {
        window.$message?.error("图片上传失败");
        return;
      }
      const message = createOutgoingMessage({
        content: response,
        messageType: "image",
        senderName: props.friendInfo[6],
        senderId: props.friendInfo[0],
        receiveId: props.friendInfo[1],
        receiverName: props.friendInfo[4],
        senderAvatar: props.friendInfo[5],
      });
      pushAndSendMessage(message);
    };

    //上传文件
    const sendFile = async (response) => {
      if (!response) {
        window.$message?.error("文件上传失败");
        return;
      }
      const currentFileInfo = uploadFileInfo.value || {
        fileType: getFileTypeByMime(extension.value, fileName.value),
        fileName: fileName.value,
        fileSize: fileSize.value,
      };
      // 构造消息对象
      const message = createOutgoingMessage({
        content: response,
        messageType: "file",
        senderName: props.friendInfo[6],
        senderId: props.friendInfo[0],
        receiveId: props.friendInfo[1],
        receiverName: props.friendInfo[4],
        senderAvatar: props.friendInfo[5],
        fileInfo: currentFileInfo,
      });

      // 将文件消息推入聊天列表
      pushAndSendMessage(message);
    };

    const cacheUploadInfo = (rawFile) => {
      fileName.value = rawFile.name;
      fileSize.value = formatFileSize(rawFile.size);
      extension.value = rawFile.type;
      uploadFileInfo.value = {
        fileType: getFileTypeByMime(rawFile.type, rawFile.name),
        fileName: rawFile.name,
        fileSize: formatFileSize(rawFile.size),
      };
    };

    const beforeUploadImage = (rawFile) => {
      cacheUploadInfo(rawFile);
      if (!rawFile.type?.startsWith("image/")) {
        window.$message?.warning("请选择图片文件");
        return false;
      }
      if (!shouldCompressUpload(rawFile, "img")) {
        return rawFile;
      }
      return new Promise(resolve => {
        imageConversion
            .compressAccurately(rawFile, 200)
            .then(res => {
              resolve(res);
            });
      });
    };

    const beforeUploadFile = (rawFile) => {
      cacheUploadInfo(rawFile);
      return rawFile;
    };

    const retryMessage = (message) => {
      message.clientStatus = "sending";
      const sent = webSocketService.sendMessage(message);
      message.clientStatus = sent ? "sent" : "failed";
      emitConversationUpdate(message);
      if (!sent) {
        window.$message?.warning("当前连接不可用，稍后再试");
      }
    };

    const handleUploadError = () => {
      window.$message?.error("上传失败，请检查后端服务或网络连接");
    };

    const video = () => {
      window.$message?.info("视频通话还在接入中");
    };

    const telephone = () => {
      window.$message?.info("语音通话还在接入中");
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
      webSocketService.connect(onMessageReceived, (connected) => {
        isConnected.value = connected;
      });
    });

    onUnmounted(() => {
      webSocketService.disconnect();
    });

    return {
      chatList,
      inputMsg,
      chatContent,
      srcImgList,
      connectionText,
      historyHasMore,
      historyLoading,
      historyError,
      loadOlderMessages,
      sendText,
      sendImg,
      sendFile,
      beforeUploadImage,
      beforeUploadFile,
      retryMessage,
      handleUploadError,
      video,
      telephone
    };
  },
};
</script>

<style lang="scss" scoped>
@import url('@/assets/fonts/iconfont.css');

.iconfont {
  font-family: "iconfont",serif !important;
  font-style: normal;
  font-size: 22px;
  vertical-align: middle;
  color: rgba(214, 224, 216, 0.62);
  transition: .3s;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.chat-window {
  display: flex;
  flex-direction: column;
  min-height: 0;
  height: 100%;
  width: 100%;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(36, 41, 51, 0.78), rgba(22, 26, 34, 0.9)),
    rgba(21, 25, 32, 0.86);
  border: 1px solid rgba(255, 255, 255, 0.07);
  border-radius: 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);

  .top {
    flex: 0 0 86px;
    display: flex;
    align-items: center;
    padding: 18px 24px;
    box-sizing: border-box;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    background: rgba(255, 255, 255, 0.025);

    .head-pic {
      flex: 0 0 auto;
    }

    .info-detail {
      min-width: 0;
      margin: 2px 20px 0 16px;

      .name {
        font-size: 17px;
        font-weight: 650;
        color: #f2f0ea;
      }

      .detail {
        color: rgba(220, 224, 218, 0.54);
        font-size: 12px;
        margin-top: 5px;
      }
    }

    .other-fun {
      margin-left: auto;

      :deep(.el-row) {
        gap: 10px;
      }

      :deep(.el-col) {
        width: auto;
        max-width: none;
        flex: 0 0 auto;
      }

      span {
        cursor: pointer;
      }

      .action-icon {
        width: 38px;
        height: 38px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.045);
        border: 1px solid rgba(255, 255, 255, 0.07);

        &:hover {
          color: #f1eee7;
          background: rgba(185, 200, 187, 0.13);
          border-color: rgba(205, 219, 209, 0.2);
        }
      }
    }
  }

  .botoom {
    flex: 1;
    min-height: 0;
    width: 100%;
    display: flex;
    flex-direction: column;
    background:
      linear-gradient(180deg, rgba(255, 255, 255, 0.018), transparent 44%),
      rgba(17, 21, 28, 0.38);
    padding: 0;
    box-sizing: border-box;

    .chat-content {
      width: 100%;
      flex: 1;
      min-height: 0;
      overflow-y: auto;
      padding: 28px 30px 22px;
      box-sizing: border-box;

      &::-webkit-scrollbar {
        width: 5px;
        height: 5px;
      }

      &::-webkit-scrollbar-thumb {
        background: rgba(210, 218, 211, 0.18);
        border-radius: 10px;
      }

      &::-webkit-scrollbar-track {
        background: transparent;
      }

      .history-loader {
        min-height: 34px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 10px;
        margin: -8px 0 14px;
        color: rgba(221, 225, 218, 0.44);
        font-size: 12px;

        .history-button {
          height: 28px;
          padding: 0 14px;
          border: 1px solid rgba(222, 229, 218, 0.12);
          border-radius: 8px;
          background: rgba(255, 255, 255, 0.045);
          color: rgba(236, 235, 228, 0.78);
          cursor: pointer;
          transition: .2s ease;

          &:hover:not(:disabled) {
            background: rgba(195, 207, 190, 0.13);
            border-color: rgba(222, 229, 218, 0.22);
            color: #f2f0ea;
          }

          &:disabled {
            cursor: default;
            opacity: .55;
          }
        }

        .history-end {
          color: rgba(221, 225, 218, 0.34);
        }

        .history-error {
          color: rgba(246, 143, 123, 0.86);
        }
      }

      .chat-wrapper {
        position: relative;
        word-break: break-all;
        display: flow-root;

        .chat-friend {
          width: 100%;
          margin-bottom: 22px;
          display: flex;
          flex-direction: column;
          justify-content: flex-start;
          align-items: flex-start;

          .chat-text {
            max-width: min(72%, 620px);
            padding: 13px 16px;
            border-radius: 14px 14px 14px 5px;
            background: rgba(255, 255, 255, 0.075);
            border: 1px solid rgba(255, 255, 255, 0.07);
            color: rgba(247, 246, 241, 0.94);
            line-height: 1.7;
            box-shadow: 0 12px 28px rgba(0, 0, 0, 0.13);

            &:hover {
              background: rgba(255, 255, 255, 0.095);
            }
          }

          .chat-img {
            img {
              max-width: min(300px, 68vw);
              max-height: 220px;
              border-radius: 12px;
              border: 1px solid rgba(255, 255, 255, 0.08);
              box-shadow: 0 14px 28px rgba(0, 0, 0, 0.22);
            }
          }

          .info-time {
            margin: 8px 0 0;
            color: rgba(242, 240, 234, 0.72);
            font-size: 12px;
            display: flex;
            align-items: center;

            img {
              width: 26px;
              height: 26px;
              border-radius: 50%;
              margin-right: 8px;
            }

            span:last-child {
              color: rgba(205, 211, 205, 0.38);
              margin-left: 8px;
            }
          }
        }

        .chat-me {
          width: 100%;
          margin-bottom: 22px;
          display: flex;
          flex-direction: column;
          justify-content: flex-end;
          align-items: flex-end;

          .chat-text {
            max-width: min(72%, 620px);
            padding: 13px 16px;
            border-radius: 14px 14px 5px 14px;
            background: linear-gradient(135deg, rgba(132, 151, 142, 0.9), rgba(83, 103, 105, 0.92));
            border: 1px solid rgba(216, 227, 216, 0.16);
            color: #fbfaf5;
            line-height: 1.7;
            box-shadow: 0 14px 30px rgba(15, 22, 24, 0.26);

            &:hover {
              filter: brightness(1.04);
            }
          }

          .chat-img {
            img {
              max-width: min(300px, 68vw);
              max-height: 220px;
              border-radius: 12px;
              border: 1px solid rgba(255, 255, 255, 0.1);
              box-shadow: 0 14px 28px rgba(0, 0, 0, 0.24);
            }
          }

          .info-time {
            margin: 8px 0 0;
            color: rgba(242, 240, 234, 0.72);
            font-size: 12px;
            display: flex;
            align-items: center;
            justify-content: flex-end;

            img {
              width: 26px;
              height: 26px;
              border-radius: 50%;
              margin-left: 8px;
            }

            span {
              line-height: 26px;
            }

            span:last-child {
              color: rgba(205, 211, 205, 0.38);
              margin-left: 8px;
            }

            .message-status {
              margin-left: 8px;
              color: rgba(205, 211, 205, 0.44);
              font-size: 12px;
            }

            .retry {
              height: 24px;
              padding: 0 8px;
              color: #efe8d6;
              cursor: pointer;
              background: rgba(178, 99, 91, 0.18);
              border: 1px solid rgba(224, 154, 145, 0.28);
              border-radius: 7px;
              transition: 0.2s ease;

              &:hover {
                background: rgba(178, 99, 91, 0.28);
              }
            }
          }
        }
      }
    }

    .chatInputs {
      flex: 0 0 auto;
      width: auto;
      display: flex;
      align-items: center;
      gap: 12px;
      margin: 0 24px 24px;
      padding: 12px;
      border-radius: 16px;
      background: rgba(255, 255, 255, 0.055);
      border: 1px solid rgba(255, 255, 255, 0.08);
      box-shadow: 0 18px 40px rgba(0, 0, 0, 0.18);

      .emoji_box {
        width: 42px;
        height: 42px;
        flex: 0 0 42px;
        display: flex;
        align-items: center;
        justify-content: center;
        background-color: rgba(255, 255, 255, 0.045);
        border-radius: 10px;
        border: 1px solid rgba(255, 255, 255, 0.07);
      }

      .box_input {
        width: 46px;
        height: 42px;
        flex: 0 0 46px;
        background-color: rgba(255, 255, 255, 0.045);
        border-radius: 10px;
        border: 1px solid rgba(255, 255, 255, 0.07);
        position: relative;
        cursor: pointer;
        transition: 0.2s ease;

        img {
          width: 25px;
          height: 25px;
          position: absolute;
          left: 50%;
          top: 50%;
          transform: translate(-50%, -50%);
        }
      }

      .emoji {
        transition: 0.3s;

        &:hover {
          background-color: rgba(185, 200, 187, 0.13);
          border-color: rgba(205, 219, 209, 0.2);
        }
      }

      .inputs {
        flex: 1;
        min-width: 0;
        height: 42px;
        background-color: rgba(14, 17, 23, 0.52);
        border-radius: 10px;
        border: 1px solid rgba(205, 219, 209, 0.14);
        padding: 0 14px;
        box-sizing: border-box;
        transition: 0.2s;
        font-size: 15px;
        color: #f5f2ea;
        font-weight: 400;
        outline: none;

        &::placeholder {
          color: rgba(220, 224, 218, 0.38);
        }

        &:focus {
          border-color: rgba(187, 205, 190, 0.46);
          box-shadow: 0 0 0 3px rgba(161, 181, 164, 0.11);
        }
      }

      .send {
        background: linear-gradient(135deg, #9eb29f, #637c7e);
        border: 0;
        transition: 0.3s;
        box-shadow: 0 12px 28px rgba(57, 76, 74, 0.28);

        &:hover {
          filter: brightness(1.05);
          box-shadow: 0 14px 32px rgba(57, 76, 74, 0.38);
        }

        &.disabled {
          cursor: default;
          opacity: 0.48;
          filter: saturate(0.75);
          box-shadow: none;
        }
      }
    }
  }

  @media (max-width: 900px) {
    .top {
      padding: 16px;

      .other-fun {
        :deep(.el-row) {
          gap: 6px;
        }

        .action-icon {
          width: 34px;
          height: 34px;
        }
      }
    }

    .info-detail .detail {
      display: none;
    }

    .botoom {
      .chat-content {
        padding: 20px 16px 16px;
      }

      .chatInputs {
        margin: 0 14px 14px;
        gap: 8px;
      }
    }
  }

  @media (max-width: 640px) {
    .top {
      .other-fun {
        display: none;
      }
    }

    .botoom {
      .chat-content {
        .chat-wrapper {
          .chat-friend,
          .chat-me {
            .chat-text {
              max-width: 86%;
            }
          }
        }
      }
    }
  }

}
</style>
