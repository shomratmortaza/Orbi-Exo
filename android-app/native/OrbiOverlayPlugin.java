package com.shomratmortaza.orbi;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.content.pm.PackageInfo;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "OrbiOverlay")
public class OrbiOverlayPlugin extends Plugin {
    @PluginMethod
    public void status(PluginCall call) {
        JSObject o = new JSObject();
        o.put("granted", Settings.canDrawOverlays(getContext()));
        o.put("running", OverlayService.running);
        call.resolve(o);
    }

    @PluginMethod
    public void request(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getContext().getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    @PluginMethod
    public void start(PluginCall call) {
        if (!Settings.canDrawOverlays(getContext())) { call.reject("Overlay permission missing"); return; }
        Intent i = new Intent(getContext(), OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) getContext().startForegroundService(i);
        else getContext().startService(i);
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        getContext().stopService(new Intent(getContext(), OverlayService.class));
        call.resolve();
    }

    @PluginMethod
    public void version(PluginCall call) {
        JSObject o = new JSObject();
        try {
            PackageInfo pi = getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
            o.put("version", pi.versionName);
        } catch (Exception e) { o.put("version", ""); }
        call.resolve(o);
    }

    @PluginMethod
    public void openUrl(PluginCall call) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(call.getString("url", "")));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }

    @PluginMethod
    public void mediaStatus(PluginCall call) {
        try { call.resolve(new JSObject(MediaBridge.status(getContext()))); }
        catch (Exception e) { call.resolve(new JSObject()); }
    }

    @PluginMethod
    public void mediaCmd(PluginCall call) {
        MediaBridge.cmd(getContext(), call.getString("cmd", ""));
        call.resolve();
    }

    @PluginMethod
    public void mediaAccess(PluginCall call) {
        Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
        call.resolve();
    }
}
