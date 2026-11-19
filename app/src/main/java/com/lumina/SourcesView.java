package com.lumina;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Pantalla para elegir dónde está la música: todo el teléfono y/o carpetas concretas. */
final class SourcesView extends FrameLayout {
    interface Callbacks {
        void onToggleAllPhone();
        void onAddFolder();
        void onRemoveFolder(String tree);
        void onDone();
    }

    private final Callbacks cb;
    private final LinearLayout content;
    private final IconView close;
    private int top, bottom;

    SourcesView(Context c, Callbacks callbacks) {
        super(c);
        this.cb = callbacks;
        setBackgroundColor(Ui.BG);
        setClickable(true);

        ScrollView scroll = new ScrollView(c);
        scroll.setVerticalScrollBarEnabled(false);
        content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        close = new IconView(c, Icons.CLOSE, Ui.TEXT);
        close.setFrac(0.5f);
        close.setOnClickListener(v -> cb.onDone());
        addView(close, new LayoutParams(Ui.dpi(52), Ui.dpi(52), Gravity.TOP | Gravity.END));
    }

    void onInsets(int t, int b) {
        top = t;
        bottom = b;
        LayoutParams lp = (LayoutParams) close.getLayoutParams();
        lp.topMargin = t + Ui.dpi(8);
        lp.rightMargin = Ui.dpi(12);
        close.setLayoutParams(lp);
        content.setPadding(Ui.dpi(24), t + Ui.dpi(28), Ui.dpi(24), b + Ui.dpi(28));
    }

    private static String[] folderLabel(String tree) {
        try {
            String id = DocumentsContract.getTreeDocumentId(Uri.parse(tree));
            int c = id.indexOf(':');
            String vol = c >= 0 ? id.substring(0, c) : id;
            String path = c >= 0 ? id.substring(c + 1) : "";
            String name = path.isEmpty() ? "Almacenamiento" : path.substring(path.lastIndexOf('/') + 1);
            String where = vol.equals("primary") ? "Almacenamiento interno" : "Tarjeta SD";
            return new String[]{name, where + (path.isEmpty() ? "" : " › " + path.replace("/", " › "))};
        } catch (Throwable t) {
            return new String[]{"Carpeta", tree};
        }
    }

    private LinearLayout card(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setPadding(Ui.dpi(16), Ui.dpi(16), Ui.dpi(16), Ui.dpi(16));
        l.setBackground(Ui.pressable(Ui.SURFACE, Ui.SURFACE2, Ui.LINE, Ui.dp(20)));
        l.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dpi(12);
        l.setLayoutParams(lp);
        return l;
    }

    private LinearLayout texts(Context c, String a, String b) {
        LinearLayout t = new LinearLayout(c);
        t.setOrientation(LinearLayout.VERTICAL);
        TextView ta = Ui.label(c, a, 16.5f, Ui.TEXT, Ui.MEDIUM);
        TextView tb = Ui.label(c, b, 13, Ui.TEXT2, Ui.REGULAR);
        tb.setPadding(0, Ui.dpi(4), 0, 0);
        t.addView(ta);
        t.addView(tb);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = Ui.dpi(14);
        lp.rightMargin = Ui.dpi(8);
        t.setLayoutParams(lp);
        return t;
    }

    private IconView badge(Context c, int kind) {
        IconView v = new IconView(c, kind, Ui.ACCENT);
        v.setCircle(Ui.withAlpha(Ui.ACCENT, 34));
        v.setFrac(0.5f);
        v.setClickable(false);
        v.setLayoutParams(new LinearLayout.LayoutParams(Ui.dpi(46), Ui.dpi(46)));
        return v;
    }

