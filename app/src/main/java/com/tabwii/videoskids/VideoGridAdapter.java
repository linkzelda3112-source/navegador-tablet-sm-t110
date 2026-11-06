package com.tabwii.videoskids;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Muestra cada video como una tarjeta grande, redondeada y de color vivo, con su miniatura y su
 * nombre debajo — pensado para que un niño pueda reconocer y tocar el video que quiere sin ayuda.
 */
public class VideoGridAdapter extends BaseAdapter {

    // Colores vivos que se van repitiendo en las tarjetas, al estilo de una app para niños.
    private static final int[] CARD_COLORS = {
            Color.parseColor("#FF5252"), // rojo
            Color.parseColor("#448AFF"), // azul
            Color.parseColor("#FFC107"), // amarillo
            Color.parseColor("#66BB6A"), // verde
            Color.parseColor("#AB47BC"), // morado
            Color.parseColor("#FF7043"), // naranja
    };

    private final Context context;
    private final List<VideoItem> items;
    private final List<Integer> order; // índices dentro de "items", en el orden en que se muestran
    private final Map<String, Bitmap> thumbnailCache = new HashMap<>();

    public VideoGridAdapter(Context context, List<VideoItem> items, List<Integer> order) {
        this.context = context;
        this.items = items;
        this.order = order;
    }

    private int dp(int v) {
        return Math.round(v * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getCount() {
        return order.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(order.get(position));
    }

    /** Índice real del video dentro de la biblioteca completa (VideoLibrary.getItems()). */
    public int libraryIndex(int position) {
        return order.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VideoItem item = items.get(order.get(position));
        int color = CARD_COLORS[position % CARD_COLORS.length];

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(16));
        bg.setStroke(dp(4), color);
        card.setBackground(bg);
        card.setPadding(dp(6), dp(6), dp(6), dp(8));

        FrameLayout thumbFrame = new FrameLayout(context);
        GradientDrawable thumbBg = new GradientDrawable();
        thumbBg.setColor(color);
        thumbBg.setCornerRadius(dp(12));
        thumbFrame.setBackground(thumbBg);

        ImageView thumb = new ImageView(context);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap cached = thumbnailCache.get(item.file.getPath());
        if (cached != null) {
            thumb.setImageBitmap(cached);
        }
        thumbFrame.addView(thumb, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // Insignia redonda de "reproducir" encima de la miniatura, para que se vea claro que es un
        // video que se puede tocar.
        TextView playBadge = new TextView(context);
        playBadge.setText("▶");
        playBadge.setTextColor(Color.WHITE);
        playBadge.setTextSize(18);
        playBadge.setGravity(Gravity.CENTER);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        badgeBg.setColor(Color.parseColor("#66000000"));
        playBadge.setBackground(badgeBg);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(dp(34), dp(34));
        badgeLp.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        badgeLp.setMargins(0, 0, dp(6), dp(6));
        thumbFrame.addView(playBadge, badgeLp);

        card.addView(thumbFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        TextView title = new TextView(context);
        title.setText(item.title);
        title.setTextColor(Color.parseColor("#333333"));
        title.setTypeface(null, Typeface.BOLD);
        title.setTextSize(13);
        title.setMaxLines(2);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setSingleLine(false);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(0, dp(4), 0, 0);
        card.addView(title, titleLp);

        loadThumbnailAsync(item, thumb);

        return card;
    }

    /** Genera la miniatura del video en segundo plano para no trabar la pantalla mientras carga. */
    @SuppressWarnings("deprecation")
    private void loadThumbnailAsync(final VideoItem item, final ImageView target) {
        Bitmap cached = thumbnailCache.get(item.file.getPath());
        if (cached != null) return;
        target.setTag(item.file.getPath());
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                try {
                    return ThumbnailUtils.createVideoThumbnail(
                            item.file.getPath(), MediaStore.Video.Thumbnails.MINI_KIND);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    thumbnailCache.put(item.file.getPath(), bitmap);
                    if (item.file.getPath().equals(target.getTag())) {
                        target.setImageBitmap(bitmap);
                    }
                }
            }
        }.execute();
    }
}
