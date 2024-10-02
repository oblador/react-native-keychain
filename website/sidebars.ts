import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docs: [
    {
      type: 'doc',
      id: 'index',
      label: 'Introduction',
    },
    {
      type: 'doc',
      id: 'usage',
      label: 'Usage',
    },
    {
      type: 'doc',
      id: 'jest',
      label: 'Unit Testing with Jest',
    },
    {
      type: 'doc',
      id: 'faq',
      label: 'Frequently Asked Questions',
    },
    /*{
      type: 'category',
      label: 'Guides',
      link: {
        type: 'generated-index',
        title: 'react-native-keychain guides',
      },
      items: ['guides/gettingStarted'],
    },*/
  ],
  api: ['api/index', require('./docs/api/typedoc-sidebar.cjs')],
};

export default sidebars;