    private TextView pill(Context c, String text, boolean filled, boolean enabled) {
        TextView t = Ui.label(c, text, 16.5f, filled ? Ui.BG : Ui.ACCENT, Ui.MEDIUM);
        t.setGravity(Gravity.CENTER);
        t.setClickable(enabled);
        if (filled) {
            t.setBackground(Ui.pressable(enabled ? Ui.ACCENT : Ui.SURFACE2, Ui.blend(Ui.ACCENT, 0xFFFFFFFF, 0.2f), 0, Ui.dp(28)));
            if (!enabled) t.setTextColor(Ui.TEXT2);
        } else {
            t.setBackground(Ui.pressable(0x00000000, Ui.SURFACE, Ui.withAlpha(Ui.ACCENT, 140), Ui.dp(28)));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(56));
        lp.topMargin = Ui.dpi(14);
        t.setLayoutParams(lp);
        return t;
    }

    /** Check dorado cuando está activo, círculo vacío si no. */
    private static final class CheckView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean on;
        CheckView(Context c, boolean on) { super(c); this.on = on; }
        @Override protected void onDraw(Canvas cv) {
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) / 2f - Ui.dp(2);
            if (on) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(Ui.ACCENT);
                cv.drawCircle(w / 2, h / 2, r, p);
                Icons.draw(cv, p, Icons.CHECK, w / 2, h / 2, r * 1.5f, Ui.BG);
            } else {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Ui.dp(1.5f));
                p.setColor(0x55FFFFFF);
                cv.drawCircle(w / 2, h / 2, r, p);
            }
        }
    }

    void refresh(boolean allPhone, List<String> folders, boolean firstRun) {
        Context c = getContext();
        content.removeAllViews();
        close.setVisibility(firstRun ? GONE : VISIBLE);

        TextView brand = Ui.label(c, firstRun ? "Lumina" : "Tu música", firstRun ? 46 : 34, Ui.TEXT, Ui.SERIF);
        content.addView(brand);
        TextView tag = Ui.label(c, firstRun ? "Tu música, tal como la guardaste." : "Elige dónde está guardada",
                firstRun ? 17 : 15, Ui.ACCENT, Ui.MEDIUM);
        tag.setPadding(0, Ui.dpi(10), 0, 0);
        content.addView(tag);
        TextView info = Ui.label(c, "Solo leeré lo que me indiques: todo el teléfono o las carpetas que elijas. "
                + "Nada sale de tu dispositivo.", 14, Ui.TEXT2, Ui.REGULAR);
        info.setLineSpacing(Ui.dp(3), 1f);
        info.setPadding(0, Ui.dpi(14), 0, Ui.dpi(18));
        content.addView(info);

        TextView sec = Ui.label(c, "FUENTES", 12, Ui.TEXT2, Ui.MEDIUM);
        sec.setLetterSpacing(0.2f);
        content.addView(sec);

        // buscar en todo el teléfono
        LinearLayout a = card(c);
        a.addView(badge(c, Icons.PHONE));
        a.addView(texts(c, "Buscar en todo el teléfono", "Encuentra automáticamente toda tu música"));
        a.addView(new CheckView(c, allPhone), new LinearLayout.LayoutParams(Ui.dpi(28), Ui.dpi(28)));
        a.setOnClickListener(v -> cb.onToggleAllPhone());
        content.addView(a);

        // carpetas elegidas
        for (final String f : folders) {
            String[] lab = folderLabel(f);
            LinearLayout fc = card(c);
            fc.addView(badge(c, Icons.FOLDER));
            fc.addView(texts(c, lab[0], lab[1]));
            IconView del = new IconView(c, Icons.TRASH, Ui.TEXT2);
            del.setFrac(0.5f);
            del.setOnClickListener(v -> cb.onRemoveFolder(f));
            fc.addView(del, new LinearLayout.LayoutParams(Ui.dpi(44), Ui.dpi(44)));
            fc.setClickable(false);
            content.addView(fc);
        }

        TextView add = pill(c, "+  Elegir una carpeta", false, true);
        add.setOnClickListener(v -> cb.onAddFolder());
        content.addView(add);

        boolean ok = allPhone || !folders.isEmpty();
        TextView done = pill(c, firstRun ? "Empezar" : "Listo", true, ok);
        ((LinearLayout.LayoutParams) done.getLayoutParams()).topMargin = Ui.dpi(30);
        done.setOnClickListener(v -> cb.onDone());
        content.addView(done);
    }
}
