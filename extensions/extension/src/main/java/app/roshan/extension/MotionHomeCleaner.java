package app.roshan.extension;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;
import java.util.WeakHashMap;

/**
 * Runtime UI cleaner for Motion 4.2.17.
 *
 * Operates only on the React Native view tree and removes selected home-screen
 * promotional/recommendation sections. It deliberately avoids ScrollView
 * containers so a match can never hide the entire home feed.
 */
public final class MotionHomeCleaner {
    private static final long RESCAN_INTERVAL_MS = 1000L;
    private static final WeakHashMap<View, Long> LAST_SCAN = new WeakHashMap<>();

    private MotionHomeCleaner() {}

    public static void schedule(final View root) {
        if (root == null) return;

        root.postDelayed(new Runnable() {
            @Override
            public void run() {
                cleanIfNeeded(root);
                root.postDelayed(this, RESCAN_INTERVAL_MS);
            }
        }, 500L);
    }

    private static void cleanIfNeeded(View root) {
        if (!root.isAttachedToWindow() || root.getWindowVisibility() != View.VISIBLE) {
            return;
        }

        long now = SystemClock.uptimeMillis();
        synchronized (LAST_SCAN) {
            Long last = LAST_SCAN.get(root);
            if (last != null && now - last < RESCAN_INTERVAL_MS) return;
            LAST_SCAN.put(root, now);
        }

        scan(root, root);
    }

    private static void scan(View view, View root) {
        if (view instanceof TextView) {
            String text = normalize(((TextView) view).getText());
            if (isMarketingTarget(text)) {
                if (isSmallActionTarget(text)) {
                    hideClickableAncestor((TextView) view);
                } else {
                    hideSectionAncestor((TextView) view, root);
                }
                return;
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                scan(group.getChildAt(i), root);
            }
        }
    }

    private static String normalize(CharSequence value) {
        if (value == null) return "";
        return value.toString()
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private static boolean isMarketingTarget(String text) {
        if (text.length() == 0) return false;

        return text.contains("invite friends")
                || text.contains("earn rewards")
                || text.startsWith("wls:")
                || text.contains("is leading with")
                || text.equals("test arena")
                || text.equals("ek package puri taiyari")
                || text.equals("selection wala combo")
                || text.equals("ai based practice")
                || text.equals("trending videos")
                || text.equals("complete your profile")
                || text.equals("daily motivation")
                || text.equals("what our students say")
                || text.contains("see what toppers are watching to boost their rank")
                || text.contains("motion made challenging topics manageable");
    }

    private static boolean isSmallActionTarget(String text) {
        return text.contains("invite friends")
                || text.contains("earn rewards")
                || text.equals("test arena")
                || text.equals("ek package puri taiyari")
                || text.equals("selection wala combo")
                || text.equals("ai based practice")
                || text.equals("complete your profile");
    }

    private static void hideClickableAncestor(TextView textView) {
        View current = textView;

        for (int depth = 0; depth < 6; depth++) {
            if (current.isClickable() || current.hasOnClickListeners()) {
                current.setVisibility(View.GONE);
                return;
            }

            ViewParent parent = current.getParent();
            if (!(parent instanceof View) || parent == current) break;
            current = (View) parent;
        }

        textView.setVisibility(View.GONE);
    }

    private static void hideSectionAncestor(TextView textView, View root) {
        final int rootWidth = root.getWidth();
        final int rootHeight = root.getHeight();

        View current = textView;
        View candidate = null;

        for (int depth = 0; depth < 10; depth++) {
            ViewParent parent = current.getParent();
            if (!(parent instanceof View) || parent == root) break;

            current = (View) parent;

            // Never hide a scrolling container: doing so can blank the whole feed.
            if (current instanceof ScrollView || current instanceof HorizontalScrollView) {
                continue;
            }

            int width = current.getWidth();
            int height = current.getHeight();

            int minHeight = dp(current, 80);
            int maxHeight = dp(current, 520);

            if (rootWidth > 0
                    && width >= (int) (rootWidth * 0.65f)
                    && height >= minHeight
                    && height <= maxHeight
                    && (rootHeight <= 0 || height < (int) (rootHeight * 0.60f))) {
                candidate = current;
                break;
            }
        }

        if (candidate != null) {
            candidate.setVisibility(View.GONE);
        } else {
            // Fail closed: hide only the matching label instead of risking
            // removal of a large parent container.
            textView.setVisibility(View.GONE);
        }
    }

    private static int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + 0.5f);
    }
}
