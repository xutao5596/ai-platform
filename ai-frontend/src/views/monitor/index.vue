<template>
  <div class="page-container monitor">
    <div class="page-header">
      <span class="page-title">{{ t('monitor.title') }}</span>
      <el-button :icon="Refresh" @click="loadAll" :loading="loading">{{ t('monitor.refresh') }}</el-button>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">{{ t('monitor.statFlow') }}</div>
          <div class="stat-value">{{ stats.flowCount }}</div>
          <el-icon class="stat-icon stat-icon-primary"><Connection /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">{{ t('monitor.statAssistant') }}</div>
          <div class="stat-value">{{ stats.assistantCount }}</div>
          <el-icon class="stat-icon stat-icon-success"><ChatDotRound /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">{{ t('monitor.statChat') }}</div>
          <div class="stat-value">{{ stats.chatToday }}</div>
          <el-icon class="stat-icon stat-icon-purple"><Promotion /></el-icon>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-label">{{ t('monitor.statError') }}</div>
          <div class="stat-value">{{ stats.errorRate }}</div>
          <el-icon class="stat-icon stat-icon-warning"><WarningFilled /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <div class="last-update">{{ t('monitor.lastUpdate', { time: lastUpdate }) }}</div>

    <el-row :gutter="16" class="charts-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-title">{{ t('monitor.chartFlowRun') }}</div>
          </template>
          <div ref="flowRunChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-title">{{ t('monitor.chartLogin') }}</div>
          </template>
          <div ref="loginChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-title">{{ t('monitor.chartNode') }}</div>
          </template>
          <div ref="nodeChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-title">{{ t('monitor.chartWebhook') }}</div>
          </template>
          <div ref="webhookChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Refresh, Connection, ChatDotRound, Promotion, WarningFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { flowApi } from '@/api/flow'
import { projectApi } from '@/api/project'
import { assistantApi } from '@/api/assistant'

const { t } = useI18n()

const loading = ref(false)
const lastUpdate = ref('-')
const stats = reactive({ flowCount: 0, assistantCount: 0, chatToday: 0, errorRate: '0%' })

const flowRunChartRef = ref<HTMLDivElement>()
const loginChartRef = ref<HTMLDivElement>()
const nodeChartRef = ref<HTMLDivElement>()
const webhookChartRef = ref<HTMLDivElement>()

let flowRunChart: echarts.ECharts | null = null
let loginChart: echarts.ECharts | null = null
let nodeChart: echarts.ECharts | null = null
let webhookChart: echarts.ECharts | null = null
let timer: number | null = null

function parsePrometheusSeries(text: string, name: string): Map<string, number> {
  const map = new Map<string, number>()
  const lines = text.split('\n')
  for (const raw of lines) {
    const line = raw.trim()
    if (!line || line.startsWith('#')) continue
    if (!line.startsWith(name)) continue
    const lastSpace = line.lastIndexOf(' ')
    if (lastSpace < 0) continue
    const head = line.substring(0, lastSpace)
    const val = parseFloat(line.substring(lastSpace + 1))
    if (Number.isNaN(val)) continue
    map.set(head, val)
  }
  return map
}

function aggregateByLabel(series: Map<string, number>): { labels: string[]; values: number[] } {
  const buckets = new Map<string, number>()
  for (const [head, val] of series.entries()) {
    const m = head.match(/\{[^}]*\}/)
    const labels = m ? m[0] : ''
    const key = labels
    buckets.set(key, (buckets.get(key) || 0) + val)
  }
  const entries = Array.from(buckets.entries()).slice(0, 8)
  return {
    labels: entries.map((e) => e[0] || '-'),
    values: entries.map((e) => Math.round(e[1]))
  }
}

async function fetchPrometheus() {
  const res = await axios.get('/actuator/prometheus', { baseURL: window.location.origin, timeout: 10000 })
  return typeof res.data === 'string' ? res.data : JSON.stringify(res.data)
}

