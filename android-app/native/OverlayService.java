package com.shomratmortaza.orbi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Foreground service that draws the ORBI island above every app. */
public class OverlayService extends Service {
    static boolean running = false;
    private static final String URL = "https://orbi-exo.vercel.app/?overlay=1";
    private final Handler ui = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private WebView web;
    private WindowManager.LayoutParams lp;
    private SharedPreferences prefs;
    private boolean docked = false, lastOpen = false, dragging = false, zone = false;
    private String side = "l";
    private int lastW, lastH, x0, y0;
    private float dx0, dy0;

    private float dn() { return getResources().getDisplayMetrics().density; }
    private int sw() { return getResources().getDisplayMetrics().widthPixels; }
    private int sh() { return getResources().getDisplayMetrics().heightPixels; }
    private int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(v, Math.max(lo, hi))); }

    private void js(boolean on) {
        if (web == null) return;
        String code = on ? "r.classList.add('docked');r.dataset.side='" + side + "'"
                         : "r.classList.remove('docked');r.dataset.side=''";
        web.evaluateJavascript("(function(){var r=document.documentElement;" + code + "})()", null);
    }

    private void applyDock() {
        lp.width = (int) (26 * dn());
        lp.height = (int) (64 * dn());
        lp.x = side.equals("l") ? 0 : sw() - lp.width;
        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    }

    private void update() { try { wm.updateViewLayout(web, lp); } catch (Exception ignored) { } }

    private void dockNow(String sd) {
        side = sd; docked = true; applyDock(); js(true); update();
        prefs.edit().putString("dock", sd).putInt("y", lp.y).apply();
    }

    private void undockNow() {
        docked = false; js(false);
        lp.width = lastW; lp.height = lastH;
        lp.x = clamp(side.equals("l") ? (int) (10 * dn()) : sw() - lastW - (int) (10 * dn()), 0, sw() - lastW);
        update();
        prefs.edit().remove("dock").putInt("x", lp.x).putInt("y", lp.y).apply();
    }

    private void finishDrag() {
        if (!lastOpen && lp.x < 24 * dn()) dockNow("l");
        else if (!lastOpen && lp.x + lp.width > sw() - 24 * dn()) dockNow("r");
        else prefs.edit().remove("dock").putInt("x", lp.x).putInt("y", lp.y).apply();
    }

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        if (i != null && "STOP".equals(i.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (!running) { startForegroundNotice(); addIsland(); running = true; }
        return START_STICKY;
    }

    private void startForegroundNotice() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26)
            nm.createNotificationChannel(new NotificationChannel("orbi_overlay", "ORBI island", NotificationManager.IMPORTANCE_MIN));
        Intent stop = new Intent(this, OverlayService.class).setAction("STOP");
        PendingIntent pi = PendingIntent.getService(this, 1, stop, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "orbi_overlay") : new Notification.Builder(this);
        b.setContentTitle("ORBI island is on").setContentText("Tap Stop to hide it")
                .setSmallIcon(android.R.drawable.ic_dialog_info).addAction(0, "Stop", pi);
        if (Build.VERSION.SDK_INT >= 34) startForeground(7, b.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(7, b.build());
    }

    private void addIsland() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences("orbi_overlay", MODE_PRIVATE);
        lastW = (int) (190 * dn());
        lastH = (int) (60 * dn());
        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Bridge(), "OrbiNative");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String u) { if (docked) js(true); }
        });
        web.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dx0 = e.getRawX(); dy0 = e.getRawY(); x0 = lp.x; y0 = lp.y;
                        dragging = false; zone = docked || e.getY() <= 64 * dn();
                        return docked;
                    case MotionEvent.ACTION_MOVE:
                        if (!zone) return false;
                        if (!dragging) {
                            if (Math.hypot(e.getRawX() - dx0, e.getRawY() - dy0) < 8 * dn()) return docked;
                            dragging = true;
                            MotionEvent c = MotionEvent.obtain(e);
                            c.setAction(MotionEvent.ACTION_CANCEL);
                            v.onTouchEvent(c);
                            c.recycle();
                            if (docked) undockNow();
                        }
                        lp.x = clamp(x0 + (int) (e.getRawX() - dx0), 0, sw() - lp.width);
                        lp.y = clamp(y0 + (int) (e.getRawY() - dy0), 0, sh() - (int) (60 * dn()));
                        update();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if (dragging) { dragging = false; finishDrag(); return true; }
                        if (docked && e.getActionMasked() == MotionEvent.ACTION_UP) { undockNow(); return true; }
                        return false;
                }
                return false;
            }
        });
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(lastW, lastH, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.x = prefs.getInt("x", (sw() - lastW) / 2);
        lp.y = prefs.getInt("y", 0);
        String d = prefs.getString("dock", null);
        if (d != null) { side = d; docked = true; applyDock(); }
        if (Build.VERSION.SDK_INT >= 28)
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        wm.addView(web, lp);
        web.loadUrl(URL);
    }

    /** Called from the page: the island tells the window how big to be. */
    private class Bridge {
        @JavascriptInterface
        public String mediaStatus() { return MediaBridge.status(OverlayService.this); }

        @JavascriptInterface
        public void mediaCmd(String c) { MediaBridge.cmd(OverlayService.this, c); }

        @JavascriptInterface
        public void size(final int w, final int h, final boolean open) {
            ui.post(new Runnable() {
                @Override public void run() {
                    if (web == null) return;
                    if (docked) return;
                    DisplayMetrics m = getResources().getDisplayMetrics();
                    int cx = lp.x + lp.width / 2;
                    lastOpen = open;
                    if (open) {
                        lp.width = (int) Math.min(m.widthPixels - 8 * m.density, 400 * m.density);
                        lp.height = (int) Math.min(m.heightPixels * 0.85, 500 * m.density);
                        lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    } else {
                        lp.width = (int) Math.min(w * m.density, m.widthPixels);
                        lp.height = (int) (h * m.density);
                        lastW = lp.width; lastH = lp.height;
                        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    }
                    lp.x = clamp(cx - lp.width / 2, 0, m.widthPixels - lp.width);
                    lp.y = clamp(lp.y, 0, m.heightPixels - lp.height);
                    try { wm.updateViewLayout(web, lp); } catch (Exception ignored) { }
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        if (web != null) {
            try { wm.removeView(web); } catch (Exception ignored) { }
            web.destroy();
            web = null;
        }
        running = false;
        super.onDestroy();
    }
}
