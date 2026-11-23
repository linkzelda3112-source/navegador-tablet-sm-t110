'use strict';
// ===== Sobrevive un año: flujo, cinematicas (tiempos del video) y minijuegos =====
let WL = {}, S = 'title', T = 0, G = null, CI = null, SEL = null, ptr = { x: 180, y: 320, down: false }, save = { clear: [0, 0, 0, 0], best: [0, 0, 0, 0] };
try { Object.assign(save, JSON.parse(localStorage.getItem('sua') || '{}')); } catch (e) { }
const persist = () => { try { localStorage.setItem('sua', JSON.stringify(save)); } catch (e) { } };
const TITLES = [['1 · LAS ISLAS', 'PROYECTO NOCTIS · OLEADA 9'], ['2 · LAS ESTRELLAS', 'PROYECTO NOCTIS · OLEADA 10'], ['3 · NADIE TIENE PRISA', 'PROYECTO NOCTIS · OLEADA IV'], ['4 · EL MÁS TRANQUILO', 'PROYECTO NOCTIS · OLEADA IV']];
const WN = ['islas', 'estrellas', 'plaza', 'calle'];
const RENDER = { islas: () => renderIslas, estrellas: () => renderEstrellas, plaza: () => renderPlaza, calle: () => renderCalle };
function scene(w, shot, t, st) { RENDER[w]()(WL[w], shot, t, st || {}); }

// ---------- audio ----------
let AU = null; try { AU = new Audio('audio.mp3'); AU.preload = 'auto'; } catch (e) { }
function playAudio(from) { try { if (AU) { AU.currentTime = from; AU.play().catch(() => { }); } } catch (e) { } }
function stopAudio() { try { if (AU) AU.pause(); } catch (e) { } }
let ac = null; function beep(f, d = .08, v = .08, ty = 'square') { try { ac = ac || new (window.AudioContext || window.webkitAudioContext)(); const o = ac.createOscillator(), g = ac.createGain(); o.type = ty; o.frequency.value = f; g.gain.value = v; o.connect(g); g.connect(ac.destination); o.start(); g.gain.exponentialRampToValueAtTime(.0001, ac.currentTime + d); o.stop(ac.currentTime + d); } catch (e) { } }

