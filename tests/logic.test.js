'use strict';
const assert = require('assert');
const L = require('../app/src/main/assets/js/logic.js');
const D = require('../app/src/main/assets/js/data.js');

let n = 0;
function t(name, fn) { fn(); n++; }

// --- Sílabas: todas las palabras del juego se separan como están escritas en los datos
t('sílabas de todas las palabras', () => {
  D.WORDS.forEach(w => assert.deepStrictEqual(L.syllabify(w.word), w.syl, w.word));
});
t('sílabas extra', () => {
  const casos = {
    'instrumento': ['ins', 'tru', 'men', 'to'], 'construir': ['cons', 'truir'],
    'país': ['pa', 'ís'], 'maíz': ['ma', 'íz'], 'río': ['rí', 'o'], 'cuídate': ['cuí', 'da', 'te'],
    'ciudad': ['ciu', 'dad'], 'Uruguay': ['U', 'ru', 'guay'], 'aeropuerto': ['a', 'e', 'ro', 'puer', 'to'],
    'extraño': ['ex', 'tra', 'ño'], 'guerra': ['gue', 'rra'], 'queso': ['que', 'so'],
    'hielo': ['hie', 'lo'], 'Camila': ['Ca', 'mi', 'la'], 'ESCUELA': ['ES', 'CUE', 'LA'],
    'rey': ['rey'], 'muy': ['muy'], 'y': ['y'], 'taxi': ['ta', 'xi'], 'monstruo': ['mons', 'truo']
  };
  Object.keys(casos).forEach(k => assert.deepStrictEqual(L.syllabify(k), casos[k], k));
});
t('las sílabas se pueden volver a unir', () => {
  ['Extraordinario', 'Ayudante', 'hipopótamo', 'psicólogo', 'zorro'].forEach(w =>
    assert.strictEqual(L.syllabify(w).join(''), w));
});

// --- Contenido
t('palabras: emoji y largo razonable', () => {
  D.WORDS.forEach(w => { assert.ok(w.emoji && w.syl.length >= 1 && w.syl.length <= 5, w.word); });
  const set = new Set(D.WORDS.map(w => w.word));
  assert.strictEqual(set.size, D.WORDS.length, 'palabras repetidas');
  assert.ok(D.WORDS.filter(w => w.syl.length === 2).length >= 8);
  assert.ok(D.WORDS.filter(w => w.syl.length === 3).length >= 8);
  assert.ok(D.WORDS.filter(w => w.syl.length >= 4).length >= 4);
  assert.ok(D.WORDS.filter(w => !w.accent && w.word.length <= 9).length >= 40);
});
t('cuentos bien formados', () => {
  assert.ok(D.STORIES.length >= 6);
  D.STORIES.forEach(s => {
    assert.ok(s.sents.length >= 5 && s.qs.length === 3, s.id);
    s.qs.forEach(q => {
      assert.strictEqual(q.o.length, 3);
      assert.ok(q.a >= 0 && q.a < 3);
      assert.ok(q.hint >= 0 && q.hint < s.sents.length);
      assert.strictEqual(new Set(q.o).size, 3, 'opciones repetidas: ' + q.q);
    });
  });
});
t('hueco de letras: plantilla coherente', () => {
  D.MIRROR.concat(D.ORTO).forEach(r => {
    assert.strictEqual((r[1].match(/_/g) || []).length, 1, r[1]);
    assert.ok(r[3].length >= 1 && r[3].indexOf(r[2]) < 0, r[1]);
  });
  assert.ok(D.MIRROR.length >= 8 && D.ORTO.length >= 12);
});
t('oraciones', () => {
  D.SENTENCES.forEach(s => assert.ok(L.words(s).length >= 5 && L.words(s).length <= 10, s));
  assert.ok(D.SENTENCES.length >= 10);
});

// --- Utilidades
t('shuffleDifferent nunca devuelve el mismo orden', () => {
  const r = L.mulberry32(7);
  for (let i = 0; i < 200; i++) {
    const a = ['El', 'gato', 'duerme', 'mucho'];
    assert.notDeepStrictEqual(L.shuffleDifferent(a, r), a);
  }
  assert.deepStrictEqual(L.shuffleDifferent(['a'], r), ['a']);
  assert.deepStrictEqual(L.shuffleDifferent(['a', 'a'], r), ['a', 'a']);
});
t('lookalikes', () => {
  const r = L.mulberry32(3);
  const v = L.lookalikes('dado', 2, r);
  assert.strictEqual(v.length, 2);
  v.forEach(x => assert.notStrictEqual(x, 'dado'));
  assert.ok(L.lookalikes('bola', 2, r).includes('dola') || L.lookalikes('bola', 3, r).length >= 2);
  assert.strictEqual(new Set(L.lookalikes('queso', 3, r)).size, L.lookalikes('queso', 3, r).length);
});
t('trazo', () => {
  const core = [1, 1, 1, 1, 0, 0, 0, 0], zone = [1, 1, 1, 1, 1, 1, 0, 0];
  let s = L.scoreTrace(core, zone, [1, 1, 1, 1, 0, 0, 0, 0]);
  assert.strictEqual(s.coverage, 1); assert.strictEqual(s.precision, 1);
  assert.ok(L.traceDone(s)); assert.strictEqual(L.traceStars(s), 3);
  s = L.scoreTrace(core, zone, [0, 0, 0, 0, 0, 0, 1, 1]);
  assert.ok(!L.traceDone(s)); assert.strictEqual(s.coverage, 0); assert.strictEqual(s.precision, 0);
  s = L.scoreTrace(core, zone, [1, 1, 1, 0, 0, 0, 0, 0]);
  assert.ok(!L.traceDone(s));
  s = L.scoreTrace(core, zone, [0, 0, 0, 0, 0, 0, 0, 0]);
  assert.strictEqual(s.coverage, 0); assert.strictEqual(s.precision, 0);
  assert.strictEqual(L.traceStars({ coverage: 0.8, precision: 0.7 }), 1);
  assert.strictEqual(L.traceStars({ coverage: 0.8, precision: 0.8 }), 2);
});
t('estrellas, nivel y pegatinas', () => {
  assert.strictEqual(L.starsFor(0, 6), 3);
  assert.strictEqual(L.starsFor(2, 6), 2);
  assert.strictEqual(L.starsFor(9, 6), 1);
  assert.strictEqual(L.levelFor(0), 1); assert.strictEqual(L.levelFor(12), 2);
  assert.strictEqual(L.stickersUnlocked(13, 6), 2);
});
t('índice de palabra por carácter', () => {
  const s = 'Una noche, un zorro';
  assert.strictEqual(L.wordIndexAt(s, 0), 0);
  assert.strictEqual(L.wordIndexAt(s, 4), 1);
  assert.strictEqual(L.wordIndexAt(s, 10), 1); // la coma sigue siendo la palabra 1
  assert.strictEqual(L.wordIndexAt(s, 12), 2);
  assert.strictEqual(L.wordIndexAt(s, 15), 3);
});
t('tokenize y estimateMs', () => {
  const tk = L.tokenize('¡Hola, niña!');
  assert.deepStrictEqual(tk.filter(x => x.word).map(x => x.text), ['Hola', 'niña']);
  assert.strictEqual(tk.map(x => x.text).join(''), '¡Hola, niña!');
  assert.ok(L.estimateMs('hola', 1) >= 900);
  assert.ok(L.estimateMs('x'.repeat(100), 0.5) > L.estimateMs('x'.repeat(100), 1));
});

console.log('OK: ' + n + ' grupos de pruebas');
