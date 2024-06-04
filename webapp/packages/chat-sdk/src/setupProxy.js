const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function (app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'https://chatdata-dev.test.seewo.com/',
      changeOrigin: true,
    })
  );
  app.use(
    '/openapi',
    createProxyMiddleware({
      target: 'https://chatdata-dev.test.seewo.com/',
      changeOrigin: true,
    })
  );
};
