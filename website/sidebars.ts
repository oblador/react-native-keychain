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
      id: 'choosing-storage-type',
      label: 'Choosing Storage Type',
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
  ],
  api: ['api/index', require('./docs/api/typedoc-sidebar.cjs')],
};

export default sidebars;
