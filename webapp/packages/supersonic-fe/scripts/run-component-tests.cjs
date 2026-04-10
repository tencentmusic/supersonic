const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..', 'src', 'components');

function findTests(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const results = [];
  for (const entry of entries) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      results.push(...findTests(fullPath));
      continue;
    }
    if (/\.test\.(js|jsx|ts|tsx)$/.test(entry.name)) {
      results.push(fullPath);
    }
  }
  return results;
}

const testFiles = fs.existsSync(root) ? findTests(root) : [];

if (testFiles.length === 0) {
  console.log('No component tests are configured under src/components.');
  process.exit(0);
}

console.error('Component test files exist, but no runnable component test harness is configured.');
console.error('Please wire Jest/Umi component testing before using test:component.');
process.exit(1);