// ---------- cinematicas (segundos = los del video original) ----------
const CINE = [
  { t0: 9, t1: 54, shots: [
    { t: 9, w: 'islas', s: 'beach', title: [9, 12], subs: [[9, 'Uno:'], [10, 'el planeta de las'], [11, 'islas.'], [12, 'Te toca una isla'], [13, 'entera para ti.'], [15, 'Arena blanca,'], [16, 'agua tibia y una'], [17, 'hamaca.']] },
    { t: 18, w: 'islas', s: 'nsea', st: { night: 1 }, subs: [[18, 'Enfrente, cruzando el mar,'], [21, 'hay un pueblo.'], [22, 'De noche ves sus'], [23, 'luces.']] },
    { t: 24, w: 'islas', s: 'whirl', st: { whirl: 1 }, subs: [[24, 'Pero un día aparece'], [25, 'un remolino en medio'], [27, 'del mar.'], [28, 'No se mueve.'], [29, 'No se cierra.']] },
    { t: 31, w: 'islas', s: 'town', st: { night: 1, whirl: 1 }, subs: [[31, 'Esa noche cuentas las'], [33, 'luces del pueblo.'], [35, 'Faltan.']] },
    { t: 36, w: 'islas', s: 'pier', st: { town: false }, subs: [[36, 'A la mañana siguiente,'], [38, 'no hay pueblo.'], [40, 'Solo agua.']] },
    { t: 42, w: 'islas', s: 'under', sy: .54, subs: [[42, 'Debajo del mar,'], [43, 'algo camina por el'], [44, 'fondo.'], [45, 'Tiene muchos ojos.'], [47, 'Todos cerrados.']] },
    { t: 48, w: 'islas', s: 'eyes', st: (tt) => ({ eye: { i: 1, v: clamp(tt * .9, 0, 1) } }), sy: .54, subs: [[48, 'Todos cerrados.'], [49, 'Uno se está abriendo.']] },
    { t: 51, w: 'islas', s: 'pier', st: { town: false }, subs: [[51, 'Y tu isla…'], [52, 'también tiene costa.']] }] },
  { t0: 54, t1: 90, shots: [
    { t: 54, w: 'estrellas', s: 'wide', st: { grow: 0, lying: 0 }, title: [54, 57], subs: [[54, 'Dos:'], [55, 'el planeta de las'], [56, 'estrellas.'], [57, 'Aquí no hay ciudades'], [59, 'ni luces.'], [60, 'El cielo de noche'], [61, 'es el más lindo']] },
    { t: 63, w: 'estrellas', s: 'cot', subs: [[63, 'Tienes un telescopio y']] },
    { t: 65, w: 'estrellas', s: 'cot2', subs: [[65, 'una cama afuera.'], [66, 'Te duermes contando estrellas.']] },
    { t: 68, w: 'estrellas', s: 'sky', st: (tt) => ({ grow: clamp(tt / 7, 0, 1) }), subs: [[68, 'Pero cada noche,'], [69, 'el cielo se ve'], [70, 'más cerca.'], [71, 'Las estrellas,'], [72, 'más grandes.'], [74, 'Más juntas.']] },
    { t: 75, w: 'estrellas', s: 'look', st: { grow: 1 }, subs: [[75, 'Miras por el telescopio.']] },
    { t: 77, scope: 1, subs: [[77, 'Un grupo de estrellas']] },
    { t: 80, w: 'estrellas', s: 'giant', st: { grow: 1, giant: 1 }, subs: [[80, 'No es el cielo.'], [82, 'Es un agujero en'], [83, 'el pecho de algo'], [84, 'muy alto.'], [85, 'Lo que entra ahí'], [86, 'no vuelve.']] },
    { t: 88, w: 'estrellas', s: 'sky', st: { grow: 1 }, subs: [[88, 'Y el cielo sigue']] }] },
  { t0: 90, t1: 133, shots: [
    { t: 90, w: 'plaza', s: 'tower', title: [90, 93], st: (tt) => ({ clk: 0 }), subs: [[90, 'Tres:'], [91, 'el planeta donde nadie'], [93, 'tiene prisa.'], [94, 'Un pueblo con plaza,'], [95, 'panadería y una torre'], [97, 'con reloj.'], [98, 'Nadie corre.'], [100, 'Nadie llega tarde.'], [102, 'Los domingos duran todo'], [103, 'el día.']] },
    { t: 105, w: 'plaza', s: 'clock', st: { clk: 0 }, subs: [[105, 'Pero una mañana,'], [106, 'todos los relojes marcan'], [108, 'la misma hora.'], [110, 'El de la torre.']] },
    { t: 112, w: 'plaza', s: 'kitchen', st: { clk: 0 }, subs: [[112, 'El de tu cocina.']] },
    { t: 113, w: 'plaza', s: 'wrist', st: { clk: 0 }, subs: [[113, 'El de tu muñeca.']] },
    { t: 115, w: 'plaza', s: 'baker', st: { freeze: 1 }, subs: [[115, 'Sales a la calle.'], [118, 'El panadero está quieto,'], [119, 'con el pan en'], [120, 'el aire.'], [121, 'Una paloma,'], [122, 'a mitad de un'], [123, 'aleteo.'], [124, 'No cae.']] },
    { t: 126, w: 'plaza', s: 'tent', st: { tent: 1, freeze: 1, clk: 0 }, subs: [[126, 'En la torre,'], [127, 'el reloj tiene tentáculos.']] },
    { t: 129, w: 'plaza', s: 'foot', subs: [[130, 'Y tu pie derecho'], [131, 'ya no termina el'], [132, 'paso.']] }] },
  { t0: 133, t1: 175, shots: [
    { t: 133, w: 'calle', s: 'day', title: [133, 137], st: (tt) => ({ dusk: clamp((tt - 4) / 3, 0, .6) }), subs: [[133, 'Y cuatro:'], [135, 'el planeta más tranquilo'], [136, 'de todos.'], [137, 'No hay ruido,'], [138, 'ni tráfico,'], [139, 'ni peleas.']] },
    { t: 140, w: 'calle', s: 'day', st: (tt) => ({ dusk: clamp(.6 + tt / 3, 0, 1), night: clamp((tt - 2) / 7, 0, 1) }), subs: [[140, 'La gente duerme diez'], [142, 'horas y despierta feliz.'], [144, 'Después de los otros'], [145, 'tres, aquí por fin'], [147, 'vas a descansar.']] },
    { t: 149, w: 'calle', s: 'neigh', st: { night: 0, dusk: 0 }, subs: [[149, 'Pero tus vecinos te'], [151, 'cuentan lo que soñaron.'], [152, 'Es el mismo sueño.']] },
    { t: 154, w: 'calle', s: 'hood', st: { night: 1, hood: 1 }, subs: [[154, 'Todos sueñan lo mismo:'], [156, 'una capucha enorme sobre'], [157, 'la ciudad,'], [158, 'con tentáculos colgando.']] },
    { t: 160, w: 'calle', s: 'street', st: { dusk: 0 }, subs: [[160, 'Una mañana,'], [161, 'nadie despierta.'], [164, 'Ni tus vecinos.'], [165, 'Ni la ciudad.']] },
    { t: 167, w: 'calle', s: 'room', subs: [[167, 'Tú aguantas tres días'], [169, 'sin dormir.']] },
    { t: 170, blink: 1, sy: .54, subs: [[170, 'Cierras los ojos un'], [171, 'segundo.'], [172, 'Y ahí está la'], [173, 'capucha.']] }] },
];
function cineAt(c, tv) { let sh = c.shots[0]; for (const s of c.shots) if (tv >= s.t) sh = s; return sh; }

