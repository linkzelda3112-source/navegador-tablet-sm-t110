package com.craterz;

/** Estado de los controles táctiles (escrito por la interfaz, leído por el juego). */
public final class Input {
    public volatile float moveX, moveY;
    public volatile boolean fire, ads;
    private float lookDX, lookDY;
    private int reload, swap, use, adsTap, pause;

    public synchronized void addLook(float dx, float dy) { lookDX += dx; lookDY += dy; }

    public synchronized void takeLook(float[] out) {
        out[0] = lookDX; out[1] = lookDY;
        lookDX = 0; lookDY = 0;
    }

    public synchronized void pressReload() { reload++; }
    public synchronized void pressSwap() { swap++; }
    public synchronized void pressUse() { use++; }
    public synchronized void pressAds() { adsTap++; }
    public synchronized void pressPause() { pause++; }

    public synchronized boolean takeReload() { boolean b = reload > 0; reload = 0; return b; }
    public synchronized boolean takeSwap() { boolean b = swap > 0; swap = 0; return b; }
    public synchronized boolean takeUse() { boolean b = use > 0; use = 0; return b; }
    public synchronized boolean takeAds() { boolean b = adsTap > 0; adsTap = 0; return b; }
    public synchronized boolean takePause() { boolean b = pause > 0; pause = 0; return b; }

    public synchronized void clear() {
        moveX = moveY = 0; fire = false;
        lookDX = lookDY = 0; reload = swap = use = adsTap = pause = 0;
    }
}
