const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  // app.use(
  //   '/api',
  //   createProxyMiddleware({
  //     target: 'http://10.91.206.71:9079',
  //     changeOrigin: true,
  //   })
  // );
  app.use(
    '/api',
    // '/api',
    createProxyMiddleware({
      target: 'http://supersonic.test.tmeoa.com',
      changeOrigin: true,
    })
  );
};