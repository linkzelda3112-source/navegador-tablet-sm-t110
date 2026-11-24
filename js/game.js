'use strict';
// ===== Sobrevive un año: flujo, cinematicas (tiempos del video) y minijuegos =====
let WL = {}, S = 'boot', T = 0, G = null, CI = null, SEL = null, ptr = { x: 180, y: 320, down: false }, save = { clear: [0, 0, 0, 0], best: [0, 0, 0, 0] };
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
function beep(f, d, v, ty) { SND.blip(f, d, v, ty); }

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

function startPlanet(i) { SND.amb('none'); SND.silence(); G = null; CI = { p: i, t: c0(i), start: T }; S = 'cine'; stopAudio(); playAudio(CINE[i].t0); beep(300, .12); }
const c0 = i => CINE[i].t0;
function endCine() { stopAudio(); if (CI.test) { S = 'test'; SND.amb('menu'); return; } enterDay(); }

// ---------- juego ----------
const STORY = [
  [[1, 'Tu isla entera.'], [40, 'Un remolino. No se cierra.'], [90, 'Ya no hay pueblo.'], [160, 'Algo camina por el fondo.'], [260, 'Casi un año. Cuenta las luces.']],
  [[1, 'Aquí no hay luces.'], [60, 'El cielo se ve más cerca.'], [150, 'Las estrellas, más grandes.'], [250, 'Más juntas.'], [320, 'El cielo sigue.']],
  [[1, 'Nadie tiene prisa.'], [70, 'Nadie llega tarde.'], [160, 'Todos los relojes marcan la misma hora.'], [260, 'Tu pie ya no termina el paso.']],
  [[1, 'No hay ruido, ni tráfico.'], [80, 'Todos sueñan lo mismo.'], [200, 'Una capucha sobre la ciudad.'], [300, 'Tú aguantas sin dormir.']],
];
function say(s, d = 3) { G.msg = { s, t: d }; }
function hurt(n, why) { G.cor = Math.max(0, G.cor - n); G.flash = .4; SND.hurt(); if (why) say(why, 1.6); }
const ramp = () => G.day / 365;

