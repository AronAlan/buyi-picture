<template>
  <div id="basicLayout">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <GlobalHeader />
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
      <a-layout-footer class="footer">
        <a href="https://github.com/AronAlan/buyi-picture" target="_blank">
          Author: Samoyer
        </a>
      </a-layout-footer>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
</script>

<style scoped>
#basicLayout .header {
  padding-inline: 20px;
  background: white;
  color: unset;
  margin-bottom: 16px;
}
#basicLayout .content {
  padding: 20px;
  background: linear-gradient(to right, #fefefe, #fff);
}
#basicLayout .footer {
  background: #efefef;
  padding: 16px;
  text-align: center;
  margin-top: -20px;
}
</style>
