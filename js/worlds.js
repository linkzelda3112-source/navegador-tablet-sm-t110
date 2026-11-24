'use strict';
// ===== Texturas y mundos (una escena 3D por planeta, copiadas del video) =====
const TX = {};
function initTex() {
  TX.sea = mkTex(64, 64, (g, w, h) => { blocks(g, w, h, '#43c8ab', 26, 8, 3); });
  TX.sand = mkTex(64, 64, (g, w, h) => { blocks(g, w, h, '#d6cc9b', 18, 4, 4); });
  TX.cloud = mkTex(128, 64, (g, w, h) => { g.fillStyle = 'rgb(186,186,186)'; g.fillRect(0, 0, w, h); rs(9); for (let i = 0; i < 20; i++) { const x = rnd() * w, y = rnd() * h, r = 6 + rnd() * 12, gr = g.createRadialGradient(x, y, 1, x, y, r); gr.addColorStop(0, 'rgba(255,255,255,.55)'); gr.addColorStop(1, 'rgba(255,255,255,0)'); g.fillStyle = gr; g.fillRect(x - r - 1, y - r * .5, r * 2 + 2, r); } blocks2(g, w, h); });
  TX.milky = mkTex(128, 64, (g, w, h) => { g.fillStyle = 'rgb(190,190,205)'; g.fillRect(0, 0, w, h); rs(11); for (let i = 0; i < 60; i++) { const x = rnd() * w, y = rnd() * h, r = 8 + rnd() * 20, gr = g.createRadialGradient(x, y, 1, x, y, r); gr.addColorStop(0, 'rgba(255,255,255,.7)'); gr.addColorStop(1, 'rgba(255,255,255,0)'); g.fillStyle = gr; g.fillRect(x - r, y - r, r * 2, r * 2); } blocks2(g, w, h); });
  TX.wood = mkTex(32, 32, (g, w, h) => { for (let y = 0; y < h; y += 8) { g.fillStyle = (y / 8) % 2 ? '#7b5a3a' : '#8a6642'; g.fillRect(0, y, w, 7); g.fillStyle = '#4a3320'; g.fillRect(0, y + 7, w, 1); } rs(2); for (let i = 0; i < 16; i++) { g.fillStyle = 'rgba(40,25,10,.35)'; g.fillRect(rnd() * w, rnd() * h, 6, 1); } });
  TX.bark = mkTex(16, 32, (g, w, h) => { blocks(g, w, h, '#93724a', 36, 4, 5); });
  TX.grass = mkTex(64, 64, (g, w, h) => { blocks(g, w, h, '#1c3329', 16, 8, 6); });
  TX.cobble = mkTex(64, 64, (g, w, h) => { g.fillStyle = '#6a6050'; g.fillRect(0, 0, w, h); rs(8); for (let y = 0; y < 64; y += 8) for (let x = 0; x < 64; x += 16) { const k = 120 + rnd() * 40, o = (y / 8) % 2 * 8; g.fillStyle = rgb(k + 10, k - 2, k - 25); g.fillRect(x + o, y, 15, 7); } });
  TX.brick = mkTex(32, 32, (g, w, h) => { g.fillStyle = '#7a6643'; g.fillRect(0, 0, w, h); rs(4); for (let y = 0; y < 32; y += 4) for (let x = 0; x < 32; x += 8) { const k = (rnd() - .5) * 30, o = (y / 4) % 2 * 4; g.fillStyle = rgb(176 + k, 150 + k, 104 + k); g.fillRect((x + o) % 32, y, 7, 3); } });
  TX.asphalt = mkTex(64, 64, (g, w, h) => { blocks(g, w, h, '#3b3a40', 16, 4, 12); });
  const fac = (lit, base) => mkTex(32, 32, (g, w, h) => { g.fillStyle = base; g.fillRect(0, 0, w, h); rs(lit ? 21 : 22); for (let y = 2; y < 32; y += 8) for (let x = 3; x < 32; x += 11) { const on = lit && rnd() < .38; g.fillStyle = on ? '#ffd98a' : '#3a4256'; g.fillRect(x, y, 6, 5); if (!on) { g.fillStyle = 'rgba(255,255,255,.18)'; g.fillRect(x, y, 6, 1); } } });
  TX.fac = fac(false, '#b9876c'); TX.facN = fac(true, '#a8806b'); TX.fac2 = fac(false, '#8f7a68'); TX.fac2N = fac(true, '#7d6c60');
  TX.tile = mkTex(32, 32, (g, w, h) => { g.fillStyle = '#e9e5d6'; g.fillRect(0, 0, w, h); g.fillStyle = '#7fa0b8'; for (let y = 0; y < 32; y += 8) for (let x = 0; x < 32; x += 8) if (((x + y) / 8) % 2 === 0) g.fillRect(x + 1, y + 1, 6, 6); });
  TX.wall = mkTex(32, 32, (g, w, h) => { blocks(g, w, h, '#e4d3a4', 14, 4, 13); });
  TX.planet = [['#33b09a', '#7fcf6a', '#f2f4d0', 1], ['#262a63', '#3f3a8f', '#d8d0ff', 2], ['#c8c04a', '#4c8a38', '#ffffff', 3], ['#a8a2b8', '#9a8ab0', '#f4eef8', 4]].map(([a, b, c, s]) => mkTex(32, 16, (g, w, h) => { blocks(g, w, h, a, 24, 2, s); rs(s * 7); for (let i = 0; i < 22; i++) { g.fillStyle = rnd() < .7 ? b : c; g.fillRect(rnd() * w, rnd() * h, 2 + rnd() * 4, 2 + rnd() * 2); } if (s === 3) { for (let y = 0; y < 16; y += 4) for (let x = 0; x < 32; x += 4) { g.fillStyle = (x + y) / 4 % 2 ? '#c8c04a' : '#5a8d38'; g.fillRect(x, y, 4, 4); } } }));
}

