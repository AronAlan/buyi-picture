<template>
  <div class="space-tag-analyze">
    <a-card title="空间图片标签分析" :loading="loading" class="analyze-card">
      <div class="chart-container">
        <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
      </div>
      <div class="chart-summary" v-if="dataList.length > 0">
        <div class="summary-item">
          <span class="summary-label">标签总数:</span>
          <span class="summary-value">{{ dataList.length }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">最热门标签:</span>
          <span class="summary-value">{{ getMostPopularTag() }}</span>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import 'echarts'
import 'echarts-wordcloud'
import { computed, ref, watchEffect } from 'vue'
import { getSpaceTagAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
import { message } from 'ant-design-vue'

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
const dataList = ref<API.SpaceCategoryAnalyzeResponse>([])
// 加载状态
const loading = ref(true)

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const res = await getSpaceTagAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

/**
 * 获取最热门的标签
 */
const getMostPopularTag = () => {
  if (dataList.value.length === 0) return '无数据'
  const maxItem = dataList.value.reduce((prev, current) => {
    return Number(prev.count) > Number(current.count) ? prev : current
  })
  return `${maxItem.tag} (${maxItem.count}次)`
}

/**
 * 监听变量，参数改变时触发数据的重新加载
 */
watchEffect(() => {
  fetchData()
})

// 预定义的颜色方案
const colorPalette = [
  '#722ED1',
  '#2F54EB',
  '#1890FF',
  '#13C2C2',
  '#52C41A',
  '#FADB14',
  '#FA8C16',
  '#FA541C',
  '#F5222D',
  '#EB2F96',
]

// 图表选项
const options = computed(() => {
  const tagData = dataList.value.map((item) => ({
    name: item.tag,
    value: item.count,
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => `${params.name}: ${params.value} 次`,
      backgroundColor: 'rgba(50, 50, 50, 0.7)',
      borderColor: '#333',
      textStyle: {
        color: '#fff',
      },
    },
    series: [
      {
        type: 'wordCloud',
        gridSize: 10,
        sizeRange: [12, 50], // 字体大小范围
        rotationRange: [-45, 45], // 减小旋转角度范围，使标签更易读
        shape: 'circle',
        drawOutOfBound: false,
        layoutAnimation: true,
        textStyle: {
          fontFamily: 'sans-serif',
          fontWeight: 'bold',
          color: function () {
            // 使用预定义的颜色方案而不是完全随机颜色
            return colorPalette[Math.floor(Math.random() * colorPalette.length)]
          },
        },
        emphasis: {
          textStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.3)',
          },
        },
        data: tagData,
      },
    ],
  }
})
</script>

<style scoped>
.space-tag-analyze {
  width: 100%;
}

.analyze-card {
  width: 100%;
  transition: all 0.3s;
  border-radius: 8px;
  overflow: hidden;
  box-shadow:
    0 1px 2px -2px rgba(0, 0, 0, 0.16),
    0 3px 6px 0 rgba(0, 0, 0, 0.12),
    0 5px 12px 4px rgba(0, 0, 0, 0.09);
}

.analyze-card:hover {
  transform: translateY(-5px);
  box-shadow:
    0 3px 6px -4px rgba(0, 0, 0, 0.16),
    0 6px 16px 0 rgba(0, 0, 0, 0.12),
    0 9px 28px 8px rgba(0, 0, 0, 0.09);
}

.chart-container {
  position: relative;
}

.chart-summary {
  display: flex;
  justify-content: space-around;
  margin-top: 8px;
  padding: 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.summary-item {
  text-align: center;
}

.summary-label {
  color: #8c8c8c;
  margin-right: 8px;
  font-size: 12px;
}

.summary-value {
  font-weight: bold;
  color: #722ed1;
  font-size: 14px;
}
</style>