function startPlanet(i) { G = null; CI = { p: i, t: c0(i), start: T }; S = 'cine'; stopAudio(); playAudio(CINE[i].t0); beep(300, .12); }
const c0 = i => CINE[i].t0;
function endCine() { stopAudio(); startGame(CI.p); }

// ---------- juego ----------
const STORY = [
  [[1, 'Tu isla entera.'], [40, 'Un remolino. No se cierra.'], [90, 'Ya no hay pueblo.'], [160, 'Algo camina por el fondo.'], [260, 'Casi un año. Cuenta las luces.']],
  [[1, 'Aquí no hay luces.'], [60, 'El cielo se ve más cerca.'], [150, 'Las estrellas, más grandes.'], [250, 'Más juntas.'], [320, 'El cielo sigue.']],
  [[1, 'Nadie tiene prisa.'], [70, 'Nadie llega tarde.'], [160, 'Todos los relojes marcan la misma hora.'], [260, 'Tu pie ya no termina el paso.']],
  [[1, 'No hay ruido, ni tráfico.'], [80, 'Todos sueñan lo mismo.'], [200, 'Una capucha sobre la ciudad.'], [300, 'Tú aguantas sin dormir.']],
];
function startGame(p) { G = { p, day: 1, cor: 100, t: 0, ev: null, next: 3.5, msg: null, evn: 0, flash: 0, over: 0, lastStory: -1 }; S = 'play'; say('DÍA 1 · ' + STORY[p][0][1]); }
function say(s, d = 3) { G.msg = { s, t: d }; }
function hurt(n, why) { G.cor = Math.max(0, G.cor - n); G.flash = .4; beep(110, .18, .12, 'sawtooth'); if (why) say(why, 1.6); }
const ramp = () => G.day / 365;

