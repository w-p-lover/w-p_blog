<template>
  <div class="page-header">
    <h1 class="page-title">{{ photoInfo.albumName }}</h1>
    <img
      class="page-cover"
      src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/wallhaven-q21drl_2560x1440.png"
      alt=""
    />
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="page-container">
      <div class="photo-container" v-viewer>
        <figure class="photo-item" v-for="photo in photoInfo.photoVOList" :key="photo.id">
          <img
            class="photo"
            :class="{ loaded: !!loadedPhotoMap[photo.id] }"
            v-lazy="photo.photoUrl"
            loading="lazy"
            decoding="async"
            fetchpriority="low"
            @load="handlePhotoLoad(photo.id)"
          />
        </figure>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getPhotoList } from "@/api/album";
import { Photo, PhotoInfo } from "@/api/album/types";
import Waves from "@/components/Waves/index.vue";

const route = useRoute();
const photoInfo = ref<PhotoInfo>({
  albumName: "",
  photoVOList: [] as Photo[],
});
const loadedPhotoMap = ref<Record<number, boolean>>({});

const handlePhotoLoad = (photoId: number) => {
  loadedPhotoMap.value[photoId] = true;
};

onMounted(async () => {
  const { data } = await getPhotoList(Number(route.params.albumId));
  photoInfo.value = data.data;
});
</script>

<style lang="scss" scoped>
.photo-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.photo-item {
  margin: 0;
  height: 12.5rem;
  flex-grow: 1;

  flex-basis: 14rem;
  min-width: 10.5rem;
  max-width: 26rem;
  border-radius: 0.5rem;
  overflow: hidden;
  background: linear-gradient(160deg, rgba(43, 64, 102, 0.14), rgba(58, 95, 158, 0.08));
  content-visibility: auto;
  contain: layout paint style;
  contain-intrinsic-size: 14rem 12.5rem;
}

.photo-item:nth-child(5n + 1) {
  flex-basis: 18rem;
}

.photo-item:nth-child(7n + 3) {
  flex-basis: 11.5rem;
}

.photo-item:nth-child(9n + 4) {
  flex-basis: 22rem;
}

.photo {
  width: 100%;
  height: 100%;
  cursor: pointer;
  object-fit: cover;
  opacity: 0;
  transform: scale(1.02);
  transition: opacity 0.25s ease, transform 0.35s ease;
}

.photo.loaded {
  opacity: 1;
  transform: scale(1);
}

@media (max-width: 567px) {
  .photo-container {
    display: block;
  }

  .photo-item {
    width: 100%;
    max-width: none;
    min-width: 0;
    height: 11rem;
  }
}
</style>
