package com.lumina;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Pantalla completa "Reproduciendo": portada grande, fondo con el color de la portada y controles. */
final class NowPlayingView extends FrameLayout {
    interface Callbacks {
        void onClose();
        void onToggle();
        void onNext();
        void onPrev();
        void onShuffle();
        void onRepeat();
        void onSeek(int ms);
        void onCover();
    }

    /** La portada, con una sombra suave que la hace flotar y un latido al pausar/reanudar. */
    private static final class ArtView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private Bitmap bm;
        private String seed = "";
        private float scale = 0.88f;
        private ValueAnimator anim;

        ArtView(Context c) { super(c); }

        void set(Bitmap b, String seed) {
            this.bm = b;
            this.seed = seed;
            invalidate();
        }

        void setPlaying(boolean playing) {
            float to = playing ? 1f : 0.88f;
            if (anim != null) anim.cancel();
            anim = ValueAnimator.ofFloat(scale, to);
            anim.setDuration(420);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addUpdateListener(v -> { scale = (float) v.getAnimatedValue(); invalidate(); });
            anim.start();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float side = Math.min(w - Ui.dp(8), h - Ui.dp(24)) * scale;
            float cx = w / 2f, cy = h / 2f;
            float rad = side * 0.07f;
            p.setStyle(Paint.Style.FILL);
            for (int i = 8; i >= 1; i--) {
                float grow = i * Ui.dp(4);
                r.set(cx - side / 2 - grow, cy - side / 2 - grow + Ui.dp(16), cx + side / 2 + grow, cy + side / 2 + grow + Ui.dp(16));
                p.setColor(Ui.withAlpha(0xFF000000, 9));
                c.drawRoundRect(r, rad + grow, rad + grow, p);
            }
            r.set(cx - side / 2, cy - side / 2, cx + side / 2, cy + side / 2);
            Ui.drawArt(c, p, bm, seed, r, rad);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Ui.dp(1));
            p.setColor(0x22FFFFFF);
            c.drawRoundRect(r, rad, rad, p);
        }
    }

    private final Callbacks cb;
    private final Paint bgPaint = new Paint();
    private final ArtView artView;
    private final TextView title, artist, album, tElapsed, tTotal;
    private final SeekView seek;
    private final IconView shuffle, repeat, play;
    private final LinearLayout col;
    private int bgColor = Ui.DK_SURFACE2;
    private ValueAnimator colorAnim;
    private int durationMs;
    private int bgStyle = Ui.npBg;
    private Bitmap blurBm;
    private final RectF dst = new RectF();

    NowPlayingView(Context c, Callbacks callbacks) {
        super(c);
        this.cb = callbacks;
        setWillNotDraw(false);
        setClickable(true);

        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        addView(col, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // barra superior
        FrameLayout top = new FrameLayout(c);
        IconView close = new IconView(c, Icons.CHEVRON_DOWN, Ui.DK_TEXT);
        close.setFrac(0.55f);
        close.setOnClickListener(v -> cb.onClose());
        top.addView(close, new LayoutParams(Ui.dpi(52), Ui.dpi(52), Gravity.START | Gravity.CENTER_VERTICAL));
        TextView head = Ui.label(c, "REPRODUCIENDO", 12, Ui.DK_TEXT2, Ui.MEDIUM);
        head.setLetterSpacing(0.22f);
        top.addView(head, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        IconView imgBtn = new IconView(c, Icons.IMAGE, Ui.DK_TEXT);
        imgBtn.setFrac(0.5f);
        imgBtn.setOnClickListener(v -> cb.onCover());
        top.addView(imgBtn, new LayoutParams(Ui.dpi(52), Ui.dpi(52), Gravity.END | Gravity.CENTER_VERTICAL));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(60));
        tlp.setMargins(Ui.dpi(12), Ui.dpi(4), Ui.dpi(12), 0);
        col.addView(top, tlp);

        // portada
        artView = new ArtView(c);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        alp.setMargins(Ui.dpi(28), Ui.dpi(6), Ui.dpi(28), 0);
        col.addView(artView, alp);

        // textos
        title = Ui.label(c, "", 27, Ui.DK_TEXT, Ui.SERIF);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        artist = Ui.label(c, "", 16.5f, Ui.ACCENT, Ui.MEDIUM);
        artist.setSingleLine(true);
        artist.setEllipsize(TextUtils.TruncateAt.END);
        album = Ui.label(c, "", 13, Ui.DK_TEXT2, Ui.REGULAR);
        album.setSingleLine(true);
        album.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams t1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        t1.setMargins(Ui.dpi(32), Ui.dpi(18), Ui.dpi(32), 0);
        col.addView(title, t1);
        LinearLayout.LayoutParams t2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        t2.setMargins(Ui.dpi(32), Ui.dpi(8), Ui.dpi(32), 0);
        col.addView(artist, t2);
        LinearLayout.LayoutParams t3 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        t3.setMargins(Ui.dpi(32), Ui.dpi(4), Ui.dpi(32), 0);
        col.addView(album, t3);

        // progreso
        seek = new SeekView(c);
        seek.setListener(new SeekView.Listener() {
            @Override public void onScrub(float f) { tElapsed.setText(Ui.fmt((long) (f * durationMs))); }
            @Override public void onSeek(float f) { cb.onSeek((int) (f * durationMs)); }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(40));
        slp.setMargins(Ui.dpi(22), Ui.dpi(14), Ui.dpi(22), 0);
        col.addView(seek, slp);

        LinearLayout times = new LinearLayout(c);
        times.setOrientation(LinearLayout.HORIZONTAL);
        tElapsed = Ui.label(c, "0:00", 12.5f, Ui.DK_TEXT2, Ui.REGULAR);
        tTotal = Ui.label(c, "0:00", 12.5f, Ui.DK_TEXT2, Ui.REGULAR);
        tTotal.setGravity(Gravity.END);
        times.addView(tElapsed, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        times.addView(tTotal, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams tmlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tmlp.setMargins(Ui.dpi(32), 0, Ui.dpi(32), 0);
        col.addView(times, tmlp);

        // controles
        LinearLayout controls = new LinearLayout(c);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        shuffle = new IconView(c, Icons.SHUFFLE, Ui.DK_TEXT2);
        shuffle.setFrac(0.5f);
        shuffle.setOnClickListener(v -> cb.onShuffle());
        IconView prev = new IconView(c, Icons.PREV, Ui.DK_TEXT);
        prev.setFrac(0.6f);
        prev.setOnClickListener(v -> cb.onPrev());
        play = new IconView(c, Icons.PLAY, Ui.DK_BG);
        play.setCircle(0xFFFFFFFF);
        play.setFrac(0.42f);
        play.setOnClickListener(v -> cb.onToggle());
        IconView next = new IconView(c, Icons.NEXT, Ui.DK_TEXT);
        next.setFrac(0.6f);
        next.setOnClickListener(v -> cb.onNext());
        repeat = new IconView(c, Icons.REPEAT, Ui.DK_TEXT2);
        repeat.setFrac(0.5f);
        repeat.setOnClickListener(v -> cb.onRepeat());
        controls.addView(shuffle, new LinearLayout.LayoutParams(0, Ui.dpi(52), 1f));
        controls.addView(prev, new LinearLayout.LayoutParams(0, Ui.dpi(60), 1f));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(Ui.dpi(76), Ui.dpi(76));
        controls.addView(play, plp);
        controls.addView(next, new LinearLayout.LayoutParams(0, Ui.dpi(60), 1f));
        controls.addView(repeat, new LinearLayout.LayoutParams(0, Ui.dpi(52), 1f));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(Ui.dpi(20), Ui.dpi(14), Ui.dpi(20), Ui.dpi(36));
        col.addView(controls, clp);
    }

    private static Bitmap makeBlur(Bitmap b) {
        if (b == null) return null;
        try {
            return Bitmap.createScaledBitmap(b, 32, 32, true);
        } catch (Throwable t) {
            return null;
        }
    }

    void setBgStyle(int style) {
        bgStyle = style;
        invalidate();
    }

    void onInsets(int top, int bottom) {
        col.setPadding(0, top, 0, bottom);
    }

    void setTrack(Track t, Bitmap art) {
        if (t == null) return;
        title.setText(t.title);
        artist.setText(t.artist);
        album.setText(t.album);
        artView.set(art, t.album + t.artist);
        blurBm = makeBlur(art);
        invalidate();
        durationMs = (int) t.durationMs;
        tTotal.setText(Ui.fmt(t.durationMs));
    }

    void setArt(Bitmap art, Track t) {
        artView.set(art, t.album + t.artist);
        blurBm = makeBlur(art);
        invalidate();
    }

    void setDuration(int ms) {
        if (ms > 0 && ms != durationMs) {
            durationMs = ms;
            tTotal.setText(Ui.fmt(ms));
        }
    }

    void setPlaying(boolean playing) {
        play.setKind(playing ? Icons.PAUSE : Icons.PLAY);
        artView.setPlaying(playing);
    }

    void setPosition(int pos, int dur) {
        if (dur > 0) durationMs = dur;
        seek.setProgress(durationMs > 0 ? pos / (float) durationMs : 0f);
        tElapsed.setText(Ui.fmt(pos));
    }

    void setShuffle(boolean on) {
        shuffle.setColor(on ? Ui.ACCENT : Ui.DK_TEXT2);
    }

    void setRepeat(int mode) {
        repeat.setKind(mode == 2 ? Icons.REPEAT_ONE : Icons.REPEAT);
        repeat.setColor(mode == 0 ? Ui.DK_TEXT2 : Ui.ACCENT);
    }

    /** Cambia el color del fondo con una transición suave. */
    void setColor(int color) {
        if (colorAnim != null) colorAnim.cancel();
        final int from = bgColor;
        colorAnim = ValueAnimator.ofFloat(0f, 1f);
        colorAnim.setDuration(600);
        colorAnim.addUpdateListener(v -> {
            bgColor = Ui.blend(from, color, (float) v.getAnimatedValue());
            invalidate();
        });
        colorAnim.start();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        if (bgStyle == 2) {
            c.drawColor(Ui.DK_BG);
            return;
        }
        if (bgStyle == 1 && blurBm != null) {
            bgPaint.setFilterBitmap(true);
            dst.set(0, 0, w, h);
            c.drawBitmap(blurBm, null, dst, bgPaint);
            bgPaint.setShader(new LinearGradient(0, 0, 0, h, new int[]{0x88000000, 0xCC0B0C10, Ui.DK_BG},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, bgPaint);
            bgPaint.setShader(null);
            return;
        }
        bgPaint.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{bgColor, Ui.blend(bgColor, Ui.DK_BG, 0.72f), Ui.DK_BG}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(new RadialGradient(w / 2f, h * 0.30f, w * 0.85f,
                Ui.withAlpha(Ui.blend(bgColor, 0xFFFFFFFF, 0.25f), 80), 0x00000000, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, bgPaint);
        bgPaint.setShader(null);
    }
}
