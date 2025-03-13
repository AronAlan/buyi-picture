<template>
  <div id="addPicturePage" class="add-picture-container">
    <h2 class="page-title">
      {{ route.query?.id ? '修改图片' : '创建图片' }}
    </h2>
    <a-typography-paragraph v-if="spaceId" type="secondary">
      保存至空间：<a :href="`/space/${spaceId}`" target="_blank">{{ spaceId }}</a>
    </a-typography-paragraph>
    <div class="content-layout">
      <!-- 左侧上传区域 -->
      <div class="upload-section">
        <a-tabs v-model:activeKey="uploadType" class="upload-tabs">
          <a-tab-pane key="file" tab="文件上传">
            <!-- 图片上传组件 -->
            <PictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
          </a-tab-pane>
          <a-tab-pane key="url" tab="URL 上传" force-render>
            <UrlPictureUpload :picture="picture" :spaceId="spaceId" :onSuccess="onSuccess" />
          </a-tab-pane>
        </a-tabs>
      </div>

      <!-- 右侧表单区域 -->
      <div class="form-section" v-if="picture">
        <a-form
          name="pictureForm"
          layout="vertical"
          :model="pictureForm"
          @finish="handleSubmit"
          class="picture-form"
        >
          <a-form-item name="name" label="名称">
            <a-input v-model:value="pictureForm.name" placeholder="请输入名称" allow-clear />
          </a-form-item>
          <a-form-item name="introduction" label="简介">
            <a-textarea
              v-model:value="pictureForm.introduction"
              placeholder="请输入简介"
              :auto-size="{ minRows: 2, maxRows: 5 }"
              allow-clear
            />
          </a-form-item>
          <a-form-item name="category" label="分类">
            <a-select
              v-model:value="pictureForm.category"
              placeholder="请输入分类"
              :options="categoryOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item name="tags" label="标签">
            <a-select
              v-model:value="pictureForm.tags"
              mode="multiple"
              :show-arrow="true"
              :show-search="false"
              placeholder="请输入标签"
              :options="tagOptions"
              allow-clear
            />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit" class="submit-button">
              {{ route.query?.id ? '修改' : '创建' }}
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import PictureUpload from '@/components/PictureUpload.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  editPictureUsingPost,
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
} from '@/api/pictureController'
import { useRoute, useRouter } from 'vue-router'
import UrlPictureUpload from '@/components/UrlPictureUpload.vue'
const picture = ref<API.PictureVo>()
const pictureForm = reactive<API.PictureEditRequest>({})
const uploadType = ref<'file' | 'url'>('file')
const router = useRouter()
const route = useRoute()
// 空间 id
const spaceId = computed(() => {
  return route.query?.spaceId
})
/**
 * 图片上传成功
 * @param newPicture
 */
const onSuccess = (newPicture: API.PictureVo) => {
  picture.value = newPicture
  pictureForm.name = newPicture.name
}

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values) => {
  // console.log(values)
  const pictureId = picture.value.id
  if (!pictureId) {
    return
  }
  const res = await editPictureUsingPost({
    id: pictureId,
    spaceId: spaceId.value,
    ...values,
  })
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success('创建成功')
    // 跳转到图片详情页
    router.push({
      path: `/picture/${pictureId}`,
    })
  } else {
    message.error('创建失败，' + res.data.message)
  }
}
const categoryOptions = ref<string[]>([])
const tagOptions = ref<string[]>([])
/**
 * 获取标签和分类选项
 * @param values
 */
