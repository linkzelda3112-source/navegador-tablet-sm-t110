package com.tabwii.browser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

/**
 * Navegador mínimo para tablets viejas (pensado para la Galaxy Tab 3 Lite SM-T110, Android 4.2.2):
 * barra de dirección, atrás/adelante/recargar, accesos directos y soporte de video en pantalla
 * completa (necesario para que los reproductores de video dentro de la página funcionen).
 *
 * Aviso honesto para quien lea este código: el motor que usa esta app es el WebView del propio
 * sistema operativo. En Android 4.2.2 ese WebView es muy viejo (no tiene WebRTC ni varias APIs de
 * JavaScript modernas), así que sitios que dependen de eso —como el cliente web de CloudMoon, o el
 * YouTube actual— probablemente no funcionen bien sin importar qué tan bien esté hecha esta app.
 * No hay forma de arreglar eso desde una sola app: haría falta un Android más nuevo en el tablet.
 */
public class BrowserActivity extends Activity {
    private static final String HOME_URL = "https://www.google.com/";
    // Se manda un identificador de navegador moderno para que las páginas no bloqueen la carga de
    // entrada con un aviso de "actualiza tu navegador"; no cambia lo que el motor realmente puede
    // ejecutar, pero evita que algunos sitios corten el paso antes siquiera de intentarlo.
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Tablet) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    private WebView webView;
    private EditText addressBar;
    private ProgressBar progressBar;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        buildUi();
        loadUrlSafe(HOME_URL);
    }

    private void buildUi() {
        FrameLayout rootFrame = new FrameLayout(this);
        rootFrame.setBackgroundColor(Color.WHITE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // --- Barra de arriba: atrás / adelante / recargar / casa + barra de dirección. ---
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(4), dp(4), dp(4), dp(4));
        toolbar.setBackgroundColor(Color.parseColor("#2E7D32"));

        Button backBtn = navButton("<", v -> { if (webView.canGoBack()) webView.goBack(); });
        Button fwdBtn = navButton(">", v -> { if (webView.canGoForward()) webView.goForward(); });
        Button reloadBtn = navButton("⟳", v -> webView.reload());
        Button homeBtn = navButton("⌂", v -> loadUrlSafe(HOME_URL));
        toolbar.addView(backBtn);
        toolbar.addView(fwdBtn);
        toolbar.addView(reloadBtn);
        toolbar.addView(homeBtn);

        addressBar = new EditText(this);
        addressBar.setSingleLine(true);
        addressBar.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        addressBar.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        addressBar.setHint("Escribe una dirección o búsqueda");
        addressBar.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_GO);
        addressBar.setOnEditorActionListener((v, actionId, event) -> {
            goToTypedUrl();
            return true;
        });
        LinearLayout.LayoutParams addrLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        addrLp.setMargins(dp(4), 0, dp(4), 0);
        toolbar.addView(addressBar, addrLp);

        Button goBtn = navButton("Ir", v -> goToTypedUrl());
        toolbar.addView(goBtn);
        root.addView(toolbar);

        // --- Accesos directos: a lo que el usuario quiere probar en este tablet. ---
        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        shortcuts.setPadding(dp(4), dp(2), dp(4), dp(2));
        shortcuts.setBackgroundColor(Color.parseColor("#E8F5E9"));
        shortcuts.addView(shortcutButton("Google", "https://www.google.com/"));
        shortcuts.addView(shortcutButton("YouTube", "https://m.youtube.com/"));
        // YouTube actual carga sus miniaturas y videos con JavaScript que este WebView no puede
        // ejecutar (se queda en los recuadros grises de "cargando" sin avanzar nunca). YouTube ya
        // no tiene un modo básico propio como Google, así que en vez de eso se deja un acceso a
        // Invidious: un front-end alternativo de código abierto para YouTube que arma páginas
        // simples en HTML, con más chance de funcionar aquí. No es oficial de YouTube y la
        // instancia pública puede estar caída a veces; si esta no responde, se puede escribir en
        // la barra de dirección el nombre de otra instancia de Invidious.
        shortcuts.addView(shortcutButton("YouTube (ligero)", "https://yewtu.be/"));
        shortcuts.addView(shortcutButton("CloudMoon", "https://web.cloudmoonapp.com/"));
        root.addView(shortcuts);

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        root.addView(progressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));

        webView = new WebView(this);
        configureWebView(webView);
        root.addView(webView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        rootFrame.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Contenedor donde se mete el reproductor de video cuando una página lo pone en pantalla
        // completa (lo necesitan YouTube y otros sitios con <video>); se mantiene vacío y oculto el
        // resto del tiempo.
        fullscreenContainer = new FrameLayout(this);
        fullscreenContainer.setBackgroundColor(Color.BLACK);
        fullscreenContainer.setVisibility(View.GONE);
        rootFrame.addView(fullscreenContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(rootFrame);
    }

    private Button navButton(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.parseColor("#1B5E20"));
        b.setOnClickListener(l);
        b.setMinimumWidth(0);
        b.setPadding(dp(10), dp(4), dp(10), dp(4));
        return b;
    }

    private Button shortcutButton(String label, String url) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTextColor(Color.parseColor("#1B5E20"));
        b.setBackgroundColor(Color.parseColor("#C8E6C9"));
        b.setPadding(dp(10), dp(2), dp(10), dp(2));
        b.setOnClickListener(v -> loadUrlSafe(url));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(lp);
        return b;
    }

    @SuppressWarnings("deprecation")
    private void configureWebView(WebView wv) {
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setUserAgentString(USER_AGENT);
        s.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Todo se queda dentro de esta misma app; no se abre un navegador aparte. Pasa por
                // loadUrlSafe para que también los enlaces/redirecciones dentro de Google usen el
                // modo básico (ver comentario en withGoogleBasicMode).
                loadUrlSafe(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                addressBar.setText(url);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // El tablet es tan viejo que su fecha/hora y sus certificados raíz suelen estar
                // desactualizados, lo que rompe HTTPS con sitios actuales aunque el certificado del
                // sitio esté bien. Se deja pasar para no bloquear la navegación por eso.
                handler.proceed();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(BrowserActivity.this, "No se pudo cargar: " + description, Toast.LENGTH_LONG).show();
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            // --- Video en pantalla completa: hace falta este par de métodos para que un <video>
            // de la página (YouTube, etc.) pueda ponerse en pantalla completa; sin esto, muchos
            // reproductores simplemente no hacen nada al tocar el botón de pantalla completa. ---
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }
                customView = view;
                customViewCallback = callback;
                fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                fullscreenContainer.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            @Override
            public void onHideCustomView() {
                exitFullscreenVideo();
            }
        });
    }

    /** Saca el video de pantalla completa (si hay uno) y vuelve a mostrar la página normal. */
    private void exitFullscreenVideo() {
        if (customView == null) return;
        fullscreenContainer.setVisibility(View.GONE);
        fullscreenContainer.removeView(customView);
        webView.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (customViewCallback != null) customViewCallback.onCustomViewHidden();
        customView = null;
        customViewCallback = null;
    }

    private void goToTypedUrl() {
        String text = addressBar.getText().toString().trim();
        if (text.isEmpty()) return;
        String url;
        if (text.contains(" ") || (!text.contains(".") && !text.startsWith("http"))) {
            // No parece una dirección: se busca en vez de intentar cargarla tal cual.
            url = "https://www.google.com/search?q=" + Uri.encode(text);
        } else if (!text.startsWith("http://") && !text.startsWith("https://")) {
            url = "https://" + text;
        } else {
            url = text;
        }
        loadUrlSafe(url);
        hideKeyboard();
    }

    private void loadUrlSafe(String url) {
        webView.loadUrl(withGoogleBasicMode(url));
    }

    /** Google manda su página normal (pesada en JavaScript) a casi cualquier navegador, y en este
     *  WebView tan viejo eso termina en una página de error en vez de resultados ("Si tienes
     *  problemas para acceder a la Búsqueda de Google…"). Google tiene desde hace años un modo
     *  "básico" en HTML puro pensado justo para navegadores viejos o con poca batería/datos —
     *  se activa con el parámetro gbv=1 — y si hay resultados reales, mucho más ligeros. Esto se
     *  aplica a toda URL de google.com que no lo tenga ya, para que también funcione al tocar un
     *  resultado de búsqueda o seguir un enlace dentro de Google. */
    private String withGoogleBasicMode(String url) {
        try {
            Uri u = Uri.parse(url);
            String host = u.getHost();
            boolean isGoogle = host != null && (host.equals("google.com") || host.endsWith(".google.com"));
            if (isGoogle && u.getQueryParameter("gbv") == null) {
                String sep = url.contains("?") ? "&" : "?";
                return url + sep + "gbv=1";
            }
        } catch (Exception ignored) {
            // Si algo de esto falla, se carga la URL tal cual en vez de romper la navegación.
        }
        return url;
    }

    private void hideKeyboard() {
        Object service = getSystemService(INPUT_METHOD_SERVICE);
        if (service instanceof android.view.inputmethod.InputMethodManager) {
            ((android.view.inputmethod.InputMethodManager) service)
                    .hideSoftInputFromWindow(addressBar.getWindowToken(), 0);
        }
        addressBar.clearFocus();
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            // Primero sale del video en pantalla completa, si hay uno.
            exitFullscreenVideo();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