const WORLDS = {};
// pixelado suave: reduce a 1/2 y vuelve a escalar para que se vea en bloques
function blocks2(g, w, h) { const c = g.canvas, t = document.createElement('canvas'); t.width = w / 2; t.height = h / 2; const k = t.getContext('2d'); k.drawImage(c, 0, 0, w / 2, h / 2); g.imageSmoothingEnabled = false; g.drawImage(t, 0, 0, w, h); }
const NIGHT = [.1, .12, .24, .62];
const L = (a, b, t) => lerp(a, b, t);
function cl(h, t, n) { return mixc(h, n, t); }

// ============================================================ 1 · LAS ISLAS
function worldIslas() {
  const o = {}; let m;
  o.dome = dome('#8bb0b3', '#c3dad2', '#9fc9c0', TX.cloud, 300, 3, .73);
  o.domeN = dome('#242a48', '#3b4262', '#2b3150', null, 300, 1);
  m = new MB(); m.quad([-600, -.05, 300], [600, -.05, 300], [600, -.05, -600], [-600, -.05, -600], '#ffffff', 90, 90); o.sea = m.build(TX.sea);
  m = new MB(); m.quad([-80, 0, 40], [80, 0, 40], [80, 0, -14], [-80, 0, -14], '#ffffff', 22, 16); o.sand = m.build(TX.sand);
  m = new MB(); m.cyl(0, 0, 0, .17, .24, 11, 7, '#ffffff', true, 1, 6); o.palm = m.build(TX.bark);
  m = new MB(); for (let i = 0; i < 6; i++) { m.push(M4.mul(M4.RY(i / 6 * PI * 2), M4.RZ(-.5))); m.box(1.4, 0, 0, 3, .08, .6, '#2f6a35'); m.pop(); } o.crown = m.build();
  // hamaca + hombre
  m = new MB(); for (let i = 0; i <= 8; i++) { const x = -2.3 + i * .6, y = 2.1 - Math.sin(i / 8 * PI) * .35; m.box(x, y, 0, .62, .05, .5, '#9a5a3a'); } o.ham = m.build();
  o.man = humanParts({ shirt: '#22252b', pants: '#2c4a8c', collar: true });
  // pueblo lejano
  m = new MB(); rs(31); for (let i = 0; i < 22; i++) { const x = -25 + i * 3.4, h = 1.6 + rnd() * 2.6; m.box(x, h / 2, -185, 3.2, h, 3.2, i % 3 ? '#cfc2bd' : '#c9b2a8'); m.cyl(x, h, -185, 0, 2.6, 1.2, 4, '#a07a70'); } o.town = m.build();
  m = new MB(); m.lit = false; for (let i = 0; i < 22; i++) { const x = -25 + i * 3.4; if (rnd() < .8) m.box(x, 1 + rnd() * 1.6, -182.9, .6, .7, .1, '#ffd88a'); } o.townLights = m.build();
  m = new MB(); m.cyl(52, 0, -184, 1.2, 1.6, 10, 8, '#e6e2e0'); m.cyl(52, 2.2, -184, 1.4, 1.4, 2, 8, '#a02c2c'); m.cyl(52, 5.8, -184, 1.4, 1.4, 2, 8, '#a02c2c'); m.cyl(52, 10, -184, 1.5, 0, 1.5, 8, '#2a2a2e'); o.light = m.build();
  // muelle
  m = new MB(); m.box(0, 0, -6, 2.6, .25, 14, '#ffffff', 2, 9); for (let i = 0; i < 6; i++) { m.box(-1.2, -1.2, -1 - i * 2.4, .28, 2.4, .28, '#5b422a'); m.box(1.2, -1.2, -1 - i * 2.4, .28, 2.4, .28, '#5b422a'); } o.pier = m.build(TX.wood);
  // remolino
  m = new MB(); m.lit = false; for (let r = 0; r < 6; r++) { const rad = 40 - r * 6, a = .38 + r * .1; m.cyl(0, .04 + r * .002, 0, rad, rad, .01, 24, [.03 + r * .01, .14 + r * .02, .14 + r * .02, a], false); } o.whirl = m.build(null, { trans: true });
  m = new MB(); m.lit = false; m.quad([-1, 0, -1], [1, 0, -1], [1, 0, 1], [-1, 0, 1], '#fff'); o.unit = m.build();
  o.stars = starMesh(22, 280, 2.4, '#cfd4ee', 8, .08);
  m = new MB(); m.lit = false; m.sph(0, 0, 0, 2.6, 2.6, 2.6, 8, '#ffffff'); o.moon = m.build();
  // submarino
  o.uDome = dome('#2d7a68', '#2d7a68', '#2d7a68', null, 200, 1);
  m = new MB(); m.quad([-90, 0, 90], [90, 0, 90], [90, 0, -90], [-90, 0, -90], '#ffffff', 30, 30); o.floor = m.build(TX.sand, {});
  m = new MB(); rs(77); for (let i = 0; i < 9; i++) m.sph(-20 + rnd() * 40, 0, -4 - rnd() * 22, 1.5 + rnd(), .6, 1.5 + rnd(), 6, '#2a5f50'); o.rocks = m.build();
  // monstruo: cuerpo con muchos bultos-ojo
  m = new MB(); m.amb = .6; m.dif = .4;
  m.sph(0, 8, 0, 4, 5.5, 3.6, 10, '#244f43'); m.sph(0, 12.5, 0, 3.6, 3.6, 3.2, 10, '#295a4c');
  m.cyl(-1.8, 0, 0, 1.2, 1.6, 6, 7, '#214a3e'); m.cyl(1.8, 0, 0, 1.2, 1.6, 6, 7, '#214a3e');
  m.sph(-4.5, 9, 0, 1.5, 3.5, 1.5, 6, '#214a3e'); m.sph(4.5, 9, 0, 1.5, 3.5, 1.5, 6, '#214a3e');
  o.mon = m.build();
  o.eyes = []; rs(5); const ep = [[-2.3, 13.3], [0, 14.4], [2.3, 13.3], [-3.1, 10.9], [3.1, 10.9], [0, 11.6], [-1.5, 8.3], [1.6, 8.5]];
  ep.forEach(([x, y]) => { const e = new MB(); e.lit = true; e.amb = .6; e.dif = .4; e.sph(0, 0, 0, 1.15, .75, .6, 8, '#1f4b3f'); o.eyes.push({ x, y, z: 3.0, m: e.build() }); });
  m = new MB(); m.lit = false; m.sph(0, 0, 0, .8, .5, .3, 12, '#ffffff'); o.eyeW = m.build();
  m = new MB(); m.lit = false; for (let i = 0; i < 5; i++) { const x = -22 + i * 11 + (i % 2) * 3; m.quad([x, 0, -30], [x + 4, 0, -30], [x + 14, 40, -26], [x + 10, 40, -26], [.8, 1, .9, .3]); } o.shafts = m.build(null, { trans: true });
  m = new MB(); m.lit = false; rs(3); for (let i = 0; i < 40; i++) { const p = [rnd() * 40 - 20, rnd() * 18, -4 - rnd() * 26]; m.quad([p[0], p[1], p[2]], [p[0] + .2, p[1], p[2]], [p[0] + .2, p[1] + .2, p[2]], [p[0], p[1] + .2, p[2]], '#e8fff0'); } o.snow = m.build();
  return o;
}
const ISL = {
  beach: { pos: [0, 2.3, 1.2], tgt: [0, 1.5, -10], fov: 62 },
  nsea: { pos: [0, 2.3, 1.2], tgt: [0, 1.9, -10], fov: 62 },
  whirl: { pos: [0, 2.6, -22], tgt: [0, 1.0, -80], fov: 60, nosand: true },
  town: { pos: [0, 2.2, -110], tgt: [24, 3.0, -185], fov: 26, nosand: true },
  pier: { pos: [0, 3.2, -18], tgt: [0, 1.4, -60], fov: 60, nosand: true, pier: true },
  under: { pos: [0, 5.5, 18], tgt: [0, 8, 0], fov: 62, under: true },
  eyes: { pos: [0, 14.2, 6.0], tgt: [0, 13.2, -2], fov: 64, under: true },
};
function renderIslas(o, shot, t, st) {
  const S = ISL[shot], n = st.night || 0, tint = [NIGHT[0], NIGHT[1], NIGHT[2], n * NIGHT[3]];
  if (S.under) {
    clear([.18, .5, .43, 1]); cur.fogr = [8, 70]; setCam({ pos: S.pos, tgt: S.tgt, fov: S.fov });
    draw(o.uDome, M4.T(...S.pos), { nofog: true, nodepth: true });
    draw(o.floor, M4.T(0, 0, 0), { tint: [.1, .25, .2, .25] });
    draw(o.rocks, M4.id());
    const sway = Math.sin(t * .6) * .15;
    draw(o.mon, M4.mul(M4.T(0, 0, -2), M4.RY(sway)), { tint: [.1, .3, .25, .1] });
    o.eyes.forEach((e, i) => {
      const open = (st.eye && st.eye.i === i) ? st.eye.v : 0, base = M4.mul(M4.T(e.x, e.y, e.z - 2), M4.RY(sway));
      draw(e.m, base, {});
      if (open > 0) draw(o.eyeW, M4.mul(base, M4.mul(M4.T(0, -.1, .55), M4.S(1, open * 1.1, 1))), { nofog: true });
    });
    draw(o.shafts, M4.T(0, 0, 0), { nofog: true });
    draw(o.snow, M4.T((t * .2) % 1, -((t * .5) % 18) + 9, 0), { nofog: true });
    return;
  }
  const day = mixc('#c3dad2', '#3b4262', n); clear(day); cur.fogr = [30, 230];
  setCam({ pos: S.pos, tgt: S.tgt, fov: S.fov });
  draw(n > .5 ? o.domeN : o.dome, M4.T(...S.pos), { nofog: true, nodepth: true, tint: n > .5 ? null : tint });
  if (n > .5) { draw(o.stars, M4.T(...S.pos), { nofog: true, nodepth: true }); draw(o.moon, M4.T(S.pos[0] + 48, S.pos[1] + 96, S.pos[2] - 200), { nofog: true }); }
  const seaT = n > 0 ? tint : null; draw(o.sea, M4.id(), { tint: seaT });
  if (!S.nosand) draw(o.sand, M4.id(), { tint: seaT });
  if (st.town !== false) { draw(o.town, M4.id(), { tint: n > 0 ? [.1, .1, .2, .5 * n] : null }); if (n > .5 && st.lights !== false) draw(o.townLights, M4.id(), { nofog: true }); draw(o.light, M4.id(), { tint: n > 0 ? [.1, .1, .2, .5 * n] : null }); if (n > .5) { const m2 = new MB(); } }
  if (st.whirl) { const k = 1 + Math.sin(t) * .02; draw(o.whirl, M4.mul(M4.T(0, 0, -60), M4.S(.26 * k, 1, .2 * k)), { tint: seaT }); }
  if (S.pier) draw(o.pier, M4.T(S.pos[0] + 3.4, 1.2 - 3.2 + 1.6, S.pos[2] - 5), { tint: seaT });
  if (shot === 'beach' || shot === 'nsea') {
    draw(o.palm, M4.mul(M4.T(-1.35, 0, -2.6), M4.RZ(-.06)), { tint: seaT }); draw(o.palm, M4.mul(M4.T(1.4, 0, -2.4), M4.RZ(.05)), { tint: seaT });
        draw(o.ham, M4.T(0, -.75, .6), { tint: seaT });

    draw(o.pier, M4.T(4.6, 1.0, -8), { tint: seaT });
  }
}

