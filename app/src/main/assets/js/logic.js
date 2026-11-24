/* Mate Hub - lógica matemática pura (sin DOM). Se puede probar con node. */
(function (root) {
  'use strict';

  var DIFFS = ['facil', 'medio', 'dificil'];
  var DIFF_NAMES = { facil: 'Fácil', medio: 'Medio', dificil: 'Difícil' };
  var SYMBOL = { add: '+', sub: '−', mul: '×', div: '÷' };

  function ri(min, max) { return min + Math.floor(Math.random() * (max - min + 1)); }
  function pick(arr) { return arr[Math.floor(Math.random() * arr.length)]; }
  function shuffle(arr) {
    var a = arr.slice();
    for (var i = a.length - 1; i > 0; i--) {
      var j = Math.floor(Math.random() * (i + 1));
      var t = a[i]; a[i] = a[j]; a[j] = t;
    }
    return a;
  }

  /* Rangos por dificultad. */
  var CFG = {
    facil:   { addMax: 20,  subMax: 20,  mulA: [1, 5],  mulB: [1, 10], divB: [2, 5],  divQ: [1, 10] },
    medio:   { addMax: 100, subMax: 100, mulA: [2, 9],  mulB: [2, 10], divB: [2, 9],  divQ: [2, 10] },
    dificil: { addMax: 500, subMax: 500, mulA: [2, 12], mulB: [2, 12], divB: [2, 12], divQ: [2, 15] }
  };

  function make(op, diff) {
    var c = CFG[diff] || CFG.facil;
    var a, b, ans;
    if (op === 'add') {
      if (diff === 'facil') { a = ri(1, c.addMax - 1); b = ri(1, c.addMax - a); }
      else if (diff === 'medio') { a = ri(10, 79); b = ri(5, c.addMax - a); }
      else { a = ri(100, 399); b = ri(20, c.addMax - a + 100); }
      ans = a + b;
    } else if (op === 'sub') {
      if (diff === 'facil') { a = ri(2, c.subMax); b = ri(1, a - 1); }
      else if (diff === 'medio') { a = ri(20, c.subMax); b = ri(5, a - 1); }
      else { a = ri(150, 999); b = ri(40, a - 10); }
      ans = a - b;
    } else if (op === 'mul') {
      if (diff === 'dificil' && Math.random() < 0.3) { a = ri(12, 25); b = ri(3, 9); }
      else { a = ri(c.mulA[0], c.mulA[1]); b = ri(c.mulB[0], c.mulB[1]); }
      ans = a * b;
    } else { // div
      b = ri(c.divB[0], c.divB[1]);
      var q = ri(c.divQ[0], c.divQ[1]);
      a = b * q; ans = q;
    }
    return { op: op, a: a, b: b, answer: ans, text: a + ' ' + SYMBOL[op] + ' ' + b };
  }

  /* Genera un problema distinto del anterior. */
  function problem(op, diff, lastText) {
    var p, tries = 0;
    do { p = make(op, diff); tries++; } while (p.text === lastText && tries < 12);
    return p;
  }

  function anyOp(diff, lastText, ops) {
    return problem(pick(ops || ['add', 'sub', 'mul', 'div']), diff, lastText);
  }

  /* Distractores creíbles + la respuesta; devuelve {options, correctIndex}. */
  function choices(p, n) {
    n = n || 4;
    var ans = p.answer, set = {}, out = [ans];
    set[ans] = true;
    var cand = [];
    if (p.op === 'add' || p.op === 'sub') {
      cand = [ans + 1, ans - 1, ans + 10, ans - 10, ans + 2, ans - 2, p.op === 'add' ? Math.abs(p.a - p.b) : p.a + p.b];
    } else if (p.op === 'mul') {
      cand = [ans + p.a, ans - p.a, ans + p.b, ans - p.b, ans + 1, ans - 1, ans + 10, ans - 10];
    } else {
      cand = [ans + 1, ans - 1, ans + 2, ans - 2, p.b, ans * 2, ans + 3, ans - 3];
    }
    cand = shuffle(cand);
    for (var i = 0; i < cand.length && out.length < n; i++) {
      var v = cand[i];
      if (v > 0 && !set[v]) { set[v] = true; out.push(v); }
    }
    var step = 1;
    while (out.length < n) {
      var w = ans + step * (step % 2 ? 1 : -1) * ri(1, 4);
      step++;
      if (w > 0 && !set[w]) { set[w] = true; out.push(w); }
      if (step > 200) { w = ans + out.length + 50; set[w] = true; out.push(w); }
    }
    var options = shuffle(out);
    return { options: options, correctIndex: options.indexOf(ans) };
  }

  /* Nivel y experiencia. Nivel L necesita 50*L*(L-1) XP. */
  function xpForLevel(l) { return 50 * l * (l - 1); }
  function levelFromXp(xp) {
    var l = 1;
    while (xpForLevel(l + 1) <= xp) l++;
    return l;
  }
  function levelProgress(xp) {
    var l = levelFromXp(xp);
    var base = xpForLevel(l), next = xpForLevel(l + 1);
    return { level: l, into: xp - base, need: next - base, ratio: (xp - base) / (next - base) };
  }

  /* Multiplicador de racha: sube cada 5 aciertos seguidos, máximo x5. */
  function multiplier(streak) { return Math.min(5, 1 + Math.floor(streak / 5)); }

  function starsFor(value, thresholds) {
    var s = 0;
    for (var i = 0; i < thresholds.length; i++) if (value >= thresholds[i]) s = i + 1;
    return s;
  }

  var api = {
    DIFFS: DIFFS, DIFF_NAMES: DIFF_NAMES, SYMBOL: SYMBOL, CFG: CFG,
    ri: ri, pick: pick, shuffle: shuffle,
    problem: problem, anyOp: anyOp, choices: choices,
    xpForLevel: xpForLevel, levelFromXp: levelFromXp, levelProgress: levelProgress,
    multiplier: multiplier, starsFor: starsFor
  };

  if (typeof module !== 'undefined' && module.exports) module.exports = api;
  else root.MathLogic = api;
})(typeof window !== 'undefined' ? window : this);
