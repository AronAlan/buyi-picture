<template>
  <div class="forbidden-container">
    <div class="content">
      <h1>403</h1>
      <h2>抱歉，您没有访问权限</h2>
      <a-button type="primary" size="large" @click="goHome">返回首页</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
const goHome = () => router.push('/')
</script>

<style scoped>
.forbidden-container {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.content {
  text-align: center;
}

h1 {
  font-size: 80px;
  color: #ff4d4f;
  margin-bottom: 16px;
  font-weight: bold;
}

h2 {
  font-size: 24px;
  color: #666;
  margin-bottom: 24px;
}
</style>
