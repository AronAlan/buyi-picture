<template>
  <div class="space-size-analyze">
    <a-card title="空间图片大小分析" :loading="loading" class="analyze-card">
      <div class="chart-container">
        <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
      </div>
      <div class="chart-summary" v-if="dataList.length > 0">
        <div class="summary-item">
          <span class="summary-label">大小区间数:</span>
          <span class="summary-value">{{ dataList.length }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">主要大小区间:</span>
          <span class="summary-value">{{ getMostCommonSizeRange() }}</span>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import 'echarts'
import { computed, ref, watchEffect } from 'vue'
import { getSpaceSizeAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
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
const dataList = ref<API.SpaceSizeAnalyzeResponse>([])
// 加载状态
const loading = ref(true)

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const res = await getSpaceSizeAnalyzeUsingPost({
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
 * 获取最常见的图片大小区间
 */
const getMostCommonSizeRange = () => {
  if (dataList.value.length === 0) return '无数据'
  const maxItem = dataList.value.reduce((prev, current) => {
    return Number(prev.count) > Number(current.count) ? prev : current
  })
  return `${maxItem.sizeRange} (${maxItem.count}张)`
}

/**
 * 监听变量，参数改变时触发数据的重新加载
 */
watchEffect(() => {
  fetchData()
})

// 预定义的颜色方案
const colorPalette = [
  '#1890FF', // 蓝色
  '#FF4D4F', // 红色
  '#52C41A', // 绿色
  '#FAAD14', // 橙色
  '#722ED1', // 紫色
  '#13C2C2', // 青色
  '#F759AB', // 粉色
  '#FFA940', // 暖橙色
  '#9254DE', // 淡紫色
  '#36CFC9'  // 浅青色
]

// 图表选项
const options = computed(() => {
  const pieData = dataList.value.map((item, index) => ({
    name: item.sizeRange,
    value: item.count,
    itemStyle: {
      color: colorPalette[index % colorPalette.length],
    },
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
      backgroundColor: 'rgba(50, 50, 50, 0.7)',
      borderColor: '#333',
      textStyle: {
        color: '#fff',
      },
    },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      bottom: 0,
      data: pieData.map((item) => item.name),
      textStyle: {
        fontSize: 12,
      },
      pageIconColor: '#52C41A',
      pageTextStyle: {
        color: '#52C41A',
      },
    },
    series: [
      {
        name: '图片大小',
        type: 'pie',
        radius: ['40%', '70%'], // 环形图
        center: ['50%', '45%'], // 居中偏上
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
          position: 'center',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: '18',
            fontWeight: 'bold',
          },
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
        labelLine: {
          show: false,
        },
        data: pieData,
      },
    ],
  }
})
</script>

<style scoped>
.space-size-analyze {
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
  color: #52c41a;
  font-size: 14px;
}
</style>