// --- eventos ---
function proj2(w, shot, st, wp) { // proyecta un punto del mundo con la camara de ese shot
  const cams = { islas: ISL, estrellas: EST, plaza: PLZ, calle: CAL }[w][shot]; setCam({ pos: cams.pos, tgt: cams.tgt, fov: cams.fov }); return proj(wp);
}
const EVENTS = [
  { // ISLAS
    luces: {
      shot: 'town', st: () => ({ night: 1, whirl: G.day >= 40, lights: false, town: G.day < 90 || true }), txt: 'CUENTA LAS LUCES · TOCA CADA UNA',
      init(e) { e.L = []; const n = 4 + Math.floor(ramp() * 3); for (let i = 0; i < n; i++) { const p = proj2('islas', 'town', {}, [-8 + rnd() * 40, 1 + rnd() * 1.8, -182.9]); e.L.push({ x: p[0], y: p[1], at: .4 + i * .6, life: 1.9 - ramp() * .6, st: 0 }); } e.n = n; },
      upd(e, dt) { e.L.forEach(l => { if (l.st === 0 && e.t > l.at) l.st = 1; if (l.st === 1 && e.t > l.at + l.life) { l.st = 3; hurt(6); } if (l.st === 2 || l.st === 3) l.k = (l.k || 0) + dt; }); if (e.L.every(l => l.st >= 2 && (l.k || 0) > .3)) return 1; },
      tap(e, x, y) { for (const l of e.L) if (l.st === 1 && Math.hypot(l.x - x, l.y - y) < 36) { l.st = 2; SND.chime(true); return; } },
      draw(e) { e.L.forEach(l => { if (l.st === 1) { X.fillStyle = 'rgba(255,220,130,.3)'; X.fillRect(l.x - 14, l.y - 14, 28, 28); X.fillStyle = '#ffd98a'; X.fillRect(l.x - 6, l.y - 6, 12, 12); } if (l.st === 2) { X.fillStyle = '#fff'; X.fillRect(l.x - 8 + (l.k || 0) * 20, l.y - 8, 16 - (l.k || 0) * 40, 16); } }); txt(Math.max(0, e.L.filter(l => l.st === 2).length) + ' / ' + e.n, RW / 2, 560, 18, '#fff'); }
    },
    ojos: {
      shot: 'eyes', st: (e) => ({ eye: e.cur }), txt: 'CIERRA LOS OJOS QUE SE ABREN · TÓCALOS',
      init(e) { e.k = 0; e.n = 3 + Math.floor(ramp() * 2); e.cur = null; e.gap = .4; },
      upd(e, dt) {
        if (!e.cur) { e.gap -= dt; if (e.gap <= 0) { if (e.k >= e.n) return 1; e.cur = { i: Math.floor(rnd() * 8), v: 0, sp: 1 / Math.max(.9, 1.8 - ramp() * .9) }; e.k++; SND.creak(.05); } return; }
        e.cur.v += dt * e.cur.sp; if (e.cur.closing) { e.cur.v -= dt * 6; if (e.cur.v <= 0) { e.cur = null; e.gap = .5; } } else if (e.cur.v >= 1) { hurt(16, 'Uno se abrió.'); e.cur = null; e.gap = .8; G.flash = 1; }
      },
      tap(e, x, y) { if (!e.cur || e.cur.closing) return; const ey = WL.islas.eyes[e.cur.i], p = proj2('islas', 'eyes', {}, [ey.x, ey.y, ey.z - 2]); if (Math.hypot(p[0] - x, p[1] - y) < 70) { e.cur.closing = true; e.cur.sp = 0; beep(220, .1); } },
      draw(e) { txt('ojos cerrados: ' + Math.max(0, e.k - (e.cur ? 1 : 0)) + ' / ' + e.n, RW / 2, 560, 12, '#fff'); }
    }
  },
  { // ESTRELLAS
    telescopio: {
      scope: true, txt: 'MANTÉN LA MIRA EN EL GRUPO DE ESTRELLAS · ARRASTRA', init(e) { e.lock = 0; e.ret = { x: 180, y: 320 }; e.sd = []; rs(9); for (let i = 0; i < 160; i++) e.sd.push([rnd() * 360, rnd() * 640, rnd()]); e.need = 2 + ramp(); },
      upd(e, dt) { const c = scopeC(e.t), d = Math.hypot(c.x - e.ret.x, c.y - e.ret.y); SND.tone(260 + e.lock / e.need * 500, d < 50 ? .03 : 0); e.ret.x += (ptr.x - e.ret.x) * Math.min(1, dt * 9); e.ret.y += (ptr.y - e.ret.y) * Math.min(1, dt * 9); if (d < 50) e.lock += dt; else { G.cor -= dt * 3.5; } if (e.lock >= e.need) return 1; if (e.t > 8) { hurt(14, 'Perdiste el grupo.'); return 1; } },
      tap() { }, draw(e) { drawScope(e, scopeC(e.t)); X.strokeStyle = '#ffd98a'; X.lineWidth = 2; X.beginPath(); X.arc(e.ret.x, e.ret.y, 40, 0, PI * 2); X.moveTo(e.ret.x - 50, e.ret.y); X.lineTo(e.ret.x + 50, e.ret.y); X.moveTo(e.ret.x, e.ret.y - 50); X.lineTo(e.ret.x, e.ret.y + 50); X.stroke(); X.fillStyle = '#ffd98a'; X.fillRect(80, 520, 200 * e.lock / e.need, 8); X.strokeStyle = '#fff'; X.strokeRect(80, 520, 200, 8); }
    },
    sueno: {
      shot: 'giant', st: (e) => ({ grow: G.day / 365, giant: .55 + .45 * Math.min(1, e.t / e.dur) }), txt: 'MANTÉN PRESIONADO · CIERRA LOS OJOS',
      init(e) { e.dur = 3.5 + ramp() * 1.5; e.leak = 0; }, upd(e, dt) { SND.rumble(.06 + .25 * (e.t / e.dur) * (ptr.down ? .4 : 1)); if (!ptr.down) { e.leak += dt; G.cor -= dt * 14; G.flash = .15; } if (e.t >= e.dur) { if (e.leak > 1.2) say('Viste dentro del agujero.', 2); return 1; } },
      tap() { }, draw(e) { if (ptr.down) { X.fillStyle = 'rgba(0,0,0,.86)'; X.fillRect(0, 0, RW, RH); txt('(cierras los ojos)', RW / 2, 320, 14, '#8890b8'); } X.fillStyle = '#fff'; X.fillRect(80, 540, 200 * (e.t / e.dur), 6); }
    }
  },
  { // PLAZA
    paso: {
      shot: 'foot', st: () => ({}), txt: 'TOCA CUANDO EL ANILLO LLEGUE AL PIE · NADIE CORRE',
      init(e) { e.step = 0; e.need = 4 + Math.floor(ramp() * 3); e.per = 1.4; e.hit = 0; },
      upd(e, dt) { const ph = (e.t % e.per) / e.per; if (e.lph !== undefined && ph < e.lph) { if (!e.tapped) hurt(3); e.tapped = false; } if (e.lph !== undefined && e.lph < .93 && ph >= .93) SND.blip(1000, .03, .02, 'sine'); e.lph = ph; if (e.step >= e.need) return 1; },
      tap(e) { const ph = (e.t % e.per) / e.per; if (Math.abs(ph - .93) < .09) { if (!e.tapped) { e.step++; e.tapped = true; SND.step(.3, 1, .8); e.hit = .3; } } else { hurt(5, '¡Nadie corre!'); e.tapped = true; } },
      draw(e) { const ph = (e.t % e.per) / e.per, r = lerp(110, 30, Math.min(1, ph / .93)); X.strokeStyle = ph > .84 && ph < 1 ? '#9f9' : '#fff'; X.lineWidth = 3; X.beginPath(); X.arc(180, 380, r, 0, PI * 2); X.stroke(); X.strokeStyle = 'rgba(255,255,255,.5)'; X.beginPath(); X.arc(180, 380, 30, 0, PI * 2); X.stroke(); txt('pasos ' + e.step + ' / ' + e.need, RW / 2, 560, 14, '#fff'); }
    },
    congelados: {
      shot: 'baker', st: () => ({ freeze: 1 }), txt: 'EMPUJA CADA COSA CONGELADA · TOCA VARIAS VECES',
      init(e) { e.T = [[1, 1.4, -4], [-6, 7, -6], [1.8, 2.2, -3.4]].map(w => { const p = proj2('plaza', 'baker', {}, w); return { x: p[0], y: p[1], k: 0 }; }); e.lim = 6 - ramp() * 1.5; },
      upd(e, dt) { if (e.T.every(t => t.k >= 4)) return 1; if (e.t > e.lim) { hurt(16, 'El tiempo no vuelve.'); return 1; } },
      tap(e, x, y) { for (const t of e.T) if (t.k < 4 && Math.hypot(t.x - x, t.y - y) < 54) { t.k++; beep(300 + t.k * 80, .06); return; } },
      draw(e) { e.T.forEach(t => { X.strokeStyle = 'rgba(255,255,255,.45)'; X.lineWidth = 2; X.beginPath(); X.arc(t.x, t.y, 30, 0, PI * 2); X.stroke(); X.strokeStyle = t.k >= 4 ? '#9f9' : '#fff'; X.lineWidth = 2; X.beginPath(); X.arc(t.x, t.y, 30, 0, -PI * 2 * (t.k / 4) - .001, true); X.stroke(); X.fillStyle = 'rgba(255,255,255,.4)'; X.fillRect(t.x - 3, t.y - 3, 6, 6); }); X.fillStyle = '#fff'; X.fillRect(60, 600, 240 * (1 - e.t / e.lim), 5); }
    }
  },
  { // CALLE
    despierto: {
      shot: 'room', st: () => ({}), txt: 'NO TE DUERMAS · TOCA SIN PARAR',
      init(e) { e.aw = 88; e.dur = 5; }, upd(e, dt) { e.hb = (e.hb || 0) - dt; if (e.hb <= 0) { e.hb = .35 + e.aw / 150; SND.heart(.08 + (100 - e.aw) * .003); } e.aw -= dt * (22 + ramp() * 14); if (e.aw <= 0) { hurt(22, 'Cierras los ojos un segundo.'); return 1; } if (e.t >= e.dur) return 1; },
      tap(e) { e.aw = Math.min(100, e.aw + 13); beep(500, .03, .04); },
      draw(e) { const c = (1 - e.aw / 100) * .5 * RH; X.fillStyle = '#000'; X.fillRect(0, 0, RW, c); X.fillRect(0, RH - c, RW, c); X.fillStyle = '#9fd'; X.fillRect(60, 580, 240 * e.aw / 100, 6); X.strokeStyle = '#fff'; X.strokeRect(60, 580, 240, 6); }
    },
    tentaculos: {
      shot: 'hood', st: () => ({ night: 1, hood: .8 }), txt: 'CORTA LOS TENTÁCULOS ANTES DE QUE BAJEN',
      init(e) { e.n = 4 + Math.floor(ramp() * 3); e.L = []; for (let i = 0; i < e.n; i++) e.L.push({ x: 40 + rnd() * 280, y: 0, sp: 70 + rnd() * 50 + ramp() * 50, at: i * .55, st: 0 }); },
      upd(e, dt) { e.L.forEach(l => { if (l.st === 0 && e.t > l.at) { l.st = 1; SND.step(.1, .5, .9); } if (l.st === 1) { l.y += l.sp * dt; if (l.y > 520) { l.st = 3; hurt(8); } } if (l.st === 2) { l.y -= 400 * dt; if (l.y < 0) l.st = 4; } }); if (e.L.every(l => l.st >= 3 && l.st !== 2 || l.st === 4)) return 1; },
      tap(e, x, y) { for (const l of e.L) if (l.st === 1 && Math.abs(l.x - x) < 36 && y < l.y + 36) { l.st = 2; beep(400, .06); return; } },
      draw(e) { e.L.forEach(l => { if (l.st === 1 || l.st === 2) { X.strokeStyle = '#1a2a4a'; X.lineWidth = 10; X.beginPath(); for (let y = 0; y <= l.y; y += 12) X.lineTo(l.x + Math.sin(y * .05 + e.t * 2) * 8, y); X.stroke(); X.fillStyle = '#2d4a7a'; X.fillRect(l.x - 7 + Math.sin(l.y * .05 + e.t * 2) * 8, l.y - 4, 14, 8); } }); }
    }
  }
];
// ---------- minijuegos extra (4 por planeta) ----------
const RR = (a, b) => a + Math.random() * (b - a);
function drawClock(x, y, r, t) {
  X.fillStyle = '#e8e2cf'; X.beginPath(); X.arc(x, y, r, 0, PI * 2); X.fill(); X.strokeStyle = '#2a1c12'; X.lineWidth = 4; X.stroke();
  for (let i = 0; i < 12; i++) { const a = i / 12 * PI * 2; X.fillStyle = '#222'; X.fillRect(x + Math.sin(a) * r * .8 - 1, y - Math.cos(a) * r * .8 - 1, 3, 3); }
  const hm = t % 60 / 60 * PI * 2, hh = (t / 60 % 12) / 12 * PI * 2; X.strokeStyle = '#111'; X.lineWidth = 3; X.beginPath(); X.moveTo(x, y); X.lineTo(x + Math.sin(hm) * r * .72, y - Math.cos(hm) * r * .72); X.stroke(); X.lineWidth = 4; X.beginPath(); X.moveTo(x, y); X.lineTo(x + Math.sin(hh) * r * .48, y - Math.cos(hh) * r * .48); X.stroke();
}
const bar = (v, y = 560, col = '#7fe0d0') => { X.fillStyle = 'rgba(0,0,0,.55)'; X.fillRect(60, y, 240, 10); X.fillStyle = col; X.fillRect(60, y, 240 * clamp(v, 0, 1), 10); X.strokeStyle = '#fff'; X.lineWidth = 1; X.strokeRect(60.5, y + .5, 240, 10); };
Object.assign(EVENTS[0], {
  remolino: {
    shot: 'whirl', st: () => ({ whirl: 1, town: G.day < 90 }), txt: '¡REMA! TOCA RÁPIDO PARA ALEJARTE',
    init(e) { e.v = .35; },
    upd(e, dt) { e.v -= dt * (.32 + ramp() * .15); SND.rumble(.04 + (1 - e.v) * .12); if (e.v >= 1) return 1; if (e.v <= 0 || e.t > 7) { hurt(15, 'El remolino te jala.'); return 1; } },
    tap(e) { e.v += .09; SND.step(.07, .6, .9); },
    draw(e) { bar(e.v); X.fillStyle = 'rgba(8,30,30,.45)'; const k = 1 - clamp(e.v, 0, 1); X.beginPath(); X.ellipse(180, 330, 40 + k * 150, 8 + k * 40, 0, 0, PI * 2); X.fill(); }
  },
  botellas: {
    shot: 'pier', st: () => ({ town: G.day < 90 }), txt: 'ATRAPA LAS BOTELLAS · TÓCALAS',
    init(e) { e.B = []; for (let i = 0; i < 5; i++) e.B.push({ x: -20, at: i * .55, sp: 100 + RR(0, 60) + ramp() * 40, y: 330 + RR(0, 170), st: 0, ph: RR(0, 6) }); },
    upd(e, dt) { e.B.forEach(b => { if (e.t > b.at && b.st === 0) { b.x += b.sp * dt; if (b.x > 380) { b.st = 2; hurt(3); } } }); if (e.B.every(b => b.st > 0)) return 1; },
    tap(e, x, y) { for (const b of e.B) if (b.st === 0 && e.t > b.at && Math.hypot(b.x - x, b.y - y) < 40) { b.st = 1; SND.chime(true); return; } },
    draw(e) { e.B.forEach(b => { if (b.st === 0 && e.t > b.at) { const yy = b.y + Math.sin(e.t * 3 + b.ph) * 3; X.fillStyle = '#4fa86a'; X.fillRect(b.x - 4, yy - 8, 8, 16); X.fillRect(b.x - 2, yy - 13, 4, 6); X.fillStyle = '#f2f2e0'; X.fillRect(b.x - 2, yy - 3, 4, 6); } }); txt(e.B.filter(b => b.st === 1).length + ' / 5', RW / 2, 580, 14, '#fff'); }
  }
});
Object.assign(EVENTS[1], {
  constelacion: {
    shot: 'sky', st: () => ({ grow: G.day / 365 }), txt: 'UNE LAS ESTRELLAS EN ORDEN · 1, 2, 3…',
    init(e) { e.P = []; let tries = 0; while (e.P.length < 5 && tries++ < 200) { const p = { x: RR(50, 310), y: RR(150, 440) }; if (e.P.every(q => Math.hypot(q.x - p.x, q.y - p.y) > 70)) e.P.push(p); } e.k = 0; },
    upd(e) { if (e.k >= e.P.length) return 1; if (e.t > 8) { hurt(10, 'Se perdieron.'); return 1; } },
    tap(e, x, y) { for (let i = 0; i < e.P.length; i++) if (Math.hypot(e.P[i].x - x, e.P[i].y - y) < 34) { if (i === e.k) { e.k++; SND.blip(400 + e.k * 90, .15, .05, 'sine'); } else if (i > e.k) hurt(3); return; } },
    draw(e) { X.strokeStyle = 'rgba(255,255,255,.7)'; X.lineWidth = 2; X.beginPath(); for (let i = 0; i < e.k; i++) { const p = e.P[i]; i ? X.lineTo(p.x, p.y) : X.moveTo(p.x, p.y); } X.stroke(); e.P.forEach((p, i) => { X.fillStyle = i < e.k ? '#ffd98a' : '#fff'; X.fillRect(p.x - 5, p.y - 5, 10, 10); txt(String(i + 1), p.x + 14, p.y - 12, 12, i === e.k ? '#ffd98a' : '#fff'); }); bar(1 - e.t / 8, 580, '#ffd98a'); }
  },
  fugaz: {
    shot: 'sky', st: () => ({ grow: G.day / 365 }), txt: 'ATRAPA LAS ESTRELLAS FUGACES',
    init(e) { e.S = []; for (let i = 0; i < 5; i++) { const x0 = RR(-20, 380), dir = RR(-1, 1); e.S.push({ at: .3 + i * .9, d: 1.5 - ramp() * .4, x0, y0: -10, x1: x0 + dir * 140, y1: RR(300, 520), st: 0 }); } },
    upd(e) { e.S.forEach(s => { s.u = (e.t - s.at) / s.d; if (s.st === 0 && s.u > 1) { s.st = 2; hurt(3); } }); if (e.S.every(s => s.st > 0)) return 1; },
    tap(e, x, y) { for (const s of e.S) if (s.st === 0 && s.u > 0 && s.u < 1) { const hx = lerp(s.x0, s.x1, s.u), hy = lerp(s.y0, s.y1, s.u); if (Math.hypot(hx - x, hy - y) < 46) { s.st = 1; SND.chime(true); return; } } },
    draw(e) { e.S.forEach(s => { if (s.st === 0 && s.u > 0 && s.u < 1) { const hx = lerp(s.x0, s.x1, s.u), hy = lerp(s.y0, s.y1, s.u), u2 = Math.max(0, s.u - .18); X.strokeStyle = 'rgba(255,255,255,.8)'; X.lineWidth = 3; X.beginPath(); X.moveTo(lerp(s.x0, s.x1, u2), lerp(s.y0, s.y1, u2)); X.lineTo(hx, hy); X.stroke(); X.fillStyle = '#fff'; X.fillRect(hx - 4, hy - 4, 8, 8); } }); txt(e.S.filter(s => s.st === 1).length + ' / 5', RW / 2, 580, 14, '#fff'); }
  }
});
Object.assign(EVENTS[2], {
  relojes: {
    shot: 'walkers', st: () => ({ clk: 0 }), txt: 'TOCA EL RELOJ QUE NO MARCA LA MISMA HORA',
    init(e) { e.n = 0; e.rt = 0; newRound(e); },
    upd(e, dt) { e.rt += dt; if (e.rt > 2.6 - ramp() * .8) { hurt(4); e.n++; newRound(e); } if (e.n >= 3) return 1; },
    tap(e, x, y) { for (let i = 0; i < 3; i++) if (Math.hypot(e.X[i] - x, e.Y - y) < 56) { if (i === e.odd) SND.chime(true); else hurt(5); e.n++; newRound(e); return; } },
    draw(e) { X.fillStyle = 'rgba(0,0,0,.6)'; X.fillRect(0, 150, RW, 300); for (let i = 0; i < 3; i++) drawClock(e.X[i], e.Y, 48, i === e.odd ? 280 + e.off : 280); bar(1 - e.rt / (2.6 - ramp() * .8), 420, '#ffd98a'); txt('ronda ' + Math.min(3, e.n + 1) + ' / 3', RW / 2, 180, 12, '#fff'); }
  },
  mirada: {
    shot: 'walkers', st: () => ({ clk: 0 }), txt: 'MANTÉN PRESIONADO PARA CAMINAR · QUIETO SI TE MIRAN',
    init(e) { e.p = 0; e.ph = 'go'; e.pt = RR(1.2, 2); e.sc = 0; },
    upd(e, dt) {
      e.pt -= dt; if (e.pt <= 0) { if (e.ph === 'go') { e.ph = 'warn'; e.pt = .5; SND.voice(); } else if (e.ph === 'warn') { e.ph = 'look'; e.pt = RR(1, 1.5); SND.thud(.15); } else { e.ph = 'go'; e.pt = RR(1.2, 2.2); } }
      if (ptr.down) { if (e.ph === 'look') { G.cor -= dt * 14; G.flash = .12; e.p = Math.max(0, e.p - dt * .2); } else { e.p += dt * .25; e.sc += dt; if (e.sc > .5) { e.sc = 0; SND.step(.12, 1, .7); } } }
      if (e.p >= 1) return 1; if (e.t > 14) { hurt(10, 'No llegaste a tiempo.'); return 1; }
    },
    tap() { }, draw(e) { bar(e.p); const col = e.ph === 'look' ? '#ff6a6a' : (e.ph === 'warn' ? '#ffd98a' : '#9f9'); X.fillStyle = 'rgba(0,0,0,.55)'; X.fillRect(0, 60, RW, 110); X.fillStyle = '#eee'; X.beginPath(); X.ellipse(180, 115, 50, e.ph === 'go' ? 6 : (e.ph === 'warn' ? 20 : 30), 0, 0, PI * 2); X.fill(); if (e.ph !== 'go') { X.fillStyle = col; X.beginPath(); X.arc(180, 115, 13, 0, PI * 2); X.fill(); } txt(e.ph === 'look' ? 'QUIETO' : (e.ph === 'warn' ? '…' : 'CAMINA'), RW / 2, 158, 14, col); }
  }
});
function newRound(e) { e.rt = 0; e.odd = Math.floor(Math.random() * 3); e.off = (Math.random() < .5 ? -1 : 1) * RR(25, 70); e.X = [62, 180, 298]; e.Y = 300; }
Object.assign(EVENTS[3], {
  ventanas: {
    shot: 'street', st: () => ({ night: 1 }), txt: 'REPITE LA SECUENCIA DE VENTANAS',
    init(e) { e.seq = []; const n = 3 + Math.floor(ramp() * 3); for (let i = 0; i < n; i++) e.seq.push(Math.floor(Math.random() * 4)); e.ph = 'show'; e.i = 0; e.k = 0; e.lit = -1; e.pt = .6; },
    upd(e, dt) { e.pt -= dt; if (e.ph === 'show' && e.pt <= 0) { if (e.lit >= 0) { e.lit = -1; e.pt = .18; if (e.i >= e.seq.length) { e.ph = 'in'; e.pt = 99; } } else { e.lit = e.seq[e.i++]; e.pt = .5; SND.blip([330, 392, 494, 587][e.lit], .35, .06, 'sine'); } } if (e.ph === 'in' && e.t > 30) return 1; if (e.done) return 1; },
    tap(e, x, y) { if (e.ph !== 'in') return; const q = winSq(x, y); if (q < 0) return; e.lit = q; setTimeout(() => { e.lit = -1; }, 160); SND.blip([330, 392, 494, 587][q], .3, .06, 'sine'); if (q === e.seq[e.k]) { e.k++; if (e.k >= e.seq.length) { SND.chime(true); e.done = 1; } } else { hurt(8, 'Ya no recuerdas.'); e.done = 1; } },
    draw(e) { const C = ['#ffd98a', '#8fd6ff', '#ff9fb0', '#b8ff9f']; for (let i = 0; i < 4; i++) { const [x, y] = [[95, 230], [265, 230], [95, 400], [265, 400]][i]; X.fillStyle = e.lit === i ? C[i] : 'rgba(20,20,40,.7)'; X.fillRect(x - 60, y - 60, 120, 120); X.strokeStyle = C[i]; X.lineWidth = 3; X.strokeRect(x - 60, y - 60, 120, 120); } txt(e.ph === 'show' ? 'MIRA…' : 'AHORA TÚ', RW / 2, 120, 14, '#fff'); }
  },
  respiracion: {
    shot: 'street', st: () => ({ dusk: .6 }), txt: 'MANTÉN AL INHALAR · SUELTA AL EXHALAR',
    init(e) { e.good = 0; e.bad = 0; e.dur = 8; },
    upd(e, dt) { const ex = Math.sin(e.t * PI / 2) > 0; if (ptr.down === ex) e.good += dt; else e.bad += dt; SND.tone(150 + (ex ? 1 : 0) * 70, .015); if (e.t >= e.dur) { if (e.good / (e.good + e.bad) < .62) hurt(10, 'Te agitaste.'); return 1; } },
    tap() { },
    draw(e) { const s = .5 - .5 * Math.cos(e.t * PI / 2), r = 40 + 70 * s, ex = Math.sin(e.t * PI / 2) > 0; X.fillStyle = 'rgba(0,0,0,.4)'; X.fillRect(0, 170, RW, 300); X.strokeStyle = ex ? '#9fd' : '#fff'; X.lineWidth = 4; X.beginPath(); X.arc(180, 320, r, 0, PI * 2); X.stroke(); X.fillStyle = ptr.down ? 'rgba(150,255,220,.5)' : 'rgba(255,255,255,.12)'; X.beginPath(); X.arc(180, 320, 34, 0, PI * 2); X.fill(); txt(ex ? 'INHALA' : 'EXHALA', RW / 2, 450, 14, '#fff'); bar(e.t / e.dur, 580, '#9fd'); }
  }
});
function winSq(x, y) { const P = [[95, 230], [265, 230], [95, 400], [265, 400]]; for (let i = 0; i < 4; i++) if (Math.abs(P[i][0] - x) < 60 && Math.abs(P[i][1] - y) < 60) return i; return -1; }

