package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Local preferences for VichGram extra features.
 */
public final class VichGramPrefs {

    private static final String PREFS = "vichgram_features";
    private static final String KEY_WATCH_DELETED = "watch_deleted_messages";
    private static final String KEY_WATCH_EDITS = "watch_message_edits";

    private VichGramPrefs() {}

    private static SharedPreferences prefs() {
        Context context = ApplicationLoader.applicationContext;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isWatchDeletedMessages() {
        return prefs().getBoolean(KEY_WATCH_DELETED, false);
    }

    public static void setWatchDeletedMessages(boolean enabled) {
        prefs().edit().putBoolean(KEY_WATCH_DELETED, enabled).apply();
    }

    public static boolean isWatchMessageEdits() {
        return prefs().getBoolean(KEY_WATCH_EDITS, false);
    }

    public static void setWatchMessageEdits(boolean enabled) {
        prefs().edit().putBoolean(KEY_WATCH_EDITS, enabled).apply();
    }
}
