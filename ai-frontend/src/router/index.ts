import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import nprogress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/store/modules/user'

nprogress.configure({ showSpinner: false })

type RouteMetaI18n = {
  public?: boolean
  title?: string
  titleKey?: string
  icon?: string
  hidden?: boolean
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    meta: { public: true, title: 'Login', titleKey: 'nav.login' }
  },
  {
    path: '/',
    component: () => import('@/layouts/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/index.vue'), meta: { title: 'Dashboard', titleKey: 'nav.dashboard', icon: 'HomeFilled' } },
      { path: 'project', component: () => import('@/views/project/list.vue'), meta: { title: 'Projects', titleKey: 'nav.project', icon: 'Folder' } },
      { path: 'project/:id', component: () => import('@/views/project/detail.vue'), meta: { title: 'Project Detail', titleKey: 'nav.projectDetail', hidden: true } },
      { path: 'project/:id/members', component: () => import('@/views/project/members.vue'), meta: { title: 'Members', titleKey: 'nav.projectMembers', hidden: true } },
      { path: 'project/:id/apikeys', component: () => import('@/views/project/apikeys.vue'), meta: { title: 'API Keys', titleKey: 'nav.projectApiKeys', hidden: true } },
      { path: 'project/:id/webhooks', component: () => import('@/views/project/webhooks.vue'), meta: { title: 'Webhooks', titleKey: 'nav.projectWebhooks', hidden: true } },
      { path: 'ai/model', component: () => import('@/views/ai/model.vue'), meta: { title: 'Models', titleKey: 'nav.aiModel', icon: 'MagicStick' } },
      { path: 'ai/mcp', component: () => import('@/views/ai/mcp.vue'), meta: { title: 'MCP', titleKey: 'nav.aiMcp', icon: 'Connection' } },
      { path: 'ai/knowledge', component: () => import('@/views/ai/knowledge.vue'), meta: { title: 'Knowledge', titleKey: 'nav.aiKnowledge', icon: 'Reading' } },
      { path: 'ai/prompt', component: () => import('@/views/ai/prompt.vue'), meta: { title: 'Prompts', titleKey: 'nav.aiPrompt', icon: 'Document' } },
      { path: 'ai/chat', component: () => import('@/views/ai/chat.vue'), meta: { title: 'AI Chat', titleKey: 'nav.aiChat', icon: 'ChatDotRound' } },
      { path: 'flow', component: () => import('@/views/flow/list.vue'), meta: { title: 'Flows', titleKey: 'nav.flow', icon: 'Share' } },
      { path: 'flow/:id', component: () => import('@/views/flow/detail.vue'), meta: { title: 'Flow Detail', titleKey: 'nav.flowDetail', hidden: true } },
      { path: 'flow/:id/editor', component: () => import('@/views/flow/editor/index.vue'), meta: { title: 'Flow Editor', titleKey: 'nav.flowEditor', hidden: true } },
      { path: 'flow/:id/runs', component: () => import('@/views/flow/runs.vue'), meta: { title: 'Run History', titleKey: 'nav.flowRuns', hidden: true } },
      { path: 'monitor', component: () => import('@/views/monitor/index.vue'), meta: { title: 'Monitor', titleKey: 'nav.monitor', icon: 'Monitor' } },
      { path: 'system/user', component: () => import('@/views/system/user.vue'), meta: { title: 'Users', titleKey: 'nav.systemUser', icon: 'User' } },
      { path: 'system/role', component: () => import('@/views/system/role.vue'), meta: { title: 'Roles', titleKey: 'nav.systemRole', icon: 'UserFilled' } },
      { path: 'system/menu', component: () => import('@/views/system/menu.vue'), meta: { title: 'Menus', titleKey: 'nav.systemMenu', icon: 'Menu' } },
      { path: 'system/dept', component: () => import('@/views/system/dept.vue'), meta: { title: 'Departments', titleKey: 'nav.systemDept', icon: 'OfficeBuilding' } },
      { path: 'system/dict', component: () => import('@/views/system/dict.vue'), meta: { title: 'Dictionaries', titleKey: 'nav.systemDict', icon: 'Collection' } },
      { path: 'system/log', component: () => import('@/views/system/log.vue'), meta: { title: 'Audit Log', titleKey: 'nav.systemLog', icon: 'Document' } },
      { path: 'assistant', component: () => import('@/views/assistant/index.vue'), meta: { title: 'Assistant', titleKey: 'nav.assistant', icon: 'ChatDotRound' } }
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
  const meta = to.meta as RouteMetaI18n
  const title = meta.title || ''
  document.title = title ? `${title} · AI Platform` : 'AI Platform'
})

export default router
