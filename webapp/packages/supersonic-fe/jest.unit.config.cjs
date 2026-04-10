module.exports = {
  testEnvironment: 'jest-environment-jsdom',
  testMatch: ['<rootDir>/tests/unit/**/*.test.js'],
  passWithNoTests: false,
  verbose: false,
  setupFiles: ['<rootDir>/tests/unit/setupEnv.js'],
};
