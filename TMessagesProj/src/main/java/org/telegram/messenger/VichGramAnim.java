package org.telegram.messenger;

import android.animation.Animator;
import android.animation.TimeInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * VichGram custom animation styles — not Telegram copies.
 */
public final class VichGramAnim {

    private VichGramAnim() {}

    public static long duration(long baseMs) {
        float scale = VichGramPrefs.getAnimDurationScale();
        long result = Math.round(baseMs * scale);
        return Math.max(1, result);
    }

    public static int duration(int baseMs) {
        return (int) duration((long) baseMs);
    }

    /** Custom interpolators per style. */
    public static TimeInterpolator interpolator() {
        switch (VichGramPrefs.getAnimationStyle()) {
            case VichGramPrefs.ANIM_STYLE_SMOOTH:
                return new DecelerateInterpolator(1.8f);
            case VichGramPrefs.ANIM_STYLE_FAST:
                return new AccelerateDecelerateInterpolator();
            case VichGramPrefs.ANIM_STYLE_MINIMAL:
                return new AccelerateDecelerateInterpolator();
            default:
                return new DecelerateInterpolator();
        }
    }

    public static TimeInterpolator overshoot() {
        if (VichGramPrefs.getAnimationStyle() == VichGramPrefs.ANIM_STYLE_SMOOTH) {
            return new OvershootInterpolator(1.2f);
        }
        if (VichGramPrefs.getAnimationStyle() == VichGramPrefs.ANIM_STYLE_FAST) {
            return new OvershootInterpolator(0.6f);
        }
        return new DecelerateInterpolator();
    }

    public static void applyTo(Animator animator) {
        if (animator == null) return;
        try {
            long d = animator.getDuration();
            if (d > 0) {
                animator.setDuration(duration(d));
            }
            animator.setInterpolator(interpolator());
        } catch (Throwable ignore) {
        }
    }
}
