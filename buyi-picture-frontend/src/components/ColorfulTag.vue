<template>
  <a-tag :color="getColor()">
    {{ text }}
  </a-tag>
</template>

<script lang="ts" setup>
import { ref } from 'vue'

const props = defineProps<{
  text: string
  useFixed?: boolean // 新增属性，控制是否使用固定颜色
}>()

const colors = [
  'pink',
  'red',
  'yellow',
  'orange',
  'cyan',
  'green',
  'blue',
  'purple',
  'geekblue',
  'magenta',
  'volcano',
  'gold',
  'lime',
]

// 根据文本内容确定固定颜色
const getFixedColor = (text: string) => {
  const hash = text.split('').reduce((acc, char) => {
    return char.charCodeAt(0) + ((acc << 5) - acc)
  }, 0)
  return colors[Math.abs(hash) % colors.length]
}

// 获取随机颜色
const getRandomColor = () => {
  return colors[Math.floor(Math.random() * colors.length)]
}

const getColor = () => {
  return props.useFixed ? getFixedColor(props.text) : getRandomColor()
}
</script>
