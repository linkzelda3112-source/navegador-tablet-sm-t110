package com.lumina;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/** Hoja para elegir qué artistas o canciones no deben salir en el aleatorio. */
final class ShuffleSheet extends Dialog {
    private final List<Track> all;
    private boolean artistMode = true;
    private String query = "";
    private final List<Object> shown = new ArrayList<>(); // String (artista) o Track
    private List<String> artistNames;
    private BaseAdapter adapter;
    private TextView tabA, tabS, summary;

    ShuffleSheet(Context c, List<Track> all) {
        super(c);
        this.all = all;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Context c = getContext();
        TreeMap<String, String> names = new TreeMap<>();
        for (Track t : all) names.putIfAbsent(Personal.norm(t.artist), t.artist);
        artistNames = new ArrayList<>(names.values());
        Collections.sort(artistNames, String.CASE_INSENSITIVE_ORDER);

        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(Ui.SURFACE);
        float r = Ui.dp(28);
        g.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        root.setBackground(g);
        root.setPadding(Ui.dpi(22), Ui.dpi(12), Ui.dpi(22), Ui.dpi(16));
        setContentView(root);
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.BOTTOM);
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (int) (c.getResources().getDisplayMetrics().heightPixels * 0.82f));
            w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        View h = new View(c);
        h.setBackground(Ui.shape(Ui.withAlpha(Ui.TEXT, 50), 0, Ui.dp(3)));
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(Ui.dpi(40), Ui.dpi(5));
        hl.gravity = Gravity.CENTER_HORIZONTAL;
        hl.bottomMargin = Ui.dpi(14);
        root.addView(h, hl);
        root.addView(Ui.label(c, "Aleatorio", 24, Ui.TEXT, Ui.SERIF));
        summary = Ui.label(c, "", 13, Ui.TEXT2, Ui.REGULAR);
        summary.setPadding(0, Ui.dpi(4), 0, Ui.dpi(12));
        root.addView(summary);

        LinearLayout tabs = new LinearLayout(c);
        tabA = tab(c, "Artistas", true);
        tabS = tab(c, "Canciones", false);
        tabs.addView(tabA, new LinearLayout.LayoutParams(0, Ui.dpi(40), 1));
        tabs.addView(tabS, new LinearLayout.LayoutParams(0, Ui.dpi(40), 1));
        root.addView(tabs);

        EditText et = new EditText(c);
        et.setHint("Buscar…");
        et.setSingleLine(true);
        et.setTextColor(Ui.TEXT);
        et.setHintTextColor(Ui.TEXT2);
        et.setTextSize(15);
        et.setBackground(Ui.shape(Ui.SURFACE2, 0, Ui.dp(14)));
        et.setPadding(Ui.dpi(16), 0, Ui.dpi(16), 0);
        LinearLayout.LayoutParams el = new LinearLayout.LayoutParams(-1, Ui.dpi(46));
        el.topMargin = Ui.dpi(10);
        el.bottomMargin = Ui.dpi(6);
        root.addView(et, el);
        et.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c2) { }
            public void onTextChanged(CharSequence s, int a, int b2, int c2) { }
            public void afterTextChanged(Editable s) { query = s.toString().trim(); refill(); }
        });

        ListView lv = new ListView(c);
        lv.setDivider(null);
        lv.setSelector(new ColorDrawable(Color.TRANSPARENT));
        adapter = new BaseAdapter() {
            public int getCount() { return shown.size(); }
            public Object getItem(int i) { return shown.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View v, ViewGroup p) {
                TextView t = v instanceof TextView ? (TextView) v : new TextView(getContext());
                Object o = shown.get(i);
                boolean ex;
                String main, sub = null;
                if (o instanceof Track) {
                    Track tr = (Track) o;
                    ex = ShuffleRules.songExcluded(tr) || ShuffleRules.artistExcluded(tr.artist);
                    main = tr.title;
                    sub = tr.artist;
                } else {
                    ex = ShuffleRules.artistExcluded((String) o);
                    main = (String) o;
                }
                String mark = ex ? "⊘  " : "     ";
                t.setText(mark + main + (sub != null ? "\n     " + sub : ""));
                t.setTextSize(15);
                t.setSingleLine(false);
                t.setMaxLines(2);
                t.setEllipsize(android.text.TextUtils.TruncateAt.END);
                t.setTextColor(ex ? Ui.withAlpha(Ui.TEXT, 120) : Ui.TEXT);
                t.setPaintFlags(ex ? t.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                        : t.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                t.setGravity(Gravity.CENTER_VERTICAL);
                t.setPadding(Ui.dpi(4), Ui.dpi(10), Ui.dpi(4), Ui.dpi(10));
                t.setMinHeight(Ui.dpi(52));
                return t;
            }
        };
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((pa, v, i, id) -> {
            Object o = shown.get(i);
            if (o instanceof Track) ShuffleRules.toggleSong((Track) o);
            else ShuffleRules.toggleArtist((String) o);
            adapter.notifyDataSetChanged();
            updateSummary();
        });
        root.addView(lv, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView clear = Ui.label(c, "Quitar todas las exclusiones", 14, Ui.ACCENT, Ui.MEDIUM);
        clear.setGravity(Gravity.CENTER);
        clear.setPadding(0, Ui.dpi(12), 0, Ui.dpi(4));
        clear.setOnClickListener(v -> { ShuffleRules.clear(); adapter.notifyDataSetChanged(); updateSummary(); });
        root.addView(clear, new LinearLayout.LayoutParams(-1, -2));

        tabA.setOnClickListener(v -> setMode(true));
        tabS.setOnClickListener(v -> setMode(false));
        refill();
        updateSummary();
    }

    private TextView tab(Context c, String s, boolean on) {
        TextView t = Ui.label(c, s, 15, on ? Ui.ACCENT : Ui.TEXT2, Ui.MEDIUM);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private void setMode(boolean artists) {
        artistMode = artists;
        tabA.setTextColor(artists ? Ui.ACCENT : Ui.TEXT2);
        tabS.setTextColor(artists ? Ui.TEXT2 : Ui.ACCENT);
        refill();
    }

    private void refill() {
        shown.clear();
        String q = Personal.norm(query);
        if (artistMode) {
            for (String a : artistNames) if (q.isEmpty() || Personal.norm(a).contains(q)) shown.add(a);
        } else {
            List<Track> l = new ArrayList<>();
            for (Track t : all) {
                if (q.isEmpty() || Personal.norm(t.title).contains(q) || Personal.norm(t.artist).contains(q)) l.add(t);
            }
            Collections.sort(l, (a, b2) -> a.title.compareToIgnoreCase(b2.title));
            shown.addAll(l);
        }
        adapter.notifyDataSetChanged();
    }

    private void updateSummary() {
        int ok = ShuffleRules.countAllowed(all);
        summary.setText("Toca para excluir o incluir. Las excluidas nunca salen en el aleatorio. Entran "
                + ok + " de " + all.size() + " canciones.");
    }
}