// --- eventos ---
function proj2(w, shot, st, wp) { // proyecta un punto del mundo con la camara de ese shot
  const cams = { islas: ISL, estrellas: EST, plaza: PLZ, calle: CAL }[w][shot]; setCam({ pos: cams.pos, tgt: cams.tgt, fov: cams.fov }); return proj(wp);
}
const EVENTS = [
  { // ISLAS
    luces: {
      shot: 'town', st: () => ({ night: 1, whirl: G.day >= 40, lights: false, town: G.day < 90 || true }), txt: 'CUENTA LAS LUCES · TOCA CADA UNA',
      init(e) { e.L = []; const n = 5 + Math.floor(ramp() * 4); for (let i = 0; i < n; i++) { const p = proj2('islas', 'town', {}, [-8 + rnd() * 40, 1 + rnd() * 1.8, -182.9]); e.L.push({ x: p[0], y: p[1], at: .5 + i * .9, life: 2.4 - ramp() * .8, st: 0 }); } e.n = n; },
      upd(e, dt) { e.L.forEach(l => { if (l.st === 0 && e.t > l.at) l.st = 1; if (l.st === 1 && e.t > l.at + l.life) { l.st = 3; hurt(6); } if (l.st === 2 || l.st === 3) l.k = (l.k || 0) + dt; }); if (e.L.every(l => l.st >= 2 && (l.k || 0) > .3)) return 1; },
      tap(e, x, y) { for (const l of e.L) if (l.st === 1 && Math.hypot(l.x - x, l.y - y) < 36) { l.st = 2; beep(660, .08); return; } },
      draw(e) { e.L.forEach(l => { if (l.st === 1) { X.fillStyle = 'rgba(255,220,130,.3)'; X.fillRect(l.x - 14, l.y - 14, 28, 28); X.fillStyle = '#ffd98a'; X.fillRect(l.x - 6, l.y - 6, 12, 12); } if (l.st === 2) { X.fillStyle = '#fff'; X.fillRect(l.x - 8 + (l.k || 0) * 20, l.y - 8, 16 - (l.k || 0) * 40, 16); } }); txt(Math.max(0, e.L.filter(l => l.st === 2).length) + ' / ' + e.n, RW / 2, 560, 18, '#fff'); }
    },
    ojos: {
      shot: 'eyes', st: (e) => ({ eye: e.cur }), txt: 'CIERRA LOS OJOS QUE SE ABREN · TÓCALOS',
      init(e) { e.k = 0; e.n = 3 + Math.floor(ramp() * 3); e.cur = null; e.gap = .6; },
      upd(e, dt) {
        if (!e.cur) { e.gap -= dt; if (e.gap <= 0) { if (e.k >= e.n) return 1; e.cur = { i: Math.floor(rnd() * 8), v: 0, sp: 1 / Math.max(1.0, 2.3 - ramp() * 1.2) }; e.k++; } return; }
        e.cur.v += dt * e.cur.sp; if (e.cur.closing) { e.cur.v -= dt * 6; if (e.cur.v <= 0) { e.cur = null; e.gap = .5; } } else if (e.cur.v >= 1) { hurt(16, 'Uno se abrió.'); e.cur = null; e.gap = .8; G.flash = 1; }
      },
      tap(e, x, y) { if (!e.cur || e.cur.closing) return; const ey = WL.islas.eyes[e.cur.i], p = proj2('islas', 'eyes', {}, [ey.x, ey.y, ey.z - 2]); if (Math.hypot(p[0] - x, p[1] - y) < 70) { e.cur.closing = true; e.cur.sp = 0; beep(220, .1); } },
      draw(e) { txt('ojos cerrados: ' + Math.max(0, e.k - (e.cur ? 1 : 0)) + ' / ' + e.n, RW / 2, 560, 12, '#fff'); }
    }
  },
  { // ESTRELLAS
    telescopio: {
      scope: true, txt: 'MANTÉN LA MIRA EN EL GRUPO DE ESTRELLAS · ARRASTRA', init(e) { e.lock = 0; e.ret = { x: 180, y: 320 }; e.sd = []; rs(9); for (let i = 0; i < 160; i++) e.sd.push([rnd() * 360, rnd() * 640, rnd()]); e.need = 3 + ramp() * 1.5; },
      upd(e, dt) { const c = scopeC(e.t), d = Math.hypot(c.x - e.ret.x, c.y - e.ret.y); e.ret.x += (ptr.x - e.ret.x) * Math.min(1, dt * 9); e.ret.y += (ptr.y - e.ret.y) * Math.min(1, dt * 9); if (d < 50) e.lock += dt; else { G.cor -= dt * 3.5; } if (e.lock >= e.need) return 1; if (e.t > 11) { hurt(14, 'Perdiste el grupo.'); return 1; } },
      tap() { }, draw(e) { drawScope(e, scopeC(e.t)); X.strokeStyle = '#ffd98a'; X.lineWidth = 2; X.beginPath(); X.arc(e.ret.x, e.ret.y, 40, 0, PI * 2); X.moveTo(e.ret.x - 50, e.ret.y); X.lineTo(e.ret.x + 50, e.ret.y); X.moveTo(e.ret.x, e.ret.y - 50); X.lineTo(e.ret.x, e.ret.y + 50); X.stroke(); X.fillStyle = '#ffd98a'; X.fillRect(80, 520, 200 * e.lock / e.need, 8); X.strokeStyle = '#fff'; X.strokeRect(80, 520, 200, 8); }
    },
    sueno: {
      shot: 'giant', st: (e) => ({ grow: G.day / 365, giant: .55 + .45 * Math.min(1, e.t / e.dur) }), txt: 'MANTÉN PRESIONADO · CIERRA LOS OJOS',
      init(e) { e.dur = 5 + ramp() * 2; e.leak = 0; }, upd(e, dt) { if (!ptr.down) { e.leak += dt; G.cor -= dt * 14; G.flash = .15; } if (e.t >= e.dur) { if (e.leak > 1.2) say('Viste dentro del agujero.', 2); return 1; } },
      tap() { }, draw(e) { if (ptr.down) { X.fillStyle = 'rgba(0,0,0,.86)'; X.fillRect(0, 0, RW, RH); txt('(cierras los ojos)', RW / 2, 320, 14, '#8890b8'); } X.fillStyle = '#fff'; X.fillRect(80, 540, 200 * (e.t / e.dur), 6); }
    }
  },
  { // PLAZA
    paso: {
      shot: 'foot', st: () => ({}), txt: 'TOCA CUANDO EL ANILLO LLEGUE AL PIE · NADIE CORRE',
      init(e) { e.step = 0; e.need = 6 + Math.floor(ramp() * 4); e.per = 1.8; e.hit = 0; },
      upd(e, dt) { const ph = (e.t % e.per) / e.per; if (e.lph !== undefined && ph < e.lph) { if (!e.tapped) hurt(3); e.tapped = false; } e.lph = ph; if (e.step >= e.need) return 1; },
      tap(e) { const ph = (e.t % e.per) / e.per; if (Math.abs(ph - .93) < .09) { if (!e.tapped) { e.step++; e.tapped = true; beep(180 + e.step * 20, .1); e.hit = .3; } } else { hurt(5, '¡Nadie corre!'); e.tapped = true; } },
      draw(e) { const ph = (e.t % e.per) / e.per, r = lerp(110, 30, Math.min(1, ph / .93)); X.strokeStyle = ph > .84 && ph < 1 ? '#9f9' : '#fff'; X.lineWidth = 3; X.beginPath(); X.arc(180, 380, r, 0, PI * 2); X.stroke(); X.strokeStyle = 'rgba(255,255,255,.5)'; X.beginPath(); X.arc(180, 380, 30, 0, PI * 2); X.stroke(); txt('pasos ' + e.step + ' / ' + e.need, RW / 2, 560, 14, '#fff'); }
    },
    congelados: {
      shot: 'baker', st: () => ({ freeze: 1 }), txt: 'EMPUJA CADA COSA CONGELADA · TOCA VARIAS VECES',
      init(e) { e.T = [[1, 1.4, -4], [-6, 7, -6], [1.8, 2.2, -3.4]].map(w => { const p = proj2('plaza', 'baker', {}, w); return { x: p[0], y: p[1], k: 0 }; }); e.lim = 8 - ramp() * 2; },
      upd(e, dt) { if (e.T.every(t => t.k >= 4)) return 1; if (e.t > e.lim) { hurt(16, 'El tiempo no vuelve.'); return 1; } },
      tap(e, x, y) { for (const t of e.T) if (t.k < 4 && Math.hypot(t.x - x, t.y - y) < 54) { t.k++; beep(300 + t.k * 80, .06); return; } },
      draw(e) { e.T.forEach(t => { X.strokeStyle = 'rgba(255,255,255,.45)'; X.lineWidth = 2; X.beginPath(); X.arc(t.x, t.y, 30, 0, PI * 2); X.stroke(); X.strokeStyle = t.k >= 4 ? '#9f9' : '#fff'; X.lineWidth = 2; X.beginPath(); X.arc(t.x, t.y, 30, 0, -PI * 2 * (t.k / 4) - .001, true); X.stroke(); X.fillStyle = 'rgba(255,255,255,.4)'; X.fillRect(t.x - 3, t.y - 3, 6, 6); }); X.fillStyle = '#fff'; X.fillRect(60, 600, 240 * (1 - e.t / e.lim), 5); }
    }
  },
  { // CALLE
    despierto: {
      shot: 'room', st: () => ({}), txt: 'NO TE DUERMAS · TOCA SIN PARAR',
      init(e) { e.aw = 88; e.dur = 8; }, upd(e, dt) { e.aw -= dt * (22 + ramp() * 14); if (e.aw <= 0) { hurt(22, 'Cierras los ojos un segundo.'); return 1; } if (e.t >= e.dur) return 1; },
      tap(e) { e.aw = Math.min(100, e.aw + 13); beep(500, .03, .04); },
      draw(e) { const c = (1 - e.aw / 100) * .5 * RH; X.fillStyle = '#000'; X.fillRect(0, 0, RW, c); X.fillRect(0, RH - c, RW, c); X.fillStyle = '#9fd'; X.fillRect(60, 580, 240 * e.aw / 100, 6); X.strokeStyle = '#fff'; X.strokeRect(60, 580, 240, 6); }
    },
    tentaculos: {
      shot: 'hood', st: () => ({ night: 1, hood: .8 }), txt: 'CORTA LOS TENTÁCULOS ANTES DE QUE BAJEN',
      init(e) { e.n = 5 + Math.floor(ramp() * 3); e.L = []; for (let i = 0; i < e.n; i++) e.L.push({ x: 40 + rnd() * 280, y: 0, sp: 50 + rnd() * 40 + ramp() * 50, at: i * .8, st: 0 }); },
      upd(e, dt) { e.L.forEach(l => { if (l.st === 0 && e.t > l.at) l.st = 1; if (l.st === 1) { l.y += l.sp * dt; if (l.y > 520) { l.st = 3; hurt(8); } } if (l.st === 2) { l.y -= 400 * dt; if (l.y < 0) l.st = 4; } }); if (e.L.every(l => l.st >= 3 && l.st !== 2 || l.st === 4)) return 1; },
      tap(e, x, y) { for (const l of e.L) if (l.st === 1 && Math.abs(l.x - x) < 36 && y < l.y + 36) { l.st = 2; beep(400, .06); return; } },
      draw(e) { e.L.forEach(l => { if (l.st === 1 || l.st === 2) { X.strokeStyle = '#1a2a4a'; X.lineWidth = 10; X.beginPath(); for (let y = 0; y <= l.y; y += 12) X.lineTo(l.x + Math.sin(y * .05 + e.t * 2) * 8, y); X.stroke(); X.fillStyle = '#2d4a7a'; X.fillRect(l.x - 7 + Math.sin(l.y * .05 + e.t * 2) * 8, l.y - 4, 14, 8); } }); }
    }
  }
];
function scopeC(t) { return { x: 180 + Math.sin(t * .9) * 80 + Math.sin(t * 2.1) * 20, y: 320 + Math.sin(t * .6 + 1) * 110 + Math.cos(t * 1.7) * 16 }; }
function drawScope(e, c) {
  X.fillStyle = '#000'; X.fillRect(0, 0, RW, RH); X.save(); X.beginPath(); X.arc(180, 320, 160, 0, PI * 2); X.clip(); const gr = X.createRadialGradient(180, 320, 20, 180, 320, 160); gr.addColorStop(0, '#6a70a8'); gr.addColorStop(1, '#343a68'); X.fillStyle = gr; X.fillRect(0, 0, RW, RH);
  e.sd.forEach(s => { const x = (s[0] + e.t * 4 * (s[2] + .2)) % 360, y = s[1] * .6 + 110; X.fillStyle = '#fff'; X.fillRect(x, y, s[2] > .9 ? 3 : 2, s[2] > .9 ? 3 : 2); });
  for (let i = 0; i < 7; i++) { X.fillStyle = '#fff'; X.fillRect(c.x + [0, 8, 14, -8, 4, -14, 12][i] - 3, c.y + [0, 6, -4, 10, -12, 0, 12][i] - 3, 6, 6); }
  X.restore();
}

