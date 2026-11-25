/* Progreso y ajustes guardados en el teléfono (localStorage, con respaldo en memoria). */
(function (root) {
  'use strict';
  var KEY = 'letrasmagicas.v1';
  var DEF = {
    mascot: null, total: 0, best: {},
    settings: { auto: true, rate: 0.9, size: 1, syl: true, focus: true, theme: 'crema', sound: true }
  };
  function clone(o) { return JSON.parse(JSON.stringify(o)); }
  function merge(d, s) {
    var o = clone(d);
    for (var k in s) {
      if (k === 'settings' && s.settings && typeof s.settings === 'object') {
        for (var q in s.settings) o.settings[q] = s.settings[q];
      } else o[k] = s[k];
    }
    return o;
  }
  var mem = null;
  try { mem = JSON.parse(root.localStorage.getItem(KEY)); } catch (e) { mem = null; }
  var S = merge(DEF, mem && typeof mem === 'object' ? mem : {});

  var Store = {
    get: function () { return S; },
    save: function () { try { root.localStorage.setItem(KEY, JSON.stringify(S)); } catch (e) { /* sin almacenamiento */ } },
    set: function (k, v) { S[k] = v; Store.save(); },
    setting: function (k, v) { S.settings[k] = v; Store.save(); if (Store.onSetting) Store.onSetting(k, v); },
    addStars: function (key, stars) {
      S.total += stars;
      if (!S.best[key] || S.best[key] < stars) S.best[key] = stars;
      Store.save();
    },
    bestOf: function (key) { return S.best[key] || 0; },
    sumBest: function (prefix) {
      var t = 0;
      for (var k in S.best) if (k === prefix || k.indexOf(prefix + ':') === 0) t += S.best[k];
      return t;
    },
    reset: function () { S = clone(DEF); Store.save(); }
  };
  root.Store = Store;
})(window);