// ---------- ambiente liminal: vineta + grano ----------
let VIG = null;
function liminal() {
  if (!VIG) { VIG = document.createElement('canvas'); VIG.width = RW; VIG.height = RH; const g = VIG.getContext('2d'), gr = g.createRadialGradient(180, 320, 160, 180, 320, 430); gr.addColorStop(0, 'rgba(0,0,0,0)'); gr.addColorStop(1, 'rgba(10,6,24,.6)'); g.fillStyle = gr; g.fillRect(0, 0, RW, RH); }
  X.drawImage(VIG, 0, 0);
  for (let i = 0; i < 80; i++) { X.fillStyle = Math.random() < .5 ? 'rgba(255,255,255,.07)' : 'rgba(0,0,0,.1)'; X.fillRect(Math.random() * RW | 0, Math.random() * RH | 0, 2, 1); }
  if (Math.random() < .015) { X.fillStyle = 'rgba(255,255,255,.05)'; X.fillRect(0, Math.random() * RH, RW, 3); }
}

function scopeC(t) { return { x: 180 + Math.sin(t * .9) * 80 + Math.sin(t * 2.1) * 20, y: 320 + Math.sin(t * .6 + 1) * 110 + Math.cos(t * 1.7) * 16 }; }
function drawScope(e, c) {
  X.fillStyle = '#000'; X.fillRect(0, 0, RW, RH); X.save(); X.beginPath(); X.arc(180, 320, 160, 0, PI * 2); X.clip(); const gr = X.createRadialGradient(180, 320, 20, 180, 320, 160); gr.addColorStop(0, '#6a70a8'); gr.addColorStop(1, '#343a68'); X.fillStyle = gr; X.fillRect(0, 0, RW, RH);
  e.sd.forEach(s => { const x = (s[0] + e.t * 4 * (s[2] + .2)) % 360, y = s[1] * .6 + 110; X.fillStyle = '#fff'; X.fillRect(x, y, s[2] > .9 ? 3 : 2, s[2] > .9 ? 3 : 2); });
  for (let i = 0; i < 7; i++) { X.fillStyle = '#fff'; X.fillRect(c.x + [0, 8, 14, -8, 4, -14, 12][i] - 3, c.y + [0, 6, -4, 10, -12, 0, 12][i] - 3, 6, 6); }
  X.restore();
}

