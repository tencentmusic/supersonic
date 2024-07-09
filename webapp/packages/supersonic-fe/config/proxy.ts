export default {
  dev: {
    '/api/': {
      target: 'http://localhost:9080',
      // target: 'https://chatdata-dev.test.seewo.com/',
      changeOrigin: true,
    },
  },
};
