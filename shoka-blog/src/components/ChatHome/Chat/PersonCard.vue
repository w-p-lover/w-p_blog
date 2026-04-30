<template>
  <div class="person-card" :class="{ activeCard: personInfo.id == current }">
    <div class="info">
      <HeadPortrait :imgUrl="personInfo.headImg"></HeadPortrait>
      <div class="info-detail">
        <div class="name-row">
          <span class="name">{{ personInfo.name }}</span>
          <span v-if="formattedTime" class="time">{{ formattedTime }}</span>
          <span v-if="personInfo.clientStatus === 'failed'" class="status failed">未发出</span>
        </div>
        <div class="message-row">
          <div class="detail">{{ personInfo.lastMsg || personInfo.detail || '还没有消息' }}</div>
          <span v-if="personInfo.unreadCount" class="unread">{{ personInfo.unreadCount > 99 ? '99+' : personInfo.unreadCount }}</span>
        </div>
      </div>

    </div>
  </div>
</template>

<script>
import HeadPortrait from "./HeadPortrait.vue";

export default {
  props: {
    personInfo: {
      default: {},
    },
    pcCurrent: {
      default: ''
    }
  },
  components: {
    HeadPortrait,
  },
  data() {
    return {
      current: '',
    }
  },
  computed: {
    formattedTime() {
      if (!this.personInfo.lastTime) {
        return "";
      }
      const date = new Date(this.personInfo.lastTime);
      if (Number.isNaN(date.getTime())) {
        return "";
      }
      return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
    }
  },
  watch: {
    pcCurrent: function () {
      this.isActive()
    }
  },
  methods: {
    isActive() {
      this.current = this.pcCurrent
    }
  }
};
</script>

<style lang="scss" scoped>
.person-card {
  width: 100%;
  height: 74px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid transparent;
  position: relative;
  margin: 10px 0;
  cursor: pointer;
  transition: 0.22s ease;

  .info {
    position: absolute;
    left: 50%;
    top: 50%;
    width: 90%;
    transform: translate(-50%, -50%);
    overflow: hidden;
    display: flex;

    .info-detail {
      min-width: 0;
      margin-top: 4px;
      margin-left: 14px;

      .name-row {
        width: 100%;
        max-width: 170px;
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 5px;
      }

      .name {
        max-width: 150px;
        color: #f2f0ea;
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        font-size: 15px;
        font-weight: 600;
      }

      .status {
        flex: 0 0 auto;
        padding: 2px 5px;
        border-radius: 6px;
        font-size: 10px;
        line-height: 1.2;
      }

      .failed {
        color: #f0d7cc;
        background: rgba(178, 99, 91, 0.18);
        border: 1px solid rgba(224, 154, 145, 0.24);
      }

      .time {
        margin-left: auto;
        color: rgba(205, 211, 205, 0.36);
        font-size: 11px;
      }

      .message-row {
        max-width: 170px;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .detail {
        max-width: 140px;
        flex: 1;
        color: rgba(207, 211, 205, 0.58);
        overflow: hidden;
        white-space: nowrap;
        text-overflow: ellipsis;
        font-size: 12px;
      }

      .unread {
        min-width: 18px;
        height: 18px;
        padding: 0 5px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        box-sizing: border-box;
        border-radius: 999px;
        color: #1d2523;
        background: #b9c8bb;
        font-size: 10px;
        font-weight: 700;
      }
    }
  }

  &:hover {
    background: rgba(255, 255, 255, 0.075);
    border-color: rgba(214, 224, 216, 0.16);
    .info {
      .info-detail {
        .detail {
          color: rgba(242, 240, 234, 0.76);
        }
      }
    }
  }
}

.activeCard {
  background: linear-gradient(135deg, rgba(133, 155, 145, 0.34), rgba(90, 99, 106, 0.22));
  border-color: rgba(205, 219, 209, 0.28);
  box-shadow: inset 3px 0 0 #b9c8bb, 0 16px 30px rgba(0, 0, 0, 0.16);

  .info {
    .info-detail {
      .detail {
        color: rgba(242, 240, 234, 0.78);
      }
    }
  }
}
</style>
