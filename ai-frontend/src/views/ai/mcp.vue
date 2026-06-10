<template>
  <div class="page-container">
    <el-empty :description="t('ai.mcp.placeholder')" />
  </div>
</template>

<script setup lang="ts">
import { useI18n } from 'vue-i18n'
const { t } = useI18n()
</script>