// ============================================================ 2 · LAS ESTRELLAS
function worldEstrellas() {
  const o = {}; let m;
  o.dome = dome('#56638f', '#7d84aa', '#2a463c', TX.milky, 300, 3, .75);
  m = new MB(); m.quad([-300, 0, 300], [300, 0, 300], [300, 0, -300], [-300, 0, -300], '#ffffff', 80, 80); o.ground = m.build(TX.grass);
  m = new MB(); rs(4); m.quad([-300, .05, -45], [300, .05, -45], [300, 6, -60], [-300, 6, -60], '#1b2d31'); o.ridge = m.build();
  m = new MB(); m.box(0, .8, 0, 3.2, .12, 1.2, '#9aa28e'); m.box(-.5, .93, 0, 1.4, .06, 1.22, '#b8343a', 4, 4); for (const [x, z] of [[-1.4, .5], [-1.4, -.5], [1.4, .5], [1.4, -.5]]) m.box(x, .4, z, .08, .8, .08, '#aaaeb0'); m.box(1.2, 1.0, 0, .7, .15, .5, '#f0f0f0'); o.cot = m.build();
  m = new MB(); m.push(M4.mul(M4.T(0, 1.9, 0), M4.RZ(.42))); m.cyl(0, -1.0, 0, .17, .17, 2.6, 8, '#f2f2f6'); m.cyl(0, -1.3, 0, .14, .14, .4, 8, '#33343a'); m.pop();
  for (const a of [0, 2.1, 4.2]) { m.push(M4.RY(a)); m.quad([0, 1.8, 0], [0, 1.8, .05], [1.2, 0, .05], [1.2, 0, 0], '#1a1a1e'); m.pop(); } o.tele = m.build();
  m = new MB(); m.sph(0, 0, 0, 1.4, .8, 1.1, 6, '#5a5c60'); o.rock = m.build();
  o.man = humanParts({ shirt: '#22252b', pants: '#2c4a8c', collar: true });
  o.st = [starMesh(90, 280, .4, '#ffffff', 21, .05), starMesh(260, 280, .55, '#ffffff', 22, .05), starMesh(520, 280, .75, '#ffffff', 23, .05), starMesh(900, 280, 1.1, '#ffffff', 24, .05)];
  // gigante
  m = new MB(); m.lit = false; const K = '#050507';
  m.box(0, 6, 0, 5.2, 12, 1.4, K); m.box(0, 13, 0, 3.4, 3, 1.4, K); m.box(-1.9, 14, 0, 1.1, 5, 1.2, K); m.box(1.9, 14, 0, 1.1, 5, 1.2, K); m.box(0, 17.2, 0, 1.6, 2.2, 1.2, K);
  m.box(-1.9, 4.5, 0, 1.3, 9, 1.3, K); m.box(1.9, 4.5, 0, 1.3, 9, 1.3, K);
  for (const sx of [-3.6, 3.6]) { m.box(sx, 6, 0, 1.1, 14, 1, K); for (let i = 0; i < 6; i++) m.box(sx + (i - 2.5) * .18, -1.4 - i * .1, 0, .06, 2.2, .06, K); }
  m.box(-1.0, -4, 0, 1.6, 22, 1.2, K); m.box(1.0, -4, 0, 1.6, 22, 1.2, K);
  o.giant = m.build();
  m = new MB(); m.lit = false; m.sph(0, 0, .8, 1.9, 2.5, .1, 12, '#a193e0'); o.hole = m.build();
  return o;
}
const EST = {
  wide: { pos: [0, 1.6, 12], tgt: [1.2, 3.2, -6], fov: 58 },
  cot: { pos: [1.5, 1.15, -6.1], tgt: [1.9, 5.6, -9.2], fov: 64 },
  cot2: { pos: [1.0, 1.15, -6.1], tgt: [3.2, 2.5, -6.7], fov: 66 },
  look: { pos: [3.05, 1.55, -5.9], tgt: [3.3, 6, -9], fov: 60 },
  sky: { pos: [0, 1.6, 12], tgt: [0, 40, -20], fov: 62 },
  giant: { pos: [0, 1.6, 2], tgt: [0, 11, -40], fov: 62 },
};
function renderEstrellas(o, shot, t, st) {
  const S = EST[shot], g = st.grow === undefined ? 0 : st.grow;
  clear([.11, .15, .19, 1]); cur.fogr = [60, 160]; setCam({ pos: S.pos, tgt: S.tgt, fov: S.fov });
  draw(o.dome, M4.T(...S.pos), { nofog: true, nodepth: true });
  const lv = Math.min(3, Math.floor(g * 4)); draw(o.st[lv], M4.T(...S.pos), { nofog: true, nodepth: true }); if (lv > 0 && g * 4 % 1 > .5) draw(o.st[Math.min(3, lv + 1)], M4.T(...S.pos), { nofog: true, nodepth: true, tint: [1, 1, 1, 0] });
  draw(o.ground, M4.id(), { tint: [.05, .08, .1, .0] }); draw(o.ridge, M4.id());
  draw(o.rock, M4.T(-4.5, .2, -9));
  const cot = M4.mul(M4.T(1.6, 0, -6.2), M4.RY(-.3)); draw(o.cot, cot); draw(o.tele, M4.mul(M4.T(3.3, .0, -6.6), M4.S(1, 1, 1)));


  if (shot === 'giant' || st.giant) { const k = st.giant || 1; draw(o.giant, M4.mul(M4.T(0, 3.6 * k, -40 + (1 - k) * -10), M4.S(k, k, 1))); draw(o.hole, M4.mul(M4.T(0, 13.4 * k, -40 + (1 - k) * -10), M4.S(k, k, 1)), { nofog: true }); }
}

