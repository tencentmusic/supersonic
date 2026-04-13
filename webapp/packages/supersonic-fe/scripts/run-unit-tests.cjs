const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');

const projectRoot = path.resolve(__dirname, '..');
const outDir = path.join(projectRoot, '.tmp-unit');
const tscBin = path.join(projectRoot, 'node_modules', 'typescript', 'bin', 'tsc');
const jestBin = path.join(projectRoot, 'node_modules', '.bin', 'jest');

function compileMenuFilter() {
  fs.rmSync(outDir, { recursive: true, force: true });
  execFileSync(process.execPath, [
    tscBin,
    path.join(projectRoot, 'src/utils/menuFilter.ts'),
    path.join(projectRoot, 'src/pages/RouteGroupRedirect/resolveRedirect.ts'),
    path.join(projectRoot, 'src/pages/ReportSchedule/utils/scheduleFormValidation.ts'),
    '--module',
    'commonjs',
    '--target',
    'es2019',
    '--rootDir',
    path.join(projectRoot, 'src'),
    '--outDir',
    outDir,
    '--esModuleInterop',
    '--skipLibCheck',
  ], {
    stdio: 'inherit',
  });
}

function runMenuFilterTests() {
  assert.ok(fs.existsSync(jestBin), 'Local jest binary not found');
  execFileSync(jestBin, [
    '--config',
    path.join(projectRoot, 'jest.unit.config.cjs'),
    '--runInBand',
  ], {
    cwd: projectRoot,
    stdio: 'inherit',
  });
}

compileMenuFilter();
runMenuFilterTests();