// ---------- guardado y dias reales ----------
let SV = null, NOTE = '', TEST = null, NOW = { ts: Date.now(), base: performance.now(), server: false }, UIB = [];
const DEFSV = () => ({ v: 1, world: null, start: null, days: 0, last: null, done: null, pend: null, cor: 100, end: null, seenTs: 0, found: {} });
const ymd = ts => { const d = new Date(ts); return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); };
const nowTs = () => NOW.ts + (performance.now() - NOW.base);
function msToMidnight(ts) { const d = new Date(ts); return new Date(d.getFullYear(), d.getMonth(), d.getDate() + 1) - d; }
const saveSV = () => { Store.save(SV); };
// cuenta un dia nuevo SOLO si abres el juego ese dia real
function verifyDay() {
  NOTE = ''; const ts = NOW.ts;
  if (SV.world === null) return;
  if (SV.seenTs && ts < SV.seenTs - 600000) { NOTE = 'El reloj del teléfono retrocedió. El día no cuenta hasta que la hora sea correcta.'; return; }
  SV.seenTs = Math.max(SV.seenTs, ts);
  if (SV.end) { saveSV(); return; }
  if (SV.pend && SV.pend === SV.last && SV.done !== SV.last) { SV.cor = Math.max(0, SV.cor - 12); SV.done = SV.last; NOTE = 'Abandonaste la prueba del día. Cordura −12.'; }
  const today = ymd(ts);
  if (SV.last !== today) { SV.days += 1; SV.last = today; SV.done = null; SV.pend = null; SV.cor = Math.min(100, SV.cor + 4); }
  if (SV.cor <= 0) SV.end = 'lost';
  saveSV();
}
const FLAV = [
  ['El mar está tibio.', 'La hamaca se mece sola.', 'Cuentas las luces. Faltan.', 'El remolino no se mueve.', 'Algo camina por el fondo.', 'Hoy tampoco hay pueblo.'],
  ['Cuentas estrellas.', 'El cielo se ve más cerca.', 'El telescopio apunta solo.', 'Hay un grupo nuevo.', 'Algo muy alto te mira.', 'No es el cielo.'],
  ['Nadie corre.', 'Es domingo otra vez.', 'Todos los relojes marcan la misma hora.', 'El pan sigue en el aire.', 'Tu pie no termina el paso.', 'La paloma no cae.'],
  ['Todos durmieron diez horas.', 'Soñaste lo mismo que ellos.', 'No hay ruido.', 'Nadie despierta.', 'La capucha está más abajo.', 'Aguantas un día más.'],
];
const NAMES = { luces: 'Contar las luces', ojos: 'Cerrar los ojos', remolino: 'Remar contra el remolino', botellas: 'Atrapar botellas', telescopio: 'Mantener la mira', sueno: 'Cerrar los ojos (gigante)', constelacion: 'Unir la constelación', fugaz: 'Estrellas fugaces', paso: 'Pasos lentos', congelados: 'Empujar lo congelado', relojes: 'El reloj distinto', mirada: 'Luz verde / roja', despierto: 'No dormirse', tentaculos: 'Cortar tentáculos', ventanas: 'Secuencia de ventanas', respiracion: 'Respirar al ritmo' };
const dailyDef = (w, day) => { const k = Object.keys(EVENTS[w]); return EVENTS[w][k[(day * 3 + w) % k.length]]; };
const newG = (p, day, cor, test) => ({ p, day, cor, t: 0, ev: null, msg: null, flash: 0, over: 0, mode: 'stamp', mt: 0, test: !!test, lost: false, cam: { yaw: 0, pitch: -.07, fov: 62 }, found: {}, drag: null, moved: false });

