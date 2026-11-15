package com.lumina;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reproduce la música en segundo plano, con notificación y controles de pantalla de bloqueo. */
public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    static final String CHANNEL = "lumina_playback";
    static final String A_TOGGLE = "com.lumina.TOGGLE", A_NEXT = "com.lumina.NEXT", A_PREV = "com.lumina.PREV";
    private static final int NOTIF_ID = 42;

    interface Listener { void onPlayerChanged(); }

    class LocalBinder extends Binder {
        MusicService get() { return MusicService.this; }
    }

    private final IBinder binder = new LocalBinder();
    private final List<Listener> listeners = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());

    private MediaPlayer mp;
    private boolean prepared, wantPlay, foreground;
    private List<Track> queue = new ArrayList<>();
    private int[] order = new int[0];
    private int pos = -1;
    private boolean shuffle;
    private int repeat; // 0 = no repetir, 1 = toda la lista, 2 = una canción
    private int errorStreak;

    private MediaSession session;
    private AudioManager am;
    private NotificationManager nm;
    private AudioFocusRequest focusReq;
    private boolean hasFocus, resumeOnFocus, noisyRegistered;
    private String artKey = "";
    private Bitmap notifArt;

    private final BroadcastReceiver noisy = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(i.getAction())) pause();
        }
    };

    // ------------------------------------------------------------- ciclo de vida

    @Override
    public void onCreate() {
        super.onCreate();
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "Reproducción", NotificationManager.IMPORTANCE_LOW);
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        }
        session = new MediaSession(this, "Lumina");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { play(); }
            @Override public void onPause() { pause(); }
            @Override public void onSkipToNext() { next(); }
            @Override public void onSkipToPrevious() { prev(); }
            @Override public void onSeekTo(long p) { seekTo((int) p); }
            @Override public void onStop() { pause(); }
        });
        session.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case A_TOGGLE: toggle(); break;
                case A_NEXT: next(); break;
                case A_PREV: prev(); break;
                default: break;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (!wantPlay) stopSelf();
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        abandonFocus();
        try { session.setActive(false); session.release(); } catch (Throwable ignored) { }
        super.onDestroy();
    }

    // ----------------------------------------------------------------- oyentes

    void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }

    void removeListener(Listener l) { listeners.remove(l); }

    private void changed() {
        for (Listener l : new ArrayList<>(listeners)) l.onPlayerChanged();
        ensureArt(current());
        updateSession();
        postNotification();
    }

    // -------------------------------------------------------------------- estado

    Track current() {
        if (pos < 0 || pos >= order.length || order[pos] >= queue.size()) return null;
        return queue.get(order[pos]);
    }

    boolean isPlaying() { return wantPlay; }

    boolean isShuffle() { return shuffle; }

    int getRepeat() { return repeat; }

    int queueSize() { return queue.size(); }

    int getPosition() {
        try { return prepared && mp != null ? mp.getCurrentPosition() : 0; } catch (Throwable t) { return 0; }
    }

    int getDuration() {
        try {
            if (prepared && mp != null) return mp.getDuration();
        } catch (Throwable ignored) { }
        Track t = current();
        return t == null ? 0 : (int) t.durationMs;
    }

    // --------------------------------------------------------------------- cola

    void setQueue(List<Track> list, int start) {
        if (list == null || list.isEmpty()) return;
        queue = new ArrayList<>(list);
        rebuildOrder(Math.max(0, Math.min(start, queue.size() - 1)));
        playPos(pos, true);
    }

    /** Activa el modo aleatorio y empieza por una canción al azar de la lista. */
    void playShuffled(List<Track> list) {
        if (list == null || list.isEmpty()) return;
        shuffle = true;
        setQueue(list, new java.util.Random().nextInt(list.size()));
    }

    private void rebuildOrder(int currentQueueIdx) {
        int n = queue.size();
        order = new int[n];
        if (!shuffle) {
            for (int i = 0; i < n; i++) order[i] = i;
            pos = currentQueueIdx;
        } else {
            List<Integer> rest = new ArrayList<>();
            for (int i = 0; i < n; i++) if (i != currentQueueIdx) rest.add(i);
            Collections.shuffle(rest);
            order[0] = currentQueueIdx;
            for (int i = 0; i < rest.size(); i++) order[i + 1] = rest.get(i);
            pos = 0;
        }
    }

    void setShuffle(boolean on) {
        if (shuffle == on) return;
        shuffle = on;
        if (!queue.isEmpty() && pos >= 0) rebuildOrder(order[pos]);
        changed();
    }

    void cycleRepeat() {
        repeat = (repeat + 1) % 3;
        changed();
    }

    // --------------------------------------------------------------- reproducción

    private void releasePlayer() {
        if (mp != null) {
            try { mp.reset(); } catch (Throwable ignored) { }
            try { mp.release(); } catch (Throwable ignored) { }
            mp = null;
        }
        prepared = false;
    }

    private void playPos(int p, boolean autoStart) {
        if (queue.isEmpty()) return;
        pos = Math.max(0, Math.min(p, order.length - 1));
        Track t = current();
        if (t == null) return;
        releasePlayer();
        wantPlay = autoStart;
        try {
            mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build());
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            mp.setOnPreparedListener(this);
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.setDataSource(this, t.uri);
            mp.prepareAsync();
        } catch (Throwable e) {
            onError(mp, 0, 0);
            return;
        }
        changed();
    }

    @Override
    public void onPrepared(MediaPlayer m) {
        prepared = true;
        if (wantPlay) {
            if (requestFocus()) {
                m.start();
                errorStreak = 0;
                registerNoisy();
            } else {
                wantPlay = false;
            }
        }
        changed();
    }

    @Override
    public void onCompletion(MediaPlayer m) {
        int n = order.length;
        if (n == 0) return;
        if (repeat == 2) {
            playPos(pos, true);
            return;
        }
        int p = pos + 1;
        if (p >= n) {
            if (repeat == 1) {
                if (shuffle) rebuildOrder(order[pos]);
                playPos(0, true);
            } else {
                playPos(0, false); // fin de la lista: se queda lista en la primera canción, en pausa
                unregisterNoisy();
            }
            return;
        }
        playPos(p, true);
    }

    @Override
    public boolean onError(MediaPlayer m, int what, int extra) {
        errorStreak++;
        if (queue.size() > 1 && errorStreak < queue.size()) {
            main.post(() -> playPos((pos + 1) % Math.max(1, order.length), true));
        } else {
            wantPlay = false;
            releasePlayer();
            changed();
        }
        return true;
    }

    void toggle() {
        if (wantPlay) pause(); else play();
    }

    void play() {
        if (queue.isEmpty()) return;
        if (mp == null) {
            playPos(pos < 0 ? 0 : pos, true);
            return;
        }
        wantPlay = true;
        if (prepared) {
            if (requestFocus()) {
                try { mp.start(); } catch (Throwable ignored) { }
                registerNoisy();
            } else {
                wantPlay = false;
            }
        }
        changed();
    }

    void pause() {
        wantPlay = false;
        resumeOnFocus = false;
        try { if (mp != null && prepared && mp.isPlaying()) mp.pause(); } catch (Throwable ignored) { }
        unregisterNoisy();
        abandonFocus();
        changed();
    }

    void next() {
        if (queue.isEmpty()) return;
        playPos((pos + 1) % order.length, true);
    }

    void prev() {
        if (queue.isEmpty()) return;
        if (getPosition() > 3000) {
            seekTo(0);
            return;
        }
        playPos(pos <= 0 ? order.length - 1 : pos - 1, true);
    }

    void seekTo(int ms) {
        try { if (mp != null && prepared) mp.seekTo(ms); } catch (Throwable ignored) { }
        updateSession();
    }

    // ------------------------------------------------------------ foco de audio

    private boolean requestFocus() {
        if (hasFocus) return true;
        int r;
        if (Build.VERSION.SDK_INT >= 26) {
            if (focusReq == null) {
                focusReq = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA).build())
                        .setOnAudioFocusChangeListener(this).build();
            }
            r = am.requestAudioFocus(focusReq);
        } else {
            r = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        hasFocus = r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        return hasFocus;
    }

    private void abandonFocus() {
        if (!hasFocus) return;
        hasFocus = false;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                if (focusReq != null) am.abandonAudioFocusRequest(focusReq);
            } else {
                am.abandonAudioFocus(this);
            }
        } catch (Throwable ignored) { }
    }

    @Override
    public void onAudioFocusChange(int change) {
        switch (change) {
            case AudioManager.AUDIOFOCUS_LOSS:
                hasFocus = false;
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (wantPlay) {
                    resumeOnFocus = true;
                    wantPlay = false;
                    try { if (mp != null && prepared) mp.pause(); } catch (Throwable ignored) { }
                    changed();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                try { if (mp != null) mp.setVolume(0.25f, 0.25f); } catch (Throwable ignored) { }
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                try { if (mp != null) mp.setVolume(1f, 1f); } catch (Throwable ignored) { }
                if (resumeOnFocus) {
                    resumeOnFocus = false;
                    wantPlay = true;
                    try { if (mp != null && prepared) mp.start(); } catch (Throwable ignored) { }
                    changed();
                }
                break;
            default:
                break;
        }
    }

    private void registerNoisy() {
        if (noisyRegistered) return;
        registerReceiver(noisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        noisyRegistered = true;
    }

    private void unregisterNoisy() {
        if (!noisyRegistered) return;
        try { unregisterReceiver(noisy); } catch (Throwable ignored) { }
        noisyRegistered = false;
    }

    // ------------------------------------------------- sesión multimedia y notificación

    /** Vuelve a cargar la portada (por ejemplo si el usuario cambió la carátula). */
    void refreshArt() {
        artKey = "";
        notifArt = null;
        changed();
    }

    private void ensureArt(Track t) {
        if (t == null) return;
        if (!t.key.equals(artKey)) {
            artKey = t.key;
            notifArt = null;
        }
        final ArtCache cache = App.get(this).art;
        Bitmap b = cache.get(t, 256, () -> {
            if (t.key.equals(artKey)) {
                notifArt = cache.get(t, 256, null);
                updateSession();
                postNotification();
            }
        });
        if (b != null) notifArt = b;
    }

    private void updateSession() {
        Track t = current();
        if (t == null || session == null) return;
        try {
            MediaMetadata.Builder mb = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, t.title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, t.artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, t.album)
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, getDuration());
            if (notifArt != null) mb.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, notifArt);
            session.setMetadata(mb.build());
            PlaybackState st = new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
                            | PlaybackState.ACTION_SKIP_TO_NEXT | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                            | PlaybackState.ACTION_SEEK_TO)
                    .setState(wantPlay ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                            getPosition(), wantPlay ? 1f : 0f)
                    .build();
            session.setPlaybackState(st);
        } catch (Throwable ignored) { }
    }

    private PendingIntent action(String a, int req) {
        Intent i = new Intent(this, MusicService.class).setAction(a);
        return PendingIntent.getService(this, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Notification buildNotification(Track t) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        b.setSmallIcon(R.drawable.ic_notif)
                .setContentTitle(t.title)
                .setContentText(t.artist)
                .setContentIntent(content)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setColor(Ui.ACCENT)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(wantPlay);
        if (notifArt != null) b.setLargeIcon(notifArt);
        b.addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_prev), "Anterior",
                action(A_PREV, 1)).build());
        b.addAction(new Notification.Action.Builder(
                Icon.createWithResource(this, wantPlay ? R.drawable.ic_pause : R.drawable.ic_play),
                wantPlay ? "Pausar" : "Reproducir", action(A_TOGGLE, 2)).build());
        b.addAction(new Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_next), "Siguiente",
                action(A_NEXT, 3)).build());
        b.setStyle(new Notification.MediaStyle()
                .setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1, 2));
        return b.build();
    }

    private void postNotification() {
        Track t = current();
        if (t == null) return;
        try {
            Notification n = buildNotification(t);
            if (wantPlay) {
                if (!foreground) {
                    try {
                        if (Build.VERSION.SDK_INT >= 29) {
                            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
                        } else {
                            startForeground(NOTIF_ID, n);
                        }
                        foreground = true;
                    } catch (Throwable e) {
                        nm.notify(NOTIF_ID, n);
                    }
                } else {
                    nm.notify(NOTIF_ID, n);
                }
            } else {
                if (foreground) {
                    stopForeground(false);
                    foreground = false;
                }
                nm.notify(NOTIF_ID, n);
            }
        } catch (Throwable ignored) { }
    }
}
