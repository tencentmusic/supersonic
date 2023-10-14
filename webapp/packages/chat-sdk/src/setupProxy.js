const { createProxyMiddleware } = require('http-proxy-middleware');
const { proxyTarget } = require('./common/env');

module.exports = function(app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: proxyTarget,
      changeOrigin: true,
    })
  );
  app.use(
    '/openapi',
    createProxyMiddleware({
      target: proxyTarget,
      changeOrigin: true,
    })
  );
};