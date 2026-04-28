package com.android.systemui.shared;

/**
 * Stub for SystemUI shared aconfig feature flags.
 * All flags return false (disabled) so the app compiles safely outside AOSP.
 */
public final class Flags {
    private Flags() {}

    public static boolean newCustomizationPickerUi() { return false; }
    public static boolean sysuiTeamfood() { return false; }
}
