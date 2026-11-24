/* Sonidos sintetizados con WebAudio (sin archivos). */
(function (root) {
  'use strict';
  var ctx = null, on = true;

  function ac() {
    if (!ctx) {
      var C = root.AudioContext || root.webkitAudioContext;
      if (!C) return null;
      try { ctx = new C(); } catch (e) { return null; }
    }
    if (ctx.state === 'suspended') { try { ctx.resume(); } catch (e) {} }
    return ctx;
  }

  function tone(f, d, type, vol, slideTo, delay) {
    var c = ac(); if (!c || !on) return;
    var t = c.currentTime + (delay || 0);
    var o = c.createOscillator(), g = c.createGain();
    o.type = type || 'sine';
    o.frequency.setValueAtTime(f, t);
    if (slideTo) o.frequency.exponentialRampToValueAtTime(slideTo, t + d);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(vol || 0.2, t + 0.012);
    g.gain.exponentialRampToValueAtTime(0.0001, t + d);
    o.connect(g); g.connect(c.destination);
    o.start(t); o.stop(t + d + 0.03);
  }

  function noise(d, vol, delay) {
    var c = ac(); if (!c || !on) return;
    var t = c.currentTime + (delay || 0);
    var n = Math.floor(c.sampleRate * d);
    var buf = c.createBuffer(1, n, c.sampleRate), data = buf.getChannelData(0);
    for (var i = 0; i < n; i++) data[i] = (Math.random() * 2 - 1) * (1 - i / n);
    var s = c.createBufferSource(), g = c.createGain();
    s.buffer = buf; g.gain.value = vol || 0.2;
    s.connect(g); g.connect(c.destination); s.start(t);
  }

  var SFX = {
    ok: function () { tone(660, 0.1, 'triangle', 0.22); tone(990, 0.16, 'triangle', 0.22, null, 0.08); },
    bad: function () { tone(220, 0.22, 'sawtooth', 0.16, 120); },
    pop: function () { noise(0.09, 0.25); tone(500, 0.08, 'sine', 0.2, 900); },
    boom: function () { noise(0.35, 0.35); tone(120, 0.3, 'sawtooth', 0.2, 40); },
    laser: function () { tone(1400, 0.14, 'square', 0.1, 300); },
    hit: function () { noise(0.12, 0.3); tone(180, 0.15, 'square', 0.18, 80); },
    crit: function () { noise(0.14, 0.3); tone(300, 0.1, 'square', 0.2, 900); tone(900, 0.2, 'square', 0.2, 1500, 0.08); },
    ouch: function () { tone(300, 0.3, 'sawtooth', 0.2, 80); noise(0.15, 0.2); },
    flip: function () { tone(520, 0.05, 'triangle', 0.15, 700); },
    match: function () { tone(587, 0.1, 'triangle', 0.22); tone(784, 0.1, 'triangle', 0.22, null, 0.09); tone(988, 0.18, 'triangle', 0.22, null, 0.18); },
    tick: function () { tone(880, 0.05, 'square', 0.08); },
    go: function () { tone(1046, 0.4, 'square', 0.14); },
    star: function () { tone(784, 0.12, 'triangle', 0.22); tone(1175, 0.2, 'triangle', 0.22, null, 0.1); },
    win: function () { [523, 659, 784, 1046].forEach(function (f, i) { tone(f, 0.18, 'triangle', 0.24, null, i * 0.12); }); tone(1046, 0.5, 'triangle', 0.24, null, 0.5); },
    lose: function () { [392, 330, 262].forEach(function (f, i) { tone(f, 0.25, 'triangle', 0.22, null, i * 0.18); }); },
    level: function () { [523, 659, 784, 1046, 1318].forEach(function (f, i) { tone(f, 0.16, 'square', 0.12, null, i * 0.09); }); },
    key: function () { tone(700, 0.03, 'square', 0.06); }
  };

  root.Sound = {
    unlock: function () { ac(); },
    play: function (n) { try { if (SFX[n]) SFX[n](); } catch (e) {} },
    setOn: function (v) { on = !!v; },
    isOn: function () { return on; },
    vibrate: function (ms) { try { if (on && navigator.vibrate) navigator.vibrate(ms); } catch (e) {} }
  };
})(window);
