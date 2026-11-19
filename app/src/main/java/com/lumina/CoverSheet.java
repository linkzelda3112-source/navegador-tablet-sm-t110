package com.lumina;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Hoja inferior para cambiar la carátula: imagen del teléfono, búsqueda en línea o quitar la personalizada. */
final class CoverSheet extends Dialog {
    interface Callbacks {
        void onPickLocal();
        void onChanged();
    }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);

    /** Miniatura cuadrada de un resultado de búsqueda; se descarga sola. */
    private static final class Thumb extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private Bitmap bm;
        private final String url;

        Thumb(Context c, String url) {
            super(c);
            this.url = url;
            setClickable(true);
            POOL.execute(() -> {
                try {
                    Bitmap b = CoverStore.decodeSampled(CoverSearch.download(url), 300);
                    if (b != null) post(() -> { bm = b; invalidate(); });
                } catch (Throwable ignored) { }
            });
        }

        @Override protected void onMeasure(int w, int h) {
            int s = MeasureSpec.getSize(w);
            setMeasuredDimension(s, s);
        }

        @Override protected void onDraw(Canvas c) {
            r.set(Ui.dp(4), Ui.dp(4), getWidth() - Ui.dp(4), getHeight() - Ui.dp(4));
            Ui.drawArt(c, p, bm, url, r, Ui.dp(14));
            if (isPressed()) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0x33FFFFFF);
                c.drawRoundRect(r, Ui.dp(14), Ui.dp(14), p);
            }
        }

        @Override public void setPressed(boolean pressed) {
            super.setPressed(pressed);
            invalidate();
        }
    }

    private final Track track;
    private final CoverStore covers;
    private final Callbacks cb;
    private final Handler main = new Handler(Looper.getMainLooper());
    private LinearLayout root;
    private int searchGen;

    CoverSheet(Context c, Track t, CoverStore covers, Callbacks cb) {
        super(c);
        this.track = t;
        this.covers = covers;
        this.cb = cb;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Ui.SURFACE);
        float r = Ui.dp(28);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        root.setBackground(g);
        root.setPadding(Ui.dpi(22), Ui.dpi(12), Ui.dpi(22), Ui.dpi(28));
        setContentView(root);
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.BOTTOM);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        showMenu();
    }

    private void handle() {
        View h = new View(getContext());
        h.setBackground(Ui.shape(Ui.withAlpha(Ui.TEXT, 50), 0, Ui.dp(3)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(5));
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.bottomMargin = Ui.dpi(16);
        root.addView(h, lp);
    }

    private LinearLayout row(int icon, String text, Runnable action) {
        Context c = getContext();
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(Ui.dpi(14), 0, Ui.dpi(14), 0);
        l.setBackground(Ui.pressable(Ui.SURFACE2, Ui.blend(Ui.SURFACE2, Ui.TEXT, 0.08f), 0, Ui.dp(18)));
        l.setClickable(true);
        IconView iv = new IconView(c, icon, Ui.ACCENT);
        iv.setFrac(0.5f);
        iv.setClickable(false);
        l.addView(iv, new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(40)));
        TextView t = Ui.label(c, text, 16.5f, Ui.TEXT, Ui.MEDIUM);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tl.leftMargin = Ui.dpi(10);
        l.addView(t, tl);
        l.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(58));
        lp.topMargin = Ui.dpi(10);
        l.setLayoutParams(lp);
        return l;
    }

    private void showMenu() {
        Context c = getContext();
        root.removeAllViews();
        handle();
        root.addView(Ui.label(c, "Carátula", 24, Ui.TEXT, Ui.SERIF));
        TextView sub = Ui.label(c, track.title + " · " + track.artist, 13.5f, Ui.TEXT2, Ui.REGULAR);
        sub.setSingleLine(true);
        sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
        sub.setPadding(0, Ui.dpi(6), 0, Ui.dpi(6));
        root.addView(sub);
        root.addView(row(Icons.IMAGE, "Elegir imagen del teléfono", () -> {
            dismiss();
            cb.onPickLocal();
        }));
        root.addView(row(Icons.SEARCH, "Buscar carátula en línea", this::showSearch));
        if (covers.has(track)) {
            root.addView(row(Icons.TRASH, "Quitar carátula personalizada", () -> {
                covers.remove(track);
                dismiss();
                cb.onChanged();
            }));
        }
        TextView note = Ui.label(c, "Se aplica a todo el álbum. La búsqueda en línea usa internet solo cuando la tocas.",
                12.5f, Ui.TEXT2, Ui.REGULAR);
        note.setLineSpacing(Ui.dp(2), 1f);
        note.setPadding(0, Ui.dpi(16), 0, 0);
        root.addView(note);
    }

    private TextView status;
    private LinearLayout grid;

    private void showSearch() {
        final Context c = getContext();
        root.removeAllViews();
        handle();

        LinearLayout head = new LinearLayout(c);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        IconView back = new IconView(c, Icons.BACK, Ui.TEXT);
        back.setFrac(0.5f);
        back.setOnClickListener(v -> {
            searchGen++;
            showMenu();
        });
        head.addView(back, new LinearLayout.LayoutParams(Ui.dpi(44), Ui.dpi(44)));
        head.addView(Ui.label(c, "Buscar en línea", 22, Ui.TEXT, Ui.SERIF));
        root.addView(head);

        final EditText et = new EditText(c);
        String album = track.album.equals(Library.UNKNOWN_ALBUM) ? track.title : track.album;
        String artist = track.artist.equals(Library.UNKNOWN_ARTIST) ? "" : track.artist;
        et.setText((artist + " " + album).trim());
        et.setSelectAllOnFocus(false);
        et.setSingleLine(true);
        et.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        et.setTextColor(Ui.TEXT);
        et.setHintTextColor(Ui.TEXT2);
        et.setHint("Artista y álbum");
        et.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.dp(16));
        et.setBackground(Ui.shape(Ui.SURFACE2, Ui.LINE, Ui.dp(24)));
        et.setPadding(Ui.dpi(18), 0, Ui.dpi(18), 0);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(48));
        elp.topMargin = Ui.dpi(10);
        root.addView(et, elp);
        et.setOnEditorActionListener((v, action, ev) -> {
            runSearch(et.getText().toString());
            return true;
        });

        status = Ui.label(c, "", 14, Ui.TEXT2, Ui.REGULAR);
        status.setPadding(0, Ui.dpi(12), 0, Ui.dpi(6));
        root.addView(status);

        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        grid = new LinearLayout(c);
        grid.setOrientation(LinearLayout.VERTICAL);
        sv.addView(grid);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(330)));

        runSearch(et.getText().toString());
    }

    private void runSearch(final String term) {
        if (term.trim().isEmpty()) return;
        final int gen = ++searchGen;
        status.setText("Buscando…");
        grid.removeAllViews();
        POOL.execute(() -> {
            List<CoverSearch.Result> res = null;
            boolean offline = false;
            try {
                res = CoverSearch.search(term.trim());
            } catch (Throwable e) {
                offline = true;
            }
            final List<CoverSearch.Result> fr = res;
            final boolean fo = offline;
            main.post(() -> {
                if (gen != searchGen || grid == null) return;
                if (fo) {
                    status.setText("No pude conectarme. Revisa tu internet.");
                } else if (fr == null || fr.isEmpty()) {
                    status.setText("Sin resultados. Prueba con otro nombre.");
                } else {
                    status.setText("Toca una para ponerla");
                    fillGrid(fr);
                }
            });
        });
    }

    private void fillGrid(List<CoverSearch.Result> items) {
        grid.removeAllViews();
        Context c = getContext();
        LinearLayout row = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % 3 == 0) {
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.HORIZONTAL);
                grid.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            final CoverSearch.Result r = items.get(i);
            Thumb t = new Thumb(c, r.thumb);
            t.setOnClickListener(v -> apply(r));
            row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        int rem = items.size() % 3;
        if (rem != 0 && row != null) {
            for (int k = rem; k < 3; k++) row.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 1f));
        }
    }

    private void apply(final CoverSearch.Result r) {
        status.setText("Aplicando…");
        grid.removeAllViews();
        POOL.execute(() -> {
            boolean ok;
            try {
                ok = covers.setFromBytes(track, CoverSearch.download(r.full));
            } catch (Throwable e) {
                ok = false;
            }
            final boolean fok = ok;
            main.post(() -> {
                if (fok) {
                    dismiss();
                    cb.onChanged();
                } else if (status != null) {
                    status.setText("No pude descargar esa imagen. Prueba con otra.");
                }
            });
        });
    }
}
