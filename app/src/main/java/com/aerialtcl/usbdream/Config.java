package com.aerialtcl.usbdream;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;

final class Config {
    static final String PREFS = "aerial_tcl_config";
    static final String TREE = "tree_uri";
    static final String TREE_NAME = "tree_name";
    static final String VIDEOS = "selected_video_uris";
    static final String SHUFFLE = "shuffle";
    static final String CLOCK = "clock";
    static final String NAME = "show_name";

    private Config() { }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void setSelectedVideos(Context context, List<Uri> list) {
        JSONArray json = new JSONArray();
        for (Uri uri : list) json.put(uri.toString());
        prefs(context).edit().putString(VIDEOS, json.toString()).apply();
    }

    static List<Uri> selectedVideos(Context context) {
        ArrayList<Uri> list = new ArrayList<>();
        String raw = prefs(context).getString(VIDEOS, "[]");
        try {
            JSONArray json = new JSONArray(raw);
            for (int i = 0; i < json.length(); i++) list.add(Uri.parse(json.getString(i)));
        } catch (JSONException ignored) { }
        return list;
    }
}
