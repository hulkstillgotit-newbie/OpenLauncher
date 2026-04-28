package com.android.window.flags;

/**
 * Stub for AOSP android.window aconfig feature flags.
 * All flags return false (disabled) so the app compiles and runs safely outside AOSP.
 */
public final class Flags {
    private Flags() {}

    public static boolean predictiveBackThreeButtonNav() { return false; }
    public static boolean enableDesktopWindowingMode() { return false; }
    public static boolean enableDesktopWindowingWallpaperActivity() { return false; }
}