const getTagCategoryOptions = async () => {
  const res = await listPictureTagCategoryUsingGet()
  if (res.data.code === 0 && res.data.data) {
    tagOptions.value = (res.data.data.tagList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
    categoryOptions.value = (res.data.data.categoryList ?? []).map((data: string) => {
      return {
        value: data,
        label: data,
      }
    })
  } else {
    message.error('获取标签分类列表失败，' + res.data.message)
  }
}
onMounted(() => {
  getTagCategoryOptions()
})
// 获取老数据
const getOldPicture = async () => {
  // 获取到 id
  const id = route.query?.id
  if (id) {
    const res = await getPictureVoByIdUsingGet({
      id,
    })
    if (res.data.code === 0 && res.data.data) {
      const data = res.data.data
      picture.value = data
      pictureForm.name = data.name
      pictureForm.introduction = data.introduction
      pictureForm.category = data.category
      pictureForm.tags = data.tags
    }
  }
}
onMounted(() => {
  getOldPicture()
})
</script>
<style scoped>
.add-picture-container {
  max-width: 1200px;
  margin: 32px auto;
  padding: 40px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
  position: relative;
}

.page-title {
  margin-bottom: 40px;
  color: #2c3e50;
  font-size: 32px;
  font-weight: 700;
  text-align: center;
  position: relative;
}

.page-title::after {
  content: '';
  position: absolute;
  bottom: -12px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 4px;
  background: linear-gradient(90deg, #1890ff, #40a9ff);
  border-radius: 4px;
}

.upload-tabs {
  margin-bottom: 40px;
  position: relative;
}

.upload-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 32px;
}

.upload-tabs :deep(.ant-tabs-tab) {
  transition: all 0.3s;
  padding: 12px 24px;
  font-size: 16px;
}

.upload-tabs :deep(.ant-tabs-tab-active) {
  transform: translateY(-2px);
}

.picture-form {
  padding: 32px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.06),
    inset 0 2px 8px rgba(255, 255, 255, 0.9);
  position: relative;
  z-index: 1;
}

.picture-form :deep(.ant-form-item-label) {
  font-weight: 500;
  color: #2c3e50;
}

.picture-form :deep(.ant-input),
.picture-form :deep(.ant-select-selector),
.picture-form :deep(.ant-input-affix-wrapper) {
  border-radius: 6px;
  border: 1px solid #d9d9d9;
  transition: all 0.3s;
  background: #ffffff;
}

/* 修复名称输入框的特殊样式 */
.picture-form :deep(.ant-input-affix-wrapper) {
  padding: 0;
  border: none;
  background: none;
  box-shadow: none;
}

.picture-form :deep(.ant-input-affix-wrapper input) {
  padding: 4px 11px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
}

.picture-form :deep(.ant-input-affix-wrapper:hover input),
.picture-form :deep(.ant-input:hover),
.picture-form :deep(.ant-select-selector:hover) {
  border-color: #40a9ff;
}

.picture-form :deep(.ant-input-affix-wrapper-focused input),
.picture-form :deep(.ant-input:focus),
.picture-form :deep(.ant-select-selector:focus) {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.submit-button {
  width: 100%;
  height: 48px;
  font-size: 18px;
  font-weight: 600;
  border-radius: 12px;
  background: linear-gradient(135deg, #1890ff, #096dd9);
  border: none;
  transition: all 0.3s;
  position: relative;
  overflow: hidden;
}

.submit-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: 0.5s;
}

.submit-button:hover {
  transform: translateY(-2px);
  box-shadow:
    0 8px 24px rgba(24, 144, 255, 0.3),
    0 2px 4px rgba(24, 144, 255, 0.1);
}

.submit-button:hover::before {
  left: 100%;
}

/* 添加动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.picture-form {
  animation: fadeIn 0.6s ease-out;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .add-picture-container {
    margin: 16px;
    padding: 24px;
  }

  .page-title {
    font-size: 28px;
    margin-bottom: 32px;
  }

  .picture-form {
    padding: 20px;
  }
}
.content-layout {
  display: grid;
  grid-template-columns: minmax(400px, 1.2fr) minmax(400px, 0.8fr);
  gap: 48px;
  align-items: start;
  margin-top: 24px;
}

.upload-section {
  position: relative;
}

.form-section {
  position: relative;
}

/* 响应式布局 */
@media (max-width: 1280px) {
  .add-picture-container {
    max-width: 95%;
    margin: 32px auto;
    padding: 32px;
  }

  .content-layout {
    gap: 32px;
  }
}

@media (max-width: 1024px) {
  .content-layout {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .add-picture-container {
    padding: 24px;
  }
}

/* 调整上传组件的样式以适应新布局 */
.upload-section :deep(.ant-upload) {
  margin: 0 !important;
}

/* 确保表单在小屏幕上也有良好的显示效果 */
.form-section .picture-form {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 优化间距 */
.upload-tabs {
  margin-bottom: 0;
}

/* 添加过渡动画 */
.content-layout > * {
  transition: all 0.3s ease-in-out;
}

@media (min-width: 1025px) {
  .upload-section {
    position: sticky;
    top: 24px;
  }
}
</style>
