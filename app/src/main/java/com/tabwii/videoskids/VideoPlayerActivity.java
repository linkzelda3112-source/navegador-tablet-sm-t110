package com.tabwii.videoskids;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.List;

/**
 * Reproduce un video de la biblioteca local y, al terminar, muestra una pantalla de "siguiente
 * video" con otros videos recomendados (al estilo de YouTube Kids): si no se toca nada, después de
 * unos segundos empieza solo el siguiente; si se toca otro video de la lista, pasa a ese en vez.
 */
public class VideoPlayerActivity extends Activity {

    public static final String EXTRA_INDEX = "com.tabwii.videoskids.INDEX";
    private static final int AUTOPLAY_SECONDS = 6;

    private FrameLayout root;
    private VideoView videoView;
    private TextView titleBar;
    private FrameLayout upNextOverlay;
    private TextView countdownText;

    private int currentIndex = -1;
    private final Handler handler = new Handler(android.os.Looper.getMainLooper());
    private Runnable countdownRunnable;
    private int secondsLeft;

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        buildUi();
        int index = getIntent().getIntExtra(EXTRA_INDEX, -1);
        playIndex(index);
    }

    private void buildUi() {
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        videoView = new VideoView(this);
        root.addView(videoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Barra con el título y un botón grande para volver a la cuadrícula, fácil de tocar.
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#99000000"));
        topBar.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView backBtn = new TextView(this);
        backBtn.setText("⟵ Mis videos");
        backBtn.setTextColor(Color.WHITE);
        backBtn.setTextSize(16);
        backBtn.setTypeface(null, Typeface.BOLD);
        backBtn.setPadding(dp(8), dp(6), dp(16), dp(6));
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        topBar.addView(backBtn);

        titleBar = new TextView(this);
        titleBar.setTextColor(Color.WHITE);
        titleBar.setTextSize(16);
        titleBar.setSingleLine(true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        topBar.addView(titleBar, titleLp);

        root.addView(topBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        buildUpNextOverlay();

        setContentView(root);
    }

    private void buildUpNextOverlay() {
        upNextOverlay = new FrameLayout(this);
        upNextOverlay.setBackgroundColor(Color.parseColor("#E6000000"));
        upNextOverlay.setVisibility(View.GONE);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        countdownText = new TextView(this);
        countdownText.setTextColor(Color.WHITE);
        countdownText.setTextSize(18);
        countdownText.setTypeface(null, Typeface.BOLD);
        countdownText.setGravity(Gravity.CENTER);
        content.addView(countdownText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        GridView recGrid = new GridView(this);
        recGrid.setNumColumns(3);
        recGrid.setHorizontalSpacing(dp(10));
        recGrid.setVerticalSpacing(dp(10));
        recGrid.setTag("recGrid");
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        gridLp.setMargins(0, dp(12), 0, 0);
        content.addView(recGrid, gridLp);

        upNextOverlay.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(upNextOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @SuppressWarnings("deprecation")
    private void playIndex(int index) {
        List<VideoItem> items = VideoLibrary.getItems();
        if (index < 0 || index >= items.size()) {
            Toast.makeText(this, "No se encontró ese video.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        cancelCountdown();
        currentIndex = index;
        VideoItem item = items.get(index);
        titleBar.setText(item.title);
        upNextOverlay.setVisibility(View.GONE);

        videoView.setVideoPath(item.file.getPath());
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(VideoPlayerActivity.this,
                        "No se pudo reproducir este video.", Toast.LENGTH_LONG).show();
                return true;
            }
        });
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                showUpNext();
            }
        });
        videoView.start();
    }

    /** Pantalla de "siguiente video", con recomendaciones y cuenta regresiva para pasar solo. */
    private void showUpNext() {
        final List<VideoItem> items = VideoLibrary.getItems();
        List<Integer> recommended = VideoLibrary.shuffledOtherIndexes(currentIndex);
        if (recommended.isEmpty()) {
            // No hay más videos en la biblioteca: no hay nada que recomendar, se vuelve a la lista.
            Toast.makeText(this, "Ese era el único video disponible.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Como máximo 6 recomendaciones en pantalla, para que las tarjetas se vean grandes.
        List<Integer> shown = recommended.size() > 6 ? recommended.subList(0, 6) : recommended;
        final int nextIndex = shown.get(0);

        GridView recGrid = (GridView) upNextOverlay.findViewWithTag("recGrid");
        final VideoGridAdapter adapter = new VideoGridAdapter(this, items, new ArrayList<>(shown));
        recGrid.setAdapter(adapter);
        recGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playIndex(adapter.libraryIndex(position));
            }
        });

        upNextOverlay.setVisibility(View.VISIBLE);
        startCountdown(nextIndex);
    }

    private void startCountdown(final int nextIndex) {
        secondsLeft = AUTOPLAY_SECONDS;
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    playIndex(nextIndex);
                    return;
                }
                countdownText.setText("Siguiente video en " + secondsLeft + "...\n"
                        + "Toca otro video para verlo en su lugar.");
                secondsLeft--;
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(countdownRunnable);
    }

    private void cancelCountdown() {
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView.isPlaying()) {
            videoView.stopPlayback();
        }
    }

    @Override
    public void onBackPressed() {
        cancelCountdown();
        finish();
    }
}
