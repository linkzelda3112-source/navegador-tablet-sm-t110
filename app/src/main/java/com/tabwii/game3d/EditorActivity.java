package com.tabwii.game3d;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import java.util.List;

/**
 * Pantalla única de la app: un visor/editor 3D. Carga modelos desde los .zip que haya en la
 * carpeta de la app, los coloca en una escena, y deja moverlos/rotarlos/escalarlos con botones
 * sencillos — pensado para programar juegos encima más adelante, no para ser un editor
 * profesional.
 */
public class EditorActivity extends Activity {

    private static final int ACCENT = Color.parseColor("#4FC3F7");

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
        refreshEmptyState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
        refreshEmptyState();
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
            @Override public void onClick(View v) { showAddModelDialog(); }
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

        // --- Mensaje de estado vacío (cuando no hay ningún .zip en la carpeta todavía). ---
        emptyStateText = new TextView(this);
        emptyStateText.setTextColor(Color.WHITE);
        emptyStateText.setTextSize(15);
        emptyStateText.setGravity(Gravity.CENTER);
        emptyStateText.setPadding(dp(30), dp(30), dp(30), dp(30));
        emptyStateText.setText("Todavía no hay modelos.\n\n"
                + "Copia un .zip con tu modelo (un archivo .obj adentro, y su .mtl + imagen si "
                + "tiene textura) a esta carpeta:\n\n" + ModelLibrary.folder(this).getPath()
                + "\n\nLuego toca \"+ Modelo\" arriba.");
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

    private void refreshEmptyState() {
        boolean hasModels = !ModelLibrary.listZips(this).isEmpty();
        emptyStateText.setVisibility(hasModels ? View.GONE : View.VISIBLE);
    }

    private void showAddModelDialog() {
        final List<File> zips = ModelLibrary.listZips(this);
        if (zips.isEmpty()) {
            Toast.makeText(this, "No hay ningún .zip en " + ModelLibrary.folder(this).getPath(), Toast.LENGTH_LONG).show();
            return;
        }
        CharSequence[] names = new CharSequence[zips.size()];
        for (int i = 0; i < zips.size(); i++) names[i] = zips.get(i).getName();

        new AlertDialog.Builder(this)
                .setTitle("Elige un modelo")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadModelInBackground(zips.get(which));
                    }
                })
                .show();
    }

    private void loadModelInBackground(final File zipFile) {
        Toast.makeText(this, "Cargando " + zipFile.getName() + "...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final RawMesh mesh = ModelLibrary.load(EditorActivity.this, zipFile);
                    final String label = mesh.sourceName + " #" + (renderer.getSceneObjectsSnapshot().size() + 1);
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
                            Toast.makeText(EditorActivity.this, "Error leyendo \"" + zipFile.getName() + "\": " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
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
