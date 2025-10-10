<template>
  <div class="page-header">
    <h1 class="page-title">{{ photoInfo.albumName }}</h1>
    <img class="page-cover"
         src="https://wangyoupeng-penghong.oss-cn-beijing.aliyuncs.com/avatar/wallhaven-q21drl_2560x1440.png" alt="">
    <Waves></Waves>
  </div>
  <div class="bg">
    <div class="page-container">
      <div class="photo-container" v-viewer>
        <img
            class="photo"
            v-for="photo in photoInfo.photoVOList"
            :key="photo.id"
            :src="photo.photoUrl"
            loading="lazy"
            @load="e => e.target.style.opacity = 1"
        />

      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {getPhotoList} from '@/api/album';
import {Photo, PhotoInfo} from '@/api/album/types';

const route = useRoute();
const photoInfo = ref<PhotoInfo>({
  albumName: "",
  photoVOList: [] as Photo[],
});
onMounted(async () => {
  const { data } = await getPhotoList(Number(route.params.albumId))
  photoInfo.value = data.data
  nextTick(() => {
    const viewer = new Viewer(document.querySelector('.photo-container')!, {
      movable: false,
      navbar: false,
    });
  });
});
</script>

<style lang="scss" scoped>
.photo-container {
  display: flex;
  flex-wrap: wrap;
}

.photo {
  flex-grow: 1;
  height: 12.5rem;
  margin: 0.1875rem;
  cursor: pointer;
  object-fit: cover;
  transform: translateZ(0);
  will-change: transform, opacity;
}

@media (max-width: 567px) {
  .photo {
    width: 100%;
  }
}
</style>
