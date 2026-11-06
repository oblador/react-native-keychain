# Documentation website

VitePress serves the current documentation directly at `/react-native-keychain/`.
There is no landing page, version selector, or archived documentation. The dark
palette and key mark follow `KeychainExample/src/theme.ts` and its app header.

Use the Node version in the repository's `.node-version` and its pinned Yarn.
From the repository root:

```sh
yarn install --immutable
yarn workspace website start
yarn workspace website typecheck
yarn workspace website build
yarn workspace website serve
```

Edit guides in `website/docs/` and navigation/theme in `website/.vitepress/`.
`start` and `build` generate the API from `src/index.ts` and its TSDoc before
running VitePress. Do not edit or commit `website/docs/api/`. After changing
library signatures during development, rerun `yarn workspace website api` and
restart the docs server to refresh the API sidebar.

`build` also emits redirects for old unversioned `/docs/` URLs and checks internal
links, assets, and heading anchors. Output goes to `website/.vitepress/dist/`.
Search is local and includes the generated API. Historical version URLs are no
longer published; their sources remain in Git history.

## Publishing

The Docs workflow runs on `release.published` (including prereleases) and
`workflow_dispatch`. A release builds its tag; a manual run builds the ref selected
in GitHub's **Run workflow** menu. Each successful deployment replaces the one
published documentation site. For a documentation-only update, run the workflow
manually after merging it.

The repository's GitHub Pages source must be **GitHub Actions**. Its `github-pages`
environment must allow the intended release tags and manually selected branches.
The first release using this build must contain the VitePress migration; older
tags still contain the previous website setup. This workflow is independent of
the npm publishing workflow.

Pull requests and pushes to `master` validate website types, build output, and
links without publishing. Release runs and manual runs repeat those checks before
deployment.
