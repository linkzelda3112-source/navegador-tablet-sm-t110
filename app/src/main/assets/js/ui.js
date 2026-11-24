/* Utilidades de interfaz compartidas. */
(function (root) {
  'use strict';

  function h(tag, attrs, kids) {
    var el = document.createElement(tag);
    if (attrs) {
      for (var k in attrs) {
        var v = attrs[k];
        if (v == null || v === false) continue;
        if (k === 'class') el.className = v;
        else if (k === 'text') el.textContent = v;
        else if (k === 'html') el.innerHTML = v;
        else if (k === 'style') el.style.cssText = v;
        else if (k.slice(0, 2) === 'on') el.addEventListener(k.slice(2), v);
        else el.setAttribute(k, v === true ? '' : v);
      }
    }
    if (kids) (Array.isArray(kids) ? kids : [kids]).forEach(function (c) {
      if (c == null) return;
      el.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
    });
    return el;
  }

  var STAR_PATH = 'M12 2.2l2.9 6.3 6.9.8-5.1 4.7 1.4 6.8L12 17.3 5.9 20.8l1.4-6.8L2.2 9.3l6.9-.8z';
  function star(on, cls) {
    var s = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    s.setAttribute('viewBox', '0 0 24 24');
    s.setAttribute('class', 'star' + (on ? ' on' : '') + (cls ? ' ' + cls : ''));
    s.setAttribute('aria-hidden', 'true');
    var p = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    p.setAttribute('d', STAR_PATH);
    s.appendChild(p);
    return s;
  }

  var HEART_PATH = 'M12 21s-7.5-4.6-9.6-9.4C.9 8 3 4.5 6.4 4.5c2 0 3.7 1.1 5.6 3.3 1.9-2.2 3.6-3.3 5.6-3.3 3.4 0 5.5 3.5 4 7.1C19.500 16.400 12 21 12 21z';
  function heart(lost) {
    var s = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    s.setAttribute('viewBox', '0 0 24 24');
    s.setAttribute('class', 'heart' + (lost ? ' lost' : ''));
    s.setAttribute('aria-hidden', 'true');
    var p = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    p.setAttribute('d', HEART_PATH);
    s.appendChild(p);
    return s;
  }

  /* Texto flotante (+10, ¡Crítico!, etc.) dentro de un contenedor posicionado. */
  function floaty(parent, text, x, y, kind) {
    var el = h('div', { class: 'floaty ' + (kind || 'good'), text: text, style: 'left:' + x + 'px;top:' + y + 'px;' });
    parent.appendChild(el);
    var a = el.animate([
      { transform: 'translate(-50%,0) scale(.6)', opacity: 0 },
      { transform: 'translate(-50%,-14px) scale(1.15)', opacity: 1, offset: 0.2 },
      { transform: 'translate(-50%,-56px) scale(1)', opacity: 0 }
    ], { duration: 850, easing: 'ease-out' });
    a.onfinish = function () { el.remove(); };
  }

  /* Chispas de colores. */
  function burst(parent, x, y, color, n) {
    n = n || 14;
    var colors = color ? [color, '#fff', '#ffd23f'] : ['#3ddc97', '#4cc9f0', '#ffb703', '#b388ff', '#ff6b8b'];
    for (var i = 0; i < n; i++) {
      var el = h('div', { class: 'pt', style: 'left:' + x + 'px;top:' + y + 'px;background:' + colors[i % colors.length] });
      parent.appendChild(el);
      var ang = Math.random() * Math.PI * 2, dist = 36 + Math.random() * 56;
      var a = el.animate([
        { transform: 'translate(-50%,-50%) scale(1)', opacity: 1 },
        { transform: 'translate(' + (Math.cos(ang) * dist - 5) + 'px,' + (Math.sin(ang) * dist - 5) + 'px) scale(.2) rotate(180deg)', opacity: 0 }
      ], { duration: 520 + Math.random() * 200, easing: 'cubic-bezier(.2,.7,.4,1)' });
      a.onfinish = (function (e) { return function () { e.remove(); }; })(el);
    }
  }

  var toastTimer = 0;
  function toast(msg) {
    var t = document.getElementById('toast');
    t.textContent = msg;
    t.classList.add('show');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { t.classList.remove('show'); }, 1800);
  }

  function showOverlay(node) {
    var ov = document.getElementById('overlay');
    ov.innerHTML = '';
    ov.appendChild(node);
    ov.hidden = false;
  }
  function hideOverlay() {
    var ov = document.getElementById('overlay');
    ov.hidden = true;
    ov.innerHTML = '';
  }
  function overlayOpen() { return !document.getElementById('overlay').hidden; }

  root.UI = { h: h, star: star, heart: heart, floaty: floaty, burst: burst, toast: toast, showOverlay: showOverlay, hideOverlay: hideOverlay, overlayOpen: overlayOpen };
})(window);
