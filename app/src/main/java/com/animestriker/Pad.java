package com.animestriker;

/** Controles táctiles: joystick flotante a la izquierda y botones a la derecha. */
final class Pad {
    static final int A = 0, B = 1, C = 2, D = 3, S = 4;
    float r;
    float jx, jy, jr;
    float baseX, baseY, knobX, knobY;
    boolean joyOn;
    int joyId = -1;
    final float[] bx = new float[5], by = new float[5], br = new float[5];
    final int[] ptr = {-1, -1, -1, -1, -1};
    final boolean[] down = new boolean[5];
    float pauseX, pauseY, pauseR;

    void layout(int w, int h) {
        r = h * 0.1f;
        jr = r * 1.35f; jx = r * 2.5f; jy = h - r * 2.3f;
        if (!joyOn) { baseX = jx; baseY = jy; knobX = jx; knobY = jy; }
        bx[B] = w - r * 1.9f; by[B] = h - r * 1.7f; br[B] = r * 1.15f;
        bx[A] = w - r * 4.15f; by[A] = h - r * 1.25f; br[A] = r * 1.0f;
        bx[C] = w - r * 3.45f; by[C] = h - r * 3.65f; br[C] = r * 0.9f;
        bx[D] = w - r * 1.3f; by[D] = h - r * 3.95f; br[D] = r * 0.85f;
        bx[S] = w - r * 5.9f; by[S] = h - r * 3.2f; br[S] = r * 0.8f;
        pauseR = r * 0.55f; pauseX = w - pauseR * 1.8f; pauseY = pauseR * 1.8f;
    }

    int hit(float x, float y) {
        int best = -1; float bd = 1e9f;
        for (int i = 0; i < 5; i++) {
            float dx = x - bx[i], dy = y - by[i], d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < br[i] * 1.2f && d < bd) { bd = d; best = i; }
        }
        return best;
    }

    boolean hitPause(float x, float y) {
        float dx = x - pauseX, dy = y - pauseY;
        return dx * dx + dy * dy < pauseR * pauseR * 2.2f;
    }
}
