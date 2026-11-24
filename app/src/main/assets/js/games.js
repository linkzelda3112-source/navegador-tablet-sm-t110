/* Los cinco minijuegos de Mate Hub. */
(function (root) {
  'use strict';
  var L = root.MathLogic, UI = root.UI, h = UI.h, S = root.Sound, Store = root.Store;

  function clamp(v, a, b) { return Math.max(a, Math.min(b, v)); }
  function restart(el, cls, ms) {
    el.classList.remove(cls); void el.offsetWidth; el.classList.add(cls);
    if (ms) setTimeout(function () { el.classList.remove(cls); }, ms);
  }

  var TITLES = ['¡Sigue practicando!', '¡Buen intento!', '¡Muy bien!', '¡Perfecto!'];
  var SUBS = [
    'Cada partida te hace más rápido.',
    'Ya casi tienes la primera estrella dorada.',
    'Estás dominando las cuentas.',
    'No se te escapó ni una.'
  ];

  /* ================= base ================= */
  class Game {
    constructor(o) {
      this.root = o.root; this.diff = o.diff; this.onEnd = o.onEnd; this.onPause = o.onPause;
      this.id = o.id; this.opClass = o.opClass;
      this.paused = false; this.ended = false; this.destroyed = false; this.running = true;
      this.timers = []; this.stat = {};
      this.score = 0; this.correct = 0; this.wrong = 0; this.streak = 0; this.bestStreak = 0;
      this.maxLives = 3; this.lives = 3; this.lastText = '';
      this.timeTotal = 0; this.timeLeft = 0;
    }
    buildFrame(opts) {
      var r = this.root; r.innerHTML = '';
      var self = this;
      var g = h('div', { class: 'game ' + this.opClass });
      var hud = h('div', { class: 'hud' });
      hud.appendChild(h('button', { class: 'pause-btn', 'aria-label': 'Pausa', onclick: function () { S.play('key'); if (self.onPause) self.onPause(); } }, [h('span'), h('span')]));
      (opts.stats || []).forEach(function (s) {
        var b = h('b', { text: s.init != null ? s.init : '0' });
        self.stat[s.key] = b;
        hud.appendChild(h('div', { class: 'stat' }, [h('small', { text: s.label }), b]));
      });
      hud.appendChild(h('div', { class: 'grow' }));
      if (opts.combo) { this.comboEl = h('div', { class: 'combo', hidden: true }); hud.appendChild(this.comboEl); }
      if (opts.hearts) { this.heartsEl = h('div', { class: 'hearts', 'aria-label': 'Vidas' }); this.drawHearts(); hud.appendChild(this.heartsEl); }
      g.appendChild(hud);
      if (opts.time) {
        this.timeTotal = opts.time; this.timeLeft = opts.time;
        this.timebar = h('div', { class: 'timebar' }, [h('i')]);
        g.appendChild(this.timebar);
      }
      r.appendChild(g);
      this.frame = g;
      return g;
    }
    start() {
      var self = this;
      this.build();
      this.last = performance.now();
      var loop = function (t) {
        if (self.destroyed) return;
        var dt = Math.min(0.05, (t - self.last) / 1000); self.last = t;
        if (!self.paused && !self.ended) self.tick(dt);
        self.raf = requestAnimationFrame(loop);
      };
      this.raf = requestAnimationFrame(loop);
    }
    tick(dt) {
      this.runTimers(dt);
      if (this.ended) return;
      if (this.timeTotal && this.running) {
        this.timeLeft -= dt;
        var r = Math.max(0, this.timeLeft / this.timeTotal);
        this.timebar.firstChild.style.transform = 'scaleX(' + r + ')';
        this.timebar.classList.toggle('low', r < 0.2);
        if (this.timeLeft <= 0) { this.timeLeft = 0; this.timeUp(); }
      }
      if (!this.ended) this.update(dt);
    }
    update() {}
    timeUp() { this.finish(500); }
    after(sec, fn) { this.timers.push({ t: sec, fn: fn }); }
    runTimers(dt) {
      for (var i = this.timers.length - 1; i >= 0; i--) {
        var tm = this.timers[i];
        if (!tm) continue;
        tm.t -= dt;
        if (tm.t <= 0) { this.timers.splice(i, 1); tm.fn(); if (this.ended) return; }
      }
    }
    pause() { this.paused = true; }
    resume() { this.paused = false; this.last = performance.now(); }
    destroy() { this.destroyed = true; cancelAnimationFrame(this.raf); this.root.innerHTML = ''; }

    drawHearts() {
      this.heartsEl.innerHTML = '';
      for (var i = 0; i < this.maxLives; i++) this.heartsEl.appendChild(UI.heart(i >= this.lives));
    }
    loseLife() {
      this.lives = Math.max(0, this.lives - 1);
      this.drawHearts();
      restart(this.heartsEl, 'hit', 400);
      S.play('ouch'); S.vibrate(60);
      return this.lives <= 0;
    }
    refreshScore() { if (this.stat.score) this.stat.score.textContent = this.score; }
    setCombo() {
      if (!this.comboEl) return;
      var m = L.multiplier(this.streak);
      if (m > 1) {
        var was = this.comboEl.textContent;
        this.comboEl.hidden = false; this.comboEl.textContent = '×' + m;
        if (was !== '×' + m) restart(this.comboEl, 'bump', 300);
      } else { this.comboEl.hidden = true; this.comboEl.textContent = ''; }
    }
    hit(base, quiet) {
      this.streak++; if (this.streak > this.bestStreak) this.bestStreak = this.streak;
      var pts = base * L.multiplier(this.streak);
      this.score += pts; this.correct++; Store.record(true);
      this.refreshScore(); this.setCombo();
      if (!quiet) S.play('ok');
      return pts;
    }
    miss(quiet) {
      this.streak = 0; this.wrong++; Store.record(false);
      this.setCombo();
      if (!quiet) { S.play('bad'); S.vibrate(40); }
    }
    stars() { return 0; }
    finish(delay) {
      if (this.ended) return;
      this.ended = true;
      var self = this;
      var st = this.stars();
      var res = {
        id: this.id, score: Math.round(this.score), correct: this.correct, wrong: this.wrong, bestStreak: this.bestStreak,
        stars: st, title: TITLES[st], sub: SUBS[st]
      };
      if (this.tweakResult) this.tweakResult(res);
      setTimeout(function () { if (!self.destroyed) self.onEnd(res); }, delay == null ? 600 : delay);
    }
  }

  /* ================= 1. Globos locos (suma y resta) ================= */
  class Globos extends Game {
    build() {
      var self = this;
      var g = this.buildFrame({ stats: [{ key: 'score', label: 'Puntos' }], combo: true, hearts: true, time: 60 });
      this.stageEl = h('div', { class: 'stage sky' });
      for (var i = 0; i < 4; i++) {
        this.stageEl.appendChild(h('div', { class: 'cloud', style: 'left:' + Math.round(Math.random() * 70) + '%;top:' + (12 + i * 22) + '%;opacity:' + (0.5 + Math.random() * 0.5) }));
      }
      this.qEl = h('div', { class: 'g-q q' });
      this.stageEl.appendChild(this.qEl);
      g.appendChild(this.stageEl);
      this.W = this.stageEl.clientWidth; this.H = this.stageEl.clientHeight;
      this.riseSecs = { facil: 9, medio: 7, dificil: 5.5 }[this.diff];
      this.balloons = []; this.state = 'wait'; this.t = 0;
      this.next();
    }
    next() {
      var self = this;
      this.balloons.forEach(function (b) { b.el.remove(); });
      this.balloons = [];
      this.q = L.anyOp(this.diff, this.lastText, ['add', 'sub']);
      this.lastText = this.q.text;
      this.qEl.textContent = this.q.text + ' = ';
      this.qEl.appendChild(h('em', { class: 'op-' + this.q.op, text: '?' }));
      var ch = L.choices(this.q, 4);
      var slots = L.shuffle([0.15, 0.38, 0.62, 0.85]);
      var cls = L.shuffle(['op-add', 'op-sub', 'op-mul', 'op-div']);
      var travel = this.H + 250;
      ch.options.forEach(function (v, i) {
        var el = h('button', { class: 'balloon ' + cls[i], 'aria-label': String(v) }, [h('div', { class: 'bl', text: String(v) })]);
        var b = {
          el: el, v: v, ok: i === ch.correctIndex, x: slots[i] * self.W + (Math.random() - 0.5) * 12,
          y: self.H + 30 + Math.random() * 90, vy: travel / self.riseSecs * (0.92 + Math.random() * 0.16), ph: Math.random() * 6
        };
        el.addEventListener('pointerdown', function (e) { e.preventDefault(); self.tap(b); });
        self.stageEl.appendChild(el);
        self.place(b);
        self.balloons.push(b);
      });
      this.state = 'play';
    }
    place(b) { b.el.style.transform = 'translate(' + (b.x + Math.sin(this.t * 1.6 + b.ph) * 9) + 'px,' + b.y + 'px)'; }
    update(dt) {
      if (this.state !== 'play') return;
      this.t += dt;
      var self = this;
      this.balloons.forEach(function (b) { b.y -= b.vy * dt; self.place(b); });
      var c = this.balloons.filter(function (b) { return b.ok; })[0];
      if (c && c.y < -110) this.fail(null);
    }
    reveal() {
      this.balloons.forEach(function (b) { if (b.ok) b.el.classList.add('reveal'); else b.el.classList.add('gone'); });
      this.qEl.lastChild.textContent = this.q.answer;
    }
    fail(tapped) {
      this.state = 'wait';
      if (tapped) tapped.el.classList.add('wrong');
      this.miss();
      this.reveal();
      var dead = this.loseLife();
      if (dead) this.after(1.0, this.finish.bind(this, 300));
      else this.after(1.1, this.next.bind(this));
    }
    tap(b) {
      if (this.state !== 'play') return;
      if (!b.ok) { this.fail(b); return; }
      this.state = 'wait';
      var base = 10 + Math.round(10 * clamp(b.y / this.H, 0, 1));
      var pts = this.hit(base, true);
      S.play('pop'); S.play('ok');
      var cx = b.x, cy = b.y + 40;
      UI.burst(this.stageEl, cx, cy, null, 16);
      UI.floaty(this.stageEl, '+' + pts, cx, cy - 10, 'good');
      this.balloons.forEach(function (o) { o.el.classList.add('gone'); });
      this.after(0.45, this.next.bind(this));
    }
    stars() { return L.starsFor(this.correct, [8, 14, 20]); }
  }

  /* ================= 2. Lluvia de meteoritos (multiplicación) ================= */
  class Meteoritos extends Game {
    build() {
      var self = this;
      var g = this.buildFrame({ stats: [{ key: 'score', label: 'Puntos' }], combo: true, hearts: true, time: 75 });
      this.stageEl = h('div', { class: 'stage space' });
      this.stageEl.appendChild(h('div', { class: 'stars-bg' }));
      this.stageEl.appendChild(h('div', { class: 'shield' }));
      var city = h('div', { class: 'city' });
      for (var i = 0; i < 9; i++) city.appendChild(h('i'));
      this.stageEl.appendChild(city);
      g.appendChild(this.stageEl);

      this.readout = h('div', { class: 'readout' });
      g.appendChild(this.readout);
      var keys = h('div', { class: 'keys pad' });
      '123456789'.split('').forEach(function (d) { keys.appendChild(self.key(d, d)); });
      keys.appendChild(h('button', { 'aria-label': 'Borrar', text: '⌫', onpointerdown: function (e) { e.preventDefault(); S.play('key'); self.typed = self.typed.slice(0, -1); self.showTyped(); } }));
      keys.appendChild(this.key('0', '0'));
      keys.appendChild(h('button', { class: 'fire', text: '¡FUEGO!', onpointerdown: function (e) { e.preventDefault(); self.evaluate(true); } }));
      g.appendChild(keys);

      this.W = this.stageEl.clientWidth; this.H = this.stageEl.clientHeight;
      this.impactY = this.H - 54 - 66;
      this.meteors = []; this.typed = ''; this.elapsed = 0; this.spawnIn = 0.8;
      this.P = {
        fall0: { facil: 14, medio: 11, dificil: 9 }[this.diff], fall1: { facil: 8.5, medio: 6.5, dificil: 5 }[this.diff],
        sp0: { facil: 3.2, medio: 2.6, dificil: 2.1 }[this.diff], sp1: { facil: 1.9, medio: 1.5, dificil: 1.2 }[this.diff]
      };
      this.showTyped();
    }
    key(label, d) {
      var self = this;
      return h('button', { text: label, onpointerdown: function (e) { e.preventDefault(); self.press(d); } });
    }
    showTyped() {
      this.readout.innerHTML = '';
      if (this.typed) this.readout.textContent = this.typed;
      else this.readout.appendChild(h('span', { class: 'ph', text: 'Escribe el resultado' }));
    }
    spawn() {
      var self = this, p, tries = 0;
      do { p = L.problem('mul', this.diff, this.lastText); tries++; }
      while (this.meteors.some(function (m) { return m.p.answer === p.answer; }) && tries < 25);
      this.lastText = p.text;
      var slots = [0.16, 0.39, 0.61, 0.84];
      var free = slots.filter(function (s) { return !self.meteors.some(function (m) { return Math.abs(m.x - s * self.W) < 20 && m.y < 110; }); });
      var s = L.pick(free.length ? free : slots);
      var k = clamp(this.elapsed / 75, 0, 1);
      var fall = (this.P.fall0 + (this.P.fall1 - this.P.fall0) * k) * (0.94 + Math.random() * 0.12);
      var el = h('div', { class: 'meteor' }, [h('div', { class: 'tail' }), h('div', { class: 'rock' }), h('div', { class: 'lb', text: p.text })]);
      var m = { el: el, p: p, x: s * this.W, y: -90, vy: (this.impactY + 90) / fall };
      el.style.transform = 'translate(' + m.x + 'px,' + m.y + 'px)';
      this.stageEl.appendChild(el);
      this.meteors.push(m);
    }
    update(dt) {
      this.elapsed += dt;
      this.spawnIn -= dt;
      if (this.spawnIn <= 0) {
        if (this.meteors.length < 4) {
          this.spawn();
          var k = clamp(this.elapsed / 75, 0, 1);
          this.spawnIn = this.P.sp0 + (this.P.sp1 - this.P.sp0) * k;
        } else this.spawnIn = 0.5;
      }
      for (var i = this.meteors.length - 1; i >= 0; i--) {
        var m = this.meteors[i];
        m.y += m.vy * dt;
        m.el.style.transform = 'translate(' + m.x + 'px,' + m.y + 'px)';
        if (m.y >= this.impactY) { this.impact(m); if (this.ended) return; }
      }
    }
    impact(m) {
      this.meteors.splice(this.meteors.indexOf(m), 1);
      m.el.remove();
      UI.burst(this.stageEl, m.x, this.impactY + 40, '#ff8a3c', 18);
      this.stageEl.appendChild(h('div', { class: 'flash' }));
      var f = this.stageEl.lastChild; setTimeout(function () { f.remove(); }, 400);
      S.play('boom');
      this.miss(true);
      if (this.loseLife()) { this.finish(700); }
    }
    press(d) {
      S.play('key');
      if (this.typed.length >= 3) return;
      this.typed += d;
      this.showTyped();
      this.evaluate(false);
    }
    evaluate(manual) {
      var t = this.typed;
      if (!t) return;
      if (!this.meteors.length) { if (manual) this.typed = '', this.showTyped(); return; }
      var match = this.meteors.filter(function (m) { return String(m.p.answer) === t; }).sort(function (a, b) { return b.y - a.y; })[0];
      var longer = this.meteors.some(function (m) { var s = String(m.p.answer); return s !== t && s.indexOf(t) === 0; });
      var any = this.meteors.some(function (m) { return String(m.p.answer).indexOf(t) === 0; });
      if (match && (manual || !longer)) this.fire(match);
      else if (manual || !any) this.fail();
    }
    fail() {
      this.typed = '';
      restart(this.readout, 'shake', 320);
      this.miss();
      this.showTyped();
    }
    fire(m) {
      this.meteors.splice(this.meteors.indexOf(m), 1);
      m.el.remove();
      var x0 = this.W / 2, y0 = this.H - 54, x1 = m.x, y1 = m.y + 30;
      var len = Math.hypot(x1 - x0, y1 - y0), ang = Math.atan2(y1 - y0, x1 - x0) * 180 / Math.PI;
      var ls = h('div', { class: 'laser op-mul', style: 'left:' + x0 + 'px;top:' + y0 + 'px;width:' + len + 'px;transform:rotate(' + ang + 'deg)' });
      this.stageEl.appendChild(ls);
      var a = ls.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 180 });
      a.onfinish = function () { ls.remove(); };
      S.play('laser');
      var bonus = Math.round(10 * clamp(1 - m.y / this.impactY, 0, 1));
      var pts = this.hit(10 + bonus, true);
      UI.burst(this.stageEl, x1, y1, '#ffb703', 18);
      UI.floaty(this.stageEl, '+' + pts, x1, y1 - 14, 'good');
      S.play('boom');
      this.typed = ''; this.showTyped();
    }
    stars() { return L.starsFor(this.correct, [10, 18, 26]); }
  }

  /* ================= 3. Batalla de monstruos (división) ================= */
  function eye(cx, cy, r) {
    return '<circle cx="' + cx + '" cy="' + cy + '" r="' + r + '" fill="#fff"/><circle cx="' + (cx + r * 0.15) + '" cy="' + (cy + r * 0.15) + '" r="' + (r * 0.5) + '" fill="#1a1448"/><circle cx="' + (cx - r * 0.1) + '" cy="' + (cy - r * 0.15) + '" r="' + (r * 0.18) + '" fill="#fff"/>';
  }
  var MONSTERS = [
    { name: 'Gelatino', svg: '<path d="M26 156C16 86 56 34 100 34s84 52 74 122q-74 24-148 0z" fill="#5be37d"/><path d="M26 156q74 24 148 0" fill="none" stroke="#2fa855" stroke-width="6"/>' + eye(76, 92, 18) + eye(124, 92, 18) + '<path d="M70 126Q100 158 130 126z" fill="#1a1448"/><path d="M80 128l7 11 7-11zM106 128l7 11 7-11z" fill="#fff"/>' },
    { name: 'Cuernitos', svg: '<path d="M52 82L44 28l38 36zM148 82l8-54-38 36z" fill="#ffe08a"/><circle cx="100" cy="120" r="64" fill="#ff6b6b"/>' + eye(78, 112, 15) + eye(122, 112, 15) + '<path d="M60 90l34 12M140 90l-34 12" stroke="#1a1448" stroke-width="7" stroke-linecap="round"/><path d="M72 150l11-12 11 12 11-12 11 12 11-12 11 12" fill="none" stroke="#1a1448" stroke-width="7" stroke-linejoin="round" stroke-linecap="round"/>' },
    { name: 'Pinchudo', svg: '<path d="M40 104L34 48l32 30 12-48 22 44 22-44 12 48 32-30-6 56z" fill="#ff9f1c"/><ellipse cx="100" cy="124" rx="68" ry="56" fill="#ffd23f"/>' + eye(100, 116, 26) + '<path d="M74 154Q100 174 126 154" stroke="#1a1448" stroke-width="7" fill="none" stroke-linecap="round"/>' },
    { name: 'Fantasmón', svg: '<path d="M34 162V96C34 52 64 28 100 28s66 24 66 68v66l-22-14-22 14-22-14-22 14-22-14z" fill="#8ecbff"/>' + eye(78, 92, 16) + eye(122, 92, 16) + '<ellipse cx="100" cy="132" rx="14" ry="18" fill="#1a1448"/>' }
  ];

  class Monstruos extends Game {
    build() {
      var self = this;
      var g = this.buildFrame({ stats: [{ key: 'score', label: 'Puntos' }, { key: 'mon', label: 'Monstruos' }], combo: true, hearts: true, time: 90 });
      this.hpMax = { facil: 4, medio: 5, dificil: 6 }[this.diff];
      this.qTime = { facil: 10, medio: 8, dificil: 6 }[this.diff];
      this.hp = this.hpMax; this.kind = 0; this.defeated = 0; this.state = 'wait';
      this.stageEl = h('div', { class: 'stage arena' });
      this.nameEl = h('div', { class: 'mname' });
      this.hpEl = h('div', { class: 'hp', 'aria-label': 'Vida del monstruo' });
      this.monEl = h('div', { class: 'monster' });
      this.qEl = h('div', { class: 'q' });
      this.qBar = h('div', { class: 'qtime' }, [h('i')]);
      this.stageEl.appendChild(this.nameEl);
      this.stageEl.appendChild(this.hpEl);
      this.stageEl.appendChild(this.monEl);
      this.stageEl.appendChild(h('div', { class: 'm-q' }, [this.qEl, this.qBar]));
      g.appendChild(this.stageEl);
      this.ansEl = h('div', { class: 'answers' });
      this.btns = [];
      for (var i = 0; i < 4; i++) {
        (function (i) {
          var b = h('button', { class: 'ans', onpointerdown: function (e) { e.preventDefault(); self.answer(i); } });
          self.btns.push(b); self.ansEl.appendChild(b);
        })(i);
      }
      g.appendChild(this.ansEl);
      this.drawMonster(false);
      this.ask();
    }
    drawMonster(anim) {
      var m = MONSTERS[this.kind % MONSTERS.length];
      this.nameEl.textContent = m.name;
      this.monEl.className = 'monster';
      this.monEl.innerHTML = '<svg viewBox="0 0 200 200" aria-hidden="true">' + m.svg + '</svg>';
      if (anim) restart(this.monEl, 'spawn', 520);
      this.drawHp();
    }
    drawHp() {
      this.hpEl.innerHTML = '';
      for (var i = 0; i < this.hpMax; i++) this.hpEl.appendChild(h('i', { class: i < this.hp ? '' : 'off' }));
    }
    ask() {
      this.q = L.problem('div', this.diff, this.lastText);
      this.lastText = this.q.text;
      this.ch = L.choices(this.q, 4);
      this.qEl.textContent = this.q.text + ' = ';
      this.qEl.appendChild(h('em', { text: '?' }));
      var self = this;
      this.btns.forEach(function (b, i) { b.textContent = self.ch.options[i]; b.className = 'ans'; b.disabled = false; });
      this.qLeft = this.qTime;
      this.state = 'play';
    }
    update(dt) {
      if (this.state !== 'play') return;
      this.qLeft -= dt;
      var r = Math.max(0, this.qLeft / this.qTime);
      this.qBar.firstChild.style.transform = 'scaleX(' + r + ')';
      this.qBar.classList.toggle('low', r < 0.3);
      if (this.qLeft <= 0) this.answer(-1);
    }
    answer(i) {
      if (this.state !== 'play') return;
      this.state = 'wait';
      var self = this, okIdx = this.ch.correctIndex;
      this.btns.forEach(function (b) { b.disabled = true; });
      this.btns[okIdx].classList.add('ok');
      var cx = this.stageEl.clientWidth / 2, cy = this.monEl.offsetTop + this.monEl.offsetHeight / 2;
      if (i === okIdx) {
        var fast = this.qLeft > this.qTime * 0.5;
        var dmg = fast ? 2 : 1;
        var pts = this.hit(10 + Math.round(this.qLeft / this.qTime * 10), true);
        S.play(fast ? 'crit' : 'hit');
        restart(this.monEl, 'hurt', 460);
        UI.burst(this.stageEl, cx, cy, '#ffd23f', 14);
        UI.floaty(this.stageEl, (fast ? '¡CRÍTICO! −' : '−') + dmg, cx, cy - 40, 'good');
        this.hp = Math.max(0, this.hp - dmg);
        this.drawHp();
        if (this.hp <= 0) {
          this.defeated++; this.stat.mon.textContent = this.defeated;
          this.score += 50 * L.multiplier(this.streak); this.refreshScore();
          this.after(0.35, function () { self.monEl.classList.remove('hurt'); self.monEl.classList.add('dead'); S.play('win'); UI.burst(self.stageEl, cx, cy, null, 24); });
          this.after(1.1, function () { self.kind++; self.hp = self.hpMax; self.drawMonster(true); });
          this.after(1.5, this.ask.bind(this));
        } else this.after(0.6, this.ask.bind(this));
      } else {
        if (i >= 0) this.btns[i].classList.add('no');
        this.miss(true);
        restart(this.monEl, 'lunge', 520);
        UI.floaty(this.stageEl, i < 0 ? '¡Tiempo!' : '¡Ay!', cx, cy + 20, 'bad');
        var dead = this.loseLife();
        if (dead) this.after(1.0, this.finish.bind(this, 300));
        else this.after(1.0, this.ask.bind(this));
      }
    }
    timeUp() { this.state = 'wait'; this.finish(500); }
    stars() { return L.starsFor(this.correct, [8, 15, 22]); }
  }

  /* ================= 4. Parejas (memoria con las cuatro operaciones) ================= */
  class Parejas extends Game {
    build() {
      var self = this;
      var g = this.buildFrame({ stats: [{ key: 'moves', label: 'Jugadas' }, { key: 'pairs', label: 'Parejas', init: '0' }, { key: 'time', label: 'Tiempo', init: '0:00' }] });
      this.pairs = { facil: 6, medio: 8, dificil: 10 }[this.diff];
      this.stat.pairs.textContent = '0/' + this.pairs;
      this.moves = 0; this.matched = 0; this.sec = 0; this.started = false; this.first = null; this.lock = false;
      var ops = L.shuffle(['add', 'sub', 'mul', 'div']);
      var cards = [], seen = {}, seenText = {};
      for (var i = 0; i < this.pairs; i++) {
        var op = ops[i % 4], p, tries = 0;
        do { p = L.problem(op, this.diff, ''); tries++; } while ((seen[p.answer] || seenText[p.text]) && tries < 40);
        seen[p.answer] = true; seenText[p.text] = true;
        cards.push({ pair: i, kind: 'q', text: p.text, op: op });
        cards.push({ pair: i, kind: 'a', text: String(p.answer), op: op });
      }
      cards = L.shuffle(cards);
      this.grid = h('div', { class: 'mem', style: 'grid-template-columns:repeat(4,1fr)' });
      this.cardEls = cards.map(function (c) {
        var len = c.text.length;
        var fs = c.kind === 'a' ? (len > 3 ? 22 : 28) : (len <= 5 ? 24 : len <= 7 ? 19 : 15);
        var el = h('button', { class: 'mc', 'aria-label': 'Carta' }, [h('div', { class: 'in' }, [
          h('div', { class: 'b', text: '?' }),
          h('div', { class: 'f op-' + c.op + (c.kind === 'a' ? ' res' : ''), text: c.text, style: 'font-size:' + fs + 'px;text-align:center;line-height:1.1;padding:4px' })
        ])]);
        c.el = el;
        el.addEventListener('pointerdown', function (e) { e.preventDefault(); self.flip(c); });
        self.grid.appendChild(el);
        return c;
      });
      g.appendChild(this.grid);
      var rows = Math.ceil(cards.length / 4);
      var gap = 10, w = (this.grid.clientWidth - gap * 3) / 4;
      var ch = Math.min(w * 1.35, (this.grid.clientHeight - 14 - gap * (rows - 1)) / rows);
      this.cardEls.forEach(function (c) { c.el.style.height = Math.max(54, ch) + 'px'; });
    }
    update(dt) {
      if (!this.started) return;
      this.sec += dt;
      var s = Math.floor(this.sec);
      this.stat.time.textContent = Math.floor(s / 60) + ':' + ('0' + (s % 60)).slice(-2);
    }
    flip(c) {
      if (this.lock || c.up || c.done || this.ended) return;
      this.started = true;
      c.up = true; c.el.classList.add('up'); S.play('flip');
      if (!this.first) { this.first = c; return; }
      var a = this.first, b = c, self = this;
      this.first = null;
      this.moves++; this.stat.moves.textContent = this.moves;
      if (a.pair === b.pair) {
        Store.record(true);
        a.done = b.done = true;
        this.matched++; this.stat.pairs.textContent = this.matched + '/' + this.pairs;
        this.after(0.3, function () {
          a.el.classList.add('done'); b.el.classList.add('done'); S.play('match');
        });
        if (this.matched === this.pairs) {
          this.started = false;
          this.after(0.9, function () { S.play('win'); self.finish(700); });
        }
      } else {
        Store.record(false);
        this.lock = true;
        this.after(0.4, function () { a.el.classList.add('bad'); b.el.classList.add('bad'); S.play('bad'); });
        this.after(1.0, function () {
          a.up = b.up = false;
          a.el.classList.remove('up', 'bad'); b.el.classList.remove('up', 'bad');
          self.lock = false;
        });
      }
    }
    stars() { var r = this.moves / this.pairs; return this.matched < this.pairs ? 0 : r <= 1.5 ? 3 : r <= 2.5 ? 2 : 1; }
    tweakResult(res) {
      var p = this.pairs;
      res.score = Math.round(p * 50 + Math.max(0, p * 3 - this.moves) * 10 + Math.max(0, 90 - this.sec) * 2);
      res.correct = this.matched; res.wrong = Math.max(0, this.moves - this.matched);
      res.bestStreak = this.moves; res.labels = ['Puntos', 'Jugadas', 'Fallos'];
      res.rowValues = [res.score, this.moves, res.wrong];
    }
  }

  /* ================= 5. Gran carrera (todas las operaciones) ================= */
  var RIVALS = [
    { name: 'Tú', color: '#ffd23f' },
    { name: 'Rayo', color: '#4cc9f0' },
    { name: 'Turbo', color: '#ff6b8b' },
    { name: 'Nube', color: '#b388ff' }
  ];
  function carSvg(c) {
    return '<svg viewBox="0 0 46 24" aria-hidden="true"><rect x="2" y="8" width="42" height="12" rx="5" fill="' + c + '"/><path d="M14 8l5-6h10l5 6z" fill="' + c + '"/><path d="M18 8l3-4h6l3 4z" fill="#cfe9ff"/><circle cx="12" cy="20" r="4" fill="#12163a"/><circle cx="34" cy="20" r="4" fill="#12163a"/><rect x="42" y="11" width="3" height="3" fill="#fff"/></svg>';
  }

  class Carrera extends Game {
    build() {
      var self = this;
      var g = this.buildFrame({ stats: [{ key: 'pos', label: 'Puesto', init: '1.º' }, { key: 'score', label: 'Puntos' }], combo: true, time: 70 });
      this.running = false;
      var range = { facil: [1.1, 1.6], medio: [1.7, 2.3], dificil: [2.3, 3.1] }[this.diff];
      this.gain = { facil: 8, medio: 7.5, dificil: 7 }[this.diff];
      this.p = [0, 0, 0, 0];
      this.base = [0, 1, 2, 3].map(function (i) { return i ? range[0] + Math.random() * (range[1] - range[0]) : 0; });
      this.spd = this.base.slice();
      this.jit = 0; this.done = []; this.state = 'count'; this.cnt = 3.2; this.stall = 0; this.lastN = 0;
      this.track = h('div', { class: 'track' });
      this.cars = RIVALS.map(function (r, i) {
        var car = h('div', { class: 'car', html: carSvg(r.color) });
        var lane = h('div', { class: 'lane' + (i === 0 ? ' me' : '') }, [h('div', { class: 'nm', text: r.name }), car]);
        self.track.appendChild(lane);
        return car;
      });
      this.track.appendChild(h('div', { class: 'finish' }));
      g.appendChild(this.track);
      this.rq = h('div', { class: 'r-q' });
      this.qEl = h('div', { class: 'q' });
      this.rq.appendChild(this.qEl);
      g.appendChild(this.rq);
      this.ansEl = h('div', { class: 'answers' });
      this.btns = [];
      for (var i = 0; i < 4; i++) {
        (function (i) {
          var b = h('button', { class: 'ans', disabled: true, onpointerdown: function (e) { e.preventDefault(); self.answer(i); } });
          self.btns.push(b); self.ansEl.appendChild(b);
        })(i);
      }
      g.appendChild(this.ansEl);
      this.qEl.textContent = '¿Listo?';
      this.countEl = h('div', { class: 'count', text: '3' });
      g.appendChild(this.countEl);
      this.layout();
    }
    layout() {
      var w = this.track.clientWidth - 20 - 56 - 24;
      var self = this;
      this.cars.forEach(function (c, i) { c.style.transform = 'translateX(' + (self.p[i] / 100 * w) + 'px)'; });
    }
    ranking() {
      var self = this, idx = [0, 1, 2, 3];
      idx.sort(function (a, b) {
        var fa = self.done.indexOf(a), fb = self.done.indexOf(b);
        if (fa >= 0 && fb >= 0) return fa - fb;
        if (fa >= 0) return -1;
        if (fb >= 0) return 1;
        return self.p[b] - self.p[a];
      });
      return idx;
    }
    ask() {
      this.q = L.anyOp(this.diff, this.lastText);
      this.lastText = this.q.text;
      this.ch = L.choices(this.q, 4);
      this.qEl.textContent = this.q.text + ' = ';
      this.qEl.appendChild(h('em', { text: '?' }));
      var self = this;
      this.btns.forEach(function (b, i) { b.textContent = self.ch.options[i]; b.className = 'ans'; b.disabled = false; });
      this.state = 'race';
    }
    update(dt) {
      var self = this;
      if (this.state === 'count') {
        this.cnt -= dt;
        var n = Math.ceil(this.cnt - 0.2);
        if (n > 0) {
          if (n !== this.lastN) { this.lastN = n; this.countEl.textContent = n; restart(this.countEl, 'count'); S.play('tick'); }
        } else {
          this.countEl.textContent = '¡YA!'; restart(this.countEl, 'count'); S.play('go');
          this.state = 'race'; this.running = true;
          this.after(0.7, function () { self.countEl.remove(); });
          this.ask();
        }
        return;
      }
      if (this.state === 'wait' && this.stall > 0) {
        this.stall -= dt;
        if (this.stall <= 0) this.ask();
      }
      this.jit -= dt;
      if (this.jit <= 0) {
        this.jit = 1;
        for (var i = 1; i < 4; i++) this.spd[i] = this.base[i] * (0.82 + Math.random() * 0.36);
      }
      for (var j = 1; j < 4; j++) {
        if (this.done.indexOf(j) >= 0) continue;
        var rub = 1 + 0.12 * clamp((this.p[0] - this.p[j]) / 40, -1, 1);
        this.p[j] = Math.min(100, this.p[j] + this.spd[j] * rub * dt);
        if (this.p[j] >= 100) this.done.push(j);
      }
      this.layout();
      var order = this.ranking();
      this.stat.pos.textContent = (order.indexOf(0) + 1) + '.º';
      if (this.done.length >= 3 && this.done.indexOf(0) < 0) { this.state = 'over'; this.finish(900); }
    }
    answer(i) {
      if (this.state !== 'race') return;
      var okIdx = this.ch.correctIndex;
      this.btns.forEach(function (b) { b.disabled = true; });
      this.btns[okIdx].classList.add('ok');
      if (i === okIdx) {
        this.hit(10);
        this.p[0] = Math.min(100, this.p[0] + this.gain + (L.multiplier(this.streak) - 1));
        restart(this.cars[0], 'boost', 300);
        this.layout();
        if (this.p[0] >= 100) {
          this.done.push(0); this.state = 'over'; S.play('win'); this.finish(900); return;
        }
        this.state = 'wait'; this.stall = 0.25;
      } else {
        this.btns[i].classList.add('no');
        this.miss();
        this.p[0] = Math.max(0, this.p[0] - 5);
        this.layout();
        this.state = 'wait'; this.stall = 0.8;
      }
    }
    timeUp() { this.state = 'over'; this.finish(500); }
    rank() { return this.ranking().indexOf(0) + 1; }
    stars() { return 4 - this.rank(); }
    tweakResult(res) {
      var r = this.rank();
      res.title = ['', '¡Ganaste la carrera!', '¡Segundo lugar!', 'Tercer lugar', 'Último, ¡pide revancha!'][r];
      res.sub = ['', 'Cruzaste la meta primero.', 'Casi lo logras, ¡a por el primero!', 'Responde más rápido para adelantar.', 'Las cuentas rápidas te darán velocidad.'][r];
      res.score = this.correct * 20 + (4 - r) * 150 + this.bestStreak * 10;
      res.stars = 4 - r;
    }
  }

  root.GAMES = [
    { id: 'globos', name: 'Globos Locos', sub: 'Sumas y restas', cls: 'op-add', sym: '+ −', create: function (o) { return new Globos(o); } },
    { id: 'meteoritos', name: 'Lluvia de Meteoritos', sub: 'Multiplicación', cls: 'op-mul', sym: '×', create: function (o) { return new Meteoritos(o); } },
    { id: 'monstruos', name: 'Batalla de Monstruos', sub: 'División', cls: 'op-div', sym: '÷', create: function (o) { return new Monstruos(o); } },
    { id: 'parejas', name: 'Parejas', sub: 'Memoria de cuentas', cls: 'op-sub', sym: '=', create: function (o) { return new Parejas(o); } },
    { id: 'carrera', name: 'Gran Carrera', sub: 'Todas las operaciones', cls: 'op-mix', sym: '+ − × ÷', span: true, create: function (o) { return new Carrera(o); } }
  ];
})(window);
