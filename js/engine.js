'use strict';
// ===== Motor 3D estilo PS1: 360x640, vertices que "tiemblan", textura nearest, dithering =====
const RW = 360, RH = 640;
const cv = document.getElementById('gl'), ui = document.getElementById('ui');
cv.width = ui.width = RW; cv.height = ui.height = RH;
const gl = cv.getContext('webgl', { antialias: false, alpha: false, preserveDrawingBuffer: true });
const X = ui.getContext('2d');
const PI = Math.PI;
let seed = 1;
const rnd = () => (seed = (seed * 16807) % 2147483647) / 2147483647;
const rs = s => { seed = s | 0 || 1; };
const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
const lerp = (a, b, t) => a + (b - a) * t;
const hex = h => { if (Array.isArray(h)) return h; if (h.length === 4) h = '#' + h[1] + h[1] + h[2] + h[2] + h[3] + h[3]; const n = parseInt(h.slice(1), 16); return [(n >> 16 & 255) / 255, (n >> 8 & 255) / 255, (n & 255) / 255, 1]; };
const mixc = (a, b, t) => { a = hex(a); b = hex(b); return [lerp(a[0], b[0], t), lerp(a[1], b[1], t), lerp(a[2], b[2], t), 1]; };

// ---------- mat4 (column-major) ----------
const M4 = {
  id: () => [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1],
  mul(a, b) { const o = new Array(16); for (let i = 0; i < 4; i++) for (let j = 0; j < 4; j++) { let s = 0; for (let k = 0; k < 4; k++) s += a[k * 4 + j] * b[i * 4 + k]; o[i * 4 + j] = s; } return o; },
  T: (x, y, z) => [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, x, y, z, 1],
  S: (x, y, z) => [x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1],
  RX(a) { const c = Math.cos(a), s = Math.sin(a); return [1, 0, 0, 0, 0, c, s, 0, 0, -s, c, 0, 0, 0, 0, 1]; },
  RY(a) { const c = Math.cos(a), s = Math.sin(a); return [c, 0, -s, 0, 0, 1, 0, 0, s, 0, c, 0, 0, 0, 0, 1]; },
  RZ(a) { const c = Math.cos(a), s = Math.sin(a); return [c, s, 0, 0, -s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]; },
  persp(fov, asp, n, f) { const t = 1 / Math.tan(fov / 2); return [t / asp, 0, 0, 0, 0, t, 0, 0, 0, 0, (f + n) / (n - f), -1, 0, 0, 2 * f * n / (n - f), 0]; },
  look(e, t, u) {
    let z = [e[0] - t[0], e[1] - t[1], e[2] - t[2]], l = Math.hypot(...z); z = z.map(v => v / l);
    let x = [u[1] * z[2] - u[2] * z[1], u[2] * z[0] - u[0] * z[2], u[0] * z[1] - u[1] * z[0]]; l = Math.hypot(...x); x = x.map(v => v / l);
    const y = [z[1] * x[2] - z[2] * x[1], z[2] * x[0] - z[0] * x[2], z[0] * x[1] - z[1] * x[0]];
    return [x[0], y[0], z[0], 0, x[1], y[1], z[1], 0, x[2], y[2], z[2], 0, -(x[0] * e[0] + x[1] * e[1] + x[2] * e[2]), -(y[0] * e[0] + y[1] * e[1] + y[2] * e[2]), -(z[0] * e[0] + z[1] * e[1] + z[2] * e[2]), 1];
  },
  pt: (m, p) => [m[0] * p[0] + m[4] * p[1] + m[8] * p[2] + m[12], m[1] * p[0] + m[5] * p[1] + m[9] * p[2] + m[13], m[2] * p[0] + m[6] * p[1] + m[10] * p[2] + m[14]],
  dir: (m, p) => [m[0] * p[0] + m[4] * p[1] + m[8] * p[2], m[1] * p[0] + m[5] * p[1] + m[9] * p[2], m[2] * p[0] + m[6] * p[1] + m[10] * p[2]],
};