async function loadMetrics() {
  try {
    const text = await fetchPrometheus()
    // 1) flow_run_duration_seconds_count — counter; aggregate per status
    const flowRun = parsePrometheusSeries(text, 'flow_run_duration_seconds_count')
    const flowRunAgg = aggregateByLabel(flowRun)
    flowRunChart?.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: flowRunAgg.labels.length ? flowRunAgg.labels : [t('monitor.noData')], axisLabel: { interval: 0, rotate: 20, fontSize: 10 } },
      yAxis: { type: 'value' },
      series: [{ type: 'line', data: flowRunAgg.values, smooth: true, areaStyle: {}, itemStyle: { color: '#409EFF' } }]
    })

    // 2) login_count_total — counter
    const loginSeries = parsePrometheusSeries(text, 'login_count_total')
    const loginAgg = aggregateByLabel(loginSeries)
    loginChart?.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: loginAgg.labels.length ? loginAgg.labels : [t('monitor.noData')], axisLabel: { fontSize: 10 } },
      yAxis: { type: 'value' },
      series: [{ type: 'line', data: loginAgg.values, smooth: true, itemStyle: { color: '#67C23A' } }]
    })

    // 3) flow_node_execute_total — counter, aggregate per node label
    const nodeSeries = parsePrometheusSeries(text, 'flow_node_execute_total')
    const nodeAgg = aggregateByLabel(nodeSeries)
    nodeChart?.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['35%', '65%'],
        data: nodeAgg.labels.length
          ? nodeAgg.labels.map((l, i) => ({ name: l, value: nodeAgg.values[i] }))
          : [{ name: t('monitor.noData'), value: 0 }]
      }]
    })

    // 4) webhook_dispatch_total — counter, aggregate per status
    const whSeries = parsePrometheusSeries(text, 'webhook_dispatch_total')
    const whAgg = aggregateByLabel(whSeries)
    webhookChart?.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: {},
      grid: { left: 40, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: whAgg.labels.length ? whAgg.labels : [t('monitor.noData')], axisLabel: { fontSize: 10 } },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', stack: 'wh', data: whAgg.values, itemStyle: { color: '#E6A23C' } }]
    })
  } catch (e) {
    // Prometheus may be empty or 401; show empty charts
    const empty = [t('monitor.noData')]
    flowRunChart?.setOption({ xAxis: { data: empty }, series: [{ data: [0] }] })
    loginChart?.setOption({ xAxis: { data: empty }, series: [{ data: [0] }] })
    nodeChart?.setOption({ series: [{ data: [{ name: empty[0], value: 0 }] }] })
    webhookChart?.setOption({ xAxis: { data: empty }, series: [{ data: [0] }] })
  }
}

async function loadStats() {
  try {
    const [flowsPage, projects] = await Promise.all([
      flowApi.page({ current: 1, size: 1 }),
      projectApi.mine()
    ])
    stats.flowCount = flowsPage.total
    // Sum assistants across all mine projects
    const projList = Array.isArray(projects) ? projects : []
    const aLists = await Promise.all(
      projList.map((p: any) => assistantApi.list(p.id).catch(() => []))
    )
    stats.assistantCount = aLists.reduce((s, l) => s + (Array.isArray(l) ? l.length : 0), 0)
  } catch {
    /* ignore */
  }
  // chatToday + errorRate: backend not implemented yet
  stats.chatToday = 0
  stats.errorRate = '0%'
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([loadMetrics(), loadStats()])
    lastUpdate.value = new Date().toLocaleTimeString()
  } finally {
    loading.value = false
  }
}

function initCharts() {
  if (flowRunChartRef.value) flowRunChart = echarts.init(flowRunChartRef.value)
  if (loginChartRef.value) loginChart = echarts.init(loginChartRef.value)
  if (nodeChartRef.value) nodeChart = echarts.init(nodeChartRef.value)
  if (webhookChartRef.value) webhookChart = echarts.init(webhookChartRef.value)
}

function resize() {
  flowRunChart?.resize()
  loginChart?.resize()
  nodeChart?.resize()
  webhookChart?.resize()
}

onMounted(() => {
  initCharts()
  loadAll()
  window.addEventListener('resize', resize)
  timer = window.setInterval(loadAll, 30000)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('resize', resize)
  flowRunChart?.dispose()
  loginChart?.dispose()
  nodeChart?.dispose()
  webhookChart?.dispose()
})
</script>

<style scoped>
.monitor { display: flex; flex-direction: column; gap: 16px; }
.stats-row { margin-bottom: 0; }
.last-update { color: var(--ai-text-secondary); font-size: 12px; }
.charts-row { margin-top: 0; }
.stat-card { position: relative; overflow: hidden; }
.stat-label { color: var(--ai-text-secondary); font-size: 13px; }
.stat-value { font-size: 28px; font-weight: 600; margin-top: 8px; }
.stat-icon { position: absolute; right: 16px; top: 50%; transform: translateY(-50%); font-size: 48px; opacity: 0.15; }
.stat-icon-primary { color: var(--ai-primary); }
.stat-icon-success { color: var(--ai-success); }
.stat-icon-warning { color: var(--ai-warning); }
.stat-icon-purple { color: #8b5cf6; }
.chart-box { width: 100%; height: 300px; }
.card-title { font-weight: 600; }
</style>
