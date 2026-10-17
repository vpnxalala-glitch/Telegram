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
    private static final String KEY_TURBO = "turbo_mode";
    private static final String KEY_ANIM_STYLE = "animation_style";

    /** Telegram default */
    public static final int ANIM_STYLE_CLASSIC = 0;
    /** Smooth custom curves, slightly longer */
    public static final int ANIM_STYLE_SMOOTH = 1;
    /** Snappy custom transitions */
    public static final int ANIM_STYLE_FAST = 2;
    /** Minimal decorative motion */
    public static final int ANIM_STYLE_MINIMAL = 3;

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

    public static boolean isTurboMode() {
        return prefs().getBoolean(KEY_TURBO, false);
    }

    public static void setTurboMode(boolean enabled) {
        prefs().edit().putBoolean(KEY_TURBO, enabled).apply();
        try {
            if (enabled) {
                SharedConfig.streamMedia = true;
                SharedConfig.streamAllVideo = true;
                SharedConfig.streamMkv = true;
                SharedConfig.saveStreamMedia = true;
            }
        } catch (Throwable ignore) {
        }
    }

    public static int getAnimationStyle() {
        return prefs().getInt(KEY_ANIM_STYLE, ANIM_STYLE_CLASSIC);
    }

    public static void setAnimationStyle(int style) {
        if (style < ANIM_STYLE_CLASSIC || style > ANIM_STYLE_MINIMAL) {
            style = ANIM_STYLE_CLASSIC;
        }
        prefs().edit().putInt(KEY_ANIM_STYLE, style).apply();
        applyAnimationStyle(style);
    }

    public static String getAnimationStyleName() {
        switch (getAnimationStyle()) {
            case ANIM_STYLE_SMOOTH:
                return "Плавный";
            case ANIM_STYLE_FAST:
                return "Быстрый";
            case ANIM_STYLE_MINIMAL:
                return "Минимальный";
            default:
                return "Классика";
        }
    }

    /** Duration multiplier for custom styles (1.0 = Telegram default). */
    public static float getAnimDurationScale() {
        switch (getAnimationStyle()) {
            case ANIM_STYLE_SMOOTH:
                return 1.25f;
            case ANIM_STYLE_FAST:
                return 0.55f;
            case ANIM_STYLE_MINIMAL:
                return 0.35f;
            default:
                return 1.0f;
        }
    }

    private static void applyAnimationStyle(int style) {
        try {
            if (style == ANIM_STYLE_MINIMAL) {
                LiteMode.setAllFlags(LiteMode.PRESET_LOW);
            } else if (style == ANIM_STYLE_SMOOTH || style == ANIM_STYLE_FAST) {
                LiteMode.setAllFlags(LiteMode.PRESET_HIGH);
            }
        } catch (Throwable ignore) {
        }
    }
}
