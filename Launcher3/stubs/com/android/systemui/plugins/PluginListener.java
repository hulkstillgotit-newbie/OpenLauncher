package com.android.systemui.plugins;

import android.content.Context;

/**
 * Stub listener interface for SystemUI plugin connections (from PluginCoreLib).
 */
public interface PluginListener<T extends Plugin> {
    void onPluginConnected(T plugin, Context pluginContext);

    default void onPluginDisconnected(T plugin) {}
}