// ============================================================ 3 · NADIE TIENE PRISA
function worldPlaza() {
  const o = {}; let m;
  o.dome = dome('#9fb3bd', '#dccfae', '#c9c29b', TX.cloud, 300, 3, .73);
  m = new MB(); m.quad([-200, 0, 200], [200, 0, 200], [200, 0, -200], [-200, 0, -200], '#ffffff', 80, 80); o.ground = m.build(TX.cobble);
  m = new MB(); m.box(0, 9, 0, 8, 18, 8, '#ffffff', 2, 5); m.cyl(0, 18, 0, 0, 7, 5, 4, '#a83a2a', true); o.tower = m.build(TX.brick);
  m = new MB(); m.box(0, 1.6, 4.1, 1.8, 3.2, .1, '#5a3a22'); o.door = m.build();
  const face = new MB(); face.lit = false; face.push(M4.RX(PI / 2)); face.cyl(0, -.2, -16, 2, 2, .2, 16, '#eee8d8', true); face.pop();
  m = new MB(); m.lit = false; for (let i = 0; i < 12; i++) { const a = i / 12 * PI * 2, x = Math.sin(a) * 1.75, y = Math.cos(a) * 1.75; m.box(x * 1.1, 13 + y * 1.1, 4.3, i % 3 ? .12 : .2, i % 3 ? .3 : .45, .05, '#222'); } o.ticks = m.build();
  m = new MB(); m.lit = false; m.push(M4.mul(M4.T(0, 13, 4.1), M4.RX(PI / 2))); m.cyl(0, -.12, 0, 2.6, 2.6, .12, 18, '#3a2418', true); m.cyl(0, -.06, 0, 2.15, 2.15, .12, 18, '#eee8d8', true); m.pop(); o.face = m.build();
  m = new MB(); m.lit = false; m.box(0, .8, 0, .2, 1.8, .05, '#111'); o.hMin = m.build(); m = new MB(); m.lit = false; m.box(0, .5, 0, .24, 1.1, .05, '#111'); o.hHr = m.build();
  const house = (c, r) => { const h = new MB(); h.box(0, 2.2, 0, 7, 4.4, 6, c); h.cyl(0, 4.4, 0, 0, 5.4, 2.2, 4, r, true); h.box(-1.5, 1.6, 3.05, 1.2, 2.2, .1, '#5a3a22'); h.box(1.8, 2.6, 3.05, 1.2, 1.2, .1, '#2c3a40'); return h.build(); };
  o.h1 = house('#e3d08a', '#a8382a'); o.h2 = house('#cdd49a', '#a8382a'); o.h3 = house('#d9b27a', '#8a3a2a');
  o.man = humanParts({ shirt: '#c9606e', pants: '#4a5a4a', skin: '#c98d6a', shoes: '#4a2a1a' });
  o.baker = humanParts({ shirt: '#f0f0ec', pants: '#555', skin: '#d9a07a', hat: true, apron: true, shoes: '#222' });
  o.you = humanParts({ shirt: '#22252b', pants: '#2c4a8c', collar: true });
  o.old = humanParts({ shirt: '#6a7048', pants: '#4a4a3a', skin: '#c8a080', shoes: '#222' });
  m = new MB(); m.sph(0, 0, 0, .35, .22, .3, 6, '#6a6a70'); m.push(M4.RZ(.9)); m.box(.6, .3, 0, 1.1, .05, .5, '#55555c'); m.pop(); m.push(M4.RZ(-.9)); m.box(-.6, .3, 0, 1.1, .05, .5, '#55555c'); m.pop(); o.pigeon = m.build();
  m = new MB(); m.sph(0, 0, 0, .9, .8, .9, 8, '#b0b0b4'); o.bread = m.build();
  // tentaculos
  m = new MB(); rs(15); for (let i = 0; i < 9; i++) { const a = (i / 9) * PI * 2; for (let s = 0; s < 22; s++) { const ang = a + Math.sin(s * .25 + i) * .35 + s * .02 * (i % 2 ? 1 : -1), r0 = 2.7 + s * .26; m.sph(Math.cos(ang) * r0, 13 + Math.sin(ang) * r0 - s * s * .01, 4.4 + s * .01, .55 - s * .014, .55 - s * .014, .45, 6, '#a2a2a0'); } } o.tent = m.build();
  // cocina
  m = new MB(); m.quad([-6, 0, -2], [6, 0, -2], [6, 7, -2], [-6, 7, -2], '#fff', 3, 3); o.kWall = m.build(TX.wall);
  m = new MB(); m.quad([-6, 0, -1.98], [6, 0, -1.98], [6, 2.4, -1.98], [-6, 2.4, -1.98], '#fff', 6, 1.2); o.kTile = m.build(TX.tile);
  m = new MB(); m.lit = false; m.cyl(1, 5, -1.95, .9, .9, .1, 16, '#c0332e', true); m.cyl(1, 5, -1.9, .72, .72, .1, 16, '#efe9dc', true); m.box(0, 3.4, -1.85, 5, .12, .3, '#7a5a3a'); m.cyl(-1.5, 3.46, -1.7, .35, .35, .9, 8, '#d9a64a'); m.cyl(-.7, 3.46, -1.7, .35, .35, .9, 8, '#87a870'); m.box(1.7, 3.8, -1.7, .4, .4, .3, '#c84a3a'); o.kit = m.build();
  m = new MB(); m.box(0, 3, -1.5, 14, 6, .1, '#2c2826'); m.lit = false; m.push(M4.mul(M4.T(0, 3, -1.4), M4.RX(PI / 2))); m.cyl(0, -.1, 0, 1.45, 1.45, .1, 18, '#cfcfcf', true); m.cyl(0, -.05, 0, 1.2, 1.2, .1, 18, '#ecebe2', true); m.pop(); o.wrist = m.build();
  m = new MB(); m.box(0, 0, 0, 1.2, .6, 3, '#8a4a1e'); m.box(0, -.4, .3, 1.4, .3, 3.6, '#2a2a2e'); m.box(0, 2.2, -.2, 1.0, 4, 1, '#2c4a8c'); o.boot = m.build();
  return o;
}
const PLZ = {
  tower: { pos: [0, 1.6, 6], tgt: [0, 6.4, -26], fov: 58 },
  walkers: { pos: [0, 1.4, 12], tgt: [0, 3.6, -26], fov: 58 },
  clock: { pos: [0, 13, -14], tgt: [0, 13, -30], fov: 40, tx: true },
  kitchen: { pos: [0, 4.2, 6], tgt: [0, 4.2, -2], fov: 46, kitchen: true },
  wrist: { pos: [0, 3, 5], tgt: [0, 3, -2], fov: 32, wrist: true },
  baker: { pos: [0, 1.5, 6], tgt: [0, 2.3, -6], fov: 58 },
  tent: { pos: [0, 12, -8], tgt: [0, 13, -30], fov: 58 },
  foot: { pos: [0, 1.6, 3], tgt: [0, 1.1, -7], fov: 62, bob: true },
};
function renderPlaza(o, shot, t, st) {
  const S = PLZ[shot]; clear([.83, .78, .66, 1]); cur.fogr = [40, 150]; const bb = S.bob ? [Math.sin(t * 2.2) * .06, Math.abs(Math.sin(t * 2.2)) * .1 - .05] : [0, 0]; setCam({ pos: [S.pos[0] + bb[0], S.pos[1] + bb[1], S.pos[2]], tgt: S.tgt, fov: S.fov });
  if (S.kitchen || S.wrist) { clear([.9, .85, .7, 1]); cur.fogr = [999, 1000]; if (S.kitchen) { draw(o.kWall, M4.id()); draw(o.kTile, M4.id()); draw(o.kit, M4.id()); const a = st.clk || 0; drawHands(o, M4.mul(M4.T(1, 5, -1.78), M4.S(.5, .5, .5)), a); } else { draw(o.wrist, M4.id()); drawHands(o, M4.mul(M4.T(0, 3, -1.3), M4.S(.5, .5, .5)), st.clk || 0); } return; }
  draw(o.dome, M4.T(...S.pos), { nofog: true, nodepth: true }); draw(o.ground, M4.id());
  draw(o.tower, M4.T(0, 0, -30)); draw(o.door, M4.T(0, 0, -30)); draw(o.face, M4.T(0, 0, -30)); draw(o.ticks, M4.T(0, 0, -30), { nofog: true }); drawHands(o, M4.T(0, 13, -25.8), st.clk || 0, true);
  draw(o.h1, M4.mul(M4.T(11, 0, -20), M4.RY(.3))); draw(o.h2, M4.mul(M4.T(-14, 0, -18), M4.RY(-.4))); draw(o.h3, M4.mul(M4.T(-30, 0, -8), M4.RY(.7))); draw(o.h3, M4.mul(M4.T(26, 0, -6), M4.RY(-.9)));
  if (shot === 'tent' || st.tent) draw(o.tent, M4.T(0, 0, -30));
  const frz = st.freeze; const w = (frz ? 0 : t);
  const walk = (P, x, z, dir, sp, ph) => { const k = Math.sin(w * sp + ph); drawHuman(P, M4.mul(M4.T(x, 0, z), M4.RY(dir)), { leg: k * .5, arm: -k * .4 }); };
  if (shot === 'walkers' || shot === 'tower' || shot === 'tent') { walk(o.man, -4 + ((w * .25) % 10) * .6, -10, PI / 2, 1.2, 0); walk(o.old, 6 - ((w * .2) % 10), -16, -PI / 2, 1.1, 1); walk(o.man, -8 + ((w * .15) % 12), -22, PI / 2, 1.0, 2); }
  if (shot === 'baker') {
    const B = M4.mul(M4.T(1, 0, -4), M4.RY(-.3)); drawHuman(o.baker, B, { leg: .55, arm: 1.0 }); draw(o.pigeon, M4.T(-3.4, 5.5, -3), {}); draw(o.h1, M4.mul(M4.T(-9, 0, -6), M4.RY(.7)));
  }
  if (shot === 'foot') { }
}
function drawHands(o, base, tm, flip) {
  draw(o.hMin, M4.mul(base, M4.RZ(-(tm || 0) - 4.0 * 1 - (flip ? 0 : 0))), { nofog: true });
  draw(o.hHr, M4.mul(base, M4.RZ(-(tm || 0) / 12 - 2.2)), { nofog: true });
}