function pickEvent() { const L = Object.keys(EVENTS[G.p]); return EVENTS[G.p][L[G.evn++ % L.length]]; }
function idleView() {
  const d = G.day, p = G.p;
  if (p === 0) { const n = (d % 6) >= 3 ? 1 : 0; return n ? ['nsea', { night: 1, whirl: d >= 40, town: d < 90 }] : (d >= 40 && d % 2 ? ['whirl', { whirl: 1, town: d < 90 }] : ['beach', { whirl: d >= 40, town: d < 90 }]); }
  if (p === 1) return ['wide', { grow: d / 365, lying: d % 2 }];
  if (p === 2) return [d > 260 ? 'tent' : 'tower', { clk: d * .35, tent: d > 260, freeze: d > 260 }];
  const n = (d % 8) >= 4 ? 1 : 0; return ['day', { night: n, dusk: n ? 1 : ((d % 8) === 3 ? 1 : 0), hood: d > 250 ? .3 : undefined }];
}

function update(dt) {
  T += dt;
  if (S === 'cine') { CI.t = CINE[CI.p].t0 + (T - CI.start); if (CI.t >= CINE[CI.p].t1) endCine(); }
  if (S === 'select') { SEL.t += dt; if (SEL.pick === null && SEL.t > 9) SEL.pick = Math.max(0, save.clear.indexOf(0)), SEL.pt = 0; if (SEL.pick !== null) { SEL.pt += dt; SEL.zoom = Math.min(1, SEL.pt / 1.1); if (SEL.pt > 1.2) startPlanet(SEL.pick); } else SEL.zoom = Math.max(0, (SEL.t - 3) / 8); }
  if (S === 'play') {
    const g = G; g.t += dt; g.flash = Math.max(0, g.flash - dt); if (g.msg) { g.msg.t -= dt; if (g.msg.t <= 0) g.msg = null; }
    if (g.over) { g.over += dt; return; }
    if (g.ev) { g.ev.t += dt; const r = g.ev.def.upd(g.ev, dt); if (r) { g.ev = null; g.next = 2.5 - ramp(); } }
    else { g.day += dt * 4.2; g.next -= dt; for (const [d, s] of STORY[g.p]) if (g.day >= d && g.lastStory < d) { g.lastStory = d; say('DÍA ' + d + ' · ' + s, 3.5); } if (g.next <= 0 && g.day < 364) { const def = pickEvent(); g.ev = { def, t: 0 }; if (def.init) def.init(g.ev); g.next = 99; } }
    if (g.cor <= 0) { g.over = .001; g.lost = true; stopAudio(); beep(80, .6, .15, 'sawtooth'); }
    else if (g.day >= 365) { g.over = .001; g.lost = false; save.clear[g.p] = 1; persist(); beep(880, .4, .1); }
    save.best[g.p] = Math.max(save.best[g.p] || 0, Math.floor(g.day)); }
}

