package com.lumina;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Pantalla de inicio al estilo Spotify: saludo, accesos (Me encanta, Me gusta…), éxitos y recientes. */
final class HomeView extends ScrollView {
    interface Callbacks {
        void onOpen(String title, List<Track> tracks);
        void onPlay(List<Track> list, int index);
        void onRefreshCharts();
        void onShuffle();
    }

    private final Callbacks cb;
    private final ArtCache art;
    private final LinearLayout col;
    private int insetBottom;
    private boolean shown;

    HomeView(Context c, ArtCache art, Callbacks cb) {
        super(c);
        this.art = art;
        this.cb = cb;
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        addView(col, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    void onInsets(int bottom) { insetBottom = bottom; }

    // ---------------------------------------------------------------- piezas

    private final class Tile extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final int kind, color;
        private final String label, sub;

        Tile(Context c, int kind, int color, String label, String sub, Runnable click) {
            super(c);
            this.kind = kind;
            this.color = color;
            this.label = label;
            this.sub = sub;
            setClickable(true);
            setOnClickListener(v -> click.run());
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            r.set(0, 0, w, h);
            p.setStyle(Paint.Style.FILL);
            p.setColor(isPressed() ? Ui.SURFACE2 : Ui.SURFACE);
            c.drawRoundRect(r, Ui.dp(16), Ui.dp(16), p);
            p.setShader(new android.graphics.LinearGradient(0, 0, w, 0, Ui.withAlpha(color, Ui.light ? 40 : 70),
                    Ui.withAlpha(color, 0), android.graphics.Shader.TileMode.CLAMP));
            c.drawRoundRect(r, Ui.dp(16), Ui.dp(16), p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Ui.dp(1));
            p.setColor(Ui.LINE);
            c.drawRoundRect(r, Ui.dp(16), Ui.dp(16), p);
            float m = Ui.dp(9);
            r.set(m, m, h - m, h - m);
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            c.drawRoundRect(r, Ui.dp(10), Ui.dp(10), p);
            Icons.draw(c, p, kind, h / 2f, h / 2f, (h - 2 * m) * 0.62f, 0xFFFFFFFF);
            float tx = h + Ui.dp(2), maxW = w - tx - Ui.dp(8);
            tp.setTypeface(Ui.MEDIUM);
            tp.setTextSize(Ui.dp(14.5f));
            tp.setColor(Ui.TEXT);
            c.drawText(TextUtils.ellipsize(label, tp, maxW, TextUtils.TruncateAt.END).toString(), tx, h / 2f - Ui.dp(2), tp);
            tp.setTypeface(Ui.REGULAR);
            tp.setTextSize(Ui.dp(12));
            tp.setColor(Ui.TEXT2);
            c.drawText(TextUtils.ellipsize(sub, tp, maxW, TextUtils.TruncateAt.END).toString(), tx, h / 2f + Ui.dp(15), tp);
        }

        @Override
        public void setPressed(boolean pressed) {
            super.setPressed(pressed);
            invalidate();
        }
    }

    private final class Card extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final Track t;
        private final int rank;
        private final Runnable redraw = this::invalidate;

        Card(Context c, Track t, int rank, Runnable click) {
            super(c);
            this.t = t;
            this.rank = rank;
            setClickable(true);
            setOnClickListener(v -> click.run());
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension(Ui.dpi(140), Ui.dpi(212));
        }

        @Override
        protected void onDraw(Canvas c) {
            float s = Ui.dp(140);
            r.set(0, 0, s, s);
            Bitmap bm = art.get(t, 320, redraw);
            Ui.drawArt(c, p, bm, t.album + t.artist, r, Ui.dp(14));
            if (rank > 0) {
                tp.setTypeface(Ui.MEDIUM);
                tp.setTextSize(Ui.dp(11.5f));
                String lab = "#" + rank;
                float tw = tp.measureText(lab);
                r.set(Ui.dp(8), s - Ui.dp(30), Ui.dp(8) + tw + Ui.dp(16), s - Ui.dp(8));
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xCC000000);
                c.drawRoundRect(r, Ui.dp(11), Ui.dp(11), p);
                tp.setColor(0xFFFFFFFF);
                c.drawText(lab, r.left + Ui.dp(8), r.centerY() + Ui.dp(4), tp);
            }
            tp.setTypeface(Ui.MEDIUM);
            tp.setTextSize(Ui.dp(14));
            tp.setColor(Ui.TEXT);
            c.drawText(TextUtils.ellipsize(t.title, tp, s, TextUtils.TruncateAt.END).toString(), 0, s + Ui.dp(22), tp);
            tp.setTypeface(Ui.REGULAR);
            tp.setTextSize(Ui.dp(12.5f));
            tp.setColor(Ui.TEXT2);
            c.drawText(TextUtils.ellipsize(t.artist, tp, s, TextUtils.TruncateAt.END).toString(), 0, s + Ui.dp(42), tp);
        }
    }

    private final class Hero extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF(), btn = new RectF();
        private final int songs, artists, albums;
        private boolean btnDown;

        Hero(Context c, int songs, int artists, int albums) {
            super(c);
            this.songs = songs;
            this.artists = artists;
            this.albums = albums;
            setClickable(true);
            setOnClickListener(v -> { if (btnDown) cb.onShuffle(); });
        }

        @Override
        public boolean onTouchEvent(android.view.MotionEvent e) {
            if (e.getActionMasked() == android.view.MotionEvent.ACTION_DOWN) btnDown = btn.contains(e.getX(), e.getY());
            return super.onTouchEvent(e);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float t = System.nanoTime() / 1e9f;
            r.set(0, 0, w, h);
            int a = Ui.ACCENT, b = Ui.blend(Ui.ACCENT, Ui.light ? 0xFFFFFFFF : 0xFF000000, Ui.light ? 0.55f : 0.62f);
            p.setStyle(Paint.Style.FILL);
            p.setShader(new android.graphics.LinearGradient(0, 0, w, h, a, b, android.graphics.Shader.TileMode.CLAMP));
            c.drawRoundRect(r, Ui.dp(26), Ui.dp(26), p);
            p.setShader(null);
            // ondas decorativas que se mueven
            c.save();
            Path clip = new Path();
            clip.addRoundRect(r, Ui.dp(26), Ui.dp(26), Path.Direction.CW);
            c.clipPath(clip);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Ui.dp(1.4f));
            for (int k = 0; k < 4; k++) {
                p.setColor(0x22FFFFFF);
                Path wave = new Path();
                float y0 = h * (0.35f + 0.13f * k);
                wave.moveTo(0, y0);
                for (float x = 0; x <= w; x += Ui.dp(8)) {
                    wave.lineTo(x, y0 + Ui.dp(10) * (float) Math.sin(x / Ui.dp(46) + t * (0.8f + k * 0.25f) + k));
                }
                c.drawPath(wave, p);
            }
            c.restore();
            p.setStyle(Paint.Style.FILL);
            tp.setLetterSpacing(0.22f);
            tp.setTypeface(Ui.MEDIUM);
            tp.setTextSize(Ui.dp(11));
            tp.setColor(0xCCFFFFFF);
            c.drawText("TU BIBLIOTECA", Ui.dp(22), Ui.dp(34), tp);
            tp.setLetterSpacing(0f);
            tp.setTypeface(Ui.SERIF);
            tp.setTextSize(Ui.dp(34));
            tp.setColor(0xFFFFFFFF);
            c.drawText(songs + (songs == 1 ? " canción" : " canciones"), Ui.dp(22), Ui.dp(76), tp);
            tp.setTypeface(Ui.REGULAR);
            tp.setTextSize(Ui.dp(13.5f));
            tp.setColor(0xDDFFFFFF);
            c.drawText(artists + " artistas · " + albums + " álbumes", Ui.dp(22), Ui.dp(100), tp);
            // botón
            float bw = Ui.dp(170), bh = Ui.dp(44);
            btn.set(Ui.dp(22), h - Ui.dp(22) - bh, Ui.dp(22) + bw, h - Ui.dp(22));
            p.setColor(0xFFFFFFFF);
            c.drawRoundRect(btn, bh / 2f, bh / 2f, p);
            Icons.draw(c, p, Icons.SHUFFLE, btn.left + Ui.dp(26), btn.centerY(), Ui.dp(24), 0xFF1A1A22);
            tp.setTypeface(Ui.MEDIUM);
            tp.setTextSize(Ui.dp(14.5f));
            tp.setColor(0xFF1A1A22);
            c.drawText("Aleatorio", btn.left + Ui.dp(48), btn.centerY() + Ui.dp(5), tp);
            postInvalidateDelayed(40);
        }
    }

    private TextView heading(String text, String subText) {
        TextView t = Ui.label(getContext(), text, 23, Ui.TEXT, Ui.SERIF);
        t.setPadding(Ui.dpi(24), Ui.dpi(22), Ui.dpi(24), subText.isEmpty() ? Ui.dpi(12) : Ui.dpi(2));
        t.setCompoundDrawablePadding(Ui.dpi(10));
        return t;
    }

    private void addCarousel(String title, String subText, List<Track> tracks, List<Integer> ranks) {
        col.addView(heading(title, subText));
        if (!subText.isEmpty()) {
            TextView st = Ui.label(getContext(), subText, 12.5f, Ui.TEXT2, Ui.REGULAR);
            st.setPadding(Ui.dpi(24), 0, Ui.dpi(24), Ui.dpi(12));
            col.addView(st);
        }
        HorizontalScrollView hs = new HorizontalScrollView(getContext());
        hs.setHorizontalScrollBarEnabled(false);
        hs.setOverScrollMode(OVER_SCROLL_NEVER);
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(Ui.dpi(24), 0, Ui.dpi(10), 0);
        final List<Track> list = new ArrayList<>(tracks);
        for (int i = 0; i < list.size(); i++) {
            final int idx = i;
            Card card = new Card(getContext(), list.get(i), ranks == null ? 0 : ranks.get(i), () -> cb.onPlay(list, idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dpi(140), Ui.dpi(212));
            lp.rightMargin = Ui.dpi(14);
            row.addView(card, lp);
        }
        hs.addView(row);
        col.addView(hs, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addTileRow(Tile a, Tile b) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams l1 = new LinearLayout.LayoutParams(0, Ui.dpi(64), 1f);
        l1.rightMargin = Ui.dpi(6);
        LinearLayout.LayoutParams l2 = new LinearLayout.LayoutParams(0, Ui.dpi(64), 1f);
        l2.leftMargin = Ui.dpi(6);
        row.addView(a, l1);
        row.addView(b, l2);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rl.setMargins(Ui.dpi(18), 0, Ui.dpi(18), Ui.dpi(12));
        col.addView(row, rl);
    }

    private static String count(int n) { return n == 1 ? "1 canción" : n + " canciones"; }

    private void open(String title, List<Track> l, String emptyMsg) {
        if (l.isEmpty()) Toast.makeText(getContext(), emptyMsg, Toast.LENGTH_LONG).show();
        else cb.onOpen(title, l);
    }

    /**
     * Reconstruye la pantalla.
     * chartState: 0 = sin datos (sin conexión), 1 = descargando, 2 = con datos.
     */
    void update(List<Track> all, List<Track> loved, List<Track> liked, List<Track> recent, List<Charts.Hit> hits,
                int chartState, int chartTotal, String chartCountry) {
        int keep = getScrollY();
        col.removeAllViews();
        col.setPadding(0, Ui.dpi(6), 0, insetBottom + Ui.dpi(120));
        Context c = getContext();

        java.util.HashSet<String> ar = new java.util.HashSet<>(), al = new java.util.HashSet<>();
        for (Track t : all) { ar.add(Personal.norm(t.artist)); al.add(Personal.norm(t.album)); }
        Hero hero = new Hero(c, all.size(), ar.size(), al.size());
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(190));
        hl.setMargins(Ui.dpi(18), Ui.dpi(8), Ui.dpi(18), Ui.dpi(18));
        col.addView(hero, hl);

        final List<Track> hitTracks = new ArrayList<>();
        final List<Integer> hitRanks = new ArrayList<>();
        for (Charts.Hit h : hits) {
            hitTracks.add(h.track);
            hitRanks.add(h.rank);
        }
        final String loveMsg = "Toca el corazón en “Reproduciendo” para guardar aquí tus canciones favoritas.";
        final String likeMsg = "Toca el pulgar en “Reproduciendo” para marcar canciones que te gustan.";
        String hitsMsg = chartState == 0 ? "Conéctate a internet para ver los éxitos del momento."
                : chartState == 1 ? "Buscando los éxitos del momento…"
                : "Ningún éxito del momento está en tu biblioteca todavía.";
        final String hitsMsgF = hitsMsg;
        Tile love = new Tile(c, Icons.HEART_FILLED, 0xFFE8557E, "Me encanta", count(loved.size()),
                () -> open("Me encanta", loved, loveMsg));
        Tile like = new Tile(c, Icons.THUMB_FILLED, 0xFFD9A441, "Me gusta", count(liked.size()),
                () -> open("Me gusta", liked, likeMsg));
        Tile hit = new Tile(c, Icons.TRENDING, 0xFF6C5CE7, "Éxitos", chartState == 2 ? hits.size() + " en tu biblioteca" : "En línea",
                () -> open("Éxitos en tu biblioteca", hitTracks, hitsMsgF));
        Tile rec = new Tile(c, Icons.CLOCK, 0xFF2BAA8A, "Recientes", count(recent.size()),
                () -> open("Escuchado recientemente", recent, "Aún no has escuchado nada. ¡Pon una canción!"));
        addTileRow(love, like);
        addTileRow(hit, rec);

        // éxitos
        if (!hits.isEmpty()) {
            String where = chartCountry.isEmpty() ? "" : "Top " + chartCountry + " · ";
            addCarousel("Éxitos en tu biblioteca", where + hits.size() + " de " + chartTotal + " canciones del momento", hitTracks, hitRanks);
        } else {
            col.addView(heading("Éxitos en tu biblioteca", ""));
            TextView note = Ui.label(c, hitsMsg + (chartState == 1 ? "" : "  Toca para actualizar."), 14, Ui.TEXT2, Ui.REGULAR);
            note.setLineSpacing(Ui.dp(3), 1f);
            note.setGravity(Gravity.START);
            note.setPadding(Ui.dpi(24), 0, Ui.dpi(24), Ui.dpi(6));
            note.setOnClickListener(v -> cb.onRefreshCharts());
            col.addView(note);
        }

        if (!recent.isEmpty()) addCarousel("Escuchado recientemente", "", recent.subList(0, Math.min(recent.size(), 20)), null);
        if (!loved.isEmpty()) addCarousel("Me encanta", "", loved.subList(0, Math.min(loved.size(), 20)), null);

        if (!shown) {
            shown = true;
            for (int i = 0; i < col.getChildCount(); i++) {
                View v = col.getChildAt(i);
                v.setAlpha(0f);
                v.setTranslationY(Ui.dp(30));
                v.animate().alpha(1f).translationY(0f).setStartDelay(80L + i * 70L).setDuration(520)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(2f)).start();
            }
        }
        final int restore = keep;
        post(() -> scrollTo(0, restore));
    }
}
