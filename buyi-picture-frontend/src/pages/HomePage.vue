<template>
  <div id="homePage">
    <!-- 搜索框区域优化 -->
    <div class="search-container">
      <h1 class="site-title">
        <span class="gradient-text">图片搜索</span>
        <div class="subtitle">发现精彩视觉世界</div>
      </h1>
      <div class="search-bar">
        <a-input-search
          v-model:value="searchParams.searchText"
          placeholder="从海量图片中搜索"
          enter-button="搜索"
          size="large"
          @search="doSearch"
        />
      </div>
    </div>

    <!-- 分类标签区域优化 -->
    <div class="filter-container">
      <a-tabs v-model:active-key="selectedCategory" @change="doSearch" class="custom-tabs">
        <a-tab-pane key="all" tab="全部" />
        <a-tab-pane v-for="category in categoryList" :tab="category" :key="category" />
      </a-tabs>

      <div class="tag-bar">
        <span class="tag-label">标签：</span>
        <a-space :size="[0, 8]" wrap>
          <a-checkable-tag
            v-for="(tag, index) in tagList"
            :key="tag"
            v-model:checked="selectedTagList[index]"
            @change="doSearch"
            class="custom-tag"
          >
            {{ tag }}
          </a-checkable-tag>
        </a-space>
      </div>
    </div>

    <!-- 图片列表优化 -->
    <div class="picture-container">
      <a-list
        :grid="{ gutter: 24, xs: 1, sm: 2, md: 3, lg: 4, xl: 4, xxl: 5 }"
        :data-source="dataList"
        :pagination="pagination"
        :loading="loading"
      >
        <template #renderItem="{ item: picture }">
          <a-list-item class="picture-item">
            <a-card hoverable @click="doClickPicture(picture)" class="picture-card">
              <template #cover>
                <img :alt="picture.name" :src="picture.thumbnailUrl" class="picture-image" />
              </template>
              <a-card-meta :title="picture.name">
                <template #description>
                  <a-flex gap="small">
                    <a-tag color="default">{{ picture.category ?? '默认' }}</a-tag>
                    <ColorfulTag v-for="tag in picture.tags" :key="tag" :text="tag" />
                  </a-flex>
                </template>
              </a-card-meta>
            </a-card>
          </a-list-item>
        </template>
      </a-list>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  listPictureTagCategoryUsingGet,
  listPictureVoByPageUsingPost,
} from '@/api/pictureController'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router' // 定义数据
import ColorfulTag from '@/components/ColorfulTag.vue'
// 定义数据
const dataList = ref<API.PictureVO[]>([])
const total = ref(0)
const loading = ref(true)
// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 20,
  sortField: 'createTime',
  sortOrder: 'descend',
})
// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const params = {
    ...searchParams,
    tags: [] as string[],
  }
  if (selectedCategory.value !== 'all') {
    params.category = selectedCategory.value
  }
  // [true, false, false] => ['java']
  selectedTagList.value.forEach((useTag, index) => {
    if (useTag) {
      params.tags.push(tagList.value[index])
    }
  })
  const res = await listPictureVoByPageUsingPost(params)
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}
// 页面加载时获取数据，请求一次
onMounted(() => {
  fetchData()
})
// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.current,
    pageSize: searchParams.pageSize,
    total: total.value,
    onChange: (page: number, pageSize: number) => {
      searchParams.current = page
      searchParams.pageSize = pageSize
      fetchData()
    },
  }
})
// 搜索
const doSearch = () => {
  // 重置搜索条件
  searchParams.current = 1
  fetchData()
}
// 标签和分类列表
const categoryList = ref<string[]>([])
const selectedCategory = ref<string>('all')
const tagList = ref<string[]>([])
const selectedTagList = ref<boolean[]>([])
/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagList.value = res.data.data.tagList ?? []
    categoryList.value = res.data.data.categoryList ?? []
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}
const router = useRouter()
// 跳转至图片详情页
const doClickPicture = (picture: API.PictureVO) => {
  router.push({
    path: `/picture/${picture.id}`,
  })
}
onMounted(() => {
  getTagCategoryOptions()
})

// 添加分类点击处理函数
const handleCategoryClick = (category: string) => {
  selectedCategory.value = category
  doSearch()
}
</script>

