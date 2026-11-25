package com.lumina;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/** Adaptador de la lista: canciones, álbumes o artistas, todos con el mismo estilo de fila. */
final class RowAdapter extends BaseAdapter {
    private final Context ctx;
    private final ArtCache art;
    private final Personal personal;
    private final Handler main = new Handler(Looper.getMainLooper());
    private List<Track> tracks = new ArrayList<>();
    private List<Library.Group> groups = new ArrayList<>();
    private boolean groupMode, artistMode;
    private String currentKey = "";
    private boolean playing;
    private boolean refreshQueued;

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            refreshQueued = false;
            notifyDataSetChanged();
        }
    };
    private final Runnable artCb = new Runnable() {
        @Override public void run() {
            if (!refreshQueued) {
                refreshQueued = true;
                main.postDelayed(refresh, 90);
            }
        }
    };

    RowAdapter(Context ctx, ArtCache art, Personal personal) {
        this.ctx = ctx;
        this.art = art;
        this.personal = personal;
    }

    void setTracks(List<Track> list) {
        tracks = list;
        groupMode = false;
        notifyDataSetChanged();
    }

    void setGroups(List<Library.Group> list, boolean artists) {
        groups = list;
        groupMode = true;
        artistMode = artists;
        notifyDataSetChanged();
    }

    void setCurrent(String key, boolean playing) {
        this.currentKey = key == null ? "" : key;
        this.playing = playing;
        notifyDataSetChanged();
    }

    private boolean pendingAnim = true;

    /** La próxima vez que se dibuje la lista, las filas entran una tras otra. */
    void animateNext() {
        pendingAnim = true;
        main.removeCallbacks(stopAnim);
    }

    private final Runnable stopAnim = new Runnable() { @Override public void run() { pendingAnim = false; } };

    void refresh() { notifyDataSetChanged(); }

    boolean isGroupMode() { return groupMode; }

    List<Track> trackList() { return tracks; }

    Library.Group group(int i) { return groups.get(i); }

    @Override public int getCount() { return groupMode ? groups.size() : tracks.size(); }

    @Override public Object getItem(int i) { return groupMode ? groups.get(i) : tracks.get(i); }

    @Override public long getItemId(int i) { return i; }

    @Override
    public View getView(int i, View convert, ViewGroup parent) {
        RowView v = convert instanceof RowView ? (RowView) convert : new RowView(ctx);
        v.art = art;
        v.artCb = artCb;
        if (groupMode) {
            Library.Group g = groups.get(i);
            v.artTrack = g.cover();
            v.title = g.name;
            v.sub = g.sub;
            v.right = "";
            v.round = artistMode;
            v.current = false;
            v.playing = false;
            v.rating = 0;
        } else {
            Track t = tracks.get(i);
            v.artTrack = t;
            v.title = t.title;
            v.sub = t.artist + (t.album.equals(Library.UNKNOWN_ALBUM) ? "" : " · " + t.album);
            v.right = t.durationMs > 0 ? Ui.fmt(t.durationMs) : "";
            v.round = false;
            v.current = t.key.equals(currentKey);
            v.playing = v.current && playing;
            v.rating = personal.rating(t);
        }
        v.animate().cancel();
        if (pendingAnim && i < 12) {
            v.setAlpha(0f);
            v.setTranslationY(Ui.dp(26));
            v.animate().alpha(1f).translationY(0f).setStartDelay(i * 45L).setDuration(420)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(2f)).start();
            main.removeCallbacks(stopAnim);
            main.postDelayed(stopAnim, 700);
        } else {
            v.setAlpha(1f);
            v.setTranslationY(0f);
        }
        v.invalidate();
        return v;
    }
}