function enterDay() {
  const p = SV.world; G = newG(p, SV.days, SV.cor, false); S = 'play'; stopAudio();
  if (SV.end) { G.mode = 'end'; G.over = .001; G.lost = SV.end === 'lost'; SND.amb('menu'); }
}
function startDaily(def) {
  G.ev = { def: def || dailyDef(G.p, G.day), t: 0 }; if (G.ev.def.init) G.ev.def.init(G.ev); G.mode = 'event';
  if (!G.test) { SV.pend = SV.last; saveSV(); }
}
function endDaily() {
  G.ev = null; SND.silence();
  if (G.test) { G.mode = 'idle'; G.cor = 100; return; }
  SV.cor = Math.max(0, G.cor); SV.done = SV.last;
  if (SV.cor <= 0) SV.end = 'lost'; else if (SV.days >= 365) SV.end = 'won';
  saveSV();
  if (SV.end) { G.mode = 'end'; G.over = .001; G.lost = SV.end === 'lost'; SND.amb('menu'); SND.silence(); SND.thud(SV.end === 'lost' ? .4 : .1); if (SV.end === 'won') SND.chime(true); }
  else { G.mode = 'idle'; say(FLAV[G.p][G.day % 6], 5); SND.chime(true); }
}
function commitWorld(i) {
  const today = ymd(nowTs()); SV = Object.assign(DEFSV(), { world: i, start: today, days: 1, last: today, seenTs: nowTs() }); saveSV();
  SEL.pick = i; SEL.pt = 0; SEL.confirm = null; stopAudio(); SND.blip(520, .1);
}
function openTest() { TEST = TEST || { w: 0, day: 100 }; S = 'test'; G = null; stopAudio(); SND.silence(); SND.amb('menu'); }
function exitTest() { SND.silence(); stopAudio(); TEST.back = null; if (SV && SV.world !== null) enterDay(); else { S = 'title'; SND.amb('menu'); } }
function testWorld(free, def) { G = newG(TEST.w, TEST.day, 100, true); G.mode = 'idle'; S = 'play'; SND.amb(WN[TEST.w]); if (def) startDaily(def); }

function idleView() {
  const d = G.day, p = G.p;
  if (p === 0) { // Las Islas: al entrar al mundo (modo 'idle') la cámara es libre; el resto del tiempo, planos fijos
    if (G.mode === 'idle') return ['beach', islasFree(G)];
    const n = (d % 6) >= 3 ? 1 : 0, b = { day: d, whirl: d >= 40, town: d < 90 };
    return n ? ['nsea', { ...b, night: 1 }] : (d >= 40 && d % 2 ? ['whirl', b] : ['beach', b]);
  }
  if (p === 1) return [d % 3 === 0 ? 'cot' : 'wide', { grow: d / 365 }];
  if (p === 2) return [d > 260 ? 'tent' : 'tower', { clk: d * .35, tent: d > 260, freeze: d > 260 }];
  const n = (d % 8) >= 4 ? 1 : 0; return ['day', { night: n, dusk: n ? 1 : ((d % 8) === 3 ? 1 : 0), hood: d > 250 ? .3 : undefined }];
}

