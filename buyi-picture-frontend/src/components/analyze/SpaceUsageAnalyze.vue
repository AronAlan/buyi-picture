<template>
  <div class="space-usage-analyze">
    <a-flex gap="middle" :wrap="true">
      <a-card title="存储空间" :loading="loading" class="usage-card">
        <div class="usage-content">
          <h3 class="usage-title">
            <span class="used-value">{{ formatSize(data.usedSize) }}</span>
            <span class="separator">/</span>
            <span class="max-value">{{ data.maxSize ? formatSize(data.maxSize) : '无限制' }}</span>
          </h3>
          <a-progress
            type="dashboard"
            :percent="data.sizeUsageRatio ?? 0"
            :stroke-color="getProgressColor(data.sizeUsageRatio ?? 0)"
            :stroke-width="8"
          />
          <div class="usage-description">
            <a-tag :color="getTagColor(data.sizeUsageRatio ?? 0)">
              {{ getUsageDescription(data.sizeUsageRatio ?? 0) }}
            </a-tag>
          </div>
        </div>
      </a-card>
      <a-card title="图片数量" :loading="loading" class="usage-card">
        <div class="usage-content">
          <h3 class="usage-title">
            <span class="used-value">{{ data.usedCount }}</span>
            <span class="separator">/</span>
            <span class="max-value">{{ data.maxCount ?? '无限制' }}</span>
          </h3>
          <a-progress
            type="dashboard"
            :percent="data.countUsageRatio ?? 0"
            :stroke-color="getProgressColor(data.countUsageRatio ?? 0)"
            :stroke-width="8"
          />
          <div class="usage-description">
            <a-tag :color="getTagColor(data.countUsageRatio ?? 0)">
              {{ getUsageDescription(data.countUsageRatio ?? 0) }}
            </a-tag>
          </div>
        </div>
      </a-card>
    </a-flex>
  </div>
</template>

<script setup lang="ts">
import { ref, watchEffect, computed } from 'vue'
import { getSpaceUsageAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'
import { formatSize } from '@/utils'

interface Props {
  queryAll?: boolean
  queryPublic?: boolean
  spaceId?: number
}

const props = withDefaults(defineProps<Props>(), {
  queryAll: false,
  queryPublic: false,
})

// 图表数据
const data = ref<API.SpaceUsageAnalyzeResponse>({})
// 加载状态
const loading = ref(true)

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const res = await getSpaceUsageAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

/**
 * 根据使用率获取进度条颜色
 */
const getProgressColor = (percent: number) => {
  if (percent >= 90) return '#ff4d4f' // 红色
  if (percent >= 70) return '#faad14' // 黄色
  return '#52c41a' // 绿色
}

/**
 * 根据使用率获取标签颜色
 */
const getTagColor = (percent: number) => {
  if (percent >= 90) return 'error'
  if (percent >= 70) return 'warning'
  return 'success'
}

/**
 * 根据使用率获取描述文本
 */
const getUsageDescription = (percent: number) => {
  if (percent >= 90) return '空间紧张'
  if (percent >= 70) return '注意空间'
  return '空间充足'
}

/**
 * 监听变量，参数改变时触发数据的重新加载
 */
watchEffect(() => {
  fetchData()
})
</script>

<style scoped>
.space-usage-analyze {
  width: 100%;
}

.usage-card {
  width: 100%;
  transition: all 0.3s;
  border-radius: 8px;
  overflow: hidden;
  box-shadow:
    0 1px 2px -2px rgba(0, 0, 0, 0.16),
    0 3px 6px 0 rgba(0, 0, 0, 0.12),
    0 5px 12px 4px rgba(0, 0, 0, 0.09);
}

.usage-card:hover {
  transform: translateY(-5px);
  box-shadow:
    0 3px 6px -4px rgba(0, 0, 0, 0.16),
    0 6px 16px 0 rgba(0, 0, 0, 0.12),
    0 9px 28px 8px rgba(0, 0, 0, 0.09);
}

.usage-content {
  height: 320px;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.usage-title {
  margin-bottom: 16px;
  font-size: 20px;
  font-weight: 500;
}

.used-value {
  color: #1890ff;
  font-weight: bold;
}

.separator {
  margin: 0 8px;
  color: #8c8c8c;
}

.max-value {
  color: #8c8c8c;
}

.usage-description {
  margin-top: 16px;
}
</style>
