import { themes as prismThemes } from 'prism-react-renderer';
import type { Config } from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';
import pkg from '../package.json';

const REPO_URL = 'https://github.com/oblador/react-native-keychain';

const config: Config = {
  title: 'react-native-keychain',
  tagline: 'react-native-keychain docs',
  url: 'https://oblador.github.io',
  baseUrl: '/react-native-keychain',
  projectName: 'oblador.github.io/react-native-keychain',
  organizationName: 'react-native-keychain',
  favicon: 'img/favicon.png',

  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',

  plugins: [
    [
      'docusaurus-plugin-typedoc',
      {
        entryPoints: ['../src/index.ts'],
        tsconfig: '../tsconfig.docs.json',
        name: 'API',
        readme: 'none',
        groupOrder: ['Functions', 'Enums'],
        suppressCommentWarningsInDeclarationFiles: true,
        excludePrivate: true,
        excludeExternals: true,
        excludeProtected: true,
        excludeCategories: ['Variables'],
      },
    ],
  ],
  presets: [
    [
      'classic',
      {
        docs: {
          path: './docs/',
          sidebarPath: require.resolve('./sidebars.ts'),
          sidebarCollapsible: false,
          editUrl: `${REPO_URL}/edit/master/website/docs/`,
          lastVersion: 'current',
          versions: {
            // add more versions if needed
            current: {
              label: `${pkg.version}`,
              path: '/',
              badge: false,
            },
          },
        },
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    navbar: {
      title: 'react-native-keychain',
      logo: {
        src: 'img/logo.png',
      },
      items: [
        {
          type: 'docsVersionDropdown',
          position: 'left',
          dropdownActiveClassDisabled: true,
        },
        {
          type: 'doc',
          docId: 'index',
          label: 'Documentation',
          position: 'right',
        },
        {
          type: 'docSidebar',
          sidebarId: 'api',
          label: 'API',
          position: 'right',
        },
        {
          href: REPO_URL,
          label: 'GitHub',
          position: 'right',
        },
        {
          type: 'search',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      copyright: `Copyright © ${new Date().getFullYear()} react-native-keychain`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
