/* Hub, navegación, pausa, resultados y progreso. */
(function (root) {
  'use strict';
  var L = root.MathLogic, UI = root.UI, h = UI.h, S = root.Sound, Store = root.Store;
  var screen = document.getElementById('screen');
  var cur = null, curMeta = null, state = 'hub';

  var ICON_ON = '<svg viewBox="0 0 24 24"><path d="M4 9v6h4l5 4V5L8 9z"/><path d="M16.500 8.500a5 5 0 0 1 0 7M19 6a8.500 8.500 0 0 1 0 12"/></svg>';
  var ICON_OFF = '<svg viewBox="0 0 24 24"><path d="M4 9v6h4l5 4V5L8 9z"/><path d="M17 9l5 6M22 9l-5 6"/></svg>';

  /* ---------- hub ---------- */
  function hub() {
    state = 'hub';
    var d = Store.get();
    var prog = L.levelProgress(d.xp);
    screen.innerHTML = '';

    var snd = h('button', { class: 'icon-btn', 'aria-label': 'Sonido', html: d.sound ? ICON_ON : ICON_OFF });
    snd.addEventListener('click', function () {
      var on = !Store.get().sound;
      Store.setSound(on); S.setOn(on);
      snd.innerHTML = on ? ICON_ON : ICON_OFF;
      if (on) S.play('ok');
    });
    screen.appendChild(h('div', { class: 'top' }, [
      h('div', { class: 'logo' }, [h('span', { text: 'Mate' }), h('b', { text: 'Hub' })]),
      snd
    ]));

    var bar = h('div', { class: 'bar' }, [h('i')]);
    screen.appendChild(h('div', { class: 'profile' }, [
      h('div', { class: 'row' }, [
        h('span', { class: 'lvl', text: 'Nivel ' + prog.level }),
        h('span', { class: 'xp', text: prog.into + ' / ' + prog.need + ' XP' })
      ]),
      bar
    ]));
    requestAnimationFrame(function () { requestAnimationFrame(function () { bar.firstChild.style.width = Math.round(prog.ratio * 100) + '%'; }); });

    var diff = h('div', { class: 'diff', role: 'radiogroup', 'aria-label': 'Dificultad' });
    L.DIFFS.forEach(function (k) {
      var b = h('button', { role: 'radio', 'aria-checked': String(d.diff === k), text: L.DIFF_NAMES[k] });
      b.addEventListener('click', function () { S.play('key'); Store.setDiff(k); hub(); });
      diff.appendChild(b);
    });
    screen.appendChild(diff);

    var cards = h('div', { class: 'cards' });
    root.GAMES.forEach(function (m) {
      var best = Store.best(m.id, d.diff);
      var stars = h('div', { class: 'stars' });
      for (var i = 0; i < 3; i++) stars.appendChild(UI.star(i < best.stars));
      if (best.score) stars.appendChild(h('span', { class: 'rec', text: 'Récord ' + best.score }));
      var c = h('button', { class: 'card ' + m.cls + (m.span ? ' span2' : ''), 'aria-label': m.name + '. ' + m.sub }, [
        h('div', { class: 'sym', text: m.sym }),
        h('div', { class: 'nm', text: m.name }),
        h('div', { class: 'sb', text: m.sub }),
        stars
      ]);
      c.addEventListener('click', function () { S.play('key'); start(m); });
      cards.appendChild(c);
    });
    screen.appendChild(cards);

    var pct = d.total ? Math.round(d.ok / d.total * 100) + '%' : '—';
    screen.appendChild(h('div', { class: 'foot' }, [
      h('span', {}, ['Estrellas ', h('b', { text: Store.totalStars() + '/' + (root.GAMES.length * 9) })]),
      h('span', {}, ['Aciertos ', h('b', { text: pct })]),
      h('span', {}, ['Cuentas ', h('b', { text: String(d.total) })])
    ]));
  }

  /* ---------- partida ---------- */
  function start(meta) {
    UI.hideOverlay();
    if (cur) cur.destroy();
    state = 'game'; curMeta = meta;
    screen.innerHTML = '';
    cur = meta.create({
      root: screen, diff: Store.get().diff, id: meta.id, opClass: meta.cls,
      onEnd: showResult, onPause: openPause
    });
    cur.start();
  }

  function exitToHub() {
    if (cur) cur.destroy();
    cur = null;
    UI.hideOverlay();
    hub();
  }

  function openPause() {
    if (state !== 'game' || !cur || cur.ended || UI.overlayOpen()) return;
    cur.pause();
    var sheet = h('div', { class: 'sheet ' + curMeta.cls }, [
      h('h2', { text: 'Pausa' }),
      h('p', { text: curMeta.name + ' · ' + L.DIFF_NAMES[Store.get().diff] }),
      h('div', { class: 'stack' }, [
        h('button', { class: 'btn wide', text: 'Seguir jugando', onclick: closePause }),
        h('button', { class: 'btn ghost wide', text: 'Empezar de nuevo', onclick: function () { start(curMeta); } }),
        h('button', { class: 'btn ghost wide', text: 'Salir al menú', onclick: exitToHub })
      ])
    ]);
    UI.showOverlay(sheet);
  }
  function closePause() {
    UI.hideOverlay();
    if (cur) cur.resume();
  }

  function showResult(res) {
    state = 'result';
    var before = L.levelFromXp(Store.get().xp);
    var xpGain = Math.max(5, Math.round(res.score / 10) + res.stars * 10);
    Store.addXp(xpGain);
    var after = L.levelFromXp(Store.get().xp);
    var rec = Store.setBest(res.id, Store.get().diff, res.stars, res.score);
    Store.flush();
    var meta = curMeta;

    var starsEl = h('div', { class: 'big-stars' }, [UI.star(false), UI.star(false), UI.star(false)]);
    var labels = res.labels || ['Puntos', 'Aciertos', 'Racha'];
    var vals = res.rowValues || [res.score, res.correct, res.bestStreak];
    var rows = h('div', { class: 'rows' }, labels.map(function (lb, i) {
      return h('div', {}, [h('b', { text: String(vals[i]) }), h('small', { text: lb })]);
    }));
    var chips = h('div', {}, [h('span', { class: 'chip', text: '+' + xpGain + ' XP' })]);
    if (rec.newScore && res.score > 0) chips.appendChild(h('span', { class: 'chip', text: '¡Récord!' }));
    if (after > before) chips.appendChild(h('span', { class: 'chip', text: '¡Nivel ' + after + '!' }));

    var sheet = h('div', { class: 'sheet ' + meta.cls }, [
      h('h2', { text: res.title }),
      h('p', { text: res.sub }),
      starsEl, rows, chips,
      h('div', { class: 'stack' }, [
        h('button', { class: 'btn wide', text: 'Jugar otra vez', onclick: function () { start(meta); } }),
        h('button', { class: 'btn ghost wide', text: 'Menú', onclick: exitToHub })
      ])
    ]);
    UI.showOverlay(sheet);
    S.play(res.stars > 0 ? 'win' : 'lose');
    var sv = starsEl.querySelectorAll('.star');
    for (var i = 0; i < res.stars; i++) {
      (function (i) { setTimeout(function () { sv[i].classList.add('on'); S.play('star'); }, 450 + i * 350); })(i);
    }
    if (after > before) setTimeout(function () { S.play('level'); }, 450 + res.stars * 350 + 200);
  }

  /* ---------- puente con Android ---------- */
  root.__back = function () {
    if (state === 'hub') return false;
    if (state === 'result') { exitToHub(); return true; }
    if (state === 'game') {
      if (UI.overlayOpen()) closePause(); else openPause();
      return true;
    }
    return false;
  };
  root.__pause = function () { if (state === 'game') openPause(); };
  document.addEventListener('visibilitychange', function () { if (document.hidden) root.__pause(); });

  /* ---------- arranque ---------- */
  Store.load();
  S.setOn(Store.get().sound);
  document.addEventListener('pointerdown', function () { S.unlock(); }, { once: true });
  hub();
})(window);
