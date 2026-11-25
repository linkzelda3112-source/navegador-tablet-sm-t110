/* Utilidades de interfaz. */
(function (root) {
  'use strict';
  var doc = root.document;

  function h(tag, props) {
    var el = doc.createElement(tag);
    props = props || {};
    Object.keys(props).forEach(function (k) {
      var v = props[k];
      if (v === null || v === undefined || v === false) return;
      if (k === 'class') el.className = v;
      else if (k === 'text') el.textContent = v;
      else if (k === 'style' && typeof v === 'object') Object.keys(v).forEach(function (s) { el.style[s] = v[s]; });
      else if (k.indexOf('on') === 0 && typeof v === 'function') el.addEventListener(k.slice(2).toLowerCase(), v);
      else el.setAttribute(k, v === true ? '' : v);
    });
    var add = function (c) {
      if (c === null || c === undefined || c === false) return;
      if (Array.isArray(c)) { c.forEach(add); return; }
      el.appendChild(typeof c === 'object' ? c : doc.createTextNode(String(c)));
    };
    for (var i = 2; i < arguments.length; i++) add(arguments[i]);
    return el;
  }

  var leave = [], overlayEl = null;
  var UI = {
    h: h,
    back: null,

    /** Cambia de pantalla; ejecuta las limpiezas de la anterior. */
    go: function (build) {
      var l = leave; leave = [];
      l.forEach(function (f) { try { f(); } catch (e) { /* nada */ } });
      UI.closeOverlay();
      root.Voice.stop();
      UI.back = null;
      var node = build();
      var screen = doc.getElementById('screen');
      screen.innerHTML = '';
      screen.appendChild(node);
      screen.scrollTop = 0;
    },
    onLeave: function (f) { leave.push(f); },

    /** Botón grande. */
    btn: function (label, cls, onclick, extra) {
      var p = { class: 'btn ' + (cls || ''), type: 'button', onclick: function (e) { root.Sfx.play('tap'); if (onclick) onclick(e); } };
      if (extra) Object.keys(extra).forEach(function (k) { p[k] = extra[k]; });
      return h('button', p, label);
    },

    header: function (title, onBack, right) {
      return h('div', { class: 'hdr' },
        h('button', { class: 'back', type: 'button', 'aria-label': 'Volver', onclick: function () { root.Sfx.play('tap'); onBack(); } }, '◀'),
        h('div', { class: 'ttl' }, title),
        h('div', { class: 'hr' }, right || null));
    },

    stars: function (n, max) {
      max = max || 3;
      var s = '';
      for (var i = 0; i < max; i++) s += i < n ? '★' : '☆';
      return h('span', { class: 'stars', 'aria-label': n + ' de ' + max + ' estrellas' }, s);
    },

    toast: function (msg, ms) {
      var t = doc.getElementById('toast');
      t.textContent = msg; t.className = 'show';
      clearTimeout(UI._tt);
      UI._tt = setTimeout(function () { t.className = ''; }, ms || 2600);
    },

    shake: function (el) {
      if (!el) return;
      el.classList.remove('shake'); void el.offsetWidth; el.classList.add('shake');
      setTimeout(function () { el.classList.remove('shake'); }, 500);
    },

    pop: function (el) {
      if (!el) return;
      el.classList.remove('pop'); void el.offsetWidth; el.classList.add('pop');
    },

    confetti: function () {
      var layer = doc.getElementById('fx');
      var em = ['⭐', '✨', '🎉', '🌟', '💛'];
      for (var i = 0; i < 9; i++) {
        var s = h('span', { class: 'conf', text: em[i % em.length] });
        s.style.left = (5 + Math.random() * 90) + '%';
        s.style.animationDelay = (Math.random() * 0.3) + 's';
        s.style.fontSize = (16 + Math.random() * 10) + 'px';
        layer.appendChild(s);
        (function (n) { setTimeout(function () { if (n.parentNode) n.parentNode.removeChild(n); }, 1500); })(s);
      }
    },

    overlay: function (node) {
      UI.closeOverlay();
      overlayEl = doc.getElementById('overlay');
      overlayEl.innerHTML = '';
      overlayEl.appendChild(node);
      overlayEl.hidden = false;
    },
    closeOverlay: function () {
      var o = doc.getElementById('overlay');
      if (o && !o.hidden) { o.hidden = true; o.innerHTML = ''; return true; }
      return false;
    },
    overlayOpen: function () { var o = doc.getElementById('overlay'); return !!(o && !o.hidden); },

    /** Sílabas con colores alternos. */
    sylSpans: function (word) {
      var parts = root.Logic.syllabify(word);
      return parts.map(function (p, i) { return h('span', { class: 's' + (i % 2) }, p); });
    },

    applySettings: function () {
      var s = root.Store.get().settings, b = doc.body;
      b.className = b.className.replace(/\bt-\w+\b/g, '').trim() + ' t-' + s.theme;
      doc.documentElement.style.setProperty('--fs', String(s.size));
    },

    pick: function (arr) { return arr[Math.floor(Math.random() * arr.length)]; }
  };

  root.UI = UI;
  root.h = h;
})(window);
