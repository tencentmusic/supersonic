export default {
  dev: {
    '/api/chat/': {
      target: 'http://localhost:9080',
      changeOrigin: true,
    },
    '/api/semantic/': {
      target: 'http://localhost:9081',
      changeOrigin: true,
    },
    '/api/': {
      target: 'http://localhost:9080',
      changeOrigin: true,
    },
  },
};
