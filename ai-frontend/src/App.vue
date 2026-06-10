<template>
  <el-config-provider :locale="elLocale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, watch, onMounted } from 'vue'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
import { useLocale } from '@/composables/useLocale'

const { locale, setLocale } = useLocale()

const elLocale = computed(() => (locale.value === 'en-US' ? en : zhCn))

onMounted(() => {
  document.documentElement.setAttribute('lang', locale.value)
})

watch(locale, (val) => {
  document.documentElement.setAttribute('lang', val)
  setLocale(val)
})
</script>