function update(dt) {
  DT = dt; T += dt; SND.tick(dt, { cor: (S === 'play' && G && !G.test && G.mode === 'event') ? G.cor : undefined });
  if (S === 'cine') { CI.t = CINE[CI.p].t0 + (T - CI.start); if (CI.t >= CINE[CI.p].t1) endCine(); }
  if (S === 'select') {
    if (!SEL.confirm) SEL.t += dt;
    if (SEL.pick === null && SEL.t > 9) commitWorld(Math.floor(Math.random() * 4));
    if (SEL.pick !== null) { SEL.pt += dt; SEL.zoom = Math.min(1, SEL.pt / 1.1); if (SEL.pt > 1.2) startPlanet(SEL.pick); } else SEL.zoom = Math.max(0, (SEL.t - 3) / 8);
  }
  if (S === 'play') {
    const g = G; g.t += dt; g.flash = Math.max(0, g.flash - dt); if (g.msg) { g.msg.t -= dt; if (g.msg.t <= 0) g.msg = null; }
    if (g.mode === 'stamp') g.mt += dt;
    else if (g.mode === 'event') { g.ev.t += dt; if (g.ev.def.upd(g.ev, dt)) endDaily(); }
    else if (g.mode === 'end') g.over += dt;
    else if (g.mode === 'idle' && !g.test && ymd(nowTs()) !== SV.last) { NOW = { ts: nowTs(), base: performance.now(), server: NOW.server }; verifyDay(); enterDay(); }
  }
}

