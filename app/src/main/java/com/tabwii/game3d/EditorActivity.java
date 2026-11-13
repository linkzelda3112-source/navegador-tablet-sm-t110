package com.tabwii.game3d;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Pantalla única de la app: un visor/editor 3D. Carga modelos desde los .zip que haya en la
 * carpeta de la app, los coloca en una escena, y deja moverlos/rotarlos/escalarlos con botones
 * sencillos — pensado para programar juegos encima más adelante, no para ser un editor
 * profesional.
 */
public class EditorActivity extends Activity {

    private static final int ACCENT = Color.parseColor("#4FC3F7");
    private static final int REQUEST_PICK_ZIP = 1001;
    private int modelsAdded = 0;

    private GLSurfaceView glView;
    private GameRenderer renderer;
    private LinearLayout chipsRow;
    private TextView selectedLabel;
    private TextView emptyStateText;
    private TextView modeButton;

    private boolean cameraMode = true;
    private SceneObject selectedObject;

    private float lastX, lastY, lastPinchDist;
    private boolean pinching;

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        renderer = new GameRenderer();
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glView.onPause();
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);

        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(renderer);
        glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        glView.setOnTouchListener(touchListener);
        root.addView(glView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // --- Barra superior: título, botón de agregar modelo, chips de objetos, modo. ---
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setBackgroundColor(Color.parseColor("#CC1B1F26"));
        headerRow.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView title = new TextView(this);
        title.setText("🧊 Mini Motor 3D");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(null, Typeface.BOLD);
        headerRow.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView addButton = pillButton("+ Modelo", ACCENT, Color.BLACK);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { pickZipFromDevice(); }
        });
        headerRow.addView(addButton);
        topBar.addView(headerRow);

        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(dp(10), dp(8), dp(10), dp(8));
        chipsScroll.addView(chipsRow, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        topBar.addView(chipsScroll);

        modeButton = pillButton("Modo: Cámara 🎥", Color.parseColor("#37474F"), Color.WHITE);
        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleMode(); }
        });
        LinearLayout modeRow = new LinearLayout(this);
        modeRow.setPadding(dp(10), 0, dp(10), dp(8));
        modeRow.addView(modeButton);
        topBar.addView(modeRow);

        root.addView(topBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        // --- Barra inferior: controles del objeto seleccionado. ---
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setBackgroundColor(Color.parseColor("#CC1B1F26"));
        bottomBar.setPadding(dp(10), dp(10), dp(10), dp(10));

        selectedLabel = new TextView(this);
        selectedLabel.setText("Ningún objeto seleccionado");
        selectedLabel.setTextColor(Color.WHITE);
        selectedLabel.setTextSize(13);
        bottomBar.addView(selectedLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        bottomBar.addView(smallButton("▲", new Runnable() {
            @Override public void run() { if (selectedObject != null) selectedObject.y += 0.2f; }
        }));
        bottomBar.addView(smallButton("▼", new Runnable() {
            @Override public void run() { if (selectedObject != null) selectedObject.y -= 0.2f; }
        }));
        bottomBar.addView(smallButton("⟲", new Runnable() {
            @Override public void run() { if (selectedObject != null) selectedObject.rotationY -= 15f; }
        }));
        bottomBar.addView(smallButton("⟳", new Runnable() {
            @Override public void run() { if (selectedObject != null) selectedObject.rotationY += 15f; }
        }));
        bottomBar.addView(smallButton("－", new Runnable() {
            @Override public void run() {
                if (selectedObject != null) selectedObject.scale = Math.max(0.1f, selectedObject.scale - 0.1f);
            }
        }));
        bottomBar.addView(smallButton("＋", new Runnable() {
            @Override public void run() { if (selectedObject != null) selectedObject.scale += 0.1f; }
        }));

        root.addView(bottomBar, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        // --- Mensaje de estado vacío (hasta que se agregue el primer modelo). ---
        emptyStateText = new TextView(this);
        emptyStateText.setTextColor(Color.WHITE);
        emptyStateText.setTextSize(15);
        emptyStateText.setGravity(Gravity.CENTER);
        emptyStateText.setPadding(dp(30), dp(30), dp(30), dp(30));
        emptyStateText.setText("Todavía no hay modelos.\n\n"
                + "Toca \"+ Modelo\" arriba y elige un archivo .zip con tu modelo 3D (un .obj "
                + "adentro, y su .mtl + imagen si tiene textura) desde donde lo tengas guardado "
                + "en el teléfono: Descargas, Drive, WhatsApp, etc.");
        root.addView(emptyStateText, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        setContentView(root);
    }

    private TextView pillButton(String text, int bgColor, int textColor) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(textColor);
        button.setTextSize(13);
        button.setTypeface(null, Typeface.BOLD);
        button.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(bgColor);
        button.setBackground(bg);
        return button;
    }

    private TextView smallButton(String text, final Runnable action) {
        TextView button = pillButton(text, Color.parseColor("#37474F"), Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(lp);
        button.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        return button;
    }

    private void toggleMode() {
        cameraMode = !cameraMode;
        modeButton.setText(cameraMode ? "Modo: Cámara 🎥" : "Modo: Objeto ✋");
    }

    /** Abre el selector de archivos del propio Android para elegir un .zip desde cualquier
     *  carpeta del teléfono (Descargas, Drive, WhatsApp...) — así no hace falta ningún permiso de
     *  almacenamiento ni navegar a una carpeta especial de la app. */
    private void pickZipFromDevice() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/zip", "application/x-zip-compressed", "application/octet-stream"});
        try {
            startActivityForResult(Intent.createChooser(intent, "Elige un .zip con tu modelo"), REQUEST_PICK_ZIP);
        } catch (Exception e) {
            Toast.makeText(this, "No se encontró ningún selector de archivos en este teléfono.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_ZIP) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            Toast.makeText(this, "No se eligió ningún archivo.", Toast.LENGTH_SHORT).show();
            return;
        }
        copyPickedZipAndLoad(data.getData());
    }

    /** El selector de archivos entrega un "content://" que no es un File normal; primero se
     *  copia a la carpeta privada de la app y de ahí sigue el mismo camino de siempre. */
    private void copyPickedZipAndLoad(final Uri uri) {
        Toast.makeText(this, "Cargando modelo...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File localCopy = new File(getCacheDir(), "modelo_elegido_" + System.currentTimeMillis() + ".zip");
                try {
                    InputStream in = getContentResolver().openInputStream(uri);
                    if (in == null) throw new Exception("no se pudo abrir el archivo elegido");
                    try {
                        FileOutputStream out = new FileOutputStream(localCopy);
                        try {
                            byte[] buffer = new byte[8192];
                            int read;
                            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                        } finally {
                            out.close();
                        }
                    } finally {
                        in.close();
                    }
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            Toast.makeText(EditorActivity.this, "No se pudo leer el archivo elegido: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }
                loadModelInBackground(localCopy);
            }
        }).start();
    }

    /** A partir de aquí ya se trabaja con un File normal; se llama desde un hilo que ya no es el
     *  principal (por eso no vuelve a mostrar el Toast de "Cargando..."). */
    private void loadModelInBackground(final File zipFile) {
        try {
            final RawMesh mesh = ModelLibrary.load(EditorActivity.this, zipFile);
            modelsAdded++;
            final String label = "Modelo #" + modelsAdded;
            renderer.requestAddModel(mesh, label, new GameRenderer.ModelLoadCallback() {
                @Override
                public void onLoaded(SceneObject object) {
                    addChip(object);
                    selectObject(object);
                    emptyStateText.setVisibility(View.GONE);
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(EditorActivity.this, "No se pudo mostrar el modelo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (final Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(EditorActivity.this, "Error leyendo el .zip: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void addChip(final SceneObject obj) {
        final TextView chip = pillButton(obj.label, Color.WHITE, Color.parseColor("#263238"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(lp);
        chip.setTag(obj);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { selectObject(obj); }
        });
        chipsRow.addView(chip);
    }

    private void selectObject(SceneObject obj) {
        selectedObject = obj;
        selectedLabel.setText("Seleccionado: " + obj.label
                + "  (modo Objeto para moverlo arrastrando la pantalla)");
        // Resalta el chip del objeto activo y apaga el resto.
        for (int i = 0; i < chipsRow.getChildCount(); i++) {
            View child = chipsRow.getChildAt(i);
            boolean active = child.getTag() == obj;
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(18));
            bg.setColor(active ? ACCENT : Color.WHITE);
            child.setBackground(bg);
        }
    }

    private final View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() >= 2) {
                        lastPinchDist = pinchDistance(event);
                        pinching = true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (pinching && event.getPointerCount() >= 2) {
                        float dist = pinchDistance(event);
                        float delta = dist - lastPinchDist;
                        float newDistance = renderer.cameraDistance - delta * 0.02f;
                        renderer.cameraDistance = clamp(newDistance, 2f, 40f);
                        lastPinchDist = dist;
                    } else if (event.getPointerCount() == 1) {
                        float dx = event.getX() - lastX;
                        float dy = event.getY() - lastY;
                        if (cameraMode) {
                            renderer.cameraYaw -= dx * 0.4f;
                            renderer.cameraPitch = clamp(renderer.cameraPitch + dy * 0.4f, -80f, 80f);
                        } else if (selectedObject != null) {
                            selectedObject.x += dx * 0.01f;
                            selectedObject.z += dy * 0.01f;
                        }
                        lastX = event.getX();
                        lastY = event.getY();
                    }
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    pinching = false;
                    break;
                case MotionEvent.ACTION_UP:
                    pinching = false;
                    break;
                default:
                    break;
            }
            return true;
        }
    };

    private float pinchDistance(MotionEvent event) {
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