// ---------- shaders ----------
const VS = `attribute vec3 p;attribute vec4 c;attribute vec2 t;uniform mat4 mvp;varying vec4 vc;varying vec2 vt;varying float fz;
void main(){vec4 q=mvp*vec4(p,1.);vec2 s=q.xy/q.w;s=floor(s*vec2(180.,320.)+.5)/vec2(180.,320.);gl_Position=vec4(s*q.w,q.z,q.w);vc=c;vt=t;fz=q.w;}`;
const FS = `precision mediump float;varying vec4 vc;varying vec2 vt;varying float fz;uniform sampler2D tx;uniform float ut;uniform vec3 fogc;uniform vec2 fogr;uniform vec4 tint;
void main(){vec4 col=vc;if(ut>.5){col*=texture2D(tx,vt);}if(col.a<.04)discard;
col.rgb=mix(col.rgb,tint.rgb,tint.a);float f=clamp((fz-fogr.x)/(fogr.y-fogr.x),0.,1.);col.rgb=mix(col.rgb,fogc,f);
vec2 fc=floor(gl_FragCoord.xy);float b=mod(mod(fc.x,2.)*2.+mod(fc.y,2.)*3.,4.)/4.-.375;col.rgb=floor(col.rgb*31.+.5+b*.55)/31.;gl_FragColor=col;}`;
function sh(t, s) { const o = gl.createShader(t); gl.shaderSource(o, s); gl.compileShader(o); if (!gl.getShaderParameter(o, gl.COMPILE_STATUS)) throw gl.getShaderInfoLog(o); return o; }
const prog = gl.createProgram(); gl.attachShader(prog, sh(gl.VERTEX_SHADER, VS)); gl.attachShader(prog, sh(gl.FRAGMENT_SHADER, FS)); gl.linkProgram(prog); gl.useProgram(prog);
const loc = n => gl.getUniformLocation(prog, n), U = { mvp: loc('mvp'), tx: loc('tx'), ut: loc('ut'), fogc: loc('fogc'), fogr: loc('fogr'), tint: loc('tint') };
const A = { p: gl.getAttribLocation(prog, 'p'), c: gl.getAttribLocation(prog, 'c'), t: gl.getAttribLocation(prog, 't') };
gl.enableVertexAttribArray(A.p); gl.enableVertexAttribArray(A.c); gl.enableVertexAttribArray(A.t);
gl.enable(gl.DEPTH_TEST); gl.enable(gl.BLEND); gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);
gl.viewport(0, 0, RW, RH);

// ---------- texturas procedurales ----------
function mkTex(w, h, fn) {
  const c = document.createElement('canvas'); c.width = w; c.height = h; const g = c.getContext('2d'); fn(g, w, h);
  const t = gl.createTexture(); gl.bindTexture(gl.TEXTURE_2D, t);
  gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, c);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
  gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.REPEAT); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.REPEAT);
  return t;
}
const rgb = (r, g, b) => `rgb(${r | 0},${g | 0},${b | 0})`;
// ruido en bloques (para mar, arena, nubes)
function blocks(g, w, h, base, amp, cell, seedv) {
  rs(seedv); const b = hex(base);
  for (let y = 0; y < h; y += cell) for (let x = 0; x < w; x += cell) { const k = (rnd() - .5) * amp; g.fillStyle = rgb((b[0] * 255 + k) , (b[1] * 255 + k), (b[2] * 255 + k)); g.fillRect(x, y, cell, cell); }
}

