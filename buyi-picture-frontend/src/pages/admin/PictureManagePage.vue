<template>
  <div id="pictureManagePage">
    <a-flex justify="space-between">
      <h2>图片管理</h2>
      <a-space>
        <a-button type="primary" href="/add_picture" target="_blank">+ 创建图片</a-button>
        <a-button type="primary" href="/add_picture/batch" target="_blank" ghost
          >+ 批量创建图片</a-button
        >
      </a-space>
    </a-flex>
    <div style="margin-bottom: 16px" />
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
      <a-form-item name="reviewStatus" label="审核状态">
        <a-select
          v-model:value="searchParams.reviewStatus"
          style="min-width: 180px"
          placeholder="请选择审核状态"
          :options="PIC_REVIEW_STATUS_OPTIONS"
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
      :scroll="{ x: 1500 }"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'webpUrl'">
          <a-image :src="record.webpUrl" :width="120" />
        </template>
        <template v-if="column.dataIndex === 'tags'">
          <a-space wrap>
            <ColorfulTag v-for="tag in JSON.parse(record.tags || '[]')" :key="tag" :text="tag" />
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
        <template v-if="column.dataIndex === 'reviewMessage'">
          <div>审核状态：{{ PIC_REVIEW_STATUS_MAP[record.reviewStatus] }}</div>
          <div>审核信息：{{ record.reviewMessage }}</div>
          <div>审核人：{{ record.reviewerId }}</div>
          <div v-if="record.reviewTime">
            审核时间：{{ dayjs(record.reviewTime).format('YYYY-MM-DD HH:mm:ss') }}
          </div>
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
        <template v-if="column.dataIndex === 'category'">
          <ColorfulTag v-if="record.category" :text="record.category" :use-fixed="true" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space wrap>
            <a-popconfirm
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.PASS"
              title="确定要通过这张图片吗？"
              ok-text="确认"
              cancel-text="取消"
              @confirm="handleReview(record, PIC_REVIEW_STATUS_ENUM.PASS)"
            >
              <a-button
                type="primary"
                size="small"
                style="background-color: #52c41a; border-color: #52c41a"
              >
                <template #icon><check-outlined /></template>
                通过
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              v-if="record.reviewStatus !== PIC_REVIEW_STATUS_ENUM.REJECT"
              title="确定要拒绝这张图片吗？"
              ok-text="确认"
              cancel-text="取消"
              @confirm="handleReview(record, PIC_REVIEW_STATUS_ENUM.REJECT)"
            >
              <a-button
                type="primary"
                size="small"
                style="background-color: #faad14; border-color: #faad14"
              >
                <template #icon><close-outlined /></template>
                拒绝
              </a-button>
            </a-popconfirm>
            <a-button
              type="primary"
              size="small"
              :href="`/add_picture?id=${record.id}`"
              style="background-color: #1890ff; border-color: #1890ff"
            >
              <template #icon><edit-outlined /></template>
              编辑
            </a-button>
            <a-popconfirm
              title="确定要删除这张图片吗？"
              ok-text="确认"
              cancel-text="取消"
              @confirm="doDelete(record.id)"
            >
              <a-button type="primary" danger size="small">
                <template #icon><delete-outlined /></template>
                删除
              </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  deletePictureUsingPost,
  doPictureReviewUsingPost,
  listPictureByPageUsingPost,
} from '@/api/pictureController'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PIC_REVIEW_STATUS_ENUM,
  PIC_REVIEW_STATUS_MAP,
  PIC_REVIEW_STATUS_OPTIONS,
} from '../../constants/picture.ts'
import { CheckOutlined, CloseOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import ColorfulTag from '@/components/ColorfulTag.vue'

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
    dataIndex: 'webpUrl',
    align: 'center',
    width: 200,
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
    title: '审核信息',
    dataIndex: 'reviewMessage',
    width: 250,
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
    width: 250,
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

// 审核图片
const handleReview = async (record: API.Picture, reviewStatus: number) => {
  const reviewMessage =
    reviewStatus === PIC_REVIEW_STATUS_ENUM.PASS ? '管理员审核通过' : '管理员审核拒绝'
  const res = await doPictureReviewUsingPost({
    id: record.id,
    reviewStatus,
    reviewMessage,
  })
  if (res.data.code === 0) {
    message.success('审核操作成功')
    // 重新获取列表数据
    fetchData()
  } else {
    message.error('审核操作失败，' + res.data.message)
  }
}
</script>
