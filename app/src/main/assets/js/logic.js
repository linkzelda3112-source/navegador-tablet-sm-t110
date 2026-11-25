/* Lógica pura (sin DOM): se prueba con node tests/logic.test.js */
(function (root) {
  'use strict';
  var L = {};

  var VOW = 'aeiouáéíóúü';
  var STRONG_LIKE = 'aeoáéóíú';   // í y ú acentuadas rompen el diptongo
  var PLAIN_WEAK = 'iuü';
  var INSEP = ['bl', 'br', 'cl', 'cr', 'dr', 'fl', 'fr', 'gl', 'gr', 'kl', 'kr', 'pl', 'pr', 'tr'];

  function isV(u) { return u.t === 'V'; }

  /** Separa una palabra en sílabas. Devuelve un arreglo de textos (conserva mayúsculas). */
  L.syllabify = function (word) {
    var w = String(word).toLowerCase();
    var n = w.length, i = 0, units = [];
    while (i < n) {
      var c = w[i], nx = w[i + 1];
      if (c === 'c' && nx === 'h') { units.push({ t: 'C', s: 'ch' }); i += 2; continue; }
      if (c === 'l' && nx === 'l') { units.push({ t: 'C', s: 'll' }); i += 2; continue; }
      if (c === 'r' && nx === 'r') { units.push({ t: 'C', s: 'rr' }); i += 2; continue; }
      if (c === 'q' && nx === 'u') { units.push({ t: 'C', s: 'qu' }); i += 2; continue; }
      if (c === 'g' && nx === 'u' && 'eiéí'.indexOf(w[i + 2] || '#') >= 0) { units.push({ t: 'C', s: 'gu' }); i += 2; continue; }
      if (VOW.indexOf(c) >= 0) { units.push({ t: 'V', s: c }); i++; continue; }
      if (c === 'y' && (i === n - 1)) { units.push({ t: 'V', s: c }); i++; continue; }
      units.push({ t: 'C', s: c }); i++;
    }
    // agrupar vocales en núcleos (diptongos / triptongos)
    var items = [];
    for (i = 0; i < units.length; i++) {
      var u = units[i];
      if (!isV(u)) { items.push({ t: 'C', s: u.s }); continue; }
      var last = items[items.length - 1];
      if (last && last.t === 'N') {
        var vs = last.v, cur = u.s, join = false;
        var lastV = vs[vs.length - 1];
        var curWeak = PLAIN_WEAK.indexOf(cur) >= 0 || cur === 'y';
        var lastWeak = PLAIN_WEAK.indexOf(lastV) >= 0 || lastV === 'y';
        if (vs.length === 1) {
          join = (lastWeak || curWeak) && !(STRONG_LIKE.indexOf(lastV) >= 0 && STRONG_LIKE.indexOf(cur) >= 0);
        } else {
          var first = vs[0];
          var firstWeak = PLAIN_WEAK.indexOf(first) >= 0 || first === 'y';
          join = curWeak && firstWeak && !lastWeak;
        }
        if (join) { vs.push(cur); continue; }
      }
      items.push({ t: 'N', v: [u.s] });
    }
    var nuc = [];
    for (i = 0; i < items.length; i++) if (items[i].t === 'N') nuc.push(i);
    if (!nuc.length) return [String(word)];

    // decidir dónde cortan las consonantes entre núcleos
    var cuts = []; // índice de item donde empieza cada sílaba (excepto la primera)
    for (var k = 0; k < nuc.length - 1; k++) {
      var a = nuc[k], b = nuc[k + 1];
      var cons = b - a - 1;
      if (cons === 0) { cuts.push(b); continue; }
      var onset = 1;
      if (cons >= 2) {
        var p = items[b - 2].s, q = items[b - 1].s;
        if (INSEP.indexOf(p + q) >= 0) onset = 2;
      }
      cuts.push(b - onset);
    }
    var text = items.map(function (it) { return it.t === 'N' ? it.v.join('') : it.s; });
    // reconstruir con el texto original (mismas longitudes)
    var lens = text.map(function (s) { return s.length; });
    var pos = [0];
    for (i = 0; i < lens.length; i++) pos.push(pos[i] + lens[i]);
    var res = [], start = 0;
    for (i = 0; i < cuts.length; i++) {
      res.push(String(word).slice(pos[start], pos[cuts[i]]));
      start = cuts[i];
    }
    res.push(String(word).slice(pos[start]));
    return res;
  };

  /** Divide un texto en trozos: palabras y separadores (espacios, puntuación). */
  L.tokenize = function (text) {
    var re = /([A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)/;
    return String(text).split(re).filter(function (s) { return s !== ''; }).map(function (s) {
      return { text: s, word: re.test(s) };
    });
  };

  /** Divide una oración en palabras (con la puntuación pegada). */
  L.words = function (sentence) {
    return String(sentence).trim().split(/\s+/);
  };

  L.mulberry32 = function (seed) {
    var a = seed >>> 0;
    return function () {
      a = (a + 0x6D2B79F5) >>> 0;
      var t = a;
      t = Math.imul(t ^ (t >>> 15), t | 1);
      t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
      return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
  };

  L.shuffle = function (arr, rnd) {
    rnd = rnd || Math.random;
    var a = arr.slice();
    for (var i = a.length - 1; i > 0; i--) {
      var j = Math.floor(rnd() * (i + 1));
      var t = a[i]; a[i] = a[j]; a[j] = t;
    }
    return a;
  };

  /** Baraja asegurando que el resultado NO quede en el orden original. */
  L.shuffleDifferent = function (arr, rnd) {
    if (arr.length < 2) return arr.slice();
    var same = function (x) { for (var i = 0; i < arr.length; i++) if (x[i] !== arr[i]) return false; return true; };
    var allEqual = arr.every(function (v) { return v === arr[0]; });
    var r = L.shuffle(arr, rnd), tries = 0;
    while (!allEqual && same(r) && tries++ < 50) r = L.shuffle(arr, rnd);
    return r;
  };

  L.sample = function (arr, n, rnd) { return L.shuffle(arr, rnd).slice(0, n); };

  /** Letras que suelen confundirse al leer. */
  var CONF = { b: 'd', d: 'b', p: 'q', q: 'p', m: 'n', n: 'm', a: 'o', o: 'a' };
  L.CONFUSABLE = CONF;

  /** Palabras parecidas (cambiando una letra confundible) para "oye y encuentra". */
  L.lookalikes = function (word, count, rnd) {
    var out = [], seen = {};
    seen[word] = true;
    var idx = [];
    for (var i = 0; i < word.length; i++) if (CONF[word[i]]) idx.push(i);
    idx = L.shuffle(idx, rnd);
    for (var k = 0; k < idx.length && out.length < count; k++) {
      var p = idx[k];
      var v = word.slice(0, p) + CONF[word[p]] + word.slice(p + 1);
      if (!seen[v]) { seen[v] = true; out.push(v); }
    }
    // si faltan, invertir el orden de dos letras vecinas
    for (var j = 0; j < word.length - 1 && out.length < count; j++) {
      var s = word.slice(0, j) + word[j + 1] + word[j] + word.slice(j + 2);
      if (!seen[s]) { seen[s] = true; out.push(s); }
    }
    return out;
  };

  /**
   * Evalúa un trazo. core/zone/ink son arreglos 0/1 del mismo tamaño.
   * core = la letra; zone = la letra "agrandada" (margen permitido); ink = lo pintado.
   * cobertura = qué parte de la letra se pintó; precision = qué parte de lo pintado quedó dentro de la zona.
   */
  L.scoreTrace = function (core, zone, ink) {
    var c = 0, cIn = 0, k = 0, kIn = 0;
    for (var i = 0; i < core.length; i++) {
      if (core[i]) { c++; if (ink[i]) cIn++; }
      if (ink[i]) { k++; if (zone[i]) kIn++; }
    }
    var coverage = c ? cIn / c : 0;
    var precision = k ? kIn / k : 0;
    return { coverage: coverage, precision: precision };
  };

  /** Estrellas (1-3) según qué tan bien se trazó. Siempre al menos 1 si se completó. */
  L.traceStars = function (s) {
    var q = Math.min(s.coverage, s.precision);
    if (q >= 0.88) return 3;
    if (q >= 0.74) return 2;
    return 1;
  };

  L.traceDone = function (s) { return s.coverage >= 0.85 && s.precision >= 0.6; };

  /** Estrellas de una ronda: 3 sin errores, 2 con pocos, 1 si terminó. */
  L.starsFor = function (mistakes, rounds) {
    if (mistakes <= 0) return 3;
    if (mistakes <= Math.max(2, Math.ceil(rounds / 2))) return 2;
    return 1;
  };

  L.levelFor = function (totalStars) { return Math.floor(totalStars / 12) + 1; };
  L.stickersUnlocked = function (totalStars, perSticker) { return Math.floor(totalStars / (perSticker || 6)); };

  /** Estimación de duración de lectura (ms) cuando no hay voz disponible. */
  L.estimateMs = function (text, rate) {
    return Math.max(900, String(text).length * 70 / Math.max(0.3, rate || 1));
  };

  /** Índice de palabra (0..n-1) que contiene el carácter 'pos' dentro de 'text'. */
  L.wordIndexAt = function (text, pos) {
    var re = /\S+/g, m, i = 0, res = 0;
    while ((m = re.exec(text))) {
      if (m.index <= pos) res = i; else break;
      i++;
    }
    return res;
  };

  if (typeof module !== 'undefined' && module.exports) module.exports = L;
  else root.Logic = L;
})(typeof window !== 'undefined' ? window : this);
