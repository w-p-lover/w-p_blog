import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import dayjs from "dayjs";
import { getArticleList } from "@/api/article";
import { Article } from "@/api/article/types";
import { getTagList } from "@/api/tag";
import { Tag } from "@/api/tag/types";
import { PageQueryArticle } from "@/model";
import EventBus from "@/eventBus";

export function useArticleFeed() {
  const queryParams = reactive<PageQueryArticle>({
    current: 1, size: 5, sort: "id", start: null, end: null, tagId: null,
  });
  const selectedTag = ref<number | undefined>();
  const dateRange = ref<[Date, Date] | undefined>();
  const articleList = ref<Article[]>([]);
  const count = ref(0);
  const loading = ref(true);
  const failed = ref(false);
  const tagList = ref<Tag[]>([]);
  const tagsFailed = ref(false);
  const hasFilters = computed(() => queryParams.tagId != null || queryParams.start != null);
  let requestId = 0;

  async function loadArticles() {
    const id = ++requestId;
    loading.value = true;
    failed.value = false;
    try {
      const { data } = await getArticleList({ ...queryParams });
      if (id !== requestId) return;
      if (!data.flag) throw new Error(data.msg);
      articleList.value = data.data.recordList || [];
      count.value = data.data.count || 0;
    } catch {
      if (id !== requestId) return;
      articleList.value = [];
      count.value = 0;
      failed.value = true;
    } finally {
      if (id === requestId) loading.value = false;
    }
  }

  async function loadTags() {
    tagsFailed.value = false;
    try {
      const { data } = await getTagList();
      if (!data.flag) throw new Error(data.msg);
      tagList.value = data.data || [];
    } catch {
      tagsFailed.value = true;
    }
  }

  function changeSort(sort: string) {
    queryParams.sort = sort;
    queryParams.current = 1;
  }

  function filterByTag() {
    queryParams.tagId = selectedTag.value || null;
    queryParams.current = 1;
  }

  function filterByDate() {
    queryParams.start = dateRange.value ? dayjs(dateRange.value[0]).startOf("day").format("YYYY-MM-DD HH:mm:ss") : null;
    queryParams.end = dateRange.value ? dayjs(dateRange.value[1]).endOf("day").format("YYYY-MM-DD HH:mm:ss") : null;
    queryParams.current = 1;
  }

  function clearFilters() {
    selectedTag.value = undefined;
    dateRange.value = undefined;
    queryParams.tagId = null;
    queryParams.start = null;
    queryParams.end = null;
    queryParams.current = 1;
  }

  function refreshArticles() {
    if (queryParams.current === 1) void loadArticles();
    else queryParams.current = 1;
  }

  // 同一轮筛选中的多个字段变更只触发一次请求，旧响应不得覆盖新条件。
  watch(queryParams, loadArticles, { immediate: true });
  onMounted(() => {
    void loadTags();
    EventBus.on("refresh-articles", refreshArticles);
  });
  onBeforeUnmount(() => {
    requestId++;
    EventBus.off("refresh-articles", refreshArticles);
  });

  return {
    queryParams, selectedTag, dateRange, articleList, count, loading, failed,
    tagList, tagsFailed, hasFilters, changeSort, filterByTag, filterByDate,
    clearFilters, loadArticles, loadTags,
  };
}