// ---------- dibujo ----------
function drawHUD() {
  const g = G; X.fillStyle = 'rgba(0,0,0,.6)'; X.fillRect(6, 6, 116, 20); txt('DÍA ' + Math.floor(g.day) + '/365', 12, 17, 12, '#fff', 'left');
  X.fillStyle = 'rgba(0,0,0,.6)'; X.fillRect(RW - 122, 6, 116, 20); X.fillStyle = g.cor > 35 ? '#3fb892' : '#e04a4a'; X.fillRect(RW - 118, 12, 108 * g.cor / 100, 8); X.strokeStyle = '#fff'; X.lineWidth = 1; X.strokeRect(RW - 118.5, 11.5, 108, 8); txt('CORDURA', RW - 64, 17, 7, '#fff', 'center', PIX, false);
}
function render() {
  X.clearRect(0, 0, RW, RH);
  if (S === 'title' || S === 'select') {
    renderPlanets(T, S === 'select' ? SEL.zoom : .0, S === 'select' ? SEL.pick : -1);
    if (S === 'title') { X.fillStyle = 'rgba(8,6,20,.55)'; X.fillRect(0, 0, RW, RH); txt('SOBREVIVE', RW / 2, 190, 26, '#fff', 'center', PIX, false); txt('UN AÑO', RW / 2, 232, 26, '#fff', 'center', PIX, false); txt('PROYECTO NOCTIS', RW / 2, 280, 9, '#cfc8ff', 'center', PIX, false); if (Math.floor(T * 2) % 2) txt('TOCA PARA EMPEZAR', RW / 2, 470, 11, '#fff', 'center', PIX, false); txt('4 planetas · 365 días cada uno', RW / 2, 560, 11, '#9a96c8'); return; }
    X.font = '19px ' + PIX; let w = X.measureText('ELIGE UN PLANETA').width + 20; X.fillStyle = 'rgba(14,14,22,.88)'; X.fillRect(RW / 2 - w / 2, 58, w, 32); txt('ELIGE UN PLANETA', RW / 2, 75, 19, '#fff', 'center', PIX, false);
    X.font = '9px ' + PIX; w = X.measureText('SOBREVIVE UN AÑO').width + 12; X.fillStyle = 'rgba(14,14,22,.8)'; X.fillRect(RW / 2 - w / 2, 96, w, 17); txt('SOBREVIVE UN AÑO', RW / 2, 105, 9, '#e8e8f0', 'center', PIX, false);
    PLC.forEach(([x, y], i) => { txt(String(i + 1), x - 80, y - 60 + (i < 2 ? 0 : 0), 14, '#fff', 'center', PIX, false); if (save.clear[i]) txt('★', x + 56, y - 56, 16, '#ffe27a'); else if (save.best[i]) txt('día ' + save.best[i], x, y + 78, 10, '#e0dcff'); });
    const cd = Math.ceil(9 - SEL.t); if (SEL.t > 3.5 && SEL.pick === null && cd > 0) txt(String(Math.max(1, Math.min(5, cd))), RW / 2, 570, 34, '#fff', 'center', PIX, false);
    return;
  }
  if (S === 'cine') {
    const c = CINE[CI.p], sh = cineAt(c, CI.t), tt = CI.t - sh.t; let st = typeof sh.st === 'function' ? sh.st(tt) : (sh.st || {});
    if (sh.blink) { const k = CI.t - 170; clear([.17, .2, .33, 1]); X.fillStyle = k < 1 ? '#232a4e' : '#bba9de'; X.fillRect(0, 0, RW, RH); if (k >= 2) { X.save(); for (let i = 0; i < 60; i++) { rs(i + 3); X.fillStyle = `rgba(240,230,255,${.3 + rnd() * .5})`; X.fillRect(rnd() * 360, rnd() * 640, 20 + rnd() * 60, 14 + rnd() * 30); } X.restore(); } }
    else if (sh.scope) { clear([0, 0, 0, 1]); drawScope({ sd: SCOPEDATA(), t: CI.t }, { x: 200 + Math.sin(CI.t) * 4, y: 360 }); }
    else scene(sh.w, sh.s, T, st);
    if (sh.title && CI.t >= sh.title[0] && CI.t < sh.title[1]) title(TITLES[CI.p][0], TITLES[CI.p][1]);
    let line = ''; for (const s of sh.subs) if (CI.t >= s[0]) line = s[1]; sub(line, sh.sy || .67);
    txt('SALTAR ▸▸', RW - 8, RH - 12, 10, 'rgba(255,255,255,.55)', 'right'); return;
  }
  if (S === 'play') {
    const g = G; let shot, st, w = WN[g.p];
    if (g.ev && g.ev.def.scope) { drawScope(g.ev, scopeC(g.ev.t)); } else {
      if (g.ev) { shot = g.ev.def.shot; st = g.ev.def.st(g.ev); } else [shot, st] = idleView();
      scene(w, shot, T, st);
    }
    if (g.ev) { g.ev.def.draw(g.ev); X.font = 'bold 11px ' + MONO; const tx = g.ev.def.txt, ww = X.measureText(tx).width + 12; X.fillStyle = 'rgba(0,0,0,.72)'; X.fillRect(RW / 2 - ww / 2, 606, ww, 18); txt(tx, RW / 2, 615, 11, '#fff'); }
    if (g.msg) sub(g.msg.s, .8);
    if (g.flash > 0) { X.fillStyle = `rgba(255,255,255,${Math.min(.6, g.flash)})`; X.fillRect(0, 0, RW, RH); }
    drawHUD();
    if (g.over) { X.fillStyle = `rgba(0,0,0,${Math.min(.85, g.over)})`; X.fillRect(0, 0, RW, RH); if (g.over > .6) { txt(g.lost ? 'TE QUEDASTE AQUÍ' : 'SOBREVIVISTE UN AÑO', RW / 2, 270, g.lost ? 15 : 14, g.lost ? '#e05a5a' : '#8fd6c0', 'center', PIX, false); txt('Día ' + Math.floor(g.day) + ' de 365', RW / 2, 320, 13, '#fff'); txt(TITLES[g.p][0], RW / 2, 350, 11, '#bbb'); if (g.over > 1.5) txt('TOCA PARA VOLVER', RW / 2, 450, 11, '#fff', 'center', PIX, false); } }
  }
}
let _sd = null; function SCOPEDATA() { if (!_sd) { rs(9); _sd = []; for (let i = 0; i < 160; i++) _sd.push([rnd() * 360, rnd() * 640, rnd()]); } return _sd; }

