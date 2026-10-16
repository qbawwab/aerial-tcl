package com.aerialtcl.usbdream;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.util.Log;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Enumerates only the user-authorized USB folder or specifically selected videos. */
final class VideoLibrary {
    private static final String TAG = "AerialTCL";
    private static final int MAX_VIDEOS = 600;
    private VideoLibrary() { }

    static List<Uri> read(Context context) {
        LinkedHashSet<Uri> found = new LinkedHashSet<>();
        String raw = Config.prefs(context).getString(Config.TREE, "");
        if (!raw.isEmpty()) {
            try {
                Uri tree = Uri.parse(raw);
                String rootId = DocumentsContract.getTreeDocumentId(tree);
                walk(context, tree, rootId, found, 0);
            } catch (Exception e) {
                Log.w(TAG, "USB folder inaccessible, re-select folder", e);
            }
        }
        found.addAll(Config.selectedVideos(context));
        return new ArrayList<>(found);
    }

    private static void walk(Context context, Uri tree, String parentId,
                             LinkedHashSet<Uri> found, int depth) {
        if (depth > 8 || found.size() >= MAX_VIDEOS) return;
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId);
        String[] columns = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
        };
        try (Cursor cursor = context.getContentResolver().query(children, columns, null, null, null)) {
            if (cursor == null) return;
            int idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE);
            int nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            while (cursor.moveToNext() && found.size() < MAX_VIDEOS) {
                String id = cursor.getString(idCol);
                String mime = cursor.getString(mimeCol);
                String name = cursor.getString(nameCol);
                if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) {
                    walk(context, tree, id, found, depth + 1);
                } else if (isVideo(name, mime)) {
                    found.add(DocumentsContract.buildDocumentUriUsingTree(tree, id));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to scan folder " + parentId, e);
        }
    }

    private static boolean isVideo(String name, String mime) {
        if (mime != null && mime.startsWith("video/")) return true;
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp4") || lower.endsWith(".m4v")
                || lower.endsWith(".mov") || lower.endsWith(".mkv")
                || lower.endsWith(".webm") || lower.endsWith(".ts");
    }
}
