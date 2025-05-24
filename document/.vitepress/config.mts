import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  title: "JimmerBuddy",
  rewrites: {
    'en/:rest*': ':rest*'
  },
  locales: {
    root: {
      label: 'English'
    },
    zh: {
      label: '简体中文'
    }
  },
  themeConfig: {
    // https://vitepress.dev/reference/default-theme-config
    logo: { src: '/logo.svg', width: 24, height: 24 },
    socialLinks: [
      { icon: 'github', link: 'https://github.com/Enaium/JimmerBuddy' }
    ]
  },
  head: [
    ['link', { rel: 'icon', type: 'image/svg+xml', href: '/logo.svg' }]
  ]
})