// ---------- constructor de mallas ----------
class MB {
  constructor() { this.a = []; this.xf = M4.id(); this.st = []; this.lit = true; this.LD = [.35, .8, .45]; const l = Math.hypot(...this.LD); this.LD = this.LD.map(v => v / l); this.amb = .5; this.dif = .5; }
  push(m) { this.st.push(this.xf); this.xf = M4.mul(this.xf, m); return this; }
  pop() { this.xf = this.st.pop(); return this; }
  tri(p0, p1, p2, c, u0 = [0, 0], u1 = [1, 0], u2 = [1, 1]) {
    const e1 = [p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]], e2 = [p2[0] - p0[0], p2[1] - p0[1], p2[2] - p0[2]];
    let n = [e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]]; const l = Math.hypot(...n) || 1; n = M4.dir(this.xf, n.map(v => v / l));
    const lt = this.lit ? this.amb + this.dif * Math.max(0, n[0] * this.LD[0] + n[1] * this.LD[1] + n[2] * this.LD[2]) : 1;
    [[p0, u0], [p1, u1], [p2, u2]].forEach(([p, u]) => {
      const col = typeof c === 'function' ? c(p) : hex(c); const q = M4.pt(this.xf, p);
      this.a.push(q[0], q[1], q[2], col[0] * lt, col[1] * lt, col[2] * lt, col[3] === undefined ? 1 : col[3], u[0], u[1]);
    });
    return this;
  }
  quad(a, b, c, d, col, us = 1, vs = 1) { this.tri(a, b, c, col, [0, 0], [us, 0], [us, vs]); this.tri(a, c, d, col, [0, 0], [us, vs], [0, vs]); return this; }
  box(x, y, z, w, h, d, col, us = 1, vs = 1) {
    const a = x - w / 2, b = x + w / 2, c = y - h / 2, e = y + h / 2, f = z - d / 2, g = z + d / 2;
    this.quad([a, c, g], [b, c, g], [b, e, g], [a, e, g], col, us, vs); this.quad([b, c, f], [a, c, f], [a, e, f], [b, e, f], col, us, vs);
    this.quad([a, c, f], [a, c, g], [a, e, g], [a, e, f], col, us, vs); this.quad([b, c, g], [b, c, f], [b, e, f], [b, e, g], col, us, vs);
    this.quad([a, e, g], [b, e, g], [b, e, f], [a, e, f], col, us, vs); this.quad([a, c, f], [b, c, f], [b, c, g], [a, c, g], col, us, vs); return this;
  }
  cyl(x, y, z, rt, rb, h, sg, col, cap = true, us = 1, vs = 1) {
    for (let i = 0; i < sg; i++) {
      const a0 = i / sg * PI * 2, a1 = (i + 1) / sg * PI * 2, c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
      this.quad([x + c0 * rb, y, z + s0 * rb], [x + c1 * rb, y, z + s1 * rb], [x + c1 * rt, y + h, z + s1 * rt], [x + c0 * rt, y + h, z + s0 * rt], col, us / sg, vs);
      if (cap && rt > 0) this.tri([x, y + h, z], [x + c0 * rt, y + h, z + s0 * rt], [x + c1 * rt, y + h, z + s1 * rt], col, [.5, .5], [.5, 0], [1, .5]);
      this.tri([x, y, z], [x + c1 * rb, y, z + s1 * rb], [x + c0 * rb, y, z + s0 * rb], col);
    }
    return this;
  }
  sph(x, y, z, rx, ry, rz, sg, col) {
    const P = (i, j) => { const a = i / sg * PI * 2, b = j / (sg / 2) * PI - PI / 2; return [x + Math.cos(a) * Math.cos(b) * rx, y + Math.sin(b) * ry, z + Math.sin(a) * Math.cos(b) * rz]; };
    for (let j = 0; j < sg / 2; j++) for (let i = 0; i < sg; i++) {
      const u0 = i / sg, u1 = (i + 1) / sg, v0 = j / (sg / 2), v1 = (j + 1) / (sg / 2);
      this.tri(P(i, j), P(i + 1, j), P(i + 1, j + 1), col, [u0, v0], [u1, v0], [u1, v1]); this.tri(P(i, j), P(i + 1, j + 1), P(i, j + 1), col, [u0, v0], [u1, v1], [u0, v1]);
    }
    return this;
  }
  build(tex = null, o = {}) {
    const b = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, b); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(this.a), gl.STATIC_DRAW);
    return Object.assign({ b, n: this.a.length / 9, tex }, o);
  }
}

