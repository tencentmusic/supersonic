(function () {
  var msg = BIND_MESSAGES;

  // Populate static text from messages
  document.getElementById('pageTitle').textContent = msg.pageTitle;
  document.getElementById('cardTitle').textContent = msg.pageTitle;
  document.getElementById('feishuUserLabel').textContent = msg.feishuUserLabel;
  document.getElementById('usernameLabel').textContent = msg.usernameLabel;
  document.getElementById('username').placeholder = msg.usernamePlaceholder;
  document.getElementById('passwordLabel').textContent = msg.passwordLabel;
  document.getElementById('password').placeholder = msg.passwordPlaceholder;
  document.getElementById('submitBtn').textContent = msg.submitBtn;
  document.getElementById('loading').textContent = msg.loading;

  var params = new URLSearchParams(window.location.search);
  var token = params.get('token');

  if (!token) {
    showResult(msg.tokenInvalid, false);
    document.getElementById('bindForm').style.display = 'none';
    return;
  }

  // Decode token payload to show Feishu user name
  try {
    var payloadB64 = token.split('.')[0];
    var b64 = payloadB64.replace(/-/g, '+').replace(/_/g, '/');
    while (b64.length % 4) b64 += '=';
    var payload = JSON.parse(atob(b64));
    if (payload.feishuUserName) {
      document.getElementById('feishuUser').textContent = payload.feishuUserName;
    }
  } catch (e) {
    // ignore decode errors
  }

  document.getElementById('bindForm').addEventListener('submit', function (e) {
    e.preventDefault();

    var username = document.getElementById('username').value.trim();
    var password = document.getElementById('password').value;
    if (!username || !password) return;

    var btn = document.getElementById('submitBtn');
    var loading = document.getElementById('loading');
    btn.disabled = true;
    loading.style.display = 'block';
    hideResult();

    fetch('/api/feishu/bind', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ bindToken: token, username: username, password: password })
    })
      .then(function (resp) {
        return resp.json();
      })
      .then(function (data) {
        loading.style.display = 'none';
        if (data.success) {
          showResult(msg.bindSuccess(data.s2UserName || username), true);
          document.getElementById('bindForm').style.display = 'none';
        } else {
          showResult(data.message || msg.bindFallbackError, false);
          btn.disabled = false;
        }
      })
      .catch(function () {
        loading.style.display = 'none';
        showResult(msg.networkError, false);
        btn.disabled = false;
      });
  });

  function showResult(text, ok) {
    var el = document.getElementById('result');
    el.textContent = text;
    el.className = 'result ' + (ok ? 'success' : 'error');
    el.style.display = 'block';
  }

  function hideResult() {
    document.getElementById('result').style.display = 'none';
  }
})();
