package com.tabwii.videoskids;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla principal: una cuadrícula grande y de colores con los videos que haya en la carpeta
 * VideosKids del tablet (ver {@link VideoLibrary}), con pestañas de categoría (una por subcarpeta)
 * y un buscador, al estilo de YouTube Kids. Pensada para que un niño pueda abrir la app y tocar
 * directamente el video que quiere ver, sin escribir nada si no quiere.
 */
public class VideoGridActivity extends Activity {

    private static final int ACCENT = Color.parseColor("#7C4DFF");

    private LinearLayout root;
    private LinearLayout categoryRow;
    private LinearLayout searchRow;
    private EditText searchInput;
    private GridView gridView;
    private TextView statusText;

    private String selectedCategory = VideoLibrary.DEFAULT_CATEGORY;
    private String searchQuery = "";
    private boolean searchVisible = false;

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        buildUi();
        loadLibrary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Por si el usuario copió más videos a la carpeta mientras la app no estaba abierta, o
        // volvió de haber visto uno.
        loadLibrary();
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F5F5F5"));

        // --- Encabezado alegre con el nombre de la app y el botón de buscar. ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(ACCENT);
        header.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView logo = new TextView(this);
        logo.setText("▶ Videos Kids");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(20);
        logo.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(logo, logoLp);

        TextView searchToggle = new TextView(this);
        searchToggle.setText("🔍");
        searchToggle.setTextSize(22);
        searchToggle.setPadding(dp(10), dp(4), dp(10), dp(4));
        searchToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearchVisible(!searchVisible);
            }
        });
        header.addView(searchToggle);
        root.addView(header);

        // --- Barra de búsqueda, oculta hasta que se toca la lupa. ---
        searchRow = new LinearLayout(this);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setBackgroundColor(Color.WHITE);
        searchRow.setPadding(dp(12), dp(8), dp(12), dp(8));
        searchRow.setVisibility(View.GONE);

        searchInput = new EditText(this);
        searchInput.setHint("Buscar un video...");
        searchInput.setTextSize(16);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        searchRow.addView(searchInput, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(searchRow);

        // --- Pestañas de categoría (una por subcarpeta de VideosKids), solo si hay más de una. ---
        HorizontalScrollView categoryScroll = new HorizontalScrollView(this);
        categoryScroll.setHorizontalScrollBarEnabled(false);
        categoryRow = new LinearLayout(this);
        categoryRow.setOrientation(LinearLayout.HORIZONTAL);
        categoryRow.setPadding(dp(10), dp(8), dp(10), dp(8));
        categoryScroll.addView(categoryRow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(categoryScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        statusText = new TextView(this);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTextColor(Color.parseColor("#666666"));
        statusText.setTextSize(16);
        statusText.setPadding(dp(24), dp(40), dp(24), dp(40));
        statusText.setVisibility(View.GONE);
        root.addView(statusText);

        gridView = new GridView(this);
        gridView.setNumColumns(3);
        gridView.setHorizontalSpacing(dp(10));
        gridView.setVerticalSpacing(dp(10));
        gridView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(gridView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void setSearchVisible(boolean visible) {
        searchVisible = visible;
        searchRow.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            searchInput.setText("");
            searchQuery = "";
            applyFilters();
        }
    }

    @SuppressWarnings("deprecation")
    private void loadLibrary() {
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Buscando videos...");
        gridView.setVisibility(View.GONE);
        categoryRow.removeAllViews();

        new AsyncTask<Void, Void, List<VideoItem>>() {
            @Override
            protected List<VideoItem> doInBackground(Void... voids) {
                if (!VideoLibrary.VIDEOS_FOLDER.exists()) {
                    VideoLibrary.VIDEOS_FOLDER.mkdirs();
                }
                return VideoScanner.scan(VideoLibrary.VIDEOS_FOLDER);
            }

            @Override
            protected void onPostExecute(List<VideoItem> found) {
                if (isFinishing()) return;
                VideoLibrary.setItems(found);
                selectedCategory = VideoLibrary.DEFAULT_CATEGORY;
                if (found.isEmpty()) {
                    showEmptyState();
                } else {
                    buildCategoryTabs();
                    applyFilters();
                }
            }
        }.execute();
    }

    private void showEmptyState() {
        gridView.setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Todavía no hay videos aquí.\n\n"
                + "Conecta el tablet a una computadora y copia tus videos a la carpeta:\n"
                + "\"" + VideoLibrary.VIDEOS_FOLDER.getPath() + "\"\n\n"
                + "Tip: si los organizas en subcarpetas (por ejemplo \"Caricaturas\", \"Música\"),\n"
                + "aparecen como pestañas distintas, como en YouTube Kids.\n\n"
                + "Formatos que funcionan mejor: MP4.\n"
                + "Luego vuelve a abrir esta app.");
    }

    /** Arma la fila de pestañas de categoría; no se muestra si solo hay "Todos". */
    private void buildCategoryTabs() {
        categoryRow.removeAllViews();
        List<String> cats = VideoLibrary.categories();
        if (cats.size() <= 1) return;
        for (final String cat : cats) {
            TextView chip = new TextView(this);
            chip.setText(cat);
            chip.setTextSize(14);
            chip.setTypeface(null, Typeface.BOLD);
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            styleChip(chip, cat.equals(selectedCategory));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedCategory = cat;
                    buildCategoryTabs();
                    applyFilters();
                }
            });
            categoryRow.addView(chip, lp);
        }
    }

    private void styleChip(TextView chip, boolean selected) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        if (selected) {
            bg.setColor(ACCENT);
            chip.setTextColor(Color.WHITE);
        } else {
            bg.setColor(Color.WHITE);
            bg.setStroke(dp(2), ACCENT);
            chip.setTextColor(ACCENT);
        }
        chip.setBackground(bg);
    }

    /** Recalcula qué videos mostrar según la categoría elegida y el texto buscado. */
    private void applyFilters() {
        List<VideoItem> items = VideoLibrary.getItems();
        List<Integer> order = new ArrayList<>();
        String query = searchQuery.trim().toLowerCase();
        for (int i = 0; i < items.size(); i++) {
            VideoItem item = items.get(i);
            boolean matchesCategory = selectedCategory.equals(VideoLibrary.DEFAULT_CATEGORY)
                    || selectedCategory.equals(item.category);
            boolean matchesQuery = query.isEmpty() || item.title.toLowerCase().contains(query);
            if (matchesCategory && matchesQuery) order.add(i);
        }

        if (order.isEmpty()) {
            gridView.setVisibility(View.GONE);
            statusText.setVisibility(View.VISIBLE);
            statusText.setText(query.isEmpty()
                    ? "No hay videos en esta categoría todavía."
                    : "No se encontró ningún video con ese nombre.");
            return;
        }

        statusText.setVisibility(View.GONE);
        gridView.setVisibility(View.VISIBLE);

        final VideoGridAdapter adapter = new VideoGridAdapter(this, items, order);
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int libraryIndex = adapter.libraryIndex(position);
                Intent intent = new Intent(VideoGridActivity.this, VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_INDEX, libraryIndex);
                startActivity(intent);
            }
        });
    }
}
