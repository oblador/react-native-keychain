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
    {
      type: 'category',
      label: 'Android',
      items: [
        {
          type: 'doc',
          id: 'choosing-storage-type',
          label: 'Choosing Storage Type',
        },
        {
          type: 'doc',
          id: 'secure-hardware-vs-software',
          label: 'Secure Hardware vs Software',
        },
      ],
    },
  ],
  api: ['api/index', require('./docs/api/typedoc-sidebar.cjs')],
};

export default sidebars;
