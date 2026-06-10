import { computed } from 'vue'
import i18n, { setI18nLocale, type LocaleKey } from '@/locales'

export function useLocale() {
  const locale = computed<LocaleKey>({
    get: () => i18n.global.locale.value as LocaleKey,
    set: (val) => setI18nLocale(val)
  })

  function setLocale(lang: LocaleKey) {
    setI18nLocale(lang)
  }

  function toggleLocale() {
    setLocale(locale.value === 'zh-CN' ? 'en-US' : 'zh-CN')
  }

  return {
    locale,
    setLocale,
    toggleLocale
  }
}
