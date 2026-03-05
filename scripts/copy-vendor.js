import { cpSync, mkdirSync } from 'fs';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = resolve(__dirname, '..');
const staticJs = resolve(root, 'src/main/resources/static/js');

mkdirSync(staticJs, { recursive: true });

const vendors = [
  {
    src: 'node_modules/lucide/dist/umd/lucide.min.js',
    dest: 'lucide.min.js'
  },
  {
    src: 'node_modules/feather-icons/dist/feather.min.js',
    dest: 'feather.min.js'
  },
  {
    src: 'node_modules/chart.js/dist/chart.umd.js',
    dest: 'chart.min.js'
  }
];

for (const { src, dest } of vendors) {
  const from = resolve(root, src);
  const to = resolve(staticJs, dest);
  try {
    cpSync(from, to);
    console.log(`  copied: ${dest}`);
  } catch (e) {
    console.error(`  FAILED: ${dest} — ${e.message}`);
  }
}

console.log('vendor JS copy done.');
