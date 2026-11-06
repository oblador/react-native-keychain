import { readdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { defineConfig, type DefaultTheme } from 'vitepress';

const repository = 'https://github.com/oblador/react-native-keychain';

// Generate the API first so newly exported symbols also appear in navigation.
const apiGroups = [
  ['Functions', 'functions'],
  ['Enums', 'enumerations'],
  ['Types', 'type-aliases'],
].map(
  ([text, directory]): DefaultTheme.SidebarItem => ({
    text,
    collapsed: true,
    items: readdirSync(
      fileURLToPath(new URL(`../docs/api/${directory}/`, import.meta.url))
    )
      .filter((file) => file.endsWith('.md'))
      .sort()
      .map((file) => ({
        text: file
          .replace(/\.md$/, '')
          .replace(/([a-z])([A-Z])/g, '$1\u200b$2'),
        link: `/api/${directory}/${file.replace(/\.md$/, '')}`,
      })),
  })
);

export default defineConfig({
  title: 'React Native Keychain',
  description:
    'Store credentials with Apple Keychain and Android Keystore. Setup, practical guides, and API reference for React Native Keychain.',
  base: '/react-native-keychain/',
  srcDir: './docs',
  appearance: 'force-dark',
  lastUpdated: true,
  head: [
    [
      'link',
      {
        rel: 'icon',
        type: 'image/svg+xml',
        href: '/react-native-keychain/key.svg',
      },
    ],
    ['meta', { name: 'theme-color', content: '#000000' }],
  ],
  markdown: { theme: 'github-dark-high-contrast' },
  transformPageData(page) {
    if (page.relativePath.startsWith('api/'))
      page.frontmatter.pageClass = 'api-page';
  },
  themeConfig: {
    notFound: {
      title: 'Page not found',
      quote:
        'This page may have moved. Open the current docs or search for an API.',
      linkText: 'Back to documentation',
      linkLabel: 'Back to documentation',
    },
    logo: { src: '/key.svg', alt: '' },
    siteTitle: 'Keychain',
    nav: [
      { text: 'Guide', link: '/', activeMatch: '^/(?!api/)' },
      { text: 'API reference', link: '/api/index', activeMatch: '^/api/' },
      { text: 'Releases', link: `${repository}/releases` },
    ],
    socialLinks: [{ icon: 'github', link: repository }],
    search: { provider: 'local' },
    sidebar: [
      {
        text: 'Get started',
        items: [
          { text: 'Introduction', link: '/' },
          { text: 'Installation', link: '/installation' },
          { text: 'Store and retrieve', link: '/usage' },
          { text: 'Example app', link: '/example-app' },
        ],
      },
      {
        text: 'Guides',
        items: [
          { text: 'Authentication', link: '/authentication' },
          { text: 'Platform storage', link: '/platform-value-storage' },
          { text: 'Data persistence', link: '/data-persistence' },
          { text: 'Testing with Jest', link: '/jest' },
          { text: 'Common questions', link: '/faq' },
        ],
      },
      {
        text: 'API reference',
        items: [{ text: 'Overview', link: '/api/index' }, ...apiGroups],
      },
    ],
    outline: { level: [2, 3], label: 'On this page' },
    editLink: {
      pattern: ({ relativePath }) =>
        relativePath.startsWith('api/')
          ? 'https://github.com/oblador/react-native-keychain/edit/master/src/index.ts'
          : `https://github.com/oblador/react-native-keychain/edit/master/website/docs/${relativePath}`,
      text: 'Edit this page on GitHub',
    },
    docFooter: { prev: 'Previous', next: 'Next' },
  },
});
