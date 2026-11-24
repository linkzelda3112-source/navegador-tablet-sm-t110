'use strict';
// ===== Guardado: localStorage + Block Store de Google (sobrevive a desinstalar) + hora verificada =====
const Store = (() => {
  const KEY = 'sua2';
  let PS = null;
  try { if (window.Capacitor && Capacitor.registerPlugin) PS = Capacitor.registerPlugin('PersistentStore'); } catch (e) { PS = null; }
  const tmo = (p, ms) => Promise.race([p, new Promise((_, rej) => setTimeout(() => rej(new Error('tiempo')), ms))]);
  const lget = () => { try { return JSON.parse(localStorage.getItem(KEY)); } catch (e) { return null; } };
  const lset = o => { try { localStorage.setItem(KEY, JSON.stringify(o)); } catch (e) { } };
  const nget = async () => { if (!PS) return null; try { const r = await tmo(PS.get({ key: KEY }), 4000); return r && r.value ? JSON.parse(r.value) : null; } catch (e) { return null; } };
  const nset = async o => { if (!PS) return; try { await tmo(PS.set({ key: KEY, value: JSON.stringify(o) }), 4000); } catch (e) { } };
  // elige el registro mas avanzado (nunca se pierde progreso ni se cambia de mundo)
  function better(a, b) {
    if (!a) return b; if (!b) return a;
    if (a.world !== null && b.world !== null && a.world !== b.world) return (a.start || '') <= (b.start || '') ? a : b;
    if (!!a.end !== !!b.end) return a.end ? a : b;
    if ((a.days || 0) !== (b.days || 0)) return a.days > b.days ? a : b;
    return (a.seenTs || 0) >= (b.seenTs || 0) ? a : b;
  }
  async function load() {
    const l = lget(), n = await nget(), r = better(l, n);
    if (r) { lset(r); if (!n || JSON.stringify(n) !== JSON.stringify(r)) nset(r); }
    return r;
  }
  function save(o) { lset(o); nset(o); }
  // hora verificada: servidor si hay internet; si no, la del telefono (con proteccion contra retrocesos)
  async function now() {
    const dev = Date.now();
    const tries = [
      async () => (await (await fetch('https://worldtimeapi.org/api/timezone/Etc/UTC', { cache: 'no-store' })).json()).unixtime * 1000,
      async () => Date.parse((await (await fetch('https://timeapi.io/api/Time/current/zone?timeZone=UTC', { cache: 'no-store' })).json()).dateTime + 'Z'),
    ];
    for (const f of tries) { try { const t = await tmo(f(), 3500); if (t > 1.6e12) return { ts: Math.abs(t - dev) > 300000 ? t : dev, server: true }; } catch (e) { } }
    return { ts: dev, server: false };
  }
  return { load, save, now, native: !!PS };
})();
