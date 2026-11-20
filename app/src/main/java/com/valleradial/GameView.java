package com.valleradial;

import android.content.Context;
import android.opengl.GLSurfaceView;

/** Superficie OpenGL ES 2.0. Se dibuja a menor resolucion (look "dibujado" y mas fluido). */
final class GameView extends GLSurfaceView {
    private int fixedW = -1, fixedH = -1;

    GameView(Context ctx, Game game) {
        super(ctx);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        setRenderer(new GLRenderer(game));
        setRenderMode(RENDERMODE_CONTINUOUSLY);
        setPreserveEGLContextOnPause(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w <= 0 || h <= 0) return;
        float s = Math.min(1f, 600f / h);
        int fw = Math.max(2, (int) (w * s)), fh = Math.max(2, (int) (h * s));
        if (fw != fixedW || fh != fixedH) {
            fixedW = fw;
            fixedH = fh;
            getHolder().setFixedSize(fw, fh);
        }
    }
}
