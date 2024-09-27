export default {
  dev: {
    '/api/': {
      target: 'http://10.91.210.65:9080',
      // target: 'http://s2.tmeoa.com',
      changeOrigin: true,
    },
  },
};