// ============================================================ 4 · EL MAS TRANQUILO
function worldCalle() {
  const o = {}; let m;
  o.domeD = dome('#8fb1b6', '#d9c8a8', '#6a6a70', TX.cloud, 300, 3, .73); o.domeN = dome('#1d2548', '#59607e', '#232846', TX.milky, 300, 3, .75);
  o.domeS = dome('#4f6a8a', '#a99890', '#3a3a48', TX.cloud, 300, 3, .73);
  o.stars = starMesh(150, 280, .5, '#e8ecff', 41, .05);
  m = new MB(); m.quad([-6, 0, 20], [6, 0, 20], [6, 0, -220], [-6, 0, -220], '#ffffff', 3, 60); o.road = m.build(TX.asphalt);
  m = new MB(); m.quad([-12, .05, 20], [-6, .05, 20], [-6, .05, -220], [-12, .05, -220], '#9a8f84', 1, 40); m.quad([6, .05, 20], [12, .05, 20], [12, .05, -220], [6, .05, -220], '#9a8f84', 1, 40); o.walk = m.build(TX.cobble);
  const bl = (tx, seedv) => { const b = new MB(); rs(seedv); for (let i = 0; i < 20; i++) for (const sx of [-1, 1]) { const w = 9 + rnd() * 3, h = 12 + rnd() * 12, z = 12 - i * 11; b.push(M4.T(sx * (12 + w / 2), 0, z)); b.box(0, h / 2, 0, w, h, 10, '#ffffff', w / 3.2, h / 4); b.pop(); } return b.build(tx); };
  o.bD = bl(TX.fac, 51); o.bN = bl(TX.facN, 51);
  m = new MB(); m.lit = false; for (let i = 0; i < 14; i++) for (const sx of [-1, 1]) { m.box(sx * 7, 3.4, 8 - i * 14, .12, 6.8, .12, '#222'); m.sph(sx * 7, 7, 8 - i * 14, .45, .45, .45, 6, '#ffe6a0'); } o.lamps = m.build();
  o.man = humanParts({ shirt: '#22252b', pants: '#2c4a8c', collar: true });
  o.n1 = humanParts({ shirt: '#c9606e', pants: '#4a5a4a' }); o.n2 = humanParts({ shirt: '#8a9ab0', pants: '#6a6a50', skin: '#d0a888' });
  m = new MB(); m.lit = false; m.sph(0, 0, 0, 58, 40, 58, 16, '#04040a'); o.hood = m.build();
  m = new MB(); m.lit = false; rs(23); for (let i = 0; i < 16; i++) { const x = -36 + i * 4.8 + rnd() * 2, z = -8 - rnd() * 14; for (let s = 0; s < 18; s++) m.sph(x + Math.sin(s * .4 + i) * 1.5, -s * 1.5, z, 1.5 - s * .05, 1.2, 1.5 - s * .05, 5, '#1d2e52'); } o.tent = m.build();
  // dormitorio
  m = new MB(); m.quad([-10, 0, 0], [10, 0, 0], [10, 0, -14], [-10, 0, -14], '#fff', 4, 4); o.floor = m.build(TX.wood);
  m = new MB(); m.box(0, 4, -8, 20, 8, .3, '#c79a62'); m.box(-4.5, 4.5, -7.7, 1, 6, .2, '#2a2f60'); m.box(-3.2, 4.5, -7.7, 1, 6, .2, '#eaeaf0'); o.room = m.build();
  m = new MB(); m.box(0, .7, 0, 6, .8, 9, '#5a3a22'); m.box(0, 1.3, 0, 5.6, .5, 8.6, '#f0eef0'); m.box(0, 2.5, -4.6, 5.6, 2.4, .3, '#7a4a2a'); m.box(4.2, .8, -3, 1.6, 1.6, 1.6, '#4a2a1a'); m.cyl(4.2, 1.6, -3, .5, .8, 1.2, 6, '#f2e6b8'); o.bed = m.build();
  return o;
}
const CAL = {
  day: { pos: [0, 1.7, 14], tgt: [0, 3.1, -60], fov: 62 },
  neigh: { pos: [-2, 1.6, 6], tgt: [-3, 1.5, -2], fov: 60 },
  hood: { pos: [0, 2, 12], tgt: [0, 30, -20], fov: 66 },
  street: { pos: [0, 1.7, 14], tgt: [0, 3.1, -60], fov: 62 },
  room: { pos: [0, 1.95, -1.3], tgt: [0, 3.7, -8], fov: 64, room: true },
};
function renderCalle(o, shot, t, st) {
  const S = CAL[shot], n = st.night || 0, d = st.dusk || 0; cur.fogr = [30, 200];
  const base = mixc(mixc('#d9c8a8', '#a99890', d), '#3a4060', n); clear(base); setCam({ pos: S.pos, tgt: S.tgt, fov: S.fov });
  if (S.room) { clear([.18, .17, .22, 1]); cur.fogr = [999, 1000]; draw(o.floor, M4.id()); draw(o.room, M4.id()); draw(o.bed, M4.T(0, 0, -3)); return; }
  draw(n > .5 ? o.domeN : (d > .5 ? o.domeS : o.domeD), M4.T(...S.pos), { nofog: true, nodepth: true }); if (n > .3) draw(o.stars, M4.T(...S.pos), { nofog: true, nodepth: true });
  const tint = n > 0 ? [.1, .1, .2, .45 * n] : (d > 0 ? [.4, .3, .3, .2 * d] : null);
  draw(o.road, M4.id(), { tint }); draw(o.walk, M4.id(), { tint }); draw(n > .5 ? o.bN : o.bD, M4.id(), { tint: n > .5 ? [.1, .1, .22, .35] : tint }); draw(o.lamps, M4.id(), { nofog: n < .3 ? false : true });
  if (shot === 'neigh') { draw(o.bD, M4.id()); drawHuman(o.n1, M4.mul(M4.T(-4, 0, -2), M4.RY(.8)), { leg: .2 * Math.sin(t * 3), arm: .9 * Math.sin(t * 3) }); drawHuman(o.n2, M4.mul(M4.T(-2.8, 0, -2), M4.RY(.3)), { arm: .5 * Math.sin(t * 2) }); }
  if (st.hood || shot === 'hood') { const k = st.hood === undefined ? 1 : st.hood; draw(o.hood, M4.T(0, 130 - k * 40, -30), {}); draw(o.tent, M4.T(0, 130 - k * 40 - 38, -18), {}); }
}

