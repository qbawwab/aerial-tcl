package com.aerialtcl.usbdream;

import android.app.Activity;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.Settings;
import android.content.ActivityNotFoundException;
import java.io.File;
import java.util.Collections;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/** Remote-control-friendly setup screen. No internet, ads or accounts. */
public final class MainActivity extends Activity {
    private static final int PICK_FOLDER = 101;
    private static final int STORAGE_SETTINGS = 103;
    private static final int VIDEO_PERMISSION = 104;
    private static final int PICK_VIDEOS = 102;
    private static final int BG = 0xff0b1019;
    private static final int CARD = 0xff18212e;
    private static final int MUTED = 0xffa7b5c7;
    private TextView sourceStatus;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setBackgroundColor(BG);
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(65), dp(35), dp(65), dp(35));
        body.setBackgroundColor(BG);
        scroll.addView(body);

        TextView kicker = label("CUSTOM SCREENSAVER  /  GOOGLE TV", 13, 0xff80bdd9);
        kicker.setLetterSpacing(0.14f);
        body.addView(kicker);
        TextView heading = label("AERIAL  /  TCL", 36, Color.WHITE);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        space(body, 11);
        body.addView(heading);
        space(body, 9);
        body.addView(label("Your own aerial videos. Played directly from USB when the TV is idle.", 19, MUTED));
        space(body, 26);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(25), dp(23), dp(25), dp(23));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(CARD);
        cardBg.setCornerRadius(dp(20));
        panel.setBackground(cardBg);
        body.addView(panel);
        panel.addView(label("01   VIDEO LIBRARY", 15, 0xff8db3d5));
        space(panel, 15);
        sourceStatus = label("", 18, Color.WHITE);
        panel.addView(sourceStatus);
        updateSourceStatus();
        space(panel, 16);
        Button folder = action("FIND USB VIDEOS (NO PICKER)", () -> selectUsbDrive());
        panel.addView(folder);
        space(panel, 9);
        Button files = action("ALTERNATIVE: ANDROID FILE PICKER", () -> chooseFolder());
        panel.addView(files);
        space(panel, 11);
        panel.addView(label("Put MP4 videos in an AerialVideos folder on your USB. Grant the normal video/media permission if Android asks. USB videos are detected via Android MediaStore; your TV must index the USB.", 14, MUTED));
        space(body, 22);

        LinearLayout prefsPanel = new LinearLayout(this);
        prefsPanel.setOrientation(LinearLayout.VERTICAL);
        prefsPanel.setPadding(dp(25), dp(20), dp(25), dp(20));
        GradientDrawable settingsBg = new GradientDrawable();
        settingsBg.setColor(CARD);
        settingsBg.setCornerRadius(dp(20));
        prefsPanel.setBackground(settingsBg);
        body.addView(prefsPanel);
        prefsPanel.addView(label("02   APPEARANCE & PLAYBACK", 15, 0xff8db3d5));
        space(prefsPanel, 12);
        toggle(prefsPanel, "Shuffle videos", Config.SHUFFLE, true);
        toggle(prefsPanel, "Show clock", Config.CLOCK, false);
        toggle(prefsPanel, "Show video name", Config.NAME, false);
        space(body, 22);
        body.addView(action("▶   PREVIEW MY SCREENSAVER", () ->
                startActivity(new Intent(this, PreviewActivity.class))));
        space(body, 17);
        body.addView(label("After testing: set Aerial TCL as the default screensaver using ADB (instructions in the included README).", 14, MUTED));
        setContentView(scroll);
    }

    private void selectUsbDrive() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_VIDEO}, VIDEO_PERMISSION);
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, VIDEO_PERMISSION);
            return;
        }
        checkUsbVideos();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != VIDEO_PERMISSION) return;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            checkUsbVideos();
        } else {
            Toast.makeText(this, "Video permission denied. Enable Files and media in Android app settings.", Toast.LENGTH_LONG).show();
        }
    }

    private void checkUsbVideos() {
        new Thread(() -> {
            int count = VideoLibrary.usbVideos(this).size();
            runOnUiThread(() -> {
                Config.prefs(this).edit().putBoolean("use_mediastore_usb", true).apply();
                updateSourceStatus();
                Toast.makeText(this,
                        count > 0 ? "Found " + count + " USB videos. Try Preview." :
                                "No USB videos indexed yet. Check the USB and try again.",
                        Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    private void chooseFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_FOLDER);
        } catch (Exception e) {
            Toast.makeText(this, "No folder picker found on this TV. Try individual videos.", Toast.LENGTH_LONG).show();
        }
    }

    private void chooseVideos() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, PICK_VIDEOS);
        } catch (Exception e) {
            Toast.makeText(this, "No file picker is installed on this TV.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        if (requestCode == PICK_FOLDER && data.getData() != null) {
            Uri uri = data.getData();
            if (!keepReadPermission(uri, data)) return;
            Config.prefs(this).edit()
                    .putString(Config.TREE, uri.toString())
                    .putString(Config.TREE_NAME, uri.getLastPathSegment())
                    .apply();
            updateSourceStatus();
            Toast.makeText(this, "USB video folder saved", Toast.LENGTH_SHORT).show();
        } else if (requestCode == PICK_VIDEOS) {
            ArrayList<Uri> uris = new ArrayList<>();
            ClipData selected = data.getClipData();
            if (selected != null) {
                for (int i = 0; i < selected.getItemCount(); i++) {
                    Uri uri = selected.getItemAt(i).getUri();
                    if (keepReadPermission(uri, data)) uris.add(uri);
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                if (keepReadPermission(uri, data)) uris.add(uri);
            }
            if (!uris.isEmpty()) {
                Config.setSelectedVideos(this, uris);
                updateSourceStatus();
                Toast.makeText(this, uris.size() + " videos saved", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean keepReadPermission(Uri uri, Intent result) {
        if ((result.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
            Toast.makeText(this, "The picker did not grant read access.", Toast.LENGTH_LONG).show();
            return false;
        }
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return true;
        } catch (SecurityException e) {
            Toast.makeText(this, "Cannot keep USB access. Try the folder picker.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void updateSourceStatus() {
        if (sourceStatus == null) return;
        if (Config.prefs(this).getBoolean("use_mediastore_usb", false)) {
            sourceStatus.setText("USB media library selected. Use Preview to test playback.");
            return;
        }
        String usbPath = Config.prefs(this).getString("usb_path", "");
        if (!usbPath.isEmpty()) {
            sourceStatus.setText("USB: " + usbPath);
            return;
        }
        String folder = Config.prefs(this).getString(Config.TREE_NAME, "");
        List<Uri> videos = Config.selectedVideos(this);
        if (folder.isEmpty() && videos.isEmpty()) {
            sourceStatus.setText("No video source selected yet");
        } else {
            sourceStatus.setText((folder.isEmpty() ? "" : "USB folder: " + folder)
                    + (videos.isEmpty() ? "" : "   •   " + videos.size() + " chosen files"));
        }
    }

    private void toggle(LinearLayout parent, String description, String key, boolean defaultValue) {
        Switch sw = new Switch(this);
        sw.setText(description);
        sw.setTextSize(19);
        sw.setTextColor(Color.WHITE);
        sw.setPadding(0, dp(10), 0, dp(10));
        sw.setChecked(Config.prefs(this).getBoolean(key, defaultValue));
        sw.setOnCheckedChangeListener((CompoundButton buttonView, boolean checked) ->
                Config.prefs(this).edit().putBoolean(key, checked).apply());
        parent.addView(sw);
    }

    private Button action(String text, Runnable callback) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setPadding(dp(17), dp(9), dp(17), dp(9));
        GradientDrawable normal = new GradientDrawable();
        normal.setColor(0xff2a415a);
        normal.setCornerRadius(dp(11));
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(0xff3a83bf);
        focused.setCornerRadius(dp(11));
        android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, normal);
        b.setBackground(states);
        b.setOnClickListener(view -> callback.run());
        return b;
    }

    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.START);
        return t;
    }

    private void space(LinearLayout parent, int dp) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(dp)));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
