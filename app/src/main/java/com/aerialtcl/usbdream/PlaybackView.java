package com.aerialtcl.usbdream;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.VideoView;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Random;

/** Shared full-screen player for manual preview and Android's DreamService. */
final class PlaybackView extends FrameLayout {
    private final VideoView video;
    private final TextView status;
    private final TextView clock;
    private final TextView title;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private List<Uri> playlist = new ArrayList<>();
    private int index = -1;
    private int failures = 0;
    private boolean closed = false;
    private final Runnable clockTicker = new Runnable() {
        @Override public void run() {
            if (closed) return;
            clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            handler.postDelayed(this, 20_000L);
        }
    };

    PlaybackView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        setFocusable(true);
        setFocusableInTouchMode(true);

        video = new VideoView(context);
        addView(video, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        status = label(context, 22, Color.WHITE);
        FrameLayout.LayoutParams middle = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        middle.setMargins(35, 10, 35, 10);
        addView(status, middle);
        status.setText("Loading videos from USB …");
        clock = label(context, 32, Color.WHITE);
        FrameLayout.LayoutParams top = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT);
        top.setMargins(0, 38, 60, 0);
        addView(clock, top);
        title = label(context, 18, Color.WHITE);
        FrameLayout.LayoutParams bottom = new FrameLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT);
        bottom.setMargins(60, 0, 0, 45);
        addView(title, bottom);

        boolean showClock = Config.prefs(context).getBoolean(Config.CLOCK, false);
        boolean showName = Config.prefs(context).getBoolean(Config.NAME, false);
        clock.setVisibility(showClock ? View.VISIBLE : View.GONE);
        title.setVisibility(showName ? View.VISIBLE : View.GONE);
        if (showClock) handler.post(clockTicker);

        video.setOnPreparedListener(mediaPlayer -> {
            try { mediaPlayer.setVolume(0f, 0f); } catch (Exception ignored) { }
            if (!closed) {
                failures = 0;
                status.setVisibility(View.GONE);
                video.start();
            }
        });
        video.setOnCompletionListener(mediaPlayer -> next());
        video.setOnErrorListener((mediaPlayer, what, extra) -> {
            failures++;
            if (failures >= playlist.size()) {
                status.setText("Unable to play these videos. Try MP4 / H.264 files.");
                status.setVisibility(View.VISIBLE);
            } else {
                handler.post(this::next);
            }
            return true;
        });
    }

    void start() {
        status.setText("Loading videos from USB …");
        new Thread(() -> {
            List<Uri> found = VideoLibrary.read(getContext().getApplicationContext());
            if (Config.prefs(getContext()).getBoolean(Config.SHUFFLE, true)) {
                Collections.shuffle(found, random);
            }
            handler.post(() -> {
                if (closed) return;
                playlist = found;
                if (playlist.isEmpty()) {
                    status.setText("No videos found. Open Aerial TCL and select your USB folder.");
                    status.setVisibility(View.VISIBLE);
                } else {
                    next();
                }
            });
        }, "USB media scan").start();
    }

    void next() {
        if (closed || playlist.isEmpty()) return;
        index++;
        if (index >= playlist.size()) {
            index = 0;
            if (Config.prefs(getContext()).getBoolean(Config.SHUFFLE, true)) {
                Collections.shuffle(playlist, random);
            }
        }
        Uri current = playlist.get(index);
        String displayName = current.getLastPathSegment();
        title.setText(displayName != null ? displayName.replaceFirst("(?i)\\.[a-z0-9]+$", "") : "Aerial");
        video.setVideoURI(current);
        video.requestFocus();
        video.start();
    }

    void close() {
        closed = true;
        handler.removeCallbacksAndMessages(null);
        try { video.stopPlayback(); } catch (Exception ignored) { }
    }

    private static TextView label(Context context, int sp, int color) {
        TextView view = new TextView(context);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setShadowLayer(4f, 1f, 2f, Color.BLACK);
        return view;
    }
}
