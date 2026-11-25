package com.lumina;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements Library.Listener, MusicService.Listener {
    private static final int REQ_MEDIA = 11, REQ_NOTIF = 12, REQ_TREE = 21, REQ_IMAGE = 22;

    private Library lib;
    private ArtCache art;
    private CoverStore covers;
    private Personal personal;
    private Charts charts;
    private HomeView home;
    private Track coverTarget;
    private MusicService svc;

    private FrameLayout root;
    private LinearLayout content, normalHeader, searchHeader, detailBar;
    private TextView titleTv, subTv, detailTitle, emptyTv, emptyBtn;
    private EditText searchEt;
    private TabsView tabs;
    private ListView list;
    private RowAdapter adapter;
    private ShuffleHeaderView shuffleHeader;
    private IconView workBtn;
    private boolean headerShown;
    private MiniPlayerView mini;
    private NowPlayingView now;
    private SourcesView sources;

    private int insetTop, insetBottom;
    private int tab;
    private Library.Group detail;
    private String query = "";
    private boolean searching, nowOpen, sourcesOpen, sourcesFirstRun, sourcesChanged, askedNotif;
    private String lastKey = "";
    private int lastColor;
    private List<Track> groupsFor;
    private List<Library.Group> albumsCache = new ArrayList<>(), artistsCache = new ArrayList<>();
    private final Map<String, Integer> colorCache = new HashMap<>();
    private final Handler tick = new Handler(Looper.getMainLooper());

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (svc != null && svc.current() != null) {
                int pos = svc.getPosition(), dur = svc.getDuration();
                mini.setProgress(dur > 0 ? pos / (float) dur : 0f);
                if (nowOpen) now.setPosition(pos, dur);
            }
            tick.postDelayed(this, 250);
        }
    };

    private final ServiceConnection conn = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n, IBinder b) {
            svc = ((MusicService.LocalBinder) b).get();
            svc.addListener(MainActivity.this);
            onPlayerChanged();
        }
        @Override public void onServiceDisconnected(ComponentName n) { svc = null; }
    };

    // ============================================================ ciclo de vida

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        App app = App.get(this);
        lib = app.library;
        art = app.art;
        covers = app.covers;
        personal = app.personal;
        charts = app.charts;

        Window w = getWindow();
        w.setBackgroundDrawable(new ColorDrawable(Ui.BG));
        w.setStatusBarColor(0);
        w.setNavigationBarColor(0);
        if (Build.VERSION.SDK_INT >= 30) w.setDecorFitsSystemWindows(false);
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (Ui.light) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        w.getDecorView().setSystemUiVisibility(flags);
        if (Build.VERSION.SDK_INT >= 29) w.setNavigationBarContrastEnforced(false);

        buildUi();
        setContentView(root);
        root.setOnApplyWindowInsetsListener((v, ins) -> {
            insetTop = ins.getSystemWindowInsetTop();
            insetBottom = ins.getSystemWindowInsetBottom();
            applyInsets();
            return ins;
        });

        lib.addListener(this);
        lib.start();
        refreshData();
        if (!lib.hasSources()) showSources(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, MusicService.class));
        bindService(new Intent(this, MusicService.class), conn, Context.BIND_AUTO_CREATE);
        tick.post(ticker);
        charts.refresh(false, this::chartsChanged);
    }

    @Override
    protected void onStop() {
        super.onStop();
        tick.removeCallbacks(ticker);
        if (svc != null) {
            svc.removeListener(this);
            svc = null;
        }
        try { unbindService(conn); } catch (Throwable ignored) { }
    }

    @Override
    protected void onDestroy() {
        lib.removeListener(this);
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (nowOpen) closeNow();
        else if (sourcesOpen && !sourcesFirstRun) finishSources();
        else if (searching) closeSearch();
        else if (detail != null) closeDetail();
        else super.onBackPressed();
    }

    // ================================================================== interfaz

    private LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);

        root.addView(new AmbientView(this), new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // cabecera normal
        FrameLayout headerFrame = new FrameLayout(this);
        normalHeader = new LinearLayout(this);
        normalHeader.setOrientation(LinearLayout.HORIZONTAL);
        normalHeader.setGravity(Gravity.CENTER_VERTICAL);
        normalHeader.setPadding(Ui.dpi(24), Ui.dpi(14), Ui.dpi(10), Ui.dpi(4));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleTv = Ui.label(this, "Tu música", 34, Ui.TEXT, Ui.SERIF);
        subTv = Ui.label(this, "", 13, Ui.TEXT2, Ui.REGULAR);
        subTv.setPadding(0, Ui.dpi(6), 0, 0);
        int hr = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        TextView greet = Ui.label(this, (hr < 6 ? "BUENAS NOCHES" : hr < 12 ? "BUENOS DÍAS" : hr < 19 ? "BUENAS TARDES" : "BUENAS NOCHES"),
                11, Ui.ACCENT, Ui.MEDIUM);
        greet.setLetterSpacing(0.25f);
        greet.setPadding(0, 0, 0, Ui.dpi(4));
        titleBox.addView(greet);
        titleBox.addView(titleTv);
        titleBox.setAlpha(0f);
        titleBox.setTranslationY(Ui.dp(14));
        titleBox.animate().alpha(1f).translationY(0f).setDuration(650).setStartDelay(120)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(2f)).start();
        titleBox.addView(subTv);
        normalHeader.addView(titleBox, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) titleBox.getLayoutParams()).weight = 1f;
        IconView searchBtn = new IconView(this, Icons.SEARCH, Ui.TEXT);
        searchBtn.setFrac(0.5f);
        searchBtn.setOnClickListener(v -> openSearch());
        IconView folderBtn = new IconView(this, Icons.FOLDER, Ui.TEXT);
        folderBtn.setFrac(0.5f);
        folderBtn.setOnClickListener(v -> showSources(false));
        workBtn = new IconView(this, Icons.LOCK, ShuffleRules.workOn() ? Ui.ACCENT : Ui.TEXT);
        workBtn.setFrac(0.5f);
        workBtn.setOnClickListener(v -> openWork());
        normalHeader.addView(workBtn, lp(Ui.dpi(48), Ui.dpi(48)));
        normalHeader.addView(searchBtn, lp(Ui.dpi(48), Ui.dpi(48)));
        IconView paletteBtn = new IconView(this, Icons.PALETTE, Ui.TEXT);
        paletteBtn.setFrac(0.5f);
        paletteBtn.setOnClickListener(v -> openSettings());
        normalHeader.addView(paletteBtn, lp(Ui.dpi(48), Ui.dpi(48)));
        normalHeader.addView(folderBtn, lp(Ui.dpi(48), Ui.dpi(48)));

        // cabecera de búsqueda
        searchHeader = new LinearLayout(this);
        searchHeader.setOrientation(LinearLayout.HORIZONTAL);
        searchHeader.setGravity(Gravity.CENTER_VERTICAL);
        searchHeader.setPadding(Ui.dpi(20), Ui.dpi(12), Ui.dpi(10), Ui.dpi(4));
        searchEt = new EditText(this);
        searchEt.setHint("Buscar canciones, artistas, álbumes");
        searchEt.setHintTextColor(Ui.TEXT2);
        searchEt.setTextColor(Ui.TEXT);
        searchEt.setTextSize(TypedValue.COMPLEX_UNIT_PX, Ui.dp(16));
        searchEt.setTypeface(Ui.REGULAR);
        searchEt.setSingleLine(true);
        searchEt.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchEt.setBackground(Ui.shape(Ui.SURFACE, Ui.LINE, Ui.dp(26)));
        searchEt.setPadding(Ui.dpi(20), 0, Ui.dpi(20), 0);
        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                query = s.toString();
                refreshData();
            }
        });
        IconView closeSearch = new IconView(this, Icons.CLOSE, Ui.TEXT);
        closeSearch.setFrac(0.5f);
        closeSearch.setOnClickListener(v -> closeSearch());
        searchHeader.addView(searchEt, lp(0, Ui.dpi(48)));
        ((LinearLayout.LayoutParams) searchEt.getLayoutParams()).weight = 1f;
        searchHeader.addView(closeSearch, lp(Ui.dpi(48), Ui.dpi(48)));
        searchHeader.setVisibility(View.GONE);

        headerFrame.addView(normalHeader);
        headerFrame.addView(searchHeader);
        content.addView(headerFrame, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // pestañas y barra de detalle
        tabs = new TabsView(this);
        tabs.setListener(i -> {
            tab = i;
            detail = null;
            adapter.animateNext();
            refreshData();
            list.setSelection(0);
            animateSwap();
        });
        content.addView(tabs, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        detailBar = new LinearLayout(this);
        detailBar.setOrientation(LinearLayout.HORIZONTAL);
        detailBar.setGravity(Gravity.CENTER_VERTICAL);
        detailBar.setPadding(Ui.dpi(10), 0, Ui.dpi(20), 0);
        IconView back = new IconView(this, Icons.BACK, Ui.TEXT);
        back.setFrac(0.5f);
        back.setOnClickListener(v -> closeDetail());
        detailTitle = Ui.label(this, "", 22, Ui.TEXT, Ui.SERIF);
        detailTitle.setSingleLine(true);
        detailTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        detailBar.addView(back, lp(Ui.dpi(46), Ui.dpi(46)));
        detailBar.addView(detailTitle, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) detailTitle.getLayoutParams()).weight = 1f;
        detailBar.setVisibility(View.GONE);
        content.addView(detailBar, lp(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dpi(46)));

        // lista
        FrameLayout listFrame = new FrameLayout(this);
        list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list.setSelector(new ColorDrawable(Ui.withAlpha(Ui.TEXT, 24)));
        list.setCacheColorHint(0);
        list.setClipToPadding(false);
        shuffleHeader = new ShuffleHeaderView(this);
        shuffleHeader.setOnClickListener(v -> shuffleAll());
        shuffleHeader.setOnSettings(() -> {
            ShuffleSheet sh = new ShuffleSheet(this, ShuffleRules.view(lib.tracks()));
            sh.setOnDismissListener(d -> shuffleHeader.setCount(ShuffleRules.countAllowed(adapter.trackList())));
            sh.show();
        });
        adapter = new RowAdapter(this, art, personal);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            int idx = position - list.getHeaderViewsCount();
            if (idx < 0) return;
            if (adapter.isGroupMode()) openDetail(adapter.group(idx));
            else playFrom(adapter.trackList(), idx);
        });
        listFrame.addView(list, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        home = new HomeView(this, art, new HomeView.Callbacks() {
            @Override public void onOpen(String title, List<Track> tracks) {
                Library.Group g = new Library.Group(title);
                g.tracks.addAll(tracks);
                openDetail(g);
            }
            @Override public void onPlay(List<Track> l, int index) { playFrom(l, index); }
            @Override public void onShuffle() {
                if (svc == null) return;
                List<Track> a = ShuffleRules.view(lib.tracks());
                if (ShuffleRules.countAllowed(a) == 0) return;
                if (!ensureNotifPermission()) maybeAskBattery();
                svc.playShuffled(a);
            }
            @Override public void onRefreshCharts() {
                charts.refresh(true, MainActivity.this::chartsChanged);
                refreshData();
            }
        });
        home.setVisibility(View.GONE);
        listFrame.addView(home, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout emptyBox = new LinearLayout(this);
        emptyBox.setOrientation(LinearLayout.VERTICAL);
        emptyBox.setGravity(Gravity.CENTER);
        emptyBox.setPadding(Ui.dpi(40), 0, Ui.dpi(40), Ui.dpi(80));
        emptyTv = Ui.label(this, "", 17, Ui.TEXT2, Ui.REGULAR);
        emptyTv.setGravity(Gravity.CENTER);
        emptyTv.setLineSpacing(Ui.dp(4), 1f);
        emptyBtn = Ui.label(this, "Elegir dónde buscar", 15.5f, Ui.BG, Ui.MEDIUM);
        emptyBtn.setGravity(Gravity.CENTER);
        emptyBtn.setPadding(Ui.dpi(28), 0, Ui.dpi(28), 0);
        emptyBtn.setBackground(Ui.pressable(Ui.ACCENT, Ui.blend(Ui.ACCENT, 0xFFFFFFFF, 0.2f), 0, Ui.dp(24)));
        emptyBtn.setOnClickListener(v -> showSources(false));
        emptyBox.addView(emptyTv, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams ebl = lp(ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dpi(48));
        ebl.topMargin = Ui.dpi(20);
        emptyBox.addView(emptyBtn, ebl);
        emptyBox.setVisibility(View.GONE);
        emptyBox.setTag("empty");
        listFrame.addView(emptyBox, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        content.addView(listFrame, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        ((LinearLayout.LayoutParams) listFrame.getLayoutParams()).weight = 1f;

        // mini reproductor
        mini = new MiniPlayerView(this, new MiniPlayerView.Callbacks() {
            @Override public void onOpen() { openNow(); }
            @Override public void onToggle() { if (svc != null) svc.toggle(); }
            @Override public void onNext() { if (svc != null) svc.next(); }
        });
        mini.setVisibility(View.GONE);
        FrameLayout.LayoutParams mlp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        mlp.leftMargin = Ui.dpi(12);
        mlp.rightMargin = Ui.dpi(12);
        root.addView(mini, mlp);

        // reproduciendo
        now = new NowPlayingView(this, new NowPlayingView.Callbacks() {
            @Override public void onClose() { closeNow(); }
            @Override public void onToggle() { if (svc != null) svc.toggle(); }
            @Override public void onNext() { if (svc != null) svc.next(); }
            @Override public void onPrev() { if (svc != null) svc.prev(); }
            @Override public void onShuffle() { if (svc != null) svc.setShuffle(!svc.isShuffle()); }
            @Override public void onRepeat() { if (svc != null) svc.cycleRepeat(); }
            @Override public void onSeek(int ms) { if (svc != null) svc.seekTo(ms); }
            @Override public void onCover() { openCoverSheet(); }
            @Override public void onLike() { rate(1); }
            @Override public void onLove() { rate(2); }
        });
        now.setVisibility(View.GONE);
        root.addView(now, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // fuentes
        sources = new SourcesView(this, new SourcesView.Callbacks() {
            @Override public void onToggleAllPhone() { toggleAllPhone(); }
            @Override public void onAddFolder() { pickFolder(); }
            @Override public void onRemoveFolder(String tree) { removeFolder(tree); }
            @Override public void onDone() { finishSources(); }
        });
        sources.setVisibility(View.GONE);
        root.addView(sources, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // el mini reproductor tiene elevación: las pantallas completas deben quedar por encima
        now.setElevation(Ui.dp(24));
        sources.setElevation(Ui.dp(32));
    }

    private void applyInsets() {
        content.setPadding(0, insetTop, 0, 0);
        FrameLayout.LayoutParams m = (FrameLayout.LayoutParams) mini.getLayoutParams();
        m.bottomMargin = insetBottom + Ui.dpi(12);
        mini.setLayoutParams(m);
        updateListPadding();
        now.onInsets(insetTop, insetBottom);
        home.onInsets(insetBottom);
        sources.onInsets(insetTop, insetBottom);
    }

    private void updateListPadding() {
        int extra = mini.getVisibility() == View.VISIBLE ? Ui.dpi(96) : Ui.dpi(24);
        list.setPadding(0, 0, 0, insetBottom + extra);
    }

    // ===================================================================== datos

    private static List<Track> filter(List<Track> src, String q) {
        if (q.isEmpty()) return src;
        List<Track> out = new ArrayList<>();
        for (Track t : src) {
            if (t.title.toLowerCase(Locale.ROOT).contains(q) || t.artist.toLowerCase(Locale.ROOT).contains(q)
                    || t.album.toLowerCase(Locale.ROOT).contains(q)) out.add(t);
        }
        return out;
    }

    @Override
    public void onLibraryChanged() {
        refreshData();
    }

    private void refreshData() {
        List<Track> all = ShuffleRules.view(lib.tracks());
        String q = query.trim().toLowerCase(Locale.ROOT);
        boolean showHome = tab == 0 && detail == null && !searching && !all.isEmpty();
        home.setVisibility(showHome ? View.VISIBLE : View.GONE);
        list.setVisibility(showHome ? View.GONE : View.VISIBLE);
        int count;
        if (showHome) {
            refreshHome(all);
            count = 1;
        } else if (detail != null) {
            List<Track> l = filter(detail.tracks, q);
            adapter.setTracks(l);
            showShuffle(true, l.size());
            count = l.size();
        } else if (tab <= 1) {
            List<Track> l = filter(all, q);
            adapter.setTracks(l);
            showShuffle(true, l.size());
            count = l.size();
        } else {
            if (groupsFor != all) {
                albumsCache = Library.albums(all);
                artistsCache = Library.artists(all);
                groupsFor = all;
            }
            List<Library.Group> src = tab == 2 ? albumsCache : artistsCache;
            List<Library.Group> out = new ArrayList<>();
            for (Library.Group g : src) {
                if (q.isEmpty() || g.name.toLowerCase(Locale.ROOT).contains(q) || g.sub.toLowerCase(Locale.ROOT).contains(q)) out.add(g);
            }
            adapter.setGroups(out, tab == 3);
            showShuffle(false, 0);
            count = out.size();
        }
        if (svc != null && svc.current() != null) adapter.setCurrent(svc.current().key, svc.isPlaying());

        subTv.setText(lib.isScanning() ? "Buscando música…  " + lib.found()
                : all.size() + (all.size() == 1 ? " canción" : " canciones"));

        View emptyBox = ((ViewGroup) list.getParent()).findViewWithTag("empty");
        if (count > 0) {
            emptyBox.setVisibility(View.GONE);
        } else {
            emptyBox.setVisibility(View.VISIBLE);
            if (lib.isScanning()) {
                emptyTv.setText("Buscando tu música…");
                emptyBtn.setVisibility(View.GONE);
            } else if (!lib.hasSources()) {
                emptyTv.setText("Aún no elegiste dónde está tu música.");
                emptyBtn.setVisibility(View.VISIBLE);
            } else if (all.isEmpty()) {
                emptyTv.setText("No encontré canciones en las fuentes elegidas.");
                emptyBtn.setVisibility(View.VISIBLE);
            } else {
                emptyTv.setText("Sin resultados" + (q.isEmpty() ? "." : " para “" + query.trim() + "”."));
                emptyBtn.setVisibility(View.GONE);
            }
        }
    }

    private void refreshHome(List<Track> all) {
        List<Track> liked = personal.rated(all, 1), loved = personal.rated(all, 2), recent = personal.recent(all);
        List<Charts.Hit> hits = charts.match(all);
        int state = charts.has() ? 2 : charts.isLoading() ? 1 : 0;
        home.update(all, loved, liked, recent, hits, state, charts.size(), charts.countryName());
    }

    private void chartsChanged() {
        if (home.getVisibility() == View.VISIBLE) refreshData();
    }

    private void rate(int which) {
        Track t = svc == null ? null : svc.current();
        if (t == null) return;
        int cur = personal.rating(t), next;
        if (which == 1) next = cur >= 1 ? 0 : 1;
        else next = cur == 2 ? 1 : 2;
        personal.setRating(t, next);
        now.setRating(next);
        adapter.refresh();
        Toast.makeText(this, next == 2 ? "Me encanta" : next == 1 ? "Me gusta" : "Quitada de tus favoritas",
                Toast.LENGTH_SHORT).show();
        if (home.getVisibility() == View.VISIBLE) refreshData();
    }

    private void showShuffle(boolean show, int n) {
        shuffleHeader.setCount(ShuffleRules.countAllowed(adapter.trackList()));
        if (show && !headerShown) {
            list.addHeaderView(shuffleHeader, null, false);
            headerShown = true;
        } else if (!show && headerShown) {
            list.removeHeaderView(shuffleHeader);
            headerShown = false;
        }
    }

    // ============================================================ navegación

    private void openDetail(Library.Group g) {
        detail = g;
        detailTitle.setText(g.name);
        tabs.setVisibility(View.GONE);
        detailBar.setVisibility(View.VISIBLE);
        adapter.animateNext();
        refreshData();
        list.setSelection(0);
        animateSwap();
    }

    private void animateSwap() {
        View v = home.getVisibility() == View.VISIBLE ? home : list;
        v.setAlpha(0f);
        v.setTranslationY(Ui.dp(18));
        v.animate().alpha(1f).translationY(0f).setDuration(380)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(2f)).start();
    }

    private void closeDetail() {
        detail = null;
        detailBar.setVisibility(View.GONE);
        tabs.setVisibility(View.VISIBLE);
        adapter.animateNext();
        refreshData();
        animateSwap();
    }

    private void openSearch() {
        searching = true;
        normalHeader.setVisibility(View.GONE);
        searchHeader.setVisibility(View.VISIBLE);
        refreshData();
        searchEt.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(searchEt, InputMethodManager.SHOW_IMPLICIT);
    }

    private void closeSearch() {
        searching = false;
        searchEt.setText("");
        query = "";
        searchHeader.setVisibility(View.GONE);
        normalHeader.setVisibility(View.VISIBLE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(searchEt.getWindowToken(), 0);
        refreshData();
    }

    private void openNow() {
        if (nowOpen || svc == null || svc.current() == null) return;
        nowOpen = true;
        now.setVisibility(View.VISIBLE);
        now.setTranslationY(Math.max(root.getHeight(), Ui.dpi(600)));
        now.animate().translationY(0).setDuration(340).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void closeNow() {
        if (!nowOpen) return;
        nowOpen = false;
        now.animate().translationY(Math.max(root.getHeight(), Ui.dpi(600))).setDuration(280)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> now.setVisibility(View.GONE)).start();
    }

    // ============================================================ reproducción

    /** Devuelve true si se mostró el diálogo de permiso de notificaciones. */
    private boolean ensureNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !askedNotif
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            askedNotif = true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
            return true;
        }
        return false;
    }

    private void playFrom(List<Track> l, int idx) {
        if (svc == null || l.isEmpty()) return;
        if (!ensureNotifPermission()) maybeAskBattery();
        svc.setQueue(l, idx);
    }

    private void shuffleAll() {
        if (svc == null || adapter.trackList().isEmpty()) return;
        if (ShuffleRules.countAllowed(adapter.trackList()) == 0) {
            Toast.makeText(this, "Todas están excluidas del aleatorio", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!ensureNotifPermission()) maybeAskBattery();
        svc.playShuffled(adapter.trackList());
    }

    private int colorFor(Track t, Bitmap bm) {
        Integer c = colorCache.get(t.key);
        if (c != null) return c;
        int col = Ui.dominant(bm, t.album + t.artist);
        if (bm != null) colorCache.put(t.key, col);
        return col;
    }

    @Override
    public void onPlayerChanged() {
        Track t = svc == null ? null : svc.current();
        if (t == null) {
            mini.setVisibility(View.GONE);
            updateListPadding();
            adapter.setCurrent(null, false);
            return;
        }
        boolean pl = svc.isPlaying();
        Runnable again = this::onPlayerChanged;
        Bitmap small = art.get(t, 160, again);
        Bitmap big = art.get(t, 720, again);
        int tint = colorFor(t, big != null ? big : small);
        mini.set(t, small != null ? small : big, pl, tint);
        if (mini.getVisibility() != View.VISIBLE) {
            mini.setVisibility(View.VISIBLE);
            mini.setAlpha(0f);
            mini.setTranslationY(Ui.dp(70));
            mini.animate().alpha(1f).translationY(0f).setDuration(520)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.1f)).start();
            updateListPadding();
        }
        if (!t.key.equals(lastKey)) {
            lastKey = t.key;
            personal.played(t);
            now.setTrack(t, big);
            now.setRating(personal.rating(t));
            if (home.getVisibility() == View.VISIBLE) refreshData();
        } else {
            now.setArt(big, t);
        }
        now.setDuration(svc.getDuration());
        now.setPlaying(pl);
        now.setShuffle(svc.isShuffle());
        now.setRepeat(svc.getRepeat());
        if (tint != lastColor) {
            lastColor = tint;
            now.setColor(tint);
        }
        adapter.setCurrent(t.key, pl);
    }

    // ================================================ ajustes, carátulas, batería

    private void openWork() {
        ShuffleSheet sh = new ShuffleSheet(this, lib.tracks(), true, () -> {
            workBtn.setColor(ShuffleRules.workOn() ? Ui.ACCENT : Ui.TEXT);
            Track cur = svc == null ? null : svc.current();
            if (ShuffleRules.workOn() && cur != null && !ShuffleRules.workAllowed(cur)) svc.pause();
            refreshData();
        });
        sh.show();
    }

    private void openSettings() {
        new AppearanceSheet(this, new AppearanceSheet.Callbacks() {
            @Override public void onTheme(int index) {
                Ui.saveTheme(MainActivity.this, index);
                recreate();
            }
            @Override public void onNowPlayingBg(int style) { now.setBgStyle(style); }
            @Override public void onBackground() { requestBackground(true); }
        }).show();
    }

    private void openCoverSheet() {
        final Track t = svc == null ? null : svc.current();
        if (t == null) return;
        new CoverSheet(this, t, covers, new CoverSheet.Callbacks() {
            @Override public void onPickLocal() {
                coverTarget = t;
                Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(i, REQ_IMAGE);
            }
            @Override public void onChanged() { coverChanged(); }
        }).show();
    }

    private void coverChanged() {
        art.bump();
        colorCache.clear();
        lastKey = "";
        lastColor = 0;
        adapter.refresh();
        if (svc != null) svc.refreshArt();
        onPlayerChanged();
        Toast.makeText(this, "Carátula actualizada", Toast.LENGTH_SHORT).show();
    }

    private boolean ignoringBattery() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm == null || pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    /** La primera vez que se reproduce algo, pide la excepción de batería para que la música no se corte. */
    private void maybeAskBattery() {
        SharedPreferences sp = getSharedPreferences("lumina_ui", MODE_PRIVATE);
        if (sp.getBoolean("askedBattery", false) || ignoringBattery()) return;
        sp.edit().putBoolean("askedBattery", true).apply();
        requestBackground(false);
    }

    private void requestBackground(boolean fromSettings) {
        if (ignoringBattery()) {
            if (fromSettings) {
                Toast.makeText(this, "Ya está permitido. Si aún se corta, en Honor/Huawei activa también "
                        + "“Ejecutar en segundo plano” en la batería de Lumina.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Throwable e) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Throwable e2) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            }
        }
    }

    // ================================================================= fuentes

    private void showSources(boolean firstRun) {
        sourcesFirstRun = firstRun;
        sourcesChanged = false;
        sources.refresh(lib.allPhone(), lib.folders(), firstRun);
        sources.setVisibility(View.VISIBLE);
        sources.setAlpha(0f);
        sources.animate().alpha(1f).setDuration(220).start();
        sourcesOpen = true;
    }

    private void refreshSources() {
        sources.refresh(lib.allPhone(), lib.folders(), sourcesFirstRun);
    }

    private void finishSources() {
        if (sourcesFirstRun && !lib.hasSources()) {
            Toast.makeText(this, "Elige al menos una fuente de música", Toast.LENGTH_SHORT).show();
            return;
        }
        if (sourcesChanged) {
            lib.rescan();
            refreshData();
        }
        sourcesOpen = false;
        sourcesFirstRun = false;
        sources.animate().alpha(0f).setDuration(180).withEndAction(() -> sources.setVisibility(View.GONE)).start();
    }

    private String mediaPermission() {
        return Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
    }

    private void toggleAllPhone() {
        if (lib.allPhone()) {
            lib.setAllPhone(false);
            sourcesChanged = true;
            refreshSources();
        } else if (checkSelfPermission(mediaPermission()) == PackageManager.PERMISSION_GRANTED) {
            enableAllPhone();
        } else {
            requestPermissions(new String[]{mediaPermission()}, REQ_MEDIA);
        }
    }

    private void enableAllPhone() {
        lib.setAllPhone(true);
        sourcesChanged = true;
        refreshSources();
    }

    private void pickFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                i.putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                        DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:Music"));
            } catch (Throwable ignored) { }
        }
        startActivityForResult(i, REQ_TREE);
    }

    private void removeFolder(String tree) {
        try {
            getContentResolver().releasePersistableUriPermission(Uri.parse(tree), Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Throwable ignored) { }
        lib.removeFolder(tree);
        sourcesChanged = true;
        refreshSources();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_IMAGE && res == RESULT_OK && data != null && data.getData() != null && coverTarget != null) {
            if (covers.setFromUri(coverTarget, data.getData())) coverChanged();
            else Toast.makeText(this, "No pude abrir esa imagen", Toast.LENGTH_SHORT).show();
            return;
        }
        if (req == REQ_TREE && res == RESULT_OK && data != null && data.getData() != null) {
            Uri u = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Throwable ignored) { }
            lib.addFolder(u.toString());
            sourcesChanged = true;
            refreshSources();
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == REQ_MEDIA) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                enableAllPhone();
            } else {
                Toast.makeText(this, "Sin permiso no puedo buscar en el teléfono. Puedes elegir una carpeta.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