// ---------- entrada ----------
function pos(e) { const r = ui.getBoundingClientRect(); return { x: (e.clientX - r.left) * RW / r.width, y: (e.clientY - r.top) * RH / r.height }; }
ui.addEventListener('pointerdown', e => {
  e.preventDefault(); const p = pos(e); ptr = { x: p.x, y: p.y, down: true };
  if (S === 'title') { S = 'select'; SEL = { t: 0, pick: null, zoom: 0, pt: 0 }; playAudio(0); beep(440, .1); return; }
  if (S === 'select') { if (SEL.pick !== null) return; PLC.forEach(([x, y], i) => { if (Math.hypot(x - p.x, y - p.y) < 80) { SEL.pick = i; SEL.pt = 0; stopAudio(); beep(520, .1); } }); return; }
  if (S === 'cine') { if (p.x > RW - 110 && p.y > RH - 40) { endCine(); } return; }
  if (S === 'play') { if (G.over > 1.5) { stopAudio(); S = 'select'; SEL = { t: 0, pick: null, zoom: 0, pt: 0 }; playAudio(0); return; } if (G.ev && !G.over) G.ev.def.tap(G.ev, p.x, p.y); }
});
ui.addEventListener('pointermove', e => { const p = pos(e); ptr.x = p.x; ptr.y = p.y; });
addEventListener('pointerup', () => { ptr.down = false; }); addEventListener('pointercancel', () => { ptr.down = false; });

// ---------- arranque ----------
function boot() {
  initTex(); initPlanets(); WL.islas = worldIslas(); WL.estrellas = worldEstrellas(); WL.plaza = worldPlaza(); WL.calle = worldCalle();
  let last = performance.now();
  function loop(now) { const dt = Math.min(.05, (now - last) / 1000); last = now; update(dt); render(); requestAnimationFrame(loop); }
  requestAnimationFrame(loop);
}
window.dbg = { // para pruebas
  cine(i, t) { S = 'cine'; CI = { p: i, t, start: T - (t - CINE[i].t0) }; }, game(p, day) { startGame(p); G.day = day; }, ev(k) { const def = EVENTS[G.p][k]; G.ev = { def, t: 0 }; def.init && def.init(G.ev); },
  sel(t) { S = 'select'; SEL = { t, pick: null, zoom: 0, pt: 0 }; }, get S() { return S; }, get G() { return G; }
};
boot();
