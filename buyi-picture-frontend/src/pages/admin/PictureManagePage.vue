<template>
  <div id="pictureManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="关键词">
        <a-input
          v-model:value="searchParams.searchText"
          placeholder="从名称和简介搜索"
          allow-clear
        />
      </a-form-item>
      <a-form-item label="类型">
        <a-input v-model:value="searchParams.category" placeholder="请输入类型" allow-clear />
      </a-form-item>
      <a-form-item label="标签">
        <a-select
          v-model:value="searchParams.tags"
          mode="tags"
          placeholder="请输入标签"
          style="min-width: 180px"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-space>
          <a-button type="primary" html-type="submit">搜索</a-button>
          <a-button @click="doReset">重置</a-button>
        </a-space>
      </a-form-item>
    </a-form>
    <div style="margin-bottom: 16px" />
    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="dataList"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'url'">
          <a-image :src="record.url" :width="120" />
        </template>
        <template v-if="column.dataIndex === 'tags'">
          <a-space wrap>
            <a-tag v-for="tag in JSON.parse(record.tags || '[]')" :key="tag">
              {{ tag }}
            </a-tag>
          </a-space>
        </template>
        <template v-if="column.dataIndex === 'picInfo'">
          <a-tooltip placement="left">
            <template #title>
              <div>格式：{{ record.picFormat }}</div>
              <div>宽度：{{ record.picWidth }}</div>
              <div>高度：{{ record.picHeight }}</div>
              <div>宽高比：{{ record.picScale }}</div>
              <div>大小：{{ (record.picSize / 1024).toFixed(2) }}KB</div>
            </template>
            <!-- 默认只显示大小信息 -->
            <div>{{ (record.picSize / 1024).toFixed(2) }}KB</div>
          </a-tooltip>
        </template>
        <template v-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-if="column.dataIndex === 'editTime'">
          {{ dayjs(record.editTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        <template v-if="column.dataIndex === 'id'">
          <a-tooltip :title="record.id">
            <div style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
              {{ record.id }}
            </div>
          </a-tooltip>
        </template>
        <template v-if="column.dataIndex === 'userId'">
          <a-tooltip :title="record.userId">
            <div style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
              {{ record.userId }}
            </div>
          </a-tooltip>
        </template>
        <template v-if="column.dataIndex === 'introduction'">
          <a-tooltip :title="record.introduction">
            <div style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap">
              {{ record.introduction }}
            </div>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" :href="`/add_picture?id=${record.id}`" target="_blank">
              编辑
            </a-button>
            <a-popconfirm
              title="确定要删除这张图片吗？"
              ok-text="确认"
              cancel-text="取消"
              @confirm="doDelete(record.id)"
            >
              <a-button danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deletePictureUsingPost, listPictureByPageUsingPost } from '@/api/pictureController'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    width: 100,
    align: 'center',
    ellipsis: true,
  },
  {
    title: '图片',
    dataIndex: 'url',
    align: 'center',
  },
  {
    title: '名称',
    dataIndex: 'name',
    width: 100,
    align: 'center',
  },
  {
    title: '简介',
    dataIndex: 'introduction',
    width: 100,
    align: 'center',
    ellipsis: true,
  },
  {
    title: '类型',
    dataIndex: 'category',
    width: 80,
    align: 'center',
  },
  {
    title: '标签',
    dataIndex: 'tags',
    width: 80,
    align: 'center',
  },
  {
    title: '图片信息',
    dataIndex: 'picInfo',
    width: 100,
    align: 'center',
  },
  {
    title: '用户 id',
    dataIndex: 'userId',
    width: 100,
    align: 'center',
    ellipsis: true,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 130,
    align: 'center',
  },
  {
    title: '编辑时间',
    dataIndex: 'editTime',
    width: 130,
    align: 'center',
  },
  {
    title: '操作',
    key: 'action',
    width: 170,
    align: 'center',
  },
]
// 定义数据
const dataList = ref<API.Picture[]>([])
const total = ref(0)
// 搜索条件
const searchParams = reactive<API.PictureQueryRequest>({
  current: 1,
  pageSize: 10,
  sortField: 'createTime',
  sortOrder: 'descend',
})
// 获取数据
const fetchData = async () => {
  const res = await listPictureByPageUsingPost({
    ...searchParams,
  })
  if (res.data.code === 0 && res.data.data) {
    dataList.value = res.data.data.records ?? []
    total.value = res.data.data.total ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
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
    showSizeChanger: true,
    showTotal: (total) => `共 ${total} 条`,
  }
})
// 表格变化之后，重新获取数据
const doTableChange = (page) => {
  searchParams.current = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}
// 搜索数据
const doSearch = () => {
  // 重置页码
  searchParams.current = 1
  fetchData()
}
// 删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    // 刷新数据
    fetchData()
  } else {
    message.error('删除失败')
  }
}
// Add reset function
const doReset = () => {
  // Reset search parameters
  searchParams.searchText = undefined
  searchParams.category = undefined
  searchParams.tags = undefined
  // Reset to first page and fetch data
  searchParams.current = 1
  fetchData()
}
// 修改删除方法，添加确认对话框
const showDeleteConfirm = (id: string) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这张图片吗？此操作不可恢复。',
    okText: '确认',
    cancelText: '取消',
    okType: 'danger',
    async onOk() {
      await doDelete(id)
    },
  })
}
</script>
