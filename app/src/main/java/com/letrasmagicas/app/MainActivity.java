package com.letrasmagicas.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Locale;

public class MainActivity extends Activity {

    private static final String START_URL = "file:///android_asset/index.html";
    private WebView web;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private volatile boolean ttsInitDone = false;
    private String pendingId = null, pendingText = null;
    private float pendingRate = 1f;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        web.setBackgroundColor(0xFFFFF8E7);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        web.setHorizontalScrollBarEnabled(false);
        web.setLongClickable(false);
        web.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setTextZoom(100);

        web.addJavascriptInterface(new TtsBridge(), "AndroidTTS");
        initTts();

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme())) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                }
                return true;
            }
        });

        setContentView(web);
        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(START_URL);
        }
        immersive();
    }

    // ---------- Voz (TextToSpeech del propio Android, en español) ----------

    private void initTts() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.SUCCESS) {
                    ttsInitDone = true;
                    flushPending();
                    return;
                }
                Locale[] opciones = new Locale[]{
                        new Locale("es", "MX"), new Locale("es", "US"),
                        new Locale("es", "ES"), new Locale("es")};
                for (Locale l : opciones) {
                    int r = tts.setLanguage(l);
                    if (r >= TextToSpeech.LANG_AVAILABLE) {
                        ttsReady = true;
                        break;
                    }
                }
                ttsInitDone = true;
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String id) {
                        emit(id, "start", 0, 0);
                    }

                    @Override
                    public void onDone(String id) {
                        emit(id, "done", 0, 0);
                    }

                    @Override
                    @SuppressWarnings("deprecation")
                    public void onError(String id) {
                        emit(id, "error", 0, 0);
                    }

                    @Override
                    public void onRangeStart(String id, int start, int end, int frame) {
                        emit(id, "range", start, end);
                    }
                });
                flushPending();
            }
        });
    }

    /** Si pidieron hablar antes de que el motor de voz estuviera listo, se dice ahora (o se avisa del error). */
    private void flushPending() {
        final String id = pendingId, text = pendingText;
        final float rate = pendingRate;
        pendingId = null;
        pendingText = null;
        if (id == null) {
            return;
        }
        if (!ttsReady || tts == null) {
            emit(id, "error", 0, 0);
            return;
        }
        tts.setSpeechRate(Math.max(0.3f, Math.min(rate, 2f)));
        tts.setPitch(1.05f);
        if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, new Bundle(), id) != TextToSpeech.SUCCESS) {
            emit(id, "error", 0, 0);
        }
    }

    private void emit(String id, String type, int a, int b) {
        long n;
        try {
            n = Long.parseLong(id);
        } catch (Exception e) {
            return;
        }
        final String js = "window.__tts&&window.__tts(" + n + ",'" + type + "'," + a + "," + b + ")";
        if (web != null) {
            web.post(new Runnable() {
                @Override
                public void run() {
                    web.evaluateJavascript(js, null);
                }
            });
        }
    }

    private class TtsBridge {
        @JavascriptInterface
        public boolean ready() {
            return ttsReady;
        }

        @JavascriptInterface
        public void speak(final String id, final String text, final float rate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!ttsInitDone) {
                        pendingId = id;
                        pendingText = text;
                        pendingRate = rate;
                        return;
                    }
                    if (!ttsReady || tts == null) {
                        emit(id, "error", 0, 0);
                        return;
                    }
                    tts.setSpeechRate(Math.max(0.3f, Math.min(rate, 2f)));
                    tts.setPitch(1.05f);
                    int r = tts.speak(text, TextToSpeech.QUEUE_FLUSH, new Bundle(), id);
                    if (r != TextToSpeech.SUCCESS) {
                        emit(id, "error", 0, 0);
                    }
                }
            });
        }

        @JavascriptInterface
        public void stop() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pendingId = null;
                    if (tts != null) {
                        tts.stop();
                    }
                }
            });
        }
    }

    @SuppressWarnings("deprecation")
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            immersive();
        }
    }

    /** El botón atrás lo maneja el juego (volver al menú); si no lo usa, se cierra la app. */
    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.__back ? window.__back() : false)", new android.webkit.ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!"true".equals(value)) {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        web.evaluateJavascript("if (window.__pause) window.__pause();", null);
        if (tts != null) {
            tts.stop();
        }
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        immersive();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }
}
