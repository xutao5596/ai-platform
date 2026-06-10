import { ref, watch, computed } from 'vue'

export type ThemeMode = 'light' | 'dark'

const STORAGE_KEY = 'theme'

function readInitial(): ThemeMode {
  if (typeof window === 'undefined') return 'light'
  try {
    const stored = window.localStorage.getItem(STORAGE_KEY) as ThemeMode | null
    if (stored === 'light' || stored === 'dark') return stored
  } catch {
    /* localStorage may be unavailable */
  }
  if (typeof window.matchMedia === 'function' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark'
  }
  return 'light'
}

function applyToHtml(mode: ThemeMode) {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  if (mode === 'dark') {
    root.classList.add('dark')
    root.setAttribute('data-theme', 'dark')
  } else {
    root.classList.remove('dark')
    root.setAttribute('data-theme', 'light')
  }
}

function persist(mode: ThemeMode) {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(STORAGE_KEY, mode)
  } catch {
    /* ignore */
  }
}

// Module-level singleton state so theme is shared across the app.
const theme = ref<ThemeMode>(readInitial())

// Apply once on module load so initial paint matches.
applyToHtml(theme.value)

watch(theme, (val) => {
  applyToHtml(val)
  persist(val)
})

export function useTheme() {
  function setTheme(t: ThemeMode) {
    theme.value = t
  }
  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }
  const isDark = computed(() => theme.value === 'dark')
  return {
    theme,
    isDark,
    setTheme,
    toggleTheme
  }
}
