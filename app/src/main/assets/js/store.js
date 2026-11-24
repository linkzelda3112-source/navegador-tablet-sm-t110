/* Progreso guardado en localStorage (con respaldo en memoria si falla). */
(function (root) {
  'use strict';
  var KEY = 'matehub.v1';
  var data = { xp: 0, diff: 'facil', sound: true, best: {}, ok: 0, total: 0 };

  function load() {
    try {
      var raw = root.localStorage.getItem(KEY);
      if (raw) {
        var d = JSON.parse(raw);
        if (d && typeof d === 'object') {
          for (var k in data) if (d[k] !== undefined) data[k] = d[k];
        }
      }
    } catch (e) {}
  }
  function save() { try { root.localStorage.setItem(KEY, JSON.stringify(data)); } catch (e) {} }

  var Store = {
    load: load,
    get: function () { return data; },
    setDiff: function (d) { data.diff = d; save(); },
    setSound: function (v) { data.sound = !!v; save(); },
    addXp: function (n) { data.xp += n; save(); },
    record: function (correct) { data.total++; if (correct) data.ok++; },
    flush: save,
    best: function (game, diff) {
      var b = data.best[game + ':' + diff];
      return b || { stars: 0, score: 0 };
    },
    /* Devuelve { newScore, newStars } */
    setBest: function (game, diff, stars, score) {
      var key = game + ':' + diff, b = data.best[key] || { stars: 0, score: 0 };
      var res = { newScore: score > b.score, newStars: stars > b.stars };
      data.best[key] = { stars: Math.max(b.stars, stars), score: Math.max(b.score, score) };
      save();
      return res;
    },
    totalStars: function () {
      var t = 0;
      for (var k in data.best) t += data.best[k].stars || 0;
      return t;
    },
    reset: function () { data = { xp: 0, diff: 'facil', sound: true, best: {}, ok: 0, total: 0 }; save(); }
  };
  root.Store = Store;
})(window);
