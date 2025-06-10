<template>
  <div class="space-user-analyze">
    <a-card title="空间图片用户分析" :loading="loading" class="analyze-card">
      <template #extra>
        <a-space>
          <a-segmented
            v-model:value="timeDimension"
            :options="timeDimensionOptions"
            size="small"
            class="time-selector"
          />
          <a-input-search
            placeholder="请输入用户 id"
            enter-button="搜索用户"
            @search="doSearch"
            size="small"
            class="user-search"
          />
        </a-space>
      </template>
      <div class="chart-container">
        <v-chart :option="options" style="height: 320px; max-width: 100%" :loading="loading" />
      </div>
      <div class="chart-summary" v-if="dataList.length > 0">
        <div class="summary-item">
          <span class="summary-label">时间区间:</span>
          <span class="summary-value">{{ dataList.length }}个</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">最活跃时段:</span>
          <span class="summary-value">{{ getMostActivePeriod() }}</span>
        </div>
        <div class="summary-item" v-if="userId">
          <span class="summary-label">当前用户:</span>
          <span class="summary-value">ID: {{ userId }}</span>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import * as echarts from 'echarts'
import { computed, ref, watchEffect } from 'vue'
import { getSpaceUserAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
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

// 时间维度选项
const timeDimension = ref<'day' | 'week' | 'month'>('day')
// 分段选择器组件的选项
const timeDimensionOptions = [
  {
    label: '日',
    value: 'day',
  },
  {
    label: '周',
    value: 'week',
  },
  {
    label: '月',
    value: 'month',
  },
]
// 用户选项
const userId = ref<string>()
const doSearch = (value: string) => {
  userId.value = value
}

// 图表数据
const dataList = ref<API.SpaceUserAnalyzeResponse[]>([])
// 加载状态
const loading = ref(true)

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const res = await getSpaceUserAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
    timeDimension: timeDimension.value,
    userId: userId.value,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

/**
 * 获取最活跃的时间段
 */
const getMostActivePeriod = () => {
  if (dataList.value.length === 0) return '无数据'
  const maxItem = dataList.value.reduce((prev, current) => {
    return Number(prev.count) > Number(current.count) ? prev : current
  })
  return `${maxItem.period} (${maxItem.count}张)`
}

/**
 * 监听变量，参数改变时触发数据的重新加载
 */
watchEffect(() => {
  fetchData()
})

// 图表选项
const options = computed(() => {
  const periods = dataList.value.map((item) => item.period) // 时间区间
  const counts = dataList.value.map((item) => item.count) // 上传数量

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(50, 50, 50, 0.7)',
      borderColor: '#333',
      textStyle: {
        color: '#fff'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '10%',
      top: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: periods,
      name: '时间区间',
      nameLocation: 'middle',
      nameGap: 30,
      axisLabel: {
        rotate: periods.length > 10 ? 45 : 0,
        interval: periods.length > 10 ? 'auto' : 0,
        fontSize: 11
      },
      axisLine: {
        lineStyle: {
          color: '#1890ff'
        }
      }
    },
    yAxis: {
      type: 'value',
      name: '上传数量',
      nameTextStyle: {
        padding: [0, 0, 0, 0]
      },
      splitLine: {
        lineStyle: {
          type: 'dashed'
        }
      }
    },
    series: [
      {
        name: '上传数量',
        type: 'line',
        data: counts,
        smooth: true, // 平滑折线
        symbol: 'emptyCircle',
        symbolSize: 8,
        itemStyle: {
          color: '#1890ff'
        },
        lineStyle: {
          width: 3,
          shadowColor: 'rgba(0, 0, 0, 0.3)',
          shadowBlur: 10,
          shadowOffsetY: 8
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24, 144, 255, 0.5)' },
            { offset: 1, color: 'rgba(24, 144, 255, 0.1)' }
          ])
        },
        emphasis: {
          focus: 'series',
          itemStyle: {
            borderWidth: 2,
            borderColor: '#fff',
            color: '#1890ff',
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.3)'
          }
        },
      },
    ],
  }
})
</script>

<style scoped>
.space-user-analyze {
  width: 100%;
}

.analyze-card {
  width: 100%;
  transition: all 0.3s;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.16), 0 3px 6px 0 rgba(0, 0, 0, 0.12), 0 5px 12px 4px rgba(0, 0, 0, 0.09);
}

.analyze-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 3px 6px -4px rgba(0, 0, 0, 0.16), 0 6px 16px 0 rgba(0, 0, 0, 0.12), 0 9px 28px 8px rgba(0, 0, 0, 0.09);
}

.chart-container {
  position: relative;
}

.time-selector {
  background-color: #f0f5ff;
  border-radius: 4px;
}

.user-search {
  width: 180px;
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