// ---------- dibujo ----------
function ubtn(x, y, w, h, label, fn, opt = {}) {
  X.fillStyle = opt.on ? '#5b4fc4' : (opt.bg || 'rgba(18,16,36,.88)'); X.fillRect(x, y, w, h); X.strokeStyle = opt.col || '#9d92e8'; X.lineWidth = 1; X.strokeRect(x + .5, y + .5, w - 1, h - 1);
  txt(label, x + w / 2, y + h / 2 + 1, opt.size || 10, '#fff'); UIB.push({ x, y, w, h, fn });
}
const hitUI = p => { for (let i = UIB.length - 1; i >= 0; i--) { const b = UIB[i]; if (p.x >= b.x && p.x <= b.x + b.w && p.y >= b.y && p.y <= b.y + b.h) { b.fn(); return true; } } return false; };
function chip() { ubtn(8, 612, 92, 20, '⚙ MODO PRUEBA', openTest, { size: 9, bg: 'rgba(0,0,0,.55)', col: '#6a6a88' }); }
function wrap(s, n) { const out = []; let l = ''; s.split(' ').forEach(w => { if ((l + ' ' + w).trim().length > n) { out.push(l); l = w; } else l = (l + ' ' + w).trim(); }); out.push(l); return out; }
function drawHUD() {
  const g = G; X.fillStyle = 'rgba(0,0,0,.6)'; X.fillRect(6, 6, 116, 20); txt('DÍA ' + Math.floor(g.day) + '/365', 12, 17, 12, '#fff', 'left');
  X.fillStyle = 'rgba(0,0,0,.6)'; X.fillRect(RW - 122, 6, 116, 20); X.fillStyle = g.cor > 35 ? '#3fb892' : '#e04a4a'; X.fillRect(RW - 118, 12, 108 * g.cor / 100, 8); X.strokeStyle = '#fff'; X.lineWidth = 1; X.strokeRect(RW - 118.5, 11.5, 108, 8); txt('CORDURA', RW - 64, 17, 7, '#fff', 'center', PIX, false);
}
function render() {
  X.clearRect(0, 0, RW, RH); UIB = [];
  if (S === 'boot') { clear([0, 0, 0, 1]); txt('VERIFICANDO EL DÍA…', RW / 2, 300, 11, '#8a86b8', 'center', PIX, false); return; }
  if (S === 'test') { drawTest(); return; }
  if (S === 'title' || S === 'select') {
    renderPlanets(T, S === 'select' ? SEL.zoom : .0, S === 'select' ? SEL.pick : -1);
    if (S === 'title') {
      X.fillStyle = 'rgba(8,6,20,.55)'; X.fillRect(0, 0, RW, RH); txt('SOBREVIVE', RW / 2, 170, 26, '#fff', 'center', PIX, false); txt('UN AÑO', RW / 2, 212, 26, '#fff', 'center', PIX, false); txt('PROYECTO NOCTIS', RW / 2, 258, 9, '#cfc8ff', 'center', PIX, false);
      ['UNA SOLA VIDA.', 'UN SOLO MUNDO.', '365 DÍAS REALES.', 'Abre el juego cada día', 'para que el día cuente.'].forEach((l, i) => txt(l, RW / 2, 330 + i * 24, i < 3 ? 11 : 10, i < 3 ? '#fff' : '#a9a4d8'));
      if (Math.floor(T * 2) % 2) txt('TOCA PARA EMPEZAR', RW / 2, 520, 11, '#fff', 'center', PIX, false); chip(); return;
    }
    X.font = '19px ' + PIX; let w = X.measureText('ELIGE UN PLANETA').width + 20; X.fillStyle = 'rgba(14,14,22,.88)'; X.fillRect(RW / 2 - w / 2, 58, w, 32); txt('ELIGE UN PLANETA', RW / 2, 75, 19, '#fff', 'center', PIX, false);
    X.font = '9px ' + PIX; w = X.measureText('SOLO PODRÁS ELEGIR UNO').width + 12; X.fillStyle = 'rgba(14,14,22,.8)'; X.fillRect(RW / 2 - w / 2, 96, w, 17); txt('SOLO PODRÁS ELEGIR UNO', RW / 2, 105, 9, '#e8e8f0', 'center', PIX, false);
    PLC.forEach(([x, y], i) => txt(String(i + 1), x - 80, y - 60, 14, '#fff', 'center', PIX, false));
    const cd = Math.ceil(9 - SEL.t); if (SEL.t > 3.5 && SEL.pick === null && cd > 0) txt(String(Math.max(1, Math.min(5, cd))), RW / 2, 570, 34, '#fff', 'center', PIX, false);
    if (SEL.confirm !== null && SEL.confirm !== undefined) {
      X.fillStyle = 'rgba(0,0,0,.72)'; X.fillRect(0, 0, RW, RH); X.fillStyle = 'rgba(18,16,36,.96)'; X.fillRect(30, 220, 300, 200); X.strokeStyle = '#9d92e8'; X.strokeRect(30.5, 220.5, 299, 199);
      txt('¿ELEGIR EL PLANETA ' + (SEL.confirm + 1) + '?', RW / 2, 254, 11, '#fff', 'center', PIX, false); ['NO PODRÁS CAMBIARLO', 'NI VOLVER A EMPEZAR,', 'AUNQUE DESINSTALES.'].forEach((l, i) => txt(l, RW / 2, 296 + i * 20, 12, '#ffd98a'));
      ubtn(50, 366, 120, 36, 'SÍ, ESTE', () => commitWorld(SEL.confirm), { on: true, size: 11 }); ubtn(190, 366, 120, 36, 'VOLVER', () => { SEL.confirm = null; }, { size: 11 });
    }
    return;
  }
  if (S === 'cine') {
    const c = CINE[CI.p], sh = cineAt(c, CI.t), tt = CI.t - sh.t; let st = typeof sh.st === 'function' ? sh.st(tt) : (sh.st || {});
    if (sh.blink) { const k = CI.t - 170; clear([.17, .2, .33, 1]); X.fillStyle = k < 1 ? '#232a4e' : '#bba9de'; X.fillRect(0, 0, RW, RH); if (k >= 2) { X.save(); for (let i = 0; i < 60; i++) { rs(i + 3); X.fillStyle = `rgba(240,230,255,${.3 + rnd() * .5})`; X.fillRect(rnd() * 360, rnd() * 640, 20 + rnd() * 60, 14 + rnd() * 30); } X.restore(); } }
    else if (sh.scope) { clear([0, 0, 0, 1]); drawScope({ sd: SCOPEDATA(), t: CI.t }, { x: 200 + Math.sin(CI.t) * 4, y: 360 }); }
    else scene(sh.w, sh.s, T, st);
    if (sh.title && CI.t >= sh.title[0] && CI.t < sh.title[1]) title(TITLES[CI.p][0], TITLES[CI.p][1]);
    let line = ''; for (const s of sh.subs) if (CI.t >= s[0]) line = s[1]; sub(line, sh.sy || .67);
    liminal(); txt('SALTAR ▸▸', RW - 8, RH - 12, 10, 'rgba(255,255,255,.55)', 'right'); return;
  }
  if (S === 'play') {
    const g = G, w = WN[g.p]; let shot, st;
    if (g.mode === 'event' && g.ev.def.scope) drawScope(g.ev, scopeC(g.ev.t));
    else { if (g.mode === 'event') { shot = g.ev.def.shot; st = g.ev.def.st(g.ev); } else [shot, st] = idleView(); scene(w, shot, T, st); if (g.mode === 'idle' && g.p === 0) glitchFX(g.day); }
    if (g.mode === 'event') { g.ev.def.draw(g.ev); X.font = 'bold 11px ' + MONO; const tx = g.ev.def.txt, ww = X.measureText(tx).width + 12; X.fillStyle = 'rgba(0,0,0,.72)'; X.fillRect(RW / 2 - ww / 2, 606, ww, 18); txt(tx, RW / 2, 615, 11, '#fff'); }
    if (g.msg) sub(g.msg.s, .8);
    if (g.flash > 0) { X.fillStyle = `rgba(255,255,255,${Math.min(.6, g.flash)})`; X.fillRect(0, 0, RW, RH); }
    liminal(); drawHUD(); if (g.mode === 'idle' && g.p === 0) drawFree(g);
    if (g.mode === 'stamp') {
      X.fillStyle = `rgba(0,0,0,${Math.min(.55, g.mt)})`; X.fillRect(0, 0, RW, RH); const a = Math.min(1, g.mt * 1.2);
      X.globalAlpha = a; txt('DÍA ' + Math.floor(g.day), RW / 2, 230, 26, '#fff', 'center', PIX, false); txt('DE 365', RW / 2, 272, 12, '#cfc8ff', 'center', PIX, false);
      txt(g.test ? '(modo prueba · no se guarda)' : (SV.done === SV.last ? 'Día verificado · ya hiciste la prueba de hoy' : 'Día real verificado'), RW / 2, 310, 11, '#a9a4d8');
      if (!g.test && NOTE) wrap(NOTE, 30).forEach((l, i) => txt(l, RW / 2, 350 + i * 18, 11, '#ffb4b4'));
      X.globalAlpha = 1; if (g.mt > 1 && Math.floor(T * 2) % 2) txt('TOCA PARA ENTRAR', RW / 2, 520, 11, '#fff', 'center', PIX, false);
    }
    if (g.mode === 'idle' && !g.test) { const ms = msToMidnight(nowTs()), hh = Math.floor(ms / 3.6e6), mm = Math.floor(ms % 3.6e6 / 6e4); X.font = 'bold 11px ' + MONO; const tx = 'DÍA ' + g.day + ' VERIFICADO · VUELVE MAÑANA (' + hh + 'h ' + mm + 'm)', ww = X.measureText(tx).width + 12; X.fillStyle = 'rgba(0,0,0,.72)'; X.fillRect(RW / 2 - ww / 2, 580, ww, 18); txt(tx, RW / 2, 589, 11, '#fff'); chip(); }
    if (g.test) ubtn(112, 30, 136, 22, '‹ SALIR DE PRUEBA', () => { SND.silence(); S = 'test'; G = null; stopAudio(); SND.amb('menu'); }, { size: 10 });
    if (g.mode === 'end') {
      X.fillStyle = `rgba(0,0,0,${Math.min(.88, g.over)})`; X.fillRect(0, 0, RW, RH);
      if (g.over > .6) {
        txt(g.lost ? 'TE QUEDASTE AQUÍ' : 'SOBREVIVISTE UN AÑO', RW / 2, 250, g.lost ? 15 : 13, g.lost ? '#e05a5a' : '#8fd6c0', 'center', PIX, false);
        txt(TITLES[g.p][0], RW / 2, 300, 11, '#cfc8ff'); txt('Día ' + Math.floor(g.day) + ' de 365', RW / 2, 326, 13, '#fff'); txt('Esta fue tu única vida.', RW / 2, 372, 11, '#a9a4d8'); chip();
      }
    }
  }
}
function drawTest() {
  renderPlanets(T, 0, -1); X.fillStyle = 'rgba(8,6,20,.78)'; X.fillRect(0, 0, RW, RH);
  txt('MODO PRUEBA', RW / 2, 28, 14, '#fff', 'center', PIX, false); txt('no guarda nada · prueba todos los mundos', RW / 2, 52, 10, '#a9a4d8');
  ['1 ISLAS', '2 ESTRELLAS', '3 PLAZA', '4 CALLE'].forEach((l, i) => ubtn(8 + i * 88, 70, 84, 32, l, () => { TEST.w = i; SND.blip(400 + i * 60, .06); }, { on: TEST.w === i, size: 9 }));
  ubtn(12, 114, 48, 30, '-30', () => { TEST.day = Math.max(1, TEST.day - 30); }); ubtn(64, 114, 48, 30, '-1', () => { TEST.day = Math.max(1, TEST.day - 1); });
  txt('DÍA ' + TEST.day, RW / 2, 130, 14, '#ffd98a'); ubtn(248, 114, 48, 30, '+1', () => { TEST.day = Math.min(365, TEST.day + 1); }); ubtn(300, 114, 48, 30, '+30', () => { TEST.day = Math.min(365, TEST.day + 30); });
  ubtn(12, 158, 336, 34, 'VER LA INTRO DEL VIDEO', () => { CI = { p: TEST.w, t: CINE[TEST.w].t0, start: T, test: true }; S = 'cine'; stopAudio(); SND.amb('none'); playAudio(CINE[TEST.w].t0); });
  ubtn(12, 200, 336, 34, 'EXPLORAR EL MUNDO (primera persona + sonido)', () => testWorld(true));
  ubtn(12, 242, 336, 34, 'DÍA VERIFICADO (pantalla de inicio de día)', () => { testWorld(true); G.mode = 'stamp'; });
  txt('MINIJUEGOS', RW / 2, 296, 10, '#a9a4d8', 'center', PIX, false);
  Object.keys(EVENTS[TEST.w]).forEach((k, i) => ubtn(12, 310 + i * 40, 336, 34, NAMES[k] || k, () => testWorld(true, EVENTS[TEST.w][k])));
  ubtn(12, 580, 336, 34, '‹ VOLVER', exitTest, { on: false, size: 11 });
}
let _sd = null; function SCOPEDATA() { if (!_sd) { rs(9); _sd = []; for (let i = 0; i < 160; i++) _sd.push([rnd() * 360, rnd() * 640, rnd()]); } return _sd; }

