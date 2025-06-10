<template>
  <div class="space-category-analyze">
    <a-card title="空间图片分类分析" :loading="loading" class="analyze-card">
      <div class="chart-container">
        <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
      </div>
      <div class="chart-summary" v-if="dataList.length > 0">
        <div class="summary-item">
          <span class="summary-label">分类总数:</span>
          <span class="summary-value">{{ dataList.length }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">分类图片数量最多:</span>
          <span class="summary-value">{{ getMostPopularCategory() }}</span>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import * as echarts from 'echarts'
import { computed, ref, watchEffect } from 'vue'
import { getSpaceCategoryAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
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
  const res = await getSpaceCategoryAnalyzeUsingPost({
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
 * 获取图片数量最多的分类
 */
const getMostPopularCategory = () => {
  if (dataList.value.length === 0) return '无数据'
  const maxItem = dataList.value.reduce((prev, current) => {
    return Number(prev.count) > Number(current.count) ? prev : current
  })
  return `${maxItem.category} (${maxItem.count}张)`
}

/**
 * 监听变量，参数改变时触发数据的重新加载
 */
watchEffect(() => {
  fetchData()
})

// 图表选项
const options = computed(() => {
  const categories = dataList.value.map((item) => item.category)
  const countData = dataList.value.map((item) => item.count)
  const sizeData = dataList.value.map((item) => (item.totalSize / (1024 * 1024)).toFixed(2)) // 转为 MB

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow',
      },
    },
    legend: {
      data: ['图片数量', '图片总大小'],
      top: 'bottom',
      textStyle: {
        fontSize: 12,
      },
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      top: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: categories,
      axisLabel: {
        rotate: categories.length > 5 ? 45 : 0,
        interval: 0,
        fontSize: 11,
      },
    },
    yAxis: [
      {
        type: 'value',
        name: '图片数量',
        nameTextStyle: {
          color: '#5470C6',
          fontSize: 12,
          padding: [0, 0, 0, 0],
        },
        axisLine: { show: true, lineStyle: { color: '#5470C6' } }, // 左轴颜色
      },
      {
        type: 'value',
        name: '图片总大小 (MB)',
        nameTextStyle: {
          color: '#91CC75',
          fontSize: 12,
          padding: [0, 0, 0, 0],
        },
        position: 'right',
        axisLine: { show: true, lineStyle: { color: '#91CC75' } }, // 右轴颜色
        splitLine: {
          lineStyle: {
            color: 'rgba(145, 204, 117, 0.2)', // 调整网格线颜色
            type: 'dashed', // 线条样式：可选 'solid', 'dashed', 'dotted'
          },
        },
      },
    ],
    series: [
      {
        name: '图片数量',
        type: 'bar',
        data: countData,
        yAxisIndex: 0,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#5470C6' },
            { offset: 1, color: '#91A7FF' },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#3E55A7' },
              { offset: 1, color: '#7A8FE0' },
            ]),
          },
        },
      },
      {
        name: '图片总大小',
        type: 'bar',
        data: sizeData,
        yAxisIndex: 1,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#91CC75' },
            { offset: 1, color: '#C2E0B4' },
          ]),
          borderRadius: [4, 4, 0, 0],
        },
        emphasis: {
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#72AD56' },
              { offset: 1, color: '#A3C98F' },
            ]),
          },
        },
      },
    ],
  }
})
</script>

<style scoped>
.space-category-analyze {
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
  color: #1890ff;
  font-size: 14px;
}
</style>
