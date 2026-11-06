import { readFile, readdir, stat } from 'node:fs/promises';
import { extname } from 'node:path';

const output = new URL('../.vitepress/dist/', import.meta.url);
const base = '/react-native-keychain/';
const origin = 'https://oblador.github.io';
const files = (await readdir(output, { recursive: true })).filter((file) =>
  file.endsWith('.html')
);
const failures = new Set();
const cache = new Map();

for (const file of files) {
  const html = await readFile(new URL(file, output), 'utf8');
  for (const [, attribute] of html.matchAll(/(?:href|src)="([^"]+)"/g)) {
    const url = new URL(
      attribute.replaceAll('&amp;', '&'),
      `${origin}${base}${file}`
    );
    if (url.origin !== origin) continue;
    if (!url.pathname.startsWith(base)) {
      failures.add(`${file}: link escapes the Pages base: ${attribute}`);
      continue;
    }
    let target = decodeURIComponent(url.pathname.slice(base.length));
    if (target.endsWith('/') || target === '') target += 'index.html';
    else if (!extname(target)) target += '.html';
    const path = new URL(target, output);
    try {
      if (!(await stat(path)).isFile()) throw new Error('Not a file');
      if (url.hash && target.endsWith('.html')) {
        if (!cache.has(target)) {
          const page = await readFile(path, 'utf8');
          cache.set(
            target,
            new Set(
              [...page.matchAll(/\bid="([^"]+)"/g)].map((match) => match[1])
            )
          );
        }
        if (!cache.get(target).has(decodeURIComponent(url.hash.slice(1)))) {
          failures.add(`${file}: missing anchor ${attribute}`);
        }
      }
    } catch {
      failures.add(`${file}: missing file ${attribute}`);
    }
  }
}
if (failures.size) {
  console.error([...failures].join('\n'));
  process.exitCode = 1;
} else {
  console.log(
    `Checked internal links, assets, and anchors in ${files.length} HTML files.`
  );
}
