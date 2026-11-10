package com.tabwii.pdfreader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

/**
 * Pantalla principal: una lista de los PDF encontrados en el tablet (ver {@link PdfScanner}), para
 * tocar directo el que se quiere leer.
 */
public class PdfListActivity extends Activity {

    private static final int ACCENT = Color.parseColor("#1565C0");

    private LinearLayout root;
    private ListView listView;
    private TextView statusText;
    private List<PdfFileItem> items;

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
        // Por si el usuario copió más PDF a la carpeta mientras la app no estaba abierta.
        loadLibrary();
    }

    private void buildUi() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F5F5F5"));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(ACCENT);
        header.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView logo = new TextView(this);
        logo.setText("📄 Lector PDF");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(20);
        logo.setTypeface(null, Typeface.BOLD);
        header.addView(logo);
        root.addView(header);

        statusText = new TextView(this);
        statusText.setGravity(Gravity.CENTER);
        statusText.setTextColor(Color.parseColor("#666666"));
        statusText.setTextSize(16);
        statusText.setPadding(dp(24), dp(40), dp(24), dp(40));
        statusText.setVisibility(View.GONE);
        root.addView(statusText);

        listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(dp(8));
        listView.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    @SuppressWarnings("deprecation")
    private void loadLibrary() {
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Buscando PDF...");
        listView.setVisibility(View.GONE);

        new AsyncTask<Void, Void, List<PdfFileItem>>() {
            @Override
            protected List<PdfFileItem> doInBackground(Void... voids) {
                if (!PdfScanner.MIS_PDF_FOLDER.exists()) {
                    PdfScanner.MIS_PDF_FOLDER.mkdirs();
                }
                return PdfScanner.scan();
            }

            @Override
            protected void onPostExecute(List<PdfFileItem> found) {
                if (isFinishing()) return;
                items = found;
                if (found.isEmpty()) {
                    showEmptyState();
                } else {
                    showList(found);
                }
            }
        }.execute();
    }

    private void showEmptyState() {
        listView.setVisibility(View.GONE);
        statusText.setVisibility(View.VISIBLE);
        statusText.setText("Todavía no hay ningún PDF aquí.\n\n"
                + "Conecta el tablet a una computadora y copia tus archivos PDF a la carpeta:\n"
                + "\"" + PdfScanner.MIS_PDF_FOLDER.getPath() + "\"\n\n"
                + "También se detectan los PDF que ya estén en la carpeta de Descargas.\n\n"
                + "Luego vuelve a abrir esta app.");
    }

    private void showList(final List<PdfFileItem> found) {
        statusText.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        listView.setAdapter(new PdfAdapter(found));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PdfFileItem item = found.get(position);
                Intent intent = new Intent(PdfListActivity.this, PdfViewerActivity.class);
                intent.putExtra(PdfViewerActivity.EXTRA_PATH, item.file.getPath());
                intent.putExtra(PdfViewerActivity.EXTRA_TITLE, item.title);
                startActivity(intent);
            }
        });
    }

    /** Fila de la lista: insignia roja "PDF", nombre y tamaño del archivo. */
    private class PdfAdapter extends BaseAdapter {
        private final List<PdfFileItem> data;

        PdfAdapter(List<PdfFileItem> data) {
            this.data = data;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            PdfFileItem item = data.get(position);

            LinearLayout row = new LinearLayout(PdfListActivity.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(dp(12));
            row.setBackground(bg);
            row.setPadding(dp(12), dp(12), dp(12), dp(12));

            TextView badge = new TextView(PdfListActivity.this);
            badge.setText("PDF");
            badge.setTextColor(Color.WHITE);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setTextSize(13);
            badge.setGravity(Gravity.CENTER);
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(Color.parseColor("#E53935"));
            badgeBg.setCornerRadius(dp(8));
            badge.setBackground(badgeBg);
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(dp(52), dp(40));
            badgeLp.setMargins(0, 0, dp(14), 0);
            row.addView(badge, badgeLp);

            LinearLayout texts = new LinearLayout(PdfListActivity.this);
            texts.setOrientation(LinearLayout.VERTICAL);

            TextView title = new TextView(PdfListActivity.this);
            title.setText(item.title);
            title.setTextColor(Color.parseColor("#222222"));
            title.setTypeface(null, Typeface.BOLD);
            title.setTextSize(16);
            title.setMaxLines(2);
            texts.addView(title);

            TextView size = new TextView(PdfListActivity.this);
            size.setText(item.readableSize());
            size.setTextColor(Color.parseColor("#888888"));
            size.setTextSize(13);
            texts.addView(size);

            row.addView(texts, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            return row;
        }
    }
}
