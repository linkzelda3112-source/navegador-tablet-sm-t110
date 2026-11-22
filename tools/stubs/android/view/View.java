package android.view;
import android.content.Context; import android.graphics.Canvas;
public class View {
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE=1, SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION=2, SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN=4, SYSTEM_UI_FLAG_HIDE_NAVIGATION=8, SYSTEM_UI_FLAG_FULLSCREEN=16, SYSTEM_UI_FLAG_IMMERSIVE_STICKY=32;
    public View(Context c) {}
    public Context getContext() { return null; }
    public void setKeepScreenOn(boolean b) {}
    public boolean post(Runnable r) { return true; }
    public void invalidate() {}
    public int getWidth() { return 0; } public int getHeight() { return 0; }
    protected void onDraw(Canvas c) {}
    public boolean onTouchEvent(MotionEvent e) { return true; }
    protected void onSizeChanged(int w, int h, int ow, int oh) {}
    public void setSystemUiVisibility(int v) {}
}
