package app.roshan.extension;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import java.util.WeakHashMap;

/**
 * Runtime UI cleaner for Motion 4.2.17.
 *
 * It intentionally operates only on the React Native view tree. It does not
 * change navigation or learning data; it hides selected home-screen marketing
 * sections after they are rendered.
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
            String text = String.valueOf(((TextView) view).getText()).trim();
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

    private static boolean isMarketingTarget(String text) {
        if (text.length() == 0) return false;

        return text.contains("Invite friends")
                || text.contains("earn rewards")
                || text.startsWith("WLS:")
                || text.contains("is leading with")
                || text.equals("Test Arena")
                || text.equals("Ek Package Puri Taiyari")
                || text.equals("Selection Wala Combo")
                || text.equals("AI Based Practice")
                || text.equals("Trending Videos")
                || text.equals("Complete Your Profile")
                || text.equals("Daily Motivation")
                || text.equals("What our students say");
    }

    private static boolean isSmallActionTarget(String text) {
        return text.contains("Invite friends")
                || text.contains("earn rewards")
                || text.equals("Test Arena")
                || text.equals("Ek Package Puri Taiyari")
                || text.equals("Selection Wala Combo")
                || text.equals("AI Based Practice")
                || text.equals("Complete Your Profile");
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
        int rootWidth = root.getWidth();
        int rootHeight = root.getHeight();

        View candidate = textView;
        View current = textView;

        for (int depth = 0; depth < 8; depth++) {
            ViewParent parent = current.getParent();
            if (!(parent instanceof View) || parent == root) break;

            current = (View) parent;
            int width = current.getWidth();
            int height = current.getHeight();

            // Section-level containers in Motion span most of the content width
            // but are much shorter than the complete React root.
            if (rootWidth > 0
                    && rootHeight > 0
                    && width >= (int) (rootWidth * 0.65f)
                    && height >= dp(current, 60)
                    && height <= (int) (rootHeight * 0.70f)) {
                candidate = current;
                break;
            }
        }

        if (candidate != textView) {
            candidate.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.GONE);
        }
    }

    private static int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + 0.5f);
    }
}
