<template>
  <div class="space-rank-analyze">
    <a-card title="空间使用排行分析" :loading="loading" class="analyze-card">
      <template #extra>
        <a-tag color="orange">排行榜</a-tag>
      </template>
      <div class="chart-container">
        <v-chart :option="options" style="height: 320px; max-width: 100%;" :loading="loading" />
      </div>
      <div class="chart-summary" v-if="dataList.length > 0">
        <div class="summary-item">
          <span class="summary-label">空间总数:</span>
          <span class="summary-value">{{ dataList.length }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">使用量最大:</span>
          <span class="summary-value">{{ getTopSpace() }}</span>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import VChart from 'vue-echarts'
import * as echarts from 'echarts'
import { computed, ref, watchEffect } from 'vue'
import { getSpaceRankAnalyzeUsingPost } from '@/api/spaceAnalyzeController.ts'
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
const dataList = ref<API.Space[]>([])
// 加载状态
const loading = ref(true)

// 获取数据
const fetchData = async () => {
  loading.value = true
  // 转换搜索参数
  const res = await getSpaceRankAnalyzeUsingPost({
    queryAll: props.queryAll,
    queryPublic: props.queryPublic,
    spaceId: props.spaceId,
    topN: 10, // 后端默认是 10
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data ?? []
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
  loading.value = false
}

/**
 * 获取使用量最大的空间
 */
const getTopSpace = () => {
  if (dataList.value.length === 0) return '无数据'
  const maxItem = dataList.value[0] // 已经排序好的数据
  return `${maxItem.spaceName} (${(maxItem.totalSize / (1024 * 1024)).toFixed(2)}MB)`
}

/**
 * 监听变量，参数改变时触发数据的重新加载
 */
watchEffect(() => {
  fetchData()
})

// 图表选项
const options = computed(() => {
  const spaceNames = dataList.value.map((item) => item.spaceName)
  const usageData = dataList.value.map((item) => (item.totalSize / (1024 * 1024)).toFixed(2)) // 转为 MB

  // 反转数据以便从大到小排列
  const reversedNames = [...spaceNames].reverse()
  const reversedData = [...usageData].reverse()

  return {
    tooltip: { 
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: '{b}: {c} MB'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      name: '空间使用量 (MB)',
      nameLocation: 'middle',
      nameGap: 30,
      axisLabel: {
        formatter: '{value} MB'
      }
    },
    yAxis: {
      type: 'category',
      data: reversedNames,
      axisLabel: {
        width: 120,
        overflow: 'truncate',
        fontSize: 12
      }
    },
    series: [
      {
        name: '空间使用量 (MB)',
        type: 'bar',
        data: reversedData,
        itemStyle: {
          color: function(params: any) {
            // 为前三名设置不同颜色
            const colorList = ['#FA8C16', '#FAAD14', '#FFC53D', '#FA541C', '#FA541C', '#FA541C', '#FA541C', '#FA541C', '#FA541C', '#FA541C']
            return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: colorList[params.dataIndex] || '#FA541C' },
              { offset: 1, color: 'rgba(250, 140, 22, 0.2)' }
            ])
          },
          borderRadius: [0, 4, 4, 0]
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.3)'
          }
        },
        label: {
          show: true,
          position: 'right',
          formatter: '{c} MB'
        }
      },
    ],
  }
})
</script>

<style scoped>
.space-rank-analyze {
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
  color: #FA8C16;
  font-size: 14px;
}
</style>
