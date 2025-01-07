<template>
  <div id="addPictureBatchPage">
    <h2 style="margin-bottom: 16px">批量创建</h2>
    <!-- 图片信息表单 -->
    <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item name="searchText" label="关键词">
        <a-input v-model:value="formData.searchText" placeholder="请输入关键词" allow-clear />
      </a-form-item>
      <a-form-item name="count" label="抓取数量">
        <a-input-number
          v-model:value="formData.count"
          placeholder="请输入数量"
          style="min-width: 180px"
          :min="1"
          :max="30"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="namePrefix" label="名称前缀">
        <a-input
          v-model:value="formData.namePrefix"
          placeholder="请输入名称前缀，会自动补充序号"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="category" label="分类">
        <a-select
          v-model:value="formData.category"
          placeholder="请选择分类"
          style="width: 100%"
          allow-clear
        >
          <a-select-option
            v-for="category in categoryList"
            :key="category.id"
            :value="category.name"
          >
            {{ category.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item name="tags" label="标签">
        <a-select
          v-model:value="formData.tags"
          mode="multiple"
          placeholder="请选择标签"
          style="width: 100%"
          allow-clear
        >
          <a-select-option v-for="tag in tagList" :key="tag.id" :value="tag.name">
            {{ tag.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
          执行任务
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  getPictureVoByIdUsingGet,
  listPictureTagCategoryUsingGet,
  uploadPictureByBatchUsingPost,
} from '@/api/pictureController.ts'
import { useRoute, useRouter } from 'vue-router'
const formData = reactive<API.PictureUploadByBatchRequest>({
  count: 10,
  searchText: '',
  namePrefix: '',
  category: undefined,
  tags: [],
})
// 提交任务状态
const loading = ref(false)
const router = useRouter()
// 分类列表
const categoryList = ref<API.PictureCategory[]>([])
// 标签列表
const tagList = ref<API.PictureTag[]>([])
/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  loading.value = true
  const res = await uploadPictureByBatchUsingPost({
    ...formData,
  })
  // 操作成功
  if (res.data.code === 0 && res.data.data) {
    message.success(`创建成功，共 ${res.data.data} 条`)
    // 跳转到主页
    router.push({
      path: `/`,
    })
  } else {
    message.error('创建失败，' + res.data.message)
  }
  loading.value = false
}
// 获取分类和标签数据
const loadData = async () => {
  try {
    const tagCategoryRes = await listPictureTagCategoryUsingGet()
    console.log('标签分类数据:', tagCategoryRes?.data)
    if (tagCategoryRes?.data?.data) {
      // 设置分类列表
      categoryList.value = tagCategoryRes.data.data.categoryList.map(
        (name: string, index: number) => ({
          id: index + 1,
          name: name,
        }),
      )
      // 设置标签列表
      tagList.value = tagCategoryRes.data.data.tagList.map((name: string, index: number) => ({
        id: index + 1,
        name: name,
      }))
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    message.error('加载分类和标签数据失败')
  }
}
onMounted(() => {
  loadData()
})
</script>
<style scoped>
#addPictureBatchPage {
  max-width: 720px;
  margin: 0 auto;
}
</style>
