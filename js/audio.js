'use strict';
// ===== Sonido liminal: todo sintetizado (WebAudio). Ambientes por planeta, pasos con eco, voces lejanas, latido =====
const SND = (() => {
  let ac = null, mst, dry, wet, nw, nb, bed = null, kind = 'none', T = {}, hbT = 0, rum = null, ton = null;
  const R = (a, b) => a + Math.random() * (b - a);
  function init() {
    if (ac) { if (ac.state === 'suspended') ac.resume(); return; }
    try { ac = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) { ac = null; return; }
    mst = ac.createGain(); mst.gain.value = .9; mst.connect(ac.destination);
    dry = ac.createGain(); dry.connect(mst);
    const cv = ac.createConvolver(), len = ac.sampleRate * 2.8, ib = ac.createBuffer(2, len, ac.sampleRate);
    for (let c = 0; c < 2; c++) { const d = ib.getChannelData(c); for (let i = 0; i < len; i++) d[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / len, 2.4); }
    cv.buffer = ib; wet = ac.createGain(); wet.gain.value = .6; wet.connect(cv); cv.connect(mst);
    nw = ac.createBuffer(1, ac.sampleRate * 3, ac.sampleRate); nb = ac.createBuffer(1, ac.sampleRate * 3, ac.sampleRate);
    const a = nw.getChannelData(0), b = nb.getChannelData(0); let last = 0;
    for (let i = 0; i < a.length; i++) { const w = Math.random() * 2 - 1; a[i] = w; last = (last + .02 * w) / 1.02; b[i] = last * 3.5; }
  }
  const on = () => ac && ac.state !== 'closed';
  const flt = (type, f, q = 1) => { const x = ac.createBiquadFilter(); x.type = type; x.frequency.value = f; x.Q.value = q; return x; };
  const loopSrc = (buf) => { const s = ac.createBufferSource(); s.buffer = buf; s.loop = true; s.start(0, Math.random() * 2); return s; };
  // salida con envio al eco; pan opcional
  function out(g, wetAmt = .4, pan = 0) {
    let n = g; if (ac.createStereoPanner && pan) { const p = ac.createStereoPanner(); p.pan.value = pan; g.connect(p); n = p; }
    n.connect(dry); const s = ac.createGain(); s.gain.value = wetAmt; n.connect(s); s.connect(wet);
  }
  function env(g, v, a, d) { const t = ac.currentTime; g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(v, t + a); g.gain.exponentialRampToValueAtTime(.0001, t + a + d); }
  function oneNoise(buf, ftype, f, q, v, a, d, wetAmt, pan) {
    const s = ac.createBufferSource(); s.buffer = buf; const fl = flt(ftype, f, q), g = ac.createGain(); s.connect(fl); fl.connect(g); env(g, v, a, d); out(g, wetAmt, pan); s.start(0, Math.random() * 2, a + d + .05);
  }
  function osc(type, f0, f1, v, a, d, wetAmt = .3, pan = 0) {
    const o = ac.createOscillator(), g = ac.createGain(); o.type = type; o.frequency.setValueAtTime(f0, ac.currentTime); if (f1) o.frequency.exponentialRampToValueAtTime(f1, ac.currentTime + a + d);
    o.connect(g); env(g, v, a, d); out(g, wetAmt, pan); o.start(); o.stop(ac.currentTime + a + d + .1);
  }

  // ---------- sonidos puntuales ----------
  const S = {
    step(v = .22, pitch = 1, wetAmt = .5, pan = 0) { if (!on()) return; oneNoise(nw, 'bandpass', 380 * pitch, .9, v, .004, .09, wetAmt, pan); osc('sine', 95 * pitch, 50, v * .8, .004, .08, wetAmt * .6, pan); },
    steps(n, gap, v, pitch, wetAmt, pan) { for (let i = 0; i < n; i++) setTimeout(() => S.step(v * (1 - i * .08), pitch * (i % 2 ? .93 : 1), wetAmt, pan), i * gap * 1000); },
    blip(f = 440, d = .08, v = .06, ty = 'square') { if (!on()) return; osc(ty === 'square' ? 'triangle' : ty, f, f * .96, v, .004, d, .35); },
    chime(ok = true) { if (!on()) return; const b = ok ? 523 : 196; osc('sine', b, 0, .06, .01, .6, .8); osc('sine', b * 1.5, 0, .035, .01, .8, .8); },
    thud(v = .25) { if (!on()) return; osc('sine', 70, 28, v, .005, .5, .5); oneNoise(nb, 'lowpass', 200, .7, v * .7, .005, .4, .6); },
    bell(f = 392, v = .05) { if (!on()) return; [1, 2.76, 5.4].forEach((m, i) => osc('sine', f * m, 0, v / (i + 1), .005, 5 - i, .9, R(-.5, .5))); },
    gull() { if (!on()) return; const p = R(-.7, .7); osc('sine', 2100, 1500, .018, .05, .3, .8, p); setTimeout(() => osc('sine', 1900, 1300, .015, .04, .35, .8, p), 380); },
    creak(v = .06) { if (!on()) return; const o = ac.createOscillator(), g = ac.createGain(), f = flt('lowpass', 500, 4); o.type = 'sawtooth'; o.frequency.setValueAtTime(70, ac.currentTime); o.frequency.linearRampToValueAtTime(140, ac.currentTime + .9); o.connect(f); f.connect(g); env(g, v, .2, .8); out(g, .6); o.start(); o.stop(ac.currentTime + 1.2); },
    drip() { if (!on()) return; osc('sine', 1500, 800, .035, .003, .12, 1, R(-.6, .6)); },
    voice() { // voz lejana ininteligible
      if (!on()) return; const s = ac.createBufferSource(); s.buffer = nw; const f1 = flt('bandpass', R(400, 800), 6), f2 = flt('bandpass', R(1200, 2200), 7), g = ac.createGain(); s.connect(f1); s.connect(f2); f1.connect(g); f2.connect(g);
      const t = ac.currentTime, d = R(.5, 1.1); g.gain.setValueAtTime(0, t); g.gain.linearRampToValueAtTime(.05, t + .15); g.gain.linearRampToValueAtTime(.02, t + d * .6); g.gain.linearRampToValueAtTime(0, t + d); f1.frequency.linearRampToValueAtTime(R(350, 900), t + d); out(g, .9, R(-.8, .8)); s.start(0, Math.random() * 2, d + .1);
    },
    heart(v = .22) { if (!on()) return; S.thud(v); setTimeout(() => S.thud(v * .7), 200); },
    hurt() { if (!on()) return; S.thud(.3); osc('sawtooth', 120, 60, .05, .01, .35, .7); },
    rumble(v) { // retumbo continuo (gigante, capucha)
      if (!on()) return; if (!rum) { const o = ac.createOscillator(), f = flt('lowpass', 110, 1), g = ac.createGain(); o.type = 'sawtooth'; o.frequency.value = 38; g.gain.value = 0; o.connect(f); f.connect(g); out(g, .3); o.start(); rum = g; }
      rum.gain.setTargetAtTime(v, ac.currentTime, .15);
    },
    tone(f, v) { // tono continuo (mira del telescopio)
      if (!on()) return; if (!ton) { const o = ac.createOscillator(), g = ac.createGain(); o.type = 'sine'; g.gain.value = 0; o.connect(g); out(g, .5); o.start(); ton = { o, g }; }
      ton.g.gain.setTargetAtTime(v, ac.currentTime, .08); if (f) ton.o.frequency.setTargetAtTime(f, ac.currentTime, .05);
    },
  };

  // ---------- ambientes (camas de sonido) ----------
  function makeBed(k) {
    const o = ac.createGain(); o.gain.value = 0; out(o, .25); const srcs = [];
    const add = n => { srcs.push(n); return n; };
    const lfo = (f, depth, param, center, type = 'sine') => { const l = add(ac.createOscillator()); l.type = type; l.frequency.value = f; const g = ac.createGain(); g.gain.value = depth; l.connect(g); g.connect(param); param.value = center; l.start(); };
    const drone = (f, v) => { const x = add(ac.createOscillator()); x.type = 'sine'; x.frequency.value = f; const g = ac.createGain(); g.gain.value = v; x.connect(g); g.connect(o); x.start(); };
    const nz = (buf, ft, f, q, v, lf, ld) => { const s = add(loopSrc(buf)), fl = flt(ft, f, q), g = ac.createGain(); g.gain.value = v; s.connect(fl); fl.connect(g); g.connect(o); if (lf) lfo(lf, ld, g.gain, v); };
    if (k === 'menu') { drone(55, .07); drone(82.5, .035); nz(nb, 'lowpass', 250, .7, .3, .06, .1); }
    if (k === 'islas') { nz(nb, 'lowpass', 520, .7, .55, .11, .35); nz(nb, 'lowpass', 950, .7, .3, .17, .2); nz(nw, 'bandpass', 500, .4, .05, .07, .04); drone(55, .04); }
    if (k === 'estrellas') {
      drone(49, .06); drone(49.7, .045); nz(nb, 'lowpass', 300, .6, .35, .05, .15);
      [[4300, 11], [4450, 13.3], [4150, 9.4], [3900, 12.1]].forEach(([f, r]) => { const x = add(ac.createOscillator()); x.type = 'sine'; x.frequency.value = f; const g = ac.createGain(); x.connect(g); g.connect(o); lfo(r, .007, g.gain, .007, 'square'); x.start(); });
    }
    if (k === 'plaza') { nz(nw, 'bandpass', 450, 1.2, .035, .4, .015); nz(nw, 'bandpass', 900, 1.5, .03, .55, .012); nz(nw, 'bandpass', 1700, 2, .018, .3, .008); nz(nb, 'lowpass', 200, .7, .25, .07, .1); drone(65, .03); }
    if (k === 'calle') { drone(50, .05); drone(100, .022); drone(150, .01); nz(nw, 'bandpass', 3100, 7, .007, 0, 0); nz(nb, 'lowpass', 170, .7, .5, .08, .2); nz(nw, 'bandpass', 700, .3, .02, .2, .01); }
    return { o, srcs };
  }
  function amb(k) {
    if (!ac || k === kind) return; kind = k; T = {};
    const old = bed; if (old) { old.o.gain.setTargetAtTime(0, ac.currentTime, .5); setTimeout(() => old.srcs.forEach(s => { try { s.stop(); } catch (e) { } }), 2600); }
    bed = null; if (k === 'none') return;
    bed = makeBed(k); bed.o.gain.setTargetAtTime(.6, ac.currentTime, .8);
  }
  // eventos aleatorios segun el planeta (pasos lejanos, voces, campanas, gaviotas, goteras)
  function tick(dt, ctx = {}) {
    if (!ac || kind === 'none') return;
    const due = (n, a, b) => { T[n] = (T[n] === undefined ? R(a, b) : T[n]) - dt; if (T[n] <= 0) { T[n] = R(a, b); return true; } return false; };
    if (kind === 'islas') { if (due('g', 7, 15)) S.gull(); if (due('c', 12, 24)) S.creak(.035); }
    if (kind === 'estrellas') { if (due('v', 14, 30)) osc('sine', 880, 300, .012, .3, 1.5, 1, R(-.8, .8)); if (due('s', 16, 30)) S.thud(.07); }
    if (kind === 'plaza') {
      if (due('st', 2.2, 5)) S.steps(Math.floor(R(3, 8)), R(.8, 1.1), .09, R(.8, 1.2), .7, R(-.8, .8));
      if (due('vo', 5, 11)) S.voice(); if (due('b', 25, 45)) S.bell(R(330, 440), .035); if (due('tk', 1, 1)) S.blip(1100, .02, .012, 'sine');
    }
    if (kind === 'calle') {
      if (due('st', 3, 7)) S.steps(Math.floor(R(4, 10)), R(.55, .7), .11, R(.7, 1), .9, R(-.9, .9));
      if (due('dr', 4, 9)) S.drip(); if (due('vo', 9, 20)) S.voice(); if (due('d', 18, 35)) S.thud(.12);
    }
    if (ctx.cor !== undefined && ctx.cor < 35) { hbT -= dt; if (hbT <= 0) { hbT = .5 + ctx.cor / 60; S.heart(.12 + (35 - ctx.cor) * .005); } }
  }
  return Object.assign({ init, amb, tick, ctx: () => ac, silence() { S.rumble(0); S.tone(0, 0); } }, S);
})();
