package com.aerialtcl.usbdream;

import android.service.dreams.DreamService;
import android.view.KeyEvent;

/** Android's actual system screensaver service, not a looping foreground Activity. */
public final class UsbDreamService extends DreamService {
    private PlaybackView player;

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setFullscreen(true);
        setScreenBright(true);
        setInteractive(true);
        player = new PlaybackView(this);
        setContentView(player);
    }

    @Override public void onDreamingStarted() {
        super.onDreamingStarted();
        if (player != null) player.start();
    }

    @Override public void onDreamingStopped() {
        if (player != null) player.close();
        super.onDreamingStopped();
    }

    @Override public void onDetachedFromWindow() {
        if (player != null) {
            player.close();
            player = null;
        }
        super.onDetachedFromWindow();
    }

    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_DPAD_RIGHT || code == KeyEvent.KEYCODE_MEDIA_NEXT) {
            if (player != null) player.next();
            return true;
        }
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_DPAD_CENTER) {
            wakeUp();
            return true;
        }
        return super.onKeyDown(code, event);
    }
}
