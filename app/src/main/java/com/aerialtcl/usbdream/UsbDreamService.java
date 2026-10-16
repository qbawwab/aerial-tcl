
package com.aerialtcl.usbdream;

import android.service.dreams.DreamService;

public final class UsbDreamService extends DreamService {
    private PlaybackView player;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setFullscreen(true);
        setScreenBright(true);
        setInteractive(true);
        player = new PlaybackView(this);
        setContentView(player);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (player != null) player.start();
    }

    @Override
    public void onDreamingStopped() {
        if (player != null) player.close();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        if (player != null) {
            player.close();
            player = null;
        }
        super.onDetachedFromWindow();
    }
}
