package com.example.tarea16.adapter;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.tarea16.R;
import com.example.tarea16.modelo.SyncEntity;

final class SyncStatusText {
    private SyncStatusText() { }

    static String of(Context context, SyncEntity item, boolean sincronizado) {
        if ("CONFLICT".equalsIgnoreCase(item.syncStatus)) {
            return context.getString(R.string.sync_status_conflict);
        }
        if (item.syncError != null && !item.syncError.trim().isEmpty()) {
            return context.getString(R.string.sync_status_error);
        }
        return context.getString(sincronizado
                ? R.string.sync_status_synced
                : R.string.sync_status_pending);
    }

    static void apply(TextView label, ImageView icon, SyncEntity item, boolean sincronizado) {
        Context context = label.getContext();
        String text = of(context, item, sincronizado);
        int color = colorFor(context, text);
        label.setText(text);
        label.setTextColor(color);
        icon.setImageResource(iconFor(text));
        icon.setContentDescription(context.getString(R.string.sync_status_description) + ": " + text);
    }

    static void applyStatic(TextView label, ImageView icon, String text) {
        Context context = label.getContext();
        String value = text == null ? "" : text;
        int color = colorFor(context, value);
        label.setText(value);
        label.setTextColor(color);
        icon.setImageResource(iconFor(value));
        icon.setContentDescription(context.getString(R.string.sync_status_description) + ": " + value);
    }

    private static int iconFor(String text) {
        if (text == null) {
            return R.drawable.ic_sync_pending;
        }
        String value = text.trim().toUpperCase();
        if (value.contains("CONFLICT")) {
            return R.drawable.ic_sync_conflict;
        }
        if (value.contains("ERROR")) {
            return R.drawable.ic_sync_error;
        }
        if (value.contains("SINCRONIZADO")) {
            return R.drawable.ic_sync_synced;
        }
        return R.drawable.ic_sync_pending;
    }

    private static int colorFor(Context context, String text) {
        if (text == null) {
            return ContextCompat.getColor(context, R.color.secondary);
        }
        String value = text.trim().toUpperCase();
        if (value.contains("CONFLICT") || value.contains("ERROR")) {
            return ContextCompat.getColor(context, R.color.error_warm);
        }
        if (value.contains("SINCRONIZADO")) {
            return ContextCompat.getColor(context, R.color.success);
        }
        return ContextCompat.getColor(context, R.color.secondary);
    }
}
