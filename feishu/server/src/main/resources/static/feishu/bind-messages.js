/**
 * Centralized UI messages for the Feishu bind page.
 * To add a new language, create a parallel object and switch based on navigator.language.
 */
var BIND_MESSAGES = {
  pageTitle: '绑定数据平台账号',
  feishuUserLabel: '飞书用户',
  usernameLabel: '平台账号',
  usernamePlaceholder: '请输入平台账号',
  passwordLabel: '密码',
  passwordPlaceholder: '请输入密码',
  submitBtn: '确认绑定',
  loading: '正在验证...',
  tokenInvalid: '绑定链接无效，请在飞书对话中重新获取。',
  bindSuccess: function (name) {
    return '绑定成功！已关联平台用户「' + name + '」，请返回飞书对话继续使用。';
  },
  bindFallbackError: '绑定失败，请重试',
  networkError: '网络请求失败，请检查网络后重试'
};
