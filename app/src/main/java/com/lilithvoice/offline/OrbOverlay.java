package com.lilithvoice.offline;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.graphics.PixelFormat;
import android.provider.Settings;

/** Floating, touch-through animated orb. Requires explicit SYSTEM_ALERT_WINDOW user grant. */
public final class OrbOverlay {
  private final Context context;
  private final WindowManager windowManager;
  private OrbView view;
  private boolean showing;

  public OrbOverlay(Context context) {
    this.context = context;
    this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
  }
  public void show(String label) {
    if (!Settings.canDrawOverlays(context)) return;
    try {
      if (view == null) {
        view = new OrbView(context);
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            dp(190), dp(216), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = dp(90);
        windowManager.addView(view, params);
        showing = true;
      }
      view.setLabel(label);
    } catch (Exception ignored) { hide(); }
  }
  public void setLabel(String label) { if (view != null) view.setLabel(label); }
  public void hide() {
    if (view != null) {
      view.setActive(false);
      try { if (showing) windowManager.removeView(view); } catch (Exception ignored) {}
      view = null;
    }
    showing = false;
  }
  private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

  private static final class OrbView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String label = "Escuchando";
    private boolean active = true;
    private final long started = SystemClock.uptimeMillis();
    OrbView(Context context) { super(context); setLayerType(View.LAYER_TYPE_SOFTWARE, null); }
    void setLabel(String next) { label = next; invalidate(); }
    void setActive(boolean active) { this.active = active; }
    @Override protected void onDraw(Canvas c) {
      super.onDraw(c);
      float scale = getResources().getDisplayMetrics().density;
      float cx = getWidth() / 2f, cy = 93f * scale;
      float t = (SystemClock.uptimeMillis() - started) / 1000f;
      float pulse = (float) Math.sin(t * 3.4f);
      float radius = (49f + pulse * 3f) * scale;
      p.setStyle(Paint.Style.FILL);
      p.setShader(new RadialGradient(cx, cy, 82f * scale,
          new int[]{0x66E83262, 0x223D0920, 0x001B0714}, null, Shader.TileMode.CLAMP));
      c.drawCircle(cx, cy, 82f * scale, p); p.setShader(null);
      for (int i = 0; i < 2; i++) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth((2f - .5f * i) * scale);
        p.setColor(i == 0 ? 0xBBFF6C91 : 0x66FF416F);
        c.drawCircle(cx, cy, radius + (9 + i * 10) * scale, p);
      }
      p.setStyle(Paint.Style.FILL);
      p.setShader(new RadialGradient(cx - 18 * scale, cy - 23 * scale, 94 * scale,
          new int[]{0xFFFFA9B9, 0xFFE02C57, 0xFF660F2D, 0xFF260715},
          new float[]{0f, .29f, .71f, 1f}, Shader.TileMode.CLAMP));
      c.drawCircle(cx, cy, radius, p); p.setShader(null);
      p.setColor(Color.WHITE); p.setAlpha(75);
      c.drawCircle(cx - 17 * scale, cy - 19 * scale, 9 * scale, p);
      p.setStyle(Paint.Style.FILL);
      p.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
      p.setTextAlign(Paint.Align.CENTER);
      p.setTextSize(15f * scale);
      p.setColor(Color.WHITE);
      c.drawText("LILITH", cx, cy + 86f * scale, p);
      p.setTextSize(12f * scale);
      p.setColor(0xFFFFB2C7);
      c.drawText(label, cx, cy + 105f * scale, p);
      if (active) postInvalidateDelayed(33);
    }
  }
}
