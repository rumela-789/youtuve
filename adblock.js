(function () {
  if (window.__ytab) return;
  window.__ytab = true;

  // 1) Remove ad data from YouTube's player/next responses before the player sees it.
  var AD_KEYS = ['adPlacements', 'adSlots', 'playerAds', 'adBreakHeartbeatParams'];
  function strip(o) {
    if (!o || typeof o !== 'object') return o;
    for (var i = 0; i < AD_KEYS.length; i++) {
      if (AD_KEYS[i] in o) { try { delete o[AD_KEYS[i]]; } catch (e) {} }
    }
    if (o.playerResponse) strip(o.playerResponse);
    return o;
  }

  try {
    var origParse = JSON.parse;
    JSON.parse = function () {
      var r = origParse.apply(this, arguments);
      try { strip(r); } catch (e) {}
      return r;
    };
  } catch (e) {}

  try {
    var origJson = Response.prototype.json;
    Response.prototype.json = function () {
      return origJson.apply(this, arguments).then(function (d) {
        try { strip(d); } catch (e) {}
        return d;
      });
    };
  } catch (e) {}

  try {
    var ipr;
    Object.defineProperty(window, 'ytInitialPlayerResponse', {
      configurable: true,
      get: function () { return ipr; },
      set: function (v) { try { strip(v); } catch (e) {} ipr = v; }
    });
  } catch (e) {}

  // 2) Hide ad containers.
  var css = [
    'ytm-promoted-sparkles-web-renderer', 'ytm-promoted-video-renderer',
    'ytm-companion-ad-renderer', 'ytm-display-ad-renderer',
    'ytm-ad-slot-renderer', 'ytm-in-feed-ad-layout-renderer',
    'ytm-statement-banner-renderer', 'ytm-banner-promo-renderer',
    '.ytp-ad-module', '.ytp-ad-overlay-container', '.ytp-ad-image-overlay',
    '.video-ads', '#player-ads', '.ad-container'
  ].join(',') + '{display:none !important;}';
  function addStyle() {
    if (document.getElementById('__ytab_css')) return;
    var s = document.createElement('style');
    s.id = '__ytab_css';
    s.textContent = css;
    (document.head || document.documentElement).appendChild(s);
  }
  addStyle();
  document.addEventListener('DOMContentLoaded', addStyle);

  // 3) If a video ad still starts: click Skip, otherwise fast-forward it to the end.
  var changed = false;
  setInterval(function () {
    try {
      var v = document.querySelector('video');
      var showing = document.querySelector('.ad-showing');
      if (showing && v) {
        var btn = document.querySelector(
          '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .ytp-ad-skip-button-container button');
        if (btn) btn.click();
        if (isFinite(v.duration) && v.duration > 0) {
          v.muted = true;
          v.playbackRate = 16;
          v.currentTime = v.duration;
          changed = true;
        }
      } else if (changed && v) {
        v.muted = false;
        v.playbackRate = 1;
        changed = false;
      }
    } catch (e) {}
  }, 250);
})();
