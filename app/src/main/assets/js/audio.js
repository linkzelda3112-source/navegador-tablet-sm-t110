/* Sonidos cortos (sintetizados) y voz (TextToSpeech de Android, o la del navegador). */
(function (root) {
  'use strict';

  // ---------- Efectos ----------
  var ctx = null;
  function ac() {
    if (!ctx) {
      var C = root.AudioContext || root.webkitAudioContext;
      if (!C) return null;
      try { ctx = new C(); } catch (e) { return null; }
    }
    if (ctx.state === 'suspended' && ctx.resume) { try { ctx.resume(); } catch (e) { /* nada */ } }
    return ctx;
  }
  function tone(f, t0, dur, type, vol) {
    var c = ac(); if (!c) return;
    try {
      var o = c.createOscillator(), g = c.createGain();
      o.type = type || 'sine'; o.frequency.value = f;
      var t = c.currentTime + t0;
      g.gain.setValueAtTime(0.0001, t);
      g.gain.exponentialRampToValueAtTime(vol || 0.12, t + 0.02);
      g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
      o.connect(g); g.connect(c.destination);
      o.start(t); o.stop(t + dur + 0.05);
    } catch (e) { /* nada */ }
  }
  var Sfx = {
    play: function (name) {
      if (!root.Store.get().settings.sound) return;
      if (name === 'tap') tone(660, 0, 0.07, 'triangle', 0.08);
      else if (name === 'ok') { tone(660, 0, 0.14, 'triangle', 0.13); tone(880, 0.12, 0.2, 'triangle', 0.13); }
      else if (name === 'no') { tone(300, 0, 0.16, 'sine', 0.09); tone(250, 0.12, 0.2, 'sine', 0.07); }
      else if (name === 'win') { [523, 659, 784, 1047].forEach(function (f, i) { tone(f, i * 0.12, 0.28, 'triangle', 0.13); }); }
    }
  };

  // ---------- Voz ----------
  var seq = 0, cur = null, timers = [];

  function clearTimers() { timers.forEach(clearTimeout); timers = []; }
  function later(fn, ms) { var t = setTimeout(fn, ms); timers.push(t); return t; }
  function nativeBridge() { return !!root.AndroidTTS; }
  function nativeOK() { try { return !!(root.AndroidTTS && root.AndroidTTS.ready()); } catch (e) { return false; } }
  var warned = false;
  function warnNoVoice() {
    if (warned || !nativeBridge() || nativeOK()) return;
    warned = true;
    if (root.UI) root.UI.toast('Este teléfono no tiene voz en español. Se instala en Ajustes ▸ Idioma ▸ Salida de texto a voz.', 5000);
  }
  function webOK() { return !!(root.speechSynthesis && root.SpeechSynthesisUtterance); }

  function finish(c) {
    if (!c || c.finished) return;
    c.finished = true;
    if (cur === c) cur = null;
    if (c.opt.onDone) { try { c.opt.onDone(); } catch (e) { /* nada */ } }
  }

  // Sin voz disponible: simula la lectura con tiempos estimados (el karaoke sigue funcionando).
  function simulate(c) {
    if (c.simulated || c.finished) return;
    c.simulated = true;
    warnNoVoice();
    var words = c.text.trim().split(/\s+/), total = root.Logic.estimateMs(c.text, c.rate);
    var weights = words.map(function (w) { return w.length + 2; });
    var sum = weights.reduce(function (a, b) { return a + b; }, 0), acc = 0;
    words.forEach(function (w, i) {
      var at = total * acc / sum;
      acc += weights[i];
      later(function () { if (cur === c && c.opt.onWord) c.opt.onWord(i); }, at);
    });
    later(function () { finish(c); }, total);
  }

  function onEvent(id, type, a) {
    var c = cur;
    if (!c || c.id !== id) return;
    if (type === 'start') { c.started = true; }
    else if (type === 'range') {
      c.ranged = true;
      if (c.opt.onWord) c.opt.onWord(root.Logic.wordIndexAt(c.text, a));
    } else if (type === 'done') { finish(c); }
    else if (type === 'error') { simulate(c); }
  }
  root.__tts = onEvent;

  var Voice = {
    available: function () { return nativeOK() || webOK(); },
    stop: function () {
      clearTimers();
      var c = cur; cur = null;
      if (c) c.finished = true;
      try { if (root.AndroidTTS) root.AndroidTTS.stop(); } catch (e) { /* nada */ }
      try { if (webOK()) root.speechSynthesis.cancel(); } catch (e) { /* nada */ }
    },
    /** Lee un texto. opt: {rate, onWord(i), onDone()} */
    say: function (text, opt) {
      opt = opt || {};
      Voice.stop();
      var rate = opt.rate || root.Store.get().settings.rate;
      var c = { id: ++seq, text: String(text), opt: opt, rate: rate, started: false, ranged: false, finished: false, simulated: false };
      cur = c;
      if (nativeBridge()) {
        try { root.AndroidTTS.speak(String(c.id), c.text, rate); } catch (e) { simulate(c); return c.id; }
        later(function () { if (cur === c && !c.started) simulate(c); }, 6000);
        // si no llegan marcas por palabra, resaltamos por tiempo estimado
        later(function () {
          if (cur === c && c.started && !c.ranged && !c.simulated) {
            c.simulated = true;
            var words = c.text.trim().split(/\s+/), total = root.Logic.estimateMs(c.text, rate), acc = 0;
            var sum = words.reduce(function (s, w) { return s + w.length + 2; }, 0);
            words.forEach(function (w, i) {
              later(function () { if (cur === c && !c.ranged && c.opt.onWord) c.opt.onWord(i); }, total * acc / sum);
              acc += w.length + 2;
            });
            c.simulated = false; // el final lo marca el motor
          }
        }, 700);
      } else if (webOK()) {
        try {
          var u = new root.SpeechSynthesisUtterance(c.text);
          u.lang = 'es-MX'; u.rate = rate;
          u.onstart = function () { onEvent(c.id, 'start'); };
          u.onboundary = function (e) { if (e && typeof e.charIndex === 'number') onEvent(c.id, 'range', e.charIndex); };
          u.onend = function () { onEvent(c.id, 'done'); };
          u.onerror = function () { onEvent(c.id, 'error'); };
          root.speechSynthesis.speak(u);
          later(function () { if (cur === c && !c.started) simulate(c); }, 1500);
        } catch (e) { simulate(c); }
      } else {
        simulate(c);
      }
      return c.id;
    },
    /** Solo habla si la voz automática está activada. */
    auto: function (text, opt) { if (root.Store.get().settings.auto) return Voice.say(text, opt); return null; },
    /** Dice las sílabas despacio y luego la palabra. */
    syllables: function (syl, opt) {
      return Voice.say(syl.join(', ') + '. ' + syl.join(''), opt);
    }
  };

  root.Sfx = Sfx;
  root.Voice = Voice;
})(window);
