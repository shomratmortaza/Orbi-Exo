package com.shomratmortaza.orbi;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.provider.Settings;
import java.util.List;
import org.json.JSONObject;

/** Reads and controls whatever player is active on the phone. No accounts or API keys needed. */
public class MediaBridge {
    static boolean granted(Context c) {
        String s = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(c.getPackageName());
    }

    static MediaController pick(Context c) {
        try {
            MediaSessionManager m = (MediaSessionManager) c.getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<MediaController> l = m.getActiveSessions(new ComponentName(c, OrbiNotifListener.class));
            MediaController first = null;
            for (MediaController mc : l) {
                if (first == null) first = mc;
                PlaybackState ps = mc.getPlaybackState();
                if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) return mc;
            }
            return first;
        } catch (Exception e) { return null; }
    }

    static String status(Context c) {
        JSONObject o = new JSONObject();
        try {
            boolean g = granted(c);
            o.put("granted", g);
            MediaController mc = g ? pick(c) : null;
            if (mc != null) {
                MediaMetadata md = mc.getMetadata();
                PlaybackState ps = mc.getPlaybackState();
                if (md != null) {
                    o.put("title", md.getString(MediaMetadata.METADATA_KEY_TITLE));
                    o.put("artist", md.getString(MediaMetadata.METADATA_KEY_ARTIST));
                }
                o.put("app", mc.getPackageName());
                o.put("playing", ps != null && ps.getState() == PlaybackState.STATE_PLAYING);
            }
        } catch (Exception ignored) { }
        return o.toString();
    }

    static void cmd(Context c, String a) {
        MediaController mc = pick(c);
        if (mc == null) return;
        MediaController.TransportControls t = mc.getTransportControls();
        if ("next".equals(a)) t.skipToNext();
        else if ("prev".equals(a)) t.skipToPrevious();
        else if ("play".equals(a)) t.play();
        else if ("pause".equals(a)) t.pause();
        else if ("toggle".equals(a)) {
            PlaybackState ps = mc.getPlaybackState();
            if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) t.pause(); else t.play();
        }
    }
}
