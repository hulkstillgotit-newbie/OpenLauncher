package com.android.wm.shell;

/**
 * Stub for AOSP WindowManager Shell aconfig feature flags.
 * All flags return false (disabled) so the app compiles and runs safely outside AOSP.
 */
public final class Flags {
    private Flags() {}

    public static boolean enableBubbleBar() { return false; }
    public static boolean enableBubbleBarInPersistentTaskBar() { return false; }
    public static boolean enableTinyTaskbar() { return false; }
    public static boolean enableLeftRightSplitInPortrait() { return false; }
    public static boolean enableTaskbarNavbarUnification() { return false; }
    public static boolean enableTaskbarOnPhones() { return false; }
    public static boolean enableAppPairs() { return false; }
    public static boolean enableSplitContextual() { return false; }
    public static boolean enableBubbleAnything() { return false; }
    public static boolean enableRetrievableBubbles() { return false; }
}
