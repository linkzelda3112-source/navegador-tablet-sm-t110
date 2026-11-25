/* Menú principal, pegatinas, ajustes y arranque. */
(function (root) {
  'use strict';
  var h = root.UI.h, UI = root.UI, S = root.Store, D = root.Data, L = root.Logic, G = root.Games;
  var Voice = root.Voice, Sfx = root.Sfx;
  var App = root.App = {};
  var greeted = false;
  var PER = 6;

  var GAMES = [
    { key: 'cuentos', title: 'Cuentos', emoji: '📖', desc: 'Lee a tu ritmo', max: 18, go: function () { UI.go(G.cuentosMenu); } },
    { key: 'silabas', title: 'Arma la palabra', emoji: '🧩', desc: 'Junta sílabas', max: 3, go: function () { UI.go(G.silabas); } },
    { key: 'deletrea', title: 'Deletrea', emoji: '🔤', desc: 'Toca las letras', max: 3, go: function () { UI.go(G.deletrea); } },
    { key: 'trazo', title: 'Trazo mágico', emoji: '✏️', desc: 'Pinta las letras', max: 12, go: function () { UI.go(G.trazoMenu); } },
    { key: 'detective', title: 'Detective de letras', emoji: '🔍', desc: 'b, d, p, q y más', max: 3, go: function () { UI.go(G.detective); } },
    { key: 'ordena', title: 'Ordena la oración', emoji: '🧱', desc: 'Arma frases', max: 3, go: function () { UI.go(G.ordena); } }
  ];

  App.menu = function () { UI.go(buildMenu); };

  function buildMenu() {
    var st = S.get(), total = st.total;
    var have = Math.min(D.STICKERS.length, L.stickersUnlocked(total, PER));
    var into = total % PER;
    var hello = h('div', { class: 'hello' },
      h('div', { class: 'mascot' }, st.mascot || '🌟'),
      h('div', { class: 'ht' },
        h('div', { class: 'hi' }, '¡Hola! ¿Qué jugamos hoy?'),
        h('div', { class: 'lvl' }, '⭐ ' + total + ' · Nivel ' + L.levelFor(total)),
        h('div', { class: 'tbar thin', 'aria-label': 'Falta poco para otra pegatina' }, h('i', { style: { width: Math.round(into / PER * 100) + '%' } })),
        h('div', { class: 'small' }, have >= D.STICKERS.length ? '¡Tienes todas las pegatinas!' : 'Te faltan ' + (PER - into) + ' ⭐ para otra pegatina')));
    var cards = h('div', { class: 'cards menu' }, GAMES.map(function (g) {
      var got = S.sumBest(g.key);
      return h('button', { class: 'card', type: 'button', onclick: function () { Sfx.play('tap'); g.go(); } },
        h('span', { class: 'emoji' }, g.emoji), h('span', { class: 'ct' }, g.title), h('span', { class: 'cd' }, g.desc),
        h('span', { class: 'cs' }, '⭐ ' + got + '/' + g.max));
    }));
    var bar = h('div', { class: 'menubar' },
      UI.btn('🏆 Pegatinas', 'soft wide', function () { UI.go(buildStickers); }),
      UI.btn('⚙️ Ajustes', 'soft wide', function () { UI.go(buildSettings); }));
    var seen = st.seen || 0;
    if (have > seen) {
      S.set('seen', have);
      setTimeout(function () { Sfx.play('win'); UI.toast('¡Ganaste una pegatina nueva! ' + D.STICKERS[have - 1], 3500); }, 400);
    }
    if (!greeted) { greeted = true; Voice.auto('¡Hola! ¿Qué jugamos hoy?'); }
    return h('div', { class: 'screen-in' }, hello, cards, bar);
  }

  function buildStickers() {
    UI.back = App.menu;
    var total = S.get().total, have = Math.min(D.STICKERS.length, L.stickersUnlocked(total, PER));
    return h('div', { class: 'screen-in' },
      UI.header('Mis pegatinas', App.menu),
      h('p', { class: 'lead' }, 'Tienes ' + have + ' de ' + D.STICKERS.length + '. Ganas una pegatina cada ' + PER + ' estrellas.'),
      h('div', { class: 'stickers' }, D.STICKERS.map(function (e, k) {
        return h('div', { class: 'stk' + (k < have ? '' : ' lock'), 'aria-label': k < have ? 'Pegatina ' + e : 'Pegatina bloqueada' }, k < have ? e : '❔');
      })));
  }

  function seg(label, options, current, onPick, note) {
    var row = h('div', { class: 'segs' });
    options.forEach(function (o) {
      var b = h('button', {
        class: 'seg' + (o.v === current ? ' on' : ''), type: 'button',
        onclick: function () {
          Sfx.play('tap'); onPick(o.v);
          [].forEach.call(row.children, function (x) { x.classList.remove('on'); }); b.classList.add('on');
        }
      }, o.dot ? h('i', { class: 'dot', style: { background: o.dot } }) : null, o.t);
      row.appendChild(b);
    });
    return h('div', { class: 'set' }, h('div', { class: 'sl' }, label), row, note ? h('div', { class: 'small' }, note) : null);
  }

  function buildSettings() {
    UI.back = App.menu;
    var s = S.get().settings;
    var yn = [{ v: true, t: 'Sí' }, { v: false, t: 'No' }];
    var warn = Voice.available() ? null : h('p', { class: 'warn' }, 'Este teléfono no tiene voz en español. Se instala en Ajustes ▸ Idioma ▸ Salida de texto a voz.');
    return h('div', { class: 'screen-in' },
      UI.header('Ajustes', App.menu),
      warn,
      seg('Leer en voz alta solo', yn, s.auto, function (v) { S.setting('auto', v); }, 'Lee las instrucciones al empezar. Siempre puedes tocar 🔊.'),
      seg('Velocidad de la voz', [{ v: 0.7, t: '🐢 Lenta' }, { v: 0.9, t: '🚶 Normal' }, { v: 1.1, t: '🐇 Rápida' }], s.rate, function (v) { S.setting('rate', v); Voice.say('Así suena mi voz.'); }),
      seg('Tamaño de letra', [{ v: 1, t: 'Mediana' }, { v: 1.2, t: 'Grande' }, { v: 1.4, t: 'Muy grande' }], s.size, function (v) { S.setting('size', v); }),
      seg('Sílabas de colores', yn, s.syl, function (v) { S.setting('syl', v); }, 'Ayuda a leer palabras largas por partes.'),
      seg('Enfocar la frase del cuento', yn, s.focus, function (v) { S.setting('focus', v); }, 'Apaga un poco las demás frases para no perder el renglón.'),
      seg('Color del fondo', [
        { v: 'crema', t: 'Crema', dot: '#FFF8E7' }, { v: 'menta', t: 'Menta', dot: '#E8F8EF' },
        { v: 'cielo', t: 'Cielo', dot: '#E8F2FF' }, { v: 'rosa', t: 'Rosa', dot: '#FFEAF2' }], s.theme, function (v) { S.setting('theme', v); }),
      seg('Sonidos', yn, s.sound, function (v) { S.setting('sound', v); }),
      h('div', { class: 'row' }, UI.btn('🔊 Probar la voz', 'go', function () { Voice.say('Hola. Esta es la voz de Letras Mágicas. Puedes cambiar su velocidad.'); })));
  }

  function chooseMascot() {
    UI.go(function () {
      return h('div', { class: 'screen-in center' },
        h('h1', { class: 'title' }, 'Letras Mágicas'),
        h('p', { class: 'lead' }, 'Elige a tu amigo para jugar:'),
        h('div', { class: 'mascots' }, D.MASCOTS.map(function (m) {
          return h('button', { class: 'mas', type: 'button', onclick: function () {
            Sfx.play('ok'); S.set('mascot', m); greeted = true; App.menu(); Voice.auto('¡Hola! Vamos a jugar.');
          } }, m);
        })));
    });
  }

  S.onSetting = function (k) { if (k === 'size' || k === 'theme') UI.applySettings(); };

  root.__back = function () {
    if (UI.overlayOpen()) { UI.closeOverlay(); return true; }
    if (UI.back) { UI.back(); return true; }
    return false;
  };
  root.__pause = function () { Voice.stop(); };

  UI.applySettings();
  if (!S.get().mascot) chooseMascot(); else App.menu();
})(window);
