export default {
  dev: {
    '/api/': {
      target: 'http://localhost:9080',
      changeOrigin: true,
    },
  },
};
