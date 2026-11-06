import { mkdir, readdir, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';

const output = new URL('../.vitepress/dist/', import.meta.url);
const pages = (await readdir(output, { recursive: true })).filter(
  (file) => file.endsWith('.html') && file !== '404.html'
);

// Preserve unversioned Docusaurus bookmarks. No old-version content is shipped.
for (const page of pages) {
  const destination = `/react-native-keychain/${page === 'index.html' ? '' : page}`;
  const html = `<!doctype html><html lang="en"><head><meta charset="utf-8"><title>Page moved</title><meta name="robots" content="noindex"><meta http-equiv="refresh" content="0;url=${destination}"><link rel="canonical" href="${destination}"><script>location.replace(${JSON.stringify(destination)} + location.search + location.hash)</script></head><body><a href="${destination}">Continue to the documentation</a></body></html>`;
  const aliases = new Set([
    join('docs', page),
    join('docs', page.replace(/(?:index)?\.html$/, ''), 'index.html'),
  ]);
  for (const alias of aliases) {
    const file = new URL(alias, output);
    await mkdir(dirname(file.pathname), { recursive: true });
    await writeFile(file, html);
  }
}
console.log(`Preserved unversioned URLs for ${pages.length} pages.`);
