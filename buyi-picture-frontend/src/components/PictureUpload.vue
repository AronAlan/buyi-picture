<template>
  <div class="picture-upload">
    <a-upload
      list-type="picture-card"
      :show-upload-list="false"
      :custom-request="handleUpload"
      :before-upload="beforeUpload"
    >
      <img v-if="picture?.webpUrl" :src="picture?.webpUrl" alt="avatar" />
      <div v-else>
        <loading-outlined v-if="loading"></loading-outlined>
        <plus-outlined v-else></plus-outlined>
        <div class="ant-upload-text">点击或拖拽上传图片</div>
      </div>
    </a-upload>
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons-vue'
import type { UploadProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { uploadPictureUsingPost } from '@/api/pictureController'
interface Props {
  picture?: API.PictureVO
  onSuccess?: (newPicture: API.PictureVO) => void
}
const props = defineProps<Props>()
/**
 * 上传图片
 * @param file
 */
const handleUpload = async ({ file }) => {
  loading.value = true
  try {
    const params = props.picture ? { id: props.picture.id } : {}
    const res = (await uploadPictureUsingPost(params, {}, file)) as any
    if (res.data.code === 0 && res.data.data) {
      message.success('图片上传成功')
      // 将上传成功的图片信息传递给父组件
      props.onSuccess?.(res.data.data)
    } else {
      message.error('图片上传失败，' + res.data.message)
    }
  } catch (error: any) {
    console.error('图片上传失败', error)
    message.error('图片上传失败，' + error.message)
  }
  loading.value = false
}
const loading = ref<boolean>(false)
/**
 * 上传前的校验
 * @param file
 */
const beforeUpload = (file: UploadProps['fileList'][number]) => {
  // 校验图片格式
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('不支持上传该格式的图片，推荐 jpg 或 png')
  }
  // 校验图片大小
  const isLt20M = file.size / 1024 / 1024 < 20
  if (!isLt20M) {
    message.error('不能上传超过 20M 的图片')
  }
  return isJpgOrPng && isLt20M
}
</script>
<style scoped>
.picture-upload {
  position: relative;
  margin: 20px 0;
}

.picture-upload :deep(.ant-upload) {
  width: 100% !important;
  height: 100% !important;
  min-width: 200px;
  min-height: 200px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 16px;
  border: 2px dashed rgba(24, 144, 255, 0.3);
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.05), rgba(24, 144, 255, 0.02));
  overflow: hidden;
}

.picture-upload :deep(.ant-upload:hover) {
  border-color: #40a9ff;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.15);
}

.picture-upload img {
  max-width: 100%;
  max-height: 480px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  object-fit: cover;
}

.picture-upload img:hover {
  transform: scale(1.02);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.ant-upload-select-picture-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(5px);
}

.ant-upload-select-picture-card i {
  font-size: 40px;
  color: #40a9ff;
  transition: all 0.3s;
  margin-bottom: 12px;
  animation: float 3s ease-in-out infinite;
}

.ant-upload-select-picture-card:hover i {
  color: #1890ff;
  transform: scale(1.1);
}

.ant-upload-select-picture-card .ant-upload-text {
  margin-top: 16px;
  color: #666;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

/* 暗色模式支持 */
@media (prefers-color-scheme: dark) {
  .picture-upload :deep(.ant-upload) {
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.05), rgba(255, 255, 255, 0.02));
    border-color: rgba(255, 255, 255, 0.1);
  }

  .ant-upload-select-picture-card {
    background: rgba(0, 0, 0, 0.2);
  }

  .ant-upload-select-picture-card .ant-upload-text {
    color: rgba(255, 255, 255, 0.85);
  }
}
</style>
