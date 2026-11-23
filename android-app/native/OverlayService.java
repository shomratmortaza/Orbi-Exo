package com.shomratmortaza.orbi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
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
        float dn = getResources().getDisplayMetrics().density;
        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Bridge(), "OrbiNative");
        web.setWebViewClient(new WebViewClient());
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams((int) (190 * dn), (int) (60 * dn), type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        if (Build.VERSION.SDK_INT >= 28)
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        wm.addView(web, lp);
        web.loadUrl(URL);
    }

    /** Called from the page: the island tells the window how big to be. */
    private class Bridge {
        @JavascriptInterface
        public void size(final int w, final int h, final boolean open) {
            ui.post(new Runnable() {
                @Override public void run() {
                    if (web == null) return;
                    DisplayMetrics m = getResources().getDisplayMetrics();
                    if (open) {
                        lp.width = (int) Math.min(m.widthPixels - 8 * m.density, 400 * m.density);
                        lp.height = (int) Math.min(m.heightPixels * 0.85, 500 * m.density);
                        lp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    } else {
                        lp.width = (int) Math.min(w * m.density, m.widthPixels);
                        lp.height = (int) (h * m.density);
                        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                    }
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
