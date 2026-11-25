/* Los 6 minijuegos. Ninguno tiene cronómetro ni castigos: los errores solo dan una pista. */
(function (root) {
  'use strict';
  var h = root.UI.h, UI = root.UI, L = root.Logic, D = root.Data, S = root.Store;
  var Voice = root.Voice, Sfx = root.Sfx, doc = root.document;
  var G = {};

  function App() { return root.App; }
  function pick(a) { return a[Math.floor(Math.random() * a.length)]; }

  // =====================================================================
  //  Motor de rondas
  // =====================================================================
  G.rounds = function (cfg) {
    var i = 0, mistakes = 0, wrongRound = 0, locked = false, finished = false;
    var stage = h('div', { class: 'stage' });
    var fb = h('div', { class: 'fb', role: 'status', 'aria-live': 'polite' });
    var dock = h('div', { class: 'dock' });
    var dots = h('div', { class: 'dots' });
    var node = h('div', { class: 'screen-in game' }, UI.header(cfg.title, cfg.exit, dots), stage, fb, dock);
    UI.back = cfg.exit;

    function drawDots() {
      dots.innerHTML = '';
      for (var k = 0; k < cfg.n; k++) dots.appendChild(h('i', { class: k < i ? 'd done' : (k === i ? 'd cur' : 'd') }));
    }

    var c = {
      stage: stage, n: cfg.n,
      get i() { return i; },
      prompt: function (text, speak) {
        var el = h('div', { class: 'prompt' },
          h('button', { class: 'spk', type: 'button', 'aria-label': 'Escuchar', onclick: function () { Sfx.play('tap'); Voice.say(text); } }, '🔊'),
          h('span', { class: 'ptxt' }, text));
        stage.appendChild(el);
        if (speak) Voice.auto(text);
        return el;
      },
      say: function (text, opt) { Voice.say(text, opt); },
      wrong: function (msg) {
        mistakes++; wrongRound++;
        Sfx.play('no');
        fb.className = 'fb soft'; fb.textContent = msg || pick(D.RETRY);
        return wrongRound;
      },
      addMistake: function (k) { mistakes += k; },
      right: function (msg) {
        if (locked) return;
        locked = true;
        Sfx.play('ok'); UI.confetti();
        fb.className = 'fb ok'; fb.textContent = msg || pick(D.PRAISE);
        dock.innerHTML = '';
        dock.appendChild(UI.btn(i === cfg.n - 1 ? 'Terminar ▶' : 'Siguiente ▶', 'go big', next));
      },
      skip: function () { locked = true; next(); }
    };

    function round() {
      stage.innerHTML = ''; dock.innerHTML = '';
      fb.textContent = ''; fb.className = 'fb';
      wrongRound = 0; locked = false;
      drawDots();
      cfg.build(i, c);
    }
    function next() {
      Voice.stop();
      i++;
      if (i >= cfg.n) finish(); else round();
    }
    function finish() {
      if (finished) return; finished = true;
      var stars = L.starsFor(mistakes, cfg.n);
      S.addStars(cfg.key, stars);
      Sfx.play('win'); UI.confetti();
      drawDots();
      stage.innerHTML = ''; dock.innerHTML = ''; fb.textContent = '';
      var msg = stars === 3 ? '¡Sin errores! ¡Eres increíble!' : stars === 2 ? '¡Muy bien! Cada vez te sale mejor.' : '¡Lo terminaste! Practicar te hace más fuerte.';
      var card = h('div', { class: 'result' },
        h('div', { class: 'big-emoji' }, S.get().mascot || '🌟'),
        h('h2', null, '¡Terminaste!'),
        h('div', { class: 'bigstars' }, UI.stars(stars)),
        h('p', null, msg),
        h('p', { class: 'small' }, 'Ganaste ' + stars + (stars === 1 ? ' estrella.' : ' estrellas.')),
        h('div', { class: 'col' },
          UI.btn('Jugar otra vez', 'go big', cfg.again),
          cfg.extra || null,
          UI.btn('Volver al menú', 'soft', function () { App().menu(); })));
      stage.appendChild(card);
      Voice.auto(msg);
    }
    setTimeout(round, 0);
    return node;
  };

  // =====================================================================
  //  1. Arma la palabra (sílabas)
  // =====================================================================
  G.silabas = function () {
    var used = {};
    return G.rounds({
      key: 'silabas', title: 'Arma la palabra', n: 6,
      exit: function () { App().menu(); },
      again: function () { UI.go(G.silabas); },
      build: function (i, c) {
        var t = i < 2 ? 2 : (i < 4 ? 3 : 4);
        var pool = D.WORDS.filter(function (w) { return !used[w.word] && (t === 4 ? w.syl.length >= 4 : w.syl.length === t); });
        if (!pool.length) pool = D.WORDS.filter(function (w) { return !used[w.word]; });
        var w = pick(pool); used[w.word] = 1;
        c.prompt('Toca las sílabas en orden para armar la palabra.', i === 0);

        var pic = h('button', { class: 'pic', type: 'button', onclick: function () { Sfx.play('tap'); Voice.say(w.word); } },
          h('span', { class: 'emoji' }, w.emoji), h('span', { class: 'hear' }, '🔊 Escuchar'));
        var slotsEl = h('div', { class: 'slots' }), tilesEl = h('div', { class: 'tiles' }), reveal = h('div', { class: 'reveal' });
        c.stage.appendChild(pic); c.stage.appendChild(slotsEl); c.stage.appendChild(tilesEl); c.stage.appendChild(reveal);

        var tiles = L.shuffleDifferent(w.syl.map(function (s, k) { return { id: k, s: s }; }), Math.random);
        var placed = [], done = false, flash = null;

        function render() {
          slotsEl.innerHTML = ''; tilesEl.innerHTML = '';
          for (var k = 0; k < w.syl.length; k++) {
            (function (k) {
              var t = placed[k];
              var cls = 'slot' + (t ? ' filled' : '') + (done ? ' ok' : '') + (flash && t ? (flash[k] ? ' good' : ' bad') : '');
              slotsEl.appendChild(h('button', {
                class: cls, type: 'button', 'aria-label': t ? 'Quitar ' + t.s : 'Espacio vacío',
                onclick: function () { if (done || flash || !t) return; Sfx.play('tap'); placed.splice(k, 1); render(); }
              }, t ? t.s : ''));
            })(k);
          }
          tiles.forEach(function (t) {
            if (placed.indexOf(t) >= 0) return;
            tilesEl.appendChild(h('button', {
              class: 'tile', type: 'button',
              onclick: function () {
                if (done || flash || placed.length >= w.syl.length) return;
                Sfx.play('tap'); placed.push(t); Voice.say(t.s, { rate: 0.8 });
                render();
                if (placed.length === w.syl.length) check();
              }
            }, t.s));
          });
        }
        function check() {
          var good = placed.map(function (t, k) { return t.s === w.syl[k]; });
          if (good.every(Boolean)) {
            done = true; render();
            reveal.innerHTML = ''; reveal.appendChild(h('div', { class: 'word-big' }, UI.sylSpans(w.word)));
            Voice.syllables(w.syl);
            c.right();
          } else {
            flash = good; render(); UI.shake(slotsEl); c.wrong();
            var keep = 0; while (keep < good.length && good[keep]) keep++;
            setTimeout(function () { flash = null; placed = placed.slice(0, keep); render(); }, 900);
          }
        }
        render();
      }
    });
  };

  // =====================================================================
  //  2. Deletrea (letras, sin escribir a mano)
  // =====================================================================
  G.deletrea = function () {
    var used = {};
    var ALPHA = 'abcdefghilmnoprstuvz'.split('');
    return G.rounds({
      key: 'deletrea', title: 'Deletrea', n: 6,
      exit: function () { App().menu(); },
      again: function () { UI.go(G.deletrea); },
      build: function (i, c) {
        var min = i < 2 ? 3 : (i < 4 ? 5 : 6), max = i < 2 ? 4 : (i < 4 ? 6 : 8);
        var pool = D.WORDS.filter(function (w) { return !used[w.word] && !/[^a-zñ]/.test(w.word) && w.word.length >= min && w.word.length <= max; });
        if (!pool.length) pool = D.WORDS.filter(function (w) { return !used[w.word] && !/[^a-zñ]/.test(w.word) && w.word.length <= 8; });
        var w = pick(pool); used[w.word] = 1;
        var letters = w.word.split('');
        c.prompt('Toca las letras en orden para escribir la palabra.', i === 0);

        var pic = h('button', { class: 'pic', type: 'button', onclick: function () { Sfx.play('tap'); Voice.say(w.word); } },
          h('span', { class: 'emoji' }, w.emoji), h('span', { class: 'hear' }, '🔊 Escuchar'));
        var slotsEl = h('div', { class: 'lslots' }), tilesEl = h('div', { class: 'ltiles' });
        c.stage.appendChild(pic); c.stage.appendChild(slotsEl); c.stage.appendChild(tilesEl);

        // fichas: las letras de la palabra + 3 distractoras (parecidas primero)
        var extra = [];
        letters.forEach(function (ch) {
          var p = L.CONFUSABLE[ch];
          if (p && letters.indexOf(p) < 0 && extra.indexOf(p) < 0 && extra.length < 2) extra.push(p);
        });
        L.shuffle(ALPHA.filter(function (x) { return letters.indexOf(x) < 0 && extra.indexOf(x) < 0; })).forEach(function (x) {
          if (extra.length < 3) extra.push(x);
        });
        var tiles = L.shuffleDifferent(letters.concat(extra).map(function (ch, k) { return { id: k, ch: ch, used: false }; }), Math.random);
        var pos = 0, wrongHere = 0, hintCh = null;

        function render() {
          slotsEl.innerHTML = ''; tilesEl.innerHTML = '';
          letters.forEach(function (ch, k) {
            slotsEl.appendChild(h('span', { class: 'lslot' + (k < pos ? ' filled' : '') + (k === pos ? ' cur' : '') }, k < pos ? ch : ''));
          });
          tiles.forEach(function (t) {
            tilesEl.appendChild(h('button', {
              class: 'ltile' + (t.used ? ' gone' : '') + (hintCh === t.ch && !t.used ? ' hint' : ''),
              type: 'button', disabled: t.used,
              onclick: function (ev) {
                if (pos >= letters.length) return;
                if (t.ch === letters[pos]) {
                  Sfx.play('tap'); t.used = true; pos++; wrongHere = 0; hintCh = null;
                  render();
                  if (pos === letters.length) {
                    reveal();
                  }
                } else {
                  UI.shake(ev.currentTarget); c.wrong('Esa no va aquí. Prueba otra letra.');
                  wrongHere++;
                  if (wrongHere >= 2) { hintCh = letters[pos]; render(); }
                }
              }
            }, t.ch));
          });
        }
        function reveal() {
          c.stage.appendChild(h('div', { class: 'reveal' }, h('div', { class: 'word-big' }, UI.sylSpans(w.word))));
          Voice.say(w.word);
          c.right();
        }
        render();
      }
    });
  };

  // =====================================================================
  //  3. Trazo mágico (practicar la letra pintando encima, con mucho margen)
  // =====================================================================
  G.trazoMenu = function () {
    UI.back = function () { App().menu(); };
    var keys = Object.keys(D.TRACE);
    return h('div', { class: 'screen-in' },
      UI.header('Trazo mágico', function () { App().menu(); }),
      h('p', { class: 'lead' }, 'Pinta encima de la letra, siguiendo su forma. Tiene mucho margen: no tiene que quedar perfecto.'),
      h('div', { class: 'cards' }, keys.map(function (k) {
        var p = D.TRACE[k];
        return h('button', { class: 'card', type: 'button', onclick: function () { Sfx.play('tap'); UI.go(function () { return G.trazo(k); }); } },
          h('span', { class: 'emoji txt' }, p.emoji), h('span', { class: 'ct' }, p.title), UI.stars(S.bestOf('trazo:' + k)));
      })));
  };

  function whenFont(cb) {
    var done = false;
    function go() { if (!done) { done = true; cb(); } }
    try {
      if (doc.fonts && doc.fonts.load) {
        doc.fonts.load('700 60px "Comic Relief"').then(go, go);
        setTimeout(go, 1200);
      } else go();
    } catch (e) { go(); }
  }

  function traceRound(c, text) {
    var stage = c.stage;
    var W = Math.min(stage.clientWidth || 320, 400);
    var H = text.length <= 1 ? 300 : (text.length <= 3 ? 230 : 190);
    var dpr = Math.min(root.devicePixelRatio || 1, 2);
    var BRUSH = 22, TOL = 40, SC = 0.5;
    var locked = false, drawing = false, last = null, lastEval = 0;

    var wrap = h('div', { class: 'trace', style: { width: W + 'px', height: H + 'px' } });
    var guide = h('canvas', { class: 'cv' }), ink = h('canvas', { class: 'cv' });
    [guide, ink].forEach(function (cv) {
      cv.width = Math.round(W * dpr); cv.height = Math.round(H * dpr);
      cv.style.width = W + 'px'; cv.style.height = H + 'px';
    });
    wrap.appendChild(guide); wrap.appendChild(ink);
    var barFill = h('i');
    var bar = h('div', { class: 'tbar', 'aria-label': 'Cuánto has pintado' }, barFill);
    var info = h('div', { class: 'tinfo' }, 'Pinta encima de la letra con tu dedo.');
    var clearBtn = UI.btn('🧽 Borrar y repetir', 'soft', function () { clearInk(); });
    var skipBtn = UI.btn('Saltar ⏭', 'soft', function () { c.addMistake(2); c.skip(); });
    c.prompt(text.length === 1 ? 'Pinta encima de la letra.' : 'Pinta encima, de izquierda a derecha.', false);
    stage.appendChild(wrap); stage.appendChild(bar); stage.appendChild(info);
    stage.appendChild(h('div', { class: 'row' }, clearBtn, skipBtn));

    var FS = text.length <= 1 ? 0.78 : 0.72;
    var fs = Math.floor(H * FS), base = H * 0.7;
    var core, zone, w2 = Math.ceil(W * SC), h2 = Math.ceil(H * SC);
    var small = doc.createElement('canvas'); small.width = w2; small.height = h2;
    var sctx = small.getContext('2d');

    function fontStr(size) { return '700 ' + size + 'px "Comic Relief", "Trebuchet MS", Verdana, sans-serif'; }

    function fit() {
      var g = guide.getContext('2d');
      fs = Math.floor(H * FS);
      g.font = fontStr(fs);
      while (g.measureText(text).width > W * 0.84 && fs > 24) { fs -= 4; g.font = fontStr(fs); }
    }
    function glyph(g, lw, fill, stroke, dash) {
      g.font = fontStr(fs); g.textAlign = 'center'; g.textBaseline = 'alphabetic'; g.lineJoin = 'round';
      if (fill) { g.fillStyle = fill; g.fillText(text, W / 2, base); }
      if (stroke) {
        g.strokeStyle = stroke; g.lineWidth = lw; g.setLineDash(dash || []);
        g.strokeText(text, W / 2, base); g.setLineDash([]);
      }
    }
    function mask(extra) {
      sctx.setTransform(1, 0, 0, 1, 0, 0); sctx.clearRect(0, 0, w2, h2);
      sctx.setTransform(SC, 0, 0, SC, 0, 0);
      glyph(sctx, 6 + extra, '#000', '#000');
      var d = sctx.getImageData(0, 0, w2, h2).data, a = new Uint8Array(w2 * h2);
      for (var k = 0; k < a.length; k++) a[k] = d[k * 4 + 3] > 128 ? 1 : 0;
      return a;
    }
    function drawGuide() {
      var g = guide.getContext('2d');
      g.setTransform(dpr, 0, 0, dpr, 0, 0); g.clearRect(0, 0, W, H);
      var lines = [[base - 0.75 * fs, true], [base - 0.5 * fs, true], [base, false], [base + 0.25 * fs, true]];
      lines.forEach(function (l) {
        g.beginPath(); g.setLineDash(l[1] ? [8, 8] : []);
        g.strokeStyle = l[1] ? '#b9c2e8' : '#7c88c8'; g.lineWidth = l[1] ? 1.5 : 2.5;
        g.moveTo(10, l[0]); g.lineTo(W - 10, l[0]); g.stroke();
      });
      g.setLineDash([]);
      glyph(g, 6, '#dfe6ff', '#dfe6ff');
      glyph(g, 2.5, null, '#4f5fd0', [7, 6]);
    }
    function inkArray() {
      sctx.setTransform(1, 0, 0, 1, 0, 0); sctx.clearRect(0, 0, w2, h2);
      sctx.drawImage(ink, 0, 0, w2, h2);
      var d = sctx.getImageData(0, 0, w2, h2).data, a = new Uint8Array(w2 * h2), n = 0;
      for (var k = 0; k < a.length; k++) { if (d[k * 4 + 3] > 40) { a[k] = 1; n++; } }
      return { a: a, n: n };
    }
    function evaluate(final) {
      if (locked || !core) return;
      var r = inkArray();
      if (!r.n) { barFill.style.width = '0%'; return; }
      var s = L.scoreTrace(core, zone, r.a);
      barFill.style.width = Math.min(100, Math.round(s.coverage / 0.85 * 100)) + '%';
      if (L.traceDone(s)) {
        locked = true;
        var stars = L.traceStars(s);
        c.addMistake(stars === 1 ? 1 : 0);
        info.textContent = stars === 3 ? '¡Trazo perfecto!' : '¡Buen trazo!';
        if (text.length > 1) Voice.say(text);
        c.right('¡Lo lograste! ' + '★'.repeat(stars));
      } else if (final) {
        if (s.precision < 0.6) info.textContent = 'Intenta pintar solo sobre la letra. Puedes borrar y repetir.';
        else info.textContent = 'Sigue pintando lo que falta de la letra.';
      }
    }
    var ictx = ink.getContext('2d');
    function brush() {
      ictx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ictx.lineCap = 'round'; ictx.lineJoin = 'round'; ictx.lineWidth = BRUSH; ictx.strokeStyle = '#ff4081'; ictx.fillStyle = '#ff4081';
    }
    function clearInk() {
      if (locked) return;
      ictx.setTransform(1, 0, 0, 1, 0, 0); ictx.clearRect(0, 0, ink.width, ink.height);
      barFill.style.width = '0%'; info.textContent = 'Pinta encima de la letra con tu dedo.';
    }
    function pos(e) {
      var r = ink.getBoundingClientRect();
      return { x: (e.clientX - r.left) * (W / r.width), y: (e.clientY - r.top) * (H / r.height) };
    }
    ink.addEventListener('pointerdown', function (e) {
      if (locked) return;
      e.preventDefault(); drawing = true;
      try { ink.setPointerCapture(e.pointerId); } catch (x) { /* nada */ }
      brush(); last = pos(e);
      ictx.beginPath(); ictx.arc(last.x, last.y, BRUSH / 2, 0, 6.2832); ictx.fill();
    });
    ink.addEventListener('pointermove', function (e) {
      if (!drawing || locked) return;
      e.preventDefault();
      var p = pos(e);
      ictx.beginPath(); ictx.moveTo(last.x, last.y); ictx.lineTo(p.x, p.y); ictx.stroke();
      last = p;
      var now = Date.now();
      if (now - lastEval > 300) { lastEval = now; evaluate(false); }
    });
    function up(e) {
      if (!drawing) return;
      drawing = false; e.preventDefault(); evaluate(true);
    }
    ink.addEventListener('pointerup', up);
    ink.addEventListener('pointercancel', up);

    whenFont(function () {
      fit(); drawGuide();
      core = mask(0); zone = mask(TOL);
      root.__trace = { core: core, zone: zone, w: w2, h: h2, W: W, H: H, sc: SC };
    });
  }

  G.trazo = function (packKey) {
    var pack = D.TRACE[packKey], items = L.shuffle(pack.items, Math.random).slice(0, 5);
    return G.rounds({
      key: 'trazo:' + packKey, title: pack.title, n: 5,
      exit: function () { UI.go(G.trazoMenu); },
      again: function () { UI.go(function () { return G.trazo(packKey); }); },
      build: function (i, c) { traceRound(c, items[i]); }
    });
  };

  // =====================================================================
  //  4. Detective de letras (b/d, p/q, ortografía y "oye y encuentra")
  // =====================================================================
  G.detective = function () {
    var usedW = {}, usedM = {}, usedO = {};
    var TYPES = ['find', 'mirror', 'find', 'orto', 'mirror', 'orto'];
    function fresh(list, used) {
      var pool = list.filter(function (r) { return !used[r[1]]; });
      if (!pool.length) { for (var k in used) delete used[k]; pool = list.slice(); }
      var r = pick(pool); used[r[1]] = 1; return r;
    }
    return G.rounds({
      key: 'detective', title: 'Detective', n: 6,
      exit: function () { App().menu(); },
      again: function () { UI.go(G.detective); },
      build: function (i, c) {
        var type = TYPES[i];
        if (type === 'find') findRound(c, i);
        else gapRound(c, i, type === 'mirror' ? fresh(D.MIRROR, usedM) : fresh(D.ORTO, usedO), type);
      }
    });

    function findRound(c, i) {
      var cands = D.WORDS.filter(function (w) {
        return !usedW[w.word] && !w.accent && w.word.length >= 3 && w.word.length <= 8 && !/ü/.test(w.word) && L.lookalikes(w.word, 2, Math.random).length >= 2;
      });
      var w = pick(cands); usedW[w.word] = 1;
      var opts = L.shuffle([w.word].concat(L.lookalikes(w.word, 2, Math.random)), Math.random);
      var prompt = 'Escucha y toca la palabra correcta.';
      c.prompt(prompt, false);
      var listen = h('button', { class: 'pic', type: 'button', onclick: function () { Sfx.play('tap'); Voice.say(w.word); } },
        h('span', { class: 'emoji' }, '🔊'), h('span', { class: 'hear' }, 'Escuchar la palabra'));
      var list = h('div', { class: 'opts' }), emo = h('div', { class: 'reveal' });
      c.stage.appendChild(listen); c.stage.appendChild(list); c.stage.appendChild(emo);
      var done = false;
      opts.forEach(function (o) {
        var b = h('button', {
          class: 'opt word', type: 'button',
          onclick: function () {
            if (done || b.disabled) return;
            if (o === w.word) {
              done = true; b.classList.add('ok');
              emo.appendChild(h('div', { class: 'emoji big' }, w.emoji));
              Voice.say(w.word); c.right();
            } else {
              b.disabled = true; b.classList.add('off'); UI.shake(b);
              var n = c.wrong();
              if (n >= 2) [].forEach.call(list.children, function (x) { if (x.textContent === w.word) x.classList.add('hint'); });
            }
          }
        }, o);
        list.appendChild(b);
      });
      if (i === 0) Voice.auto(prompt + ' ' + w.word);
      else Voice.auto(w.word);
    }

    function gapRound(c, i, r, type) {
      var emoji = r[0], tpl = r[1], right = r[2], wrongs = r[3];
      var full = tpl.replace('_', right), at = tpl.indexOf('_');
      var prompt = type === 'mirror' ? 'Mira el dibujo. ¿Qué letra falta?' : 'Mira el dibujo. ¿Cómo se escribe?';
      c.prompt(prompt, i < 2);
      var pic = h('button', { class: 'pic', type: 'button', onclick: function () { Sfx.play('tap'); Voice.say(full); } },
        h('span', { class: 'emoji' }, emoji), h('span', { class: 'hear' }, '🔊 Escuchar'));
      var blank = h('span', { class: 'blank' }, '?');
      var wordEl = h('div', { class: 'gapword' }, tpl.slice(0, at), blank, tpl.slice(at + 1));
      var list = h('div', { class: 'opts row2' });
      c.stage.appendChild(pic); c.stage.appendChild(wordEl); c.stage.appendChild(list);
      var done = false;
      L.shuffle([right].concat(wrongs), Math.random).forEach(function (o) {
        var b = h('button', {
          class: 'opt letter', type: 'button',
          onclick: function () {
            if (done || b.disabled) return;
            if (o === right) {
              done = true; b.classList.add('ok'); blank.textContent = right; blank.classList.add('ok');
              Voice.say(full); c.right();
            } else {
              b.disabled = true; b.classList.add('off'); UI.shake(b);
              var n = c.wrong();
              if (n >= 2) [].forEach.call(list.children, function (x) { if (x.textContent === right) x.classList.add('hint'); });
            }
          }
        }, o);
        list.appendChild(b);
      });
    }
  };

  // =====================================================================
  //  5. Ordena la oración
  // =====================================================================
  G.ordena = function () {
    var used = {};
    return G.rounds({
      key: 'ordena', title: 'Ordena la oración', n: 5,
      exit: function () { App().menu(); },
      again: function () { UI.go(G.ordena); },
      build: function (i, c) {
        var pool = D.SENTENCES.filter(function (s) { return !used[s]; });
        var s = pick(pool); used[s] = 1;
        var words = L.words(s);
        c.prompt('Toca las palabras en orden para armar la oración.', i === 0);
        var answer = h('div', { class: 'answer' }), poolEl = h('div', { class: 'pool' });
        var listen = UI.btn('🔊 Leer lo que armé', 'soft', function () {
          if (!placed.length) { UI.toast('Primero toca algunas palabras.'); return; }
          Voice.say(placed.map(function (t) { return t.w; }).join(' '));
        });
        var reset = UI.btn('↺ Empezar de nuevo', 'soft', function () { if (!done && !flash) { placed = []; render(); } });
        c.stage.appendChild(answer); c.stage.appendChild(poolEl);
        c.stage.appendChild(h('div', { class: 'row' }, listen, reset));
        var tiles = L.shuffleDifferent(words.map(function (w, k) { return { id: k, w: w }; }), Math.random);
        var placed = [], done = false, flash = null;

        function render() {
          answer.innerHTML = ''; poolEl.innerHTML = '';
          answer.className = 'answer' + (placed.length ? '' : ' empty') + (done ? ' ok' : '');
          if (!placed.length) answer.appendChild(h('span', { class: 'ph' }, 'Aquí se arma la oración'));
          placed.forEach(function (t, k) {
            answer.appendChild(h('button', {
              class: 'chip placed' + (flash ? (flash[k] ? ' good' : ' bad') : ''), type: 'button',
              onclick: function () { if (done || flash) return; Sfx.play('tap'); placed.splice(k, 1); render(); }
            }, t.w));
          });
          tiles.forEach(function (t) {
            if (placed.indexOf(t) >= 0) return;
            poolEl.appendChild(h('button', {
              class: 'chip', type: 'button',
              onclick: function () {
                if (done || flash) return;
                Sfx.play('tap'); placed.push(t); render();
                if (placed.length === words.length) check();
              }
            }, t.w));
          });
        }
        function check() {
          var good = placed.map(function (t, k) { return t.w === words[k]; });
          if (good.every(Boolean)) {
            done = true; render(); Voice.say(s); c.right();
          } else {
            flash = good; render(); UI.shake(answer); c.wrong('Casi. Las verdes están bien; las otras vuelven.');
            var keep = 0; while (keep < good.length && good[keep]) keep++;
            setTimeout(function () { flash = null; placed = placed.slice(0, keep); render(); }, 1400);
          }
        }
        render();
      }
    });
  };

  // =====================================================================
  //  6. Cuentos a tu ritmo (lectura guiada + preguntas)
  // =====================================================================
  G.cuentosMenu = function () {
    UI.back = function () { App().menu(); };
    return h('div', { class: 'screen-in' },
      UI.header('Cuentos', function () { App().menu(); }),
      h('p', { class: 'lead' }, 'Elige un cuento. Lo lees a tu ritmo y también lo puedes escuchar.'),
      h('div', { class: 'cards' }, D.STORIES.map(function (st) {
        return h('button', { class: 'card', type: 'button', onclick: function () { Sfx.play('tap'); UI.go(function () { return G.reader(st); }); } },
          h('span', { class: 'emoji' }, st.emoji), h('span', { class: 'ct' }, st.title), UI.stars(S.bestOf('cuentos:' + st.id)));
      })));
  };

  var RATES = [{ v: 0.7, ico: '🐢', name: 'Lenta' }, { v: 0.9, ico: '🚶', name: 'Normal' }, { v: 1.1, ico: '🐇', name: 'Rápida' }];
  function rateIdx() {
    var r = S.get().settings.rate, best = 0;
    RATES.forEach(function (x, k) { if (Math.abs(x.v - r) < Math.abs(RATES[best].v - r)) best = k; });
    return best;
  }

  G.reader = function (st) {
    var idx = 0, auto = false, last = st.sents.length - 1;
    var box = h('div', { class: 'story' }), sentEls = [];
    var progress = h('div', { class: 'prog' });
    var nextBtn;

    UI.back = function () { UI.go(G.cuentosMenu); };
    UI.onLeave(function () { auto = false; });

    function wordParts(w) {
      if (!S.get().settings.syl) return w;
      return L.tokenize(w).map(function (t) { return t.word ? h('span', { class: 'sy' }, UI.sylSpans(t.text)) : t.text; });
    }
    function sayWord(w, span) {
      var clean = w.replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ]/g, '');
      if (!clean) return;
      span.classList.add('tap'); setTimeout(function () { span.classList.remove('tap'); }, 700);
      var syl = L.syllabify(clean);
      if (S.get().settings.syl && syl.length > 1) Voice.syllables(syl.map(function (x) { return x.toLowerCase(); }), { rate: 0.75 });
      else Voice.say(clean, { rate: 0.8 });
    }
    function build() {
      box.innerHTML = ''; sentEls = [];
      st.sents.forEach(function (s, k) {
        var p = h('p', { class: 'sent', onclick: function () { if (k !== idx) { go(k); } } });
        L.words(s).forEach(function (w) {
          var span = h('span', { class: 'w' }, wordParts(w));
          span.addEventListener('click', function (e) { if (k === idx) { e.stopPropagation(); sayWord(w, span); } });
          p.appendChild(span); p.appendChild(doc.createTextNode(' '));
        });
        sentEls.push(p); box.appendChild(p);
      });
      mark();
    }
    function mark() {
      sentEls.forEach(function (p, k) { p.className = 'sent' + (k === idx ? ' cur' : '') + (S.get().settings.focus && k !== idx ? ' dim' : ''); });
      progress.textContent = 'Frase ' + (idx + 1) + ' de ' + (last + 1);
      if (nextBtn) nextBtn.textContent = idx === last ? '¡Listo! ✓' : '▶';
      var el = sentEls[idx];
      if (el && el.scrollIntoView) { try { el.scrollIntoView({ block: 'center', behavior: 'smooth' }); } catch (e) { el.scrollIntoView(); } }
    }
    function clearNow() { [].forEach.call(box.querySelectorAll('.w.now'), function (x) { x.classList.remove('now'); }); }
    function go(k) {
      Voice.stop(); clearNow();
      idx = Math.max(0, Math.min(last, k)); mark();
    }
    function listen() {
      clearNow();
      var ws = sentEls[idx].querySelectorAll('.w'), mine = idx;
      Voice.say(st.sents[idx], {
        rate: RATES[rateIdx()].v,
        onWord: function (i) { clearNow(); if (ws[i]) ws[i].classList.add('now'); },
        onDone: function () {
          clearNow();
          if (!auto || mine !== idx) return;
          if (idx < last) setTimeout(function () { if (auto && mine === idx) { go(idx + 1); listen(); } }, 800);
          else { auto = false; syncChips(); }
        }
      });
    }
    var chipRate, chipFocus, chipSyl, chipAuto;
    function syncChips() {
      var r = RATES[rateIdx()];
      chipRate.textContent = r.ico + ' ' + r.name;
      chipFocus.className = 'chip tog' + (S.get().settings.focus ? ' on' : '');
      chipSyl.className = 'chip tog' + (S.get().settings.syl ? ' on' : '');
      chipAuto.className = 'chip tog' + (auto ? ' on' : '');
      chipAuto.textContent = auto ? '⏸ Parar' : '🎧 Leer todo';
    }
    chipRate = h('button', { class: 'chip tog on', type: 'button', onclick: function () { Sfx.play('tap'); S.setting('rate', RATES[(rateIdx() + 1) % RATES.length].v); syncChips(); } });
    chipFocus = h('button', { type: 'button', onclick: function () { Sfx.play('tap'); S.setting('focus', !S.get().settings.focus); syncChips(); mark(); } }, '🔦 Enfocar');
    chipSyl = h('button', { type: 'button', onclick: function () { Sfx.play('tap'); S.setting('syl', !S.get().settings.syl); syncChips(); build(); } }, '🌈 Sílabas');
    chipAuto = h('button', { type: 'button', onclick: function () {
      Sfx.play('tap'); auto = !auto; syncChips();
      if (auto) listen(); else { Voice.stop(); clearNow(); }
    } });

    nextBtn = h('button', { class: 'btn go big', type: 'button', 'aria-label': 'Siguiente frase', onclick: function () {
      Sfx.play('tap');
      if (idx === last) { UI.go(function () { return G.quiz(st); }); } else go(idx + 1);
    } }, '▶');
    var prevBtn = UI.btn('◀', 'soft big', function () { go(idx - 1); }, { 'aria-label': 'Frase anterior' });
    var hearBtn = UI.btn('🔊 Escuchar', 'go big wide', function () { auto = false; syncChips(); listen(); });

    var node = h('div', { class: 'screen-in reader' },
      UI.header(st.title, function () { UI.go(G.cuentosMenu); }, h('span', { class: 'emoji sm' }, st.emoji)),
      h('div', { class: 'chips' }, chipRate, chipFocus, chipSyl, chipAuto),
      box,
      h('div', { class: 'readdock' }, progress, h('div', { class: 'row3' }, prevBtn, hearBtn, nextBtn)));
    build(); syncChips();
    setTimeout(function () { mark(); }, 50);
    return node;
  };

  G.quiz = function (st) {
    return G.rounds({
      key: 'cuentos:' + st.id, title: '¿Qué entendiste?', n: 3,
      exit: function () { UI.go(G.cuentosMenu); },
      again: function () { UI.go(function () { return G.reader(st); }); },
      extra: UI.btn('Otro cuento', 'soft', function () { UI.go(G.cuentosMenu); }),
      build: function (i, c) {
        var q = st.qs[i];
        c.prompt(q.q, true);
        var opts = L.shuffle(q.o.map(function (t, k) { return { t: t, ok: k === q.a }; }), Math.random);
        var list = h('div', { class: 'opts' }), hint = h('div', { class: 'hintbox' });
        var all = UI.btn('🔊 Leer las opciones', 'soft', function () {
          Voice.say(q.q + ' ' + opts.map(function (o, k) { return 'Opción ' + (k + 1) + ': ' + o.t + '.'; }).join(' '));
        });
        c.stage.appendChild(list); c.stage.appendChild(all); c.stage.appendChild(hint);
        var done = false;
        opts.forEach(function (o, k) {
          var b = h('button', {
            class: 'opt', type: 'button',
            onclick: function () {
              if (done || b.disabled) return;
              if (o.ok) { done = true; b.classList.add('ok'); Voice.say(o.t); c.right(); }
              else {
                b.disabled = true; b.classList.add('off'); UI.shake(b);
                var n = c.wrong('Casi. Mira la pista y prueba otra vez.');
                hint.innerHTML = '';
                hint.appendChild(h('div', { class: 'hl' }, '💡 Pista: lee esta parte del cuento'));
                hint.appendChild(h('div', { class: 'hs' }, st.sents[q.hint]));
                hint.appendChild(UI.btn('🔊 Escuchar la pista', 'soft', function () { Voice.say(st.sents[q.hint]); }));
              }
            }
          }, h('span', { class: 'num' }, String(k + 1)), h('span', { class: 'otxt' }, o.t));
          list.appendChild(b);
        });
      }
    });
  };

  root.Games = G;
})(window);
