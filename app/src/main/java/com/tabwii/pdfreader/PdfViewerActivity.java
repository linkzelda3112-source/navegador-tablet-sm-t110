package com.tabwii.pdfreader;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;

import java.io.File;

/**
 * Muestra un PDF con la librería android-pdf-viewer (basada en PDFium, el motor de Chrome), con
 * zoom, deslizar entre páginas, y memoria de "por dónde ibas": la última página leída de cada
 * archivo se guarda sola y se recupera la próxima vez que se abre ese mismo PDF.
 */
public class PdfViewerActivity extends Activity {

    public static final String EXTRA_PATH = "com.tabwii.pdfreader.PATH";
    public static final String EXTRA_TITLE = "com.tabwii.pdfreader.TITLE";
    private static final String PREFS_NAME = "pdf_progreso";

    private PDFView pdfView;
    private TextView titleBar;
    private TextView pageIndicator;
    private String prefsKey;

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        buildUi();
        openFromIntent();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.parseColor("#424242"));

        pdfView = new PDFView(this, null);
        root.addView(pdfView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#99000000"));
        topBar.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView backBtn = new TextView(this);
        backBtn.setText("⟵ Mis PDF");
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

        pageIndicator = new TextView(this);
        pageIndicator.setTextColor(Color.WHITE);
        pageIndicator.setTextSize(14);
        topBar.addView(pageIndicator);

        root.addView(topBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void openFromIntent() {
        String path = getIntent().getStringExtra(EXTRA_PATH);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        Uri data = getIntent().getData();

        if (path != null) {
            File file = new File(path);
            prefsKey = file.getAbsolutePath();
            titleBar.setText(title != null ? title : file.getName());
            loadPdf(pdfView.fromFile(file));
        } else if (data != null) {
            // La app se abrió desde "Abrir con..." de otra app (administrador de archivos, correo,
            // etc.), así que viene un content:// o file:// en vez de una ruta de nuestra lista.
            prefsKey = data.toString();
            titleBar.setText(guessTitleFromUri(data));
            loadPdf(pdfView.fromUri(data));
        } else {
            Toast.makeText(this, "No se encontró el PDF.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private String guessTitleFromUri(Uri uri) {
        String last = uri.getLastPathSegment();
        return last != null ? last : "PDF";
    }

    private void loadPdf(PDFView.Configurator configurator) {
        final int startPage = getSavedPage();
        configurator
                .defaultPage(startPage)
                .enableSwipe(true)
                .swipeHorizontal(false)
                .enableDoubletap(true)
                .spacing(dp(4))
                .onPageChange(new OnPageChangeListener() {
                    @Override
                    public void onPageChanged(int page, int pageCount) {
                        pageIndicator.setText((page + 1) + " / " + pageCount);
                        savePage(page);
                    }
                })
                .onLoad(new OnLoadCompleteListener() {
                    @Override
                    public void loadComplete(int nbPages) {
                        pageIndicator.setText((pdfView.getCurrentPage() + 1) + " / " + nbPages);
                    }
                })
                .onError(new OnErrorListener() {
                    @Override
                    public void onError(Throwable t) {
                        Toast.makeText(PdfViewerActivity.this,
                                "No se pudo abrir este PDF.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .load();
    }

    private SharedPreferences prefs() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private int getSavedPage() {
        if (prefsKey == null) return 0;
        return prefs().getInt(prefsKey, 0);
    }

    private void savePage(int page) {
        if (prefsKey == null) return;
        prefs().edit().putInt(prefsKey, page).apply();
    }
}
