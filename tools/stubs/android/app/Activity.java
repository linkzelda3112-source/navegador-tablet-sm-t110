package android.app;
import android.content.Context; import android.os.Bundle; import android.view.*;
public class Activity extends Context {
    protected void onCreate(Bundle b) {} protected void onResume() {} protected void onPause() {} protected void onDestroy() {}
    public void onWindowFocusChanged(boolean f) {} public void onBackPressed() {} public void finish() {}
    public Window getWindow() { return null; } public void setContentView(View v) {}
}