<style scoped>
#homePage {
  padding: 24px;
  min-height: 100vh;
  background: #f5f5f5;
}

.search-container {
  text-align: center;
  padding: 60px 0;
  background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%);
  border-radius: 16px;
  margin-bottom: 32px;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(24, 144, 255, 0.2);
}

.search-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 60%);
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.site-title {
  margin-bottom: 24px;
}

.gradient-text {
  color: rgba(255, 255, 255, 0.85);
  text-shadow: 0 0 3px rgba(54, 207, 201, 0.95);
  font-size: 3.5em;
  font-weight: 800;
  letter-spacing: 2px;
}

.subtitle {
  color: rgba(255, 255, 255, 0.9);
  font-size: 1.2em;
  margin-top: 16px;
  font-weight: 300;
}

.search-bar {
  max-width: 700px;
  margin: 0 auto;
  padding: 0 20px;
  position: relative;
  z-index: 1;
}

:deep(.ant-input-search) {
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
  border-radius: 24px;
  overflow: hidden;
}

:deep(.ant-input-search-button) {
  height: 48px !important;
  font-size: 16px;
  background: linear-gradient(45deg, #1890ff, #36cfc9);
  border: none;
  transition: all 0.3s;
}

:deep(.ant-input-search-button:hover) {
  opacity: 0.9;
  transform: translateX(2px);
}

:deep(.ant-input-affix-wrapper) {
  height: 48px !important;
  font-size: 16px;
  border: none;
}

.filter-container {
  background: white;
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 32px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.filter-container:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
}

.custom-tabs :deep(.ant-tabs-nav::before) {
  border-bottom: none;
}

.custom-tabs :deep(.ant-tabs-tab) {
  transition: all 0.3s;
  padding: 12px 24px;
  font-size: 16px;
}

.custom-tabs :deep(.ant-tabs-tab-active) {
  transform: scale(1.05);
  background: transparent !important;
}

.custom-tabs :deep(.ant-tabs-tab-btn) {
  color: rgba(0, 0, 0, 0.85) !important;
  font-weight: 500;
}

.custom-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #1890ff !important;
  font-weight: 500;
}

.custom-tabs :deep(.ant-tabs-tab:hover .ant-tabs-tab-btn) {
  color: #1890ff !important;
}

.custom-tabs :deep(.ant-tabs-ink-bar) {
  background: #1890ff;
}

.picture-container {
  background: white;
  padding: 32px;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.picture-card {
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.picture-card:hover {
  transform: translateY(-8px) scale(1.02);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}

.picture-image {
  height: 240px;
  object-fit: cover;
  transition: all 0.4s;
}

.picture-card:hover .picture-image {
  transform: scale(1.1);
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .search-container {
    padding: 40px 0;
  }

  .site-title {
    font-size: 2em;
  }

  .picture-image {
    height: 200px;
  }
}

::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: #1890ff;
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: #36cfc9;
}

:deep(.ant-pagination-item) {
  border-radius: 8px;
  transition: all 0.3s;
}

:deep(.ant-pagination-item:hover) {
  transform: scale(1.1);
}

:deep(.ant-pagination-item-active) {
  background: linear-gradient(45deg, #1890ff, #36cfc9);
  border: none;
}

:deep(.ant-pagination-item-active a) {
  color: white !important;
}

.tag-bar {
  margin-top: 5px;
  padding: 8px 0;
  border-radius: 12px;
}

.tag-label {
  font-size: 14px;
  font-weight: 500;
  color: #666;
  margin-right: 16px;
  position: relative;
}

.tag-label::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 100%;
  height: 2px;
  background: linear-gradient(90deg, #1890ff, transparent);
}

:deep(.custom-tag) {
  margin: 4px 8px;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 13px;
  border: 1px solid #e8e8e8;
  background: white;
  transition: all 0.3s;
  cursor: pointer;
}

:deep(.custom-tag:hover) {
  transform: translateY(-2px);
  border-color: #1890ff;
  color: #1890ff;
}

:deep(.custom-tag.ant-tag-checkable-checked) {
  background: linear-gradient(45deg, #1890ff, #36cfc9);
  border: none;
  color: white;
}

:deep(.custom-tag.ant-tag-checkable-checked:hover) {
  opacity: 0.9;
}
</style>
