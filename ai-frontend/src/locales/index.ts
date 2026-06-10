import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

export type LocaleKey = 'zh-CN' | 'en-US'

const STORAGE_KEY = 'locale'

function getInitialLocale(): LocaleKey {
  if (typeof window === 'undefined') return 'zh-CN'
  const stored = window.localStorage.getItem(STORAGE_KEY)
  if (stored === 'zh-CN' || stored === 'en-US') return stored
  const nav = (window.navigator?.language || '').toLowerCase()
  if (nav.startsWith('en')) return 'en-US'
  return 'zh-CN'
}

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: getInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export default i18n

export function setI18nLocale(lang: LocaleKey) {
  i18n.global.locale.value = lang
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, lang)
    document.documentElement.setAttribute('lang', lang)
  }
}