// ---------- dibujo ----------
const cur = { cam: null };
function setCam(c) {
  const asp = RW / RH, v = M4.look(c.pos, c.tgt, [Math.sin(c.roll || 0), Math.cos(c.roll || 0), 0]);
  cur.cam = { pos: c.pos, v, p: M4.persp((c.fov || 60) * PI / 180, asp, .1, 600) }; cur.vp = M4.mul(cur.cam.p, v);
}
function draw(m, mdl, o = {}) {
  if (!m || !m.n) return;
  const mvp = M4.mul(cur.vp, mdl || M4.id());
  gl.uniformMatrix4fv(U.mvp, false, new Float32Array(mvp));
  const t = o.tint || [0, 0, 0, 0]; gl.uniform4f(U.tint, t[0], t[1], t[2], t[3]);
  gl.uniform3fv(U.fogc, cur.fogc); gl.uniform2f(U.fogr, o.nofog ? 9999 : cur.fogr[0], o.nofog ? 10000 : cur.fogr[1]);
  gl.depthMask(!(m.trans || o.nodepth)); if (o.nodepth) gl.disable(gl.DEPTH_TEST); else gl.enable(gl.DEPTH_TEST);
  gl.bindBuffer(gl.ARRAY_BUFFER, m.b);
  gl.vertexAttribPointer(A.p, 3, gl.FLOAT, false, 36, 0); gl.vertexAttribPointer(A.c, 4, gl.FLOAT, false, 36, 12); gl.vertexAttribPointer(A.t, 2, gl.FLOAT, false, 36, 28);
  if (m.tex) { gl.bindTexture(gl.TEXTURE_2D, m.tex); gl.uniform1f(U.ut, 1); } else gl.uniform1f(U.ut, 0);
  gl.drawArrays(gl.TRIANGLES, 0, m.n);
}
function clear(bg) { gl.depthMask(true); gl.enable(gl.DEPTH_TEST); cur.fogc = bg; gl.clearColor(bg[0], bg[1], bg[2], 1); gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT); }

// ---------- 2D (HUD, subtitulos) ----------
const MONO = '"Courier New",ui-monospace,monospace', PIX = '"Press Start 2P","Arial Black",sans-serif';
function txt(s, x, y, size, col = '#fff', al = 'center', font = MONO, bold = true) { X.font = (bold ? 'bold ' : '') + size + 'px ' + font; X.textAlign = al; X.textBaseline = 'middle'; X.fillStyle = col; X.fillText(s, x, y); }
function sub(s, yf = .67) {
  if (!s) return; X.font = 'bold 14px ' + MONO; const w = X.measureText(s).width + 10, y = RH * yf;
  X.fillStyle = 'rgba(0,0,0,.78)'; X.fillRect(RW / 2 - w / 2, y - 10, w, 20); txt(s, RW / 2, y + 1, 14, '#f4f4f4');
}
function title(a, b, y = 30) {
  X.font = '16px ' + PIX; let w = X.measureText(a).width + 18; X.fillStyle = 'rgba(14,14,20,.88)'; X.fillRect(RW / 2 - w / 2, y - 14, w, 28); txt(a, RW / 2, y + 1, 16, '#fff', 'center', PIX, false);
  X.font = '7px ' + PIX; w = X.measureText(b).width + 12; X.fillStyle = 'rgba(14,14,20,.8)'; X.fillRect(RW / 2 - w / 2, y + 19, w, 14); txt(b, RW / 2, y + 27, 7, '#d8d8e0', 'center', PIX, false);
}
function proj(p) { const q = M4.pt(cur.vp, p); const w = cur.vp[3] * p[0] + cur.vp[7] * p[1] + cur.vp[11] * p[2] + cur.vp[15]; return [(q[0] / w * .5 + .5) * RW, (1 - (q[1] / w * .5 + .5)) * RH, w]; }