// ---------- cámara libre (mundo 1 · Las Islas): mira alrededor y busca lo que cambia cada día ----------
let DT = .016;
const foundMap = () => G.test ? G.found : (SV.found || (SV.found = {}));
function islasFree(g) { const d = g.day; return { free: g.cam, day: d, night: (d % 6) >= 3 ? 1 : 0, whirl: d >= 40, town: d < 90, anoms: anomsFor(d) }; }
function tapFree(x, y) {
  const g = G; if (!g || g.p !== 0 || g.mode !== 'idle') return;
  setCam(freeCamIslas(g.cam)); let best = null, bd = 1e9;
  for (const a of anomsFor(g.day)) { const q = proj(a.c); if (q[2] <= 0) continue; const d = Math.hypot(q[0] - x, q[1] - y), R = Math.max(34, a.sz * 533 / q[2] + 14); if (d < R && d < bd) { best = a; bd = d; } }
  if (!best) { beep(130, .04, .03); return; }
  const f = foundMap(); if (f[best.id]) { say('Ya la registraste.', 1.6); beep(200, .05, .03); return; }
  f[best.id] = g.day; g.flash = .35; SND.chime(true); say(best.txt, 4); if (!g.test) saveSV();
}
function drawFree(g) {
  const A = anomsFor(g.day), f = foundMap(); let n = 0; setCam(freeCamIslas(g.cam));
  for (const a of A) if (f[a.id]) { n++; const q = proj(a.c); if (q[2] > 0 && q[0] > -12 && q[0] < RW + 12 && q[1] > -12 && q[1] < RH + 12) { X.strokeStyle = 'rgba(255,255,255,.6)'; X.lineWidth = 1; X.strokeRect(q[0] - 10.5, q[1] - 10.5, 21, 21); } }
  txt('ANOMALÍAS ' + n + ' / ' + A.length, RW / 2, 66, 11, '#ffd98a');
  if (!g.moved) { txt('ARRASTRA PARA MIRAR', RW / 2, 84, 10, '#cfc8ff'); txt('TOCA LO QUE SE VE EXTRAÑO', RW / 2, 98, 10, '#cfc8ff'); }
  if (g.test) { const sd = k => () => { g.day = clamp(g.day + k, 1, 365); }; ubtn(8, 586, 44, 22, '-30', sd(-30), { size: 9 }); ubtn(56, 586, 44, 22, '-1', sd(-1), { size: 9 }); ubtn(RW - 100, 586, 44, 22, '+1', sd(1), { size: 9 }); ubtn(RW - 52, 586, 44, 22, '+30', sd(30), { size: 9 }); }
}
// glitch de pantalla: cortes horizontales, franjas de color y mensajes; más frecuente cuanto más avanza el año
let GFX = { t: 0, txt: '', x: 0, y: 0 };
const GL_TXT = ['FALTAN', 'NO MIRES', 'CUENTA LAS LUCES', 'ESTÁ AQUÍ', 'NO HAY PUEBLO', 'TODOS LOS OJOS', 'ERROR 0x0000', 'SIGUE AHÍ'];
function glitchFX(day) {
  if (day < 8) return; const gi = clamp(day / 365, 0, 1);
  if (GFX.t > 0) GFX.t -= DT; else if (Math.random() < DT * (.12 + gi)) { GFX.t = .06 + Math.random() * (.1 + gi * .25); GFX.txt = Math.random() < gi * .6 ? GL_TXT[Math.random() * GL_TXT.length | 0] : ''; GFX.x = 60 + Math.random() * 240; GFX.y = 120 + Math.random() * 380; }
  if (GFX.t <= 0) return;
  for (let i = 0, k = 2 + Math.floor(gi * 7); i < k; i++) { const y = Math.random() * RH | 0, h = Math.min(RH - y, 3 + (Math.random() * (10 + gi * 40) | 0)), dx = (Math.random() - .5) * (20 + gi * 80); X.drawImage(cv, 0, y, RW, h, dx, y, RW, h); }
  X.fillStyle = `rgba(255,0,90,${.06 + gi * .14})`; X.fillRect(0, Math.random() * RH, RW, 2 + Math.random() * 8);
  X.fillStyle = `rgba(0,255,230,${.05 + gi * .1})`; X.fillRect(0, Math.random() * RH, RW, 2 + Math.random() * 6);
  if (GFX.txt) txt(GFX.txt, GFX.x, GFX.y, 12 + gi * 8, 'rgba(255,255,255,.9)');
}

// ---------- entrada ----------
function pos(e) { const r = ui.getBoundingClientRect(); return { x: (e.clientX - r.left) * RW / r.width, y: (e.clientY - r.top) * RH / r.height }; }
ui.addEventListener('pointerdown', e => {
  e.preventDefault(); const p = pos(e); ptr = { x: p.x, y: p.y, down: true };
  if (!SND.ctx()) { SND.init(); if (S === 'play' && G && G.mode !== 'end') SND.amb(WN[G.p]); } else SND.init();
  if (hitUI(p)) return;
  if (S === 'title') { S = 'select'; SEL = { t: 0, pick: null, zoom: 0, pt: 0, confirm: null }; SND.amb('menu'); playAudio(0); SND.blip(440, .1); return; }
  if (S === 'select') { if (SEL.pick !== null || SEL.confirm !== null) return; PLC.forEach(([x, y], i) => { if (Math.hypot(x - p.x, y - p.y) < 80) { SEL.confirm = i; SND.blip(500, .06); } }); return; }
  if (S === 'cine') { if (p.x > RW - 110 && p.y > RH - 40) endCine(); return; }
  if (S === 'play') {
    if (G.mode === 'stamp' && G.mt > 1) { if (SV && !G.test && SV.done === SV.last) { G.mode = 'idle'; say(FLAV[G.p][G.day % 6], 5); } else startDaily(G.test ? null : undefined); SND.amb(WN[G.p]); return; }
    if (G.mode === 'event') G.ev.def.tap(G.ev, p.x, p.y);
    if (G.mode === 'idle' && G.p === 0) G.drag = { x: p.x, y: p.y, lx: p.x, ly: p.y, t: T, mv: 0 };
  }
});
ui.addEventListener('pointermove', e => {
  const p = pos(e); ptr.x = p.x; ptr.y = p.y;
  const d = S === 'play' && G && G.drag; // arrastrar = mirar alrededor (el mundo sigue al dedo)
  if (d) { const dx = p.x - d.lx, dy = p.y - d.ly, k = .0022 * (G.cam.fov / 62); d.lx = p.x; d.ly = p.y; d.mv += Math.abs(dx) + Math.abs(dy); G.cam.yaw -= dx * k; G.cam.pitch = clamp(G.cam.pitch + dy * k, -1.3, 1.3); if (d.mv > 10) G.moved = true; }
});
addEventListener('pointerup', () => { ptr.down = false; const d = S === 'play' && G && G.drag; if (d) { G.drag = null; if (d.mv < 10 && T - d.t < .6) tapFree(d.x, d.y); } });
addEventListener('pointercancel', () => { ptr.down = false; if (G) G.drag = null; });

// ---------- arranque ----------
async function start() {
  S = 'boot';
  let rec = null; try { rec = await Store.load(); } catch (e) { }
  SV = Object.assign(DEFSV(), rec || {});
  try { const n = await Store.now(); NOW = { ts: n.ts, base: performance.now(), server: n.server }; } catch (e) { }
  verifyDay();
  if (SV.world === null) { S = 'title'; } else { enterDay(); if (G.mode !== 'end' && SV.done === SV.last && false) G.mode = 'idle'; }
}
function boot() {
  initTex(); initPlanets(); WL.islas = worldIslas(); WL.estrellas = worldEstrellas(); WL.plaza = worldPlaza(); WL.calle = worldCalle();
  let last = performance.now();
  function loop(now) { const dt = Math.min(.05, (now - last) / 1000); last = now; update(dt); render(); requestAnimationFrame(loop); }
  requestAnimationFrame(loop); start();
}
window.dbg = { // para pruebas
  cine(i, t) { S = 'cine'; CI = { p: i, t, start: T - (t - CINE[i].t0), test: true }; }, game(p, day) { TEST = TEST || { w: 0, day: 100 }; TEST.w = p; TEST.day = day; testWorld(true); }, ev(k) { startDaily(EVENTS[G.p][k]); },
  sel(t) { S = 'select'; SEL = { t, pick: null, zoom: 0, pt: 0, confirm: null }; }, openTest, get S() { return S; }, get G() { return G; }, get SV() { return SV; }, set SV(v) { SV = v; }, verifyDay, enterDay, now(ts) { NOW = { ts, base: performance.now(), server: false }; }, commitWorld
};
boot();
