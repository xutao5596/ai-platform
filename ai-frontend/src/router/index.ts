import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import nprogress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/store/modules/user'

nprogress.configure({ showSpinner: false })

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: '工作台', icon: 'HomeFilled' } },
      { path: 'project', component: () => import('@/views/project/list.vue'), meta: { title: '我的项目', icon: 'Folder' } },
      { path: 'project/:id', component: () => import('@/views/project/detail.vue'), meta: { title: '项目详情', hidden: true } },
      { path: 'project/:id/members', component: () => import('@/views/project/members.vue'), meta: { title: '项目成员', hidden: true } },
      { path: 'project/:id/apikeys', component: () => import('@/views/project/apikeys.vue'), meta: { title: 'API Key 管理', hidden: true } },
      { path: 'ai/model', component: () => import('@/views/ai/model.vue'), meta: { title: '模型管理', icon: 'MagicStick' } },
      { path: 'ai/mcp', component: () => import('@/views/ai/mcp.vue'), meta: { title: 'MCP 服务', icon: 'Connection' } },
      { path: 'ai/knowledge', component: () => import('@/views/ai/knowledge.vue'), meta: { title: '知识库', icon: 'Reading' } },
      { path: 'ai/prompt', component: () => import('@/views/ai/prompt.vue'), meta: { title: '提示词', icon: 'Document' } },
      { path: 'ai/chat', component: () => import('@/views/ai/chat.vue'), meta: { title: 'AI 对话', icon: 'ChatDotRound' } },
      { path: 'flow', component: () => import('@/views/flow/list.vue'), meta: { title: '流程', icon: 'Share' } },
      { path: 'flow/:id', component: () => import('@/views/flow/detail.vue'), meta: { title: '流程详情', hidden: true } },
      { path: 'flow/:id/editor', component: () => import('@/views/flow/editor/index.vue'), meta: { title: '流程编辑器', hidden: true } },
      { path: 'flow/:id/runs', component: () => import('@/views/flow/runs.vue'), meta: { title: '运行历史', hidden: true } },
      { path: 'monitor', component: () => import('@/views/monitor/index.vue'), meta: { title: '监控', icon: 'Monitor' } },
      { path: 'system/user', component: () => import('@/views/system/user.vue'), meta: { title: '用户管理', icon: 'User' } },
      { path: 'system/role', component: () => import('@/views/system/role.vue'), meta: { title: '角色管理', icon: 'UserFilled' } },
      { path: 'system/menu', component: () => import('@/views/system/menu.vue'), meta: { title: '菜单管理', icon: 'Menu' } },
      { path: 'system/dept', component: () => import('@/views/system/dept.vue'), meta: { title: '部门管理', icon: 'OfficeBuilding' } },
      { path: 'system/dict', component: () => import('@/views/system/dict.vue'), meta: { title: '字典管理', icon: 'Collection' } },
      { path: 'system/log', component: () => import('@/views/system/log.vue'), meta: { title: '操作日志', icon: 'Document' } },
      { path: 'assistant', component: () => import('@/views/assistant/index.vue'), meta: { title: 'AI 助手', icon: 'ChatDotRound' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  nprogress.start()
  const userStore = useUserStore()
  if (to.meta.public) {
    next()
  } else if (!userStore.isLogin) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

router.afterEach((to) => {
  nprogress.done()
  const title = (to.meta.title as string) || ''
  document.title = title ? `${title} · AI Platform` : 'AI Platform'
})

export default router
