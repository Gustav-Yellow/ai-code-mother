<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'

// 保证在第一时间获取到登录用户的信息，因为项目启动时最先加载的就是 App.vue
const loginUserStore = useLoginUserStore();
loginUserStore.fetchLoginUser();
</script>

<template>
  <BasicLayout />
</template>

<style>
</style>