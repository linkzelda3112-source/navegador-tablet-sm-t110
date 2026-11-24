const L = require('../app/src/main/assets/js/logic.js');
const assert = require('assert');

let n = 0;
for (const diff of L.DIFFS) {
  for (const op of ['add', 'sub', 'mul', 'div']) {
    for (let i = 0; i < 4000; i++) {
      const p = L.problem(op, diff, '');
      n++;
      assert(Number.isInteger(p.answer) && p.answer > 0, `${diff} ${op} respuesta inválida ${p.text}`);
      const calc = { add: p.a + p.b, sub: p.a - p.b, mul: p.a * p.b, div: p.a / p.b }[op];
      assert.strictEqual(calc, p.answer, `${diff} ${op} cuenta mal ${p.text}`);
      assert(p.b > 0 && p.a > 0, 'operandos > 0');
      if (op === 'div') assert(p.a % p.b === 0, 'división exacta');
      if (diff === 'facil' && op === 'add') assert(p.answer <= 20, 'suma fácil <= 20');
      if (diff === 'facil' && op === 'sub') assert(p.a <= 20, 'resta fácil <= 20');
      const c = L.choices(p, 4);
      assert.strictEqual(c.options.length, 4);
      assert.strictEqual(new Set(c.options).size, 4, 'opciones únicas ' + p.text + ' ' + c.options);
      assert.strictEqual(c.options[c.correctIndex], p.answer);
      assert(c.options.every(v => Number.isInteger(v) && v > 0), 'opciones positivas ' + c.options);
    }
  }
}

// no repite el problema anterior
let same = 0;
for (let i = 0; i < 2000; i++) {
  const a = L.problem('mul', 'medio', '');
  const b = L.problem('mul', 'medio', a.text);
  if (a.text === b.text) same++;
}
assert(same < 20, 'repite demasiado: ' + same);

// niveles
assert.strictEqual(L.levelFromXp(0), 1);
assert.strictEqual(L.levelFromXp(99), 1);
assert.strictEqual(L.levelFromXp(100), 2);
assert.strictEqual(L.levelFromXp(299), 2);
assert.strictEqual(L.levelFromXp(300), 3);
let prev = 0;
for (let xp = 0; xp < 20000; xp += 7) { const l = L.levelFromXp(xp); assert(l >= prev); prev = l; }
const pr = L.levelProgress(150);
assert(pr.level === 2 && pr.into === 50 && pr.need === 200 && Math.abs(pr.ratio - 0.25) < 1e-9);

// racha y estrellas
assert.strictEqual(L.multiplier(0), 1);
assert.strictEqual(L.multiplier(4), 1);
assert.strictEqual(L.multiplier(5), 2);
assert.strictEqual(L.multiplier(99), 5);
assert.strictEqual(L.starsFor(5, [8, 14, 20]), 0);
assert.strictEqual(L.starsFor(8, [8, 14, 20]), 1);
assert.strictEqual(L.starsFor(20, [8, 14, 20]), 3);

console.log('OK: ' + n + ' problemas verificados');