// ============================================================ Seleccion de planetas
const PL = {};
function initPlanets() {
  const m = new MB(); m.lit = true; m.amb = .65; m.dif = .45; m.sph(0, 0, 0, 1, 1, 1, 14, '#ffffff'); PL.sph = m.build();
  const r = new MB(); r.lit = false; r.sph(0, 0, 0, 1.08, 1.08, 1.08, 14, [.58, .4, .95, 1]); PL.ring = r.build();
  PL.stars = starMesh(260, 100, .5, '#ffffff', 61, -1);
  PL.bg = mkTex(32, 64, (g, w, h) => { const gr = g.createLinearGradient(0, 0, 0, h); gr.addColorStop(0, '#3e4a6e'); gr.addColorStop(.5, '#5c4e92'); gr.addColorStop(1, '#6b4aa8'); g.fillStyle = gr; g.fillRect(0, 0, w, h); rs(2); for (let i = 0; i < 60; i++) { g.fillStyle = `rgba(190,150,230,${.1 + rnd() * .15})`; g.fillRect(rnd() * w, rnd() * h, 3 + rnd() * 6, 2 + rnd() * 3); } });
  const q = new MB(); q.lit = false; q.quad([-60, -100, -90], [60, -100, -90], [60, 100, -90], [-60, 100, -90], '#fff'); PL.back = q.build(PL.bg);
}
const PLC = [[96, 220], [260, 220], [96, 410], [260, 410]];
function renderPlanets(t, zoom, pick) {
  clear([.3, .25, .5, 1]); cur.fogr = [999, 1000]; setCam({ pos: [0, 0, 0], tgt: [0, 0, -1], fov: 60 });
  draw(PL.back, M4.id(), { nofog: true, nodepth: true }); draw(PL.stars, M4.id(), { nofog: true });
  const th = Math.tan(30 * PI / 180);
  PLC.forEach(([sx, sy], i) => {
    const d = 40 - (pick === i ? zoom * 22 : zoom * 14), z = -d, r = 70 / RH * 2 * th * d, wx = ((sx - 15 * zoom * (i % 2 ? -1 : 1)) / RW * 2 - 1) * th * (RW / RH) * d, wy = -(sy / RH * 2 - 1) * th * d;
    const mm = M4.mul(M4.T(wx, wy, z), M4.mul(M4.S(r, r, r), M4.mul(M4.RX(.3), M4.RY(t * .4 + i))));
    if (i === 1) draw(PL.ring, M4.mul(M4.T(wx, wy, z - r * .16), M4.S(r * 1.07, r * 1.07, r * 1.07)), { nofog: true });
    draw({ ...PL.sph, tex: TX.planet[i] }, mm, { nofog: true });
  });
}