// utilidades de ayuda: estrellas, humanos
function starMesh(n, rad, size, col = '#ffffff', seedv = 5, ymin = -.1) {
  const m = new MB(); m.lit = false; rs(seedv);
  for (let i = 0; i < n; i++) {
    const a = rnd() * PI * 2, y = ymin + rnd() * (1 - ymin), r = Math.sqrt(1 - y * y), p = [Math.cos(a) * r * rad, y * rad, Math.sin(a) * r * rad], s = size * (.5 + rnd());
    const nx = [-Math.sin(a), 0, Math.cos(a)], ny = [0, 1, 0];
    const q = (u, v) => [p[0] + (nx[0] * u + ny[0] * v) * s, p[1] + (nx[1] * u + ny[1] * v) * s, p[2] + (nx[2] * u + ny[2] * v) * s];
    m.quad(q(-1, -1), q(1, -1), q(1, 1), q(-1, 1), col);
  }
  return m.build(null, { trans: false });
}
function dome(top, hor, bot, tex, rad = 300, us = 4, div = 1) {
  const m = new MB(); m.lit = false; const sg = 24, R = 12;
  const pt = (i, j) => { const a = i / sg * PI * 2, b = j / R * PI / 2; return [Math.cos(a) * Math.cos(b) * rad, Math.sin(b) * rad, Math.sin(a) * Math.cos(b) * rad]; };
  const cf = p => { const y = p[1] / rad; const c = y > 0 ? mixc(hor, top, Math.pow(y, .6)) : mixc(hor, bot, Math.min(1, -y * 3)); return [Math.min(1, c[0] / div), Math.min(1, c[1] / div), Math.min(1, c[2] / div), 1]; };
  for (let j = -R; j < R; j++) for (let i = 0; i < sg; i++) {
    const uv = (ii, jj) => [ii / sg * us, (jj + R) / (2 * R) * 3];
    m.tri(pt(i, j), pt(i + 1, j), pt(i + 1, j + 1), cf, uv(i, j), uv(i + 1, j), uv(i + 1, j + 1));
    m.tri(pt(i, j), pt(i + 1, j + 1), pt(i, j + 1), cf, uv(i, j), uv(i + 1, j + 1), uv(i, j + 1));
  }
  return m.build(tex);
}
// humano low-poly articulado
function humanParts(o) {
  const cs = o.shirt || '#222428', pa = o.pants || '#2c4a8c', sk = o.skin || '#c98d6a', hr = o.hair || '#15110f', sh = o.shoes || '#8a4a1e', ex = o.extra;
  const P = {}; let m;
  m = new MB(); m.box(0, 0, 0, .5, .62, .26, cs); if (o.apron) m.box(0, -.05, .14, .4, .5, .02, '#e8e8e4'); if (o.collar) m.box(0, .22, .14, .14, .2, .02, '#e8e8f0'); P.torso = m.build();
  m = new MB(); m.box(0, 0, 0, .22, .26, .24, sk); m.box(0, .1, -.02, .25, .12, .26, hr); if (o.hat) m.cyl(0, .12, 0, .13, .13, .22, 8, '#f2f2f2'); P.head = m.build();
  m = new MB(); m.box(0, -.28, 0, .14, .58, .15, cs); m.box(0, -.6, 0, .11, .1, .11, sk); P.arm = m.build();
  m = new MB(); m.box(0, -.3, 0, .2, .6, .2, pa); m.box(0, -.64, .05, .22, .1, .3, sh); P.leg = m.build();
  return P;
}
function drawHuman(P, base, pose = {}) {
  const w = (m, o) => draw(m, M4.mul(base, o));
  const ls = pose.leg || 0, as = pose.arm || 0;
  w(P.torso, M4.T(0, 1.02, 0)); w(P.head, M4.T(0, 1.55, 0));
  w(P.leg, M4.mul(M4.T(-.13, .7, 0), M4.RX(ls))); w(P.leg, M4.mul(M4.T(.13, .7, 0), M4.RX(-ls)));
  w(P.arm, M4.mul(M4.T(-.34, 1.26, 0), M4.RX(-as))); w(P.arm, M4.mul(M4.T(.34, 1.26, 0), M4.RX(as)));
}
