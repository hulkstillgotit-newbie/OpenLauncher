/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.allapps.search;

import static com.android.launcher3.allapps.BaseAllAppsAdapter.VIEW_TYPE_EMPTY_SEARCH;
import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;

import androidx.annotation.AnyThread;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem;
import com.android.launcher3.icons.BitmapInfo;
import com.android.launcher3.icons.LauncherIcons;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.search.SearchAlgorithm;
import com.android.launcher3.search.SearchCallback;
import com.android.launcher3.search.StringMatcherUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Searches installed apps and phone settings — no web results.
 */
public class DefaultAppSearchAlgorithm implements SearchAlgorithm<AdapterItem> {

    private static final int MAX_APP_RESULTS = 5;
    private static final int MAX_SETTINGS_RESULTS = 3;

    /**
     * Settings entries: {display name, android.settings action, space-separated keywords}
     * The action is used to build the launch Intent; keywords broaden the match beyond the title.
     */
    private static final String[][] SETTINGS_TABLE = {
        {"Wi-Fi",              Settings.ACTION_WIFI_SETTINGS,
            "wifi wireless internet network wlan"},
        {"Bluetooth",          Settings.ACTION_BLUETOOTH_SETTINGS,
            "bluetooth pair connect device"},
        {"Mobile Data",        Settings.ACTION_DATA_ROAMING_SETTINGS,
            "mobile data cellular sim 4g 5g lte roaming"},
        {"Sound & Vibration",  Settings.ACTION_SOUND_SETTINGS,
            "sound ringtone volume notification vibration mute"},
        {"Display",            Settings.ACTION_DISPLAY_SETTINGS,
            "display brightness screen dark mode adaptive"},
        {"Battery",            Settings.ACTION_BATTERY_SAVER_SETTINGS,
            "battery power charging saver"},
        {"Storage",            Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            "storage memory space files sd card"},
        {"Apps",               Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS,
            "apps applications installed manage uninstall"},
        {"Location",           Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            "location gps maps tracking"},
        {"Privacy",            Settings.ACTION_PRIVACY_SETTINGS,
            "privacy permissions data usage"},
        {"Security",           Settings.ACTION_SECURITY_SETTINGS,
            "security lock screen fingerprint pin password passcode"},
        {"Accounts",           Settings.ACTION_SYNC_SETTINGS,
            "accounts sync google email"},
        {"Accessibility",      Settings.ACTION_ACCESSIBILITY_SETTINGS,
            "accessibility talkback large text font"},
        {"Language & Input",   Settings.ACTION_LOCALE_SETTINGS,
            "language locale keyboard input method typing"},
        {"Date & Time",        Settings.ACTION_DATE_SETTINGS,
            "date time clock timezone"},
        {"Network & Internet", Settings.ACTION_WIRELESS_SETTINGS,
            "network internet vpn hotspot tethering"},
        {"Notifications",      Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS,
            "notifications alerts do not disturb"},
        {"About Phone",        Settings.ACTION_DEVICE_INFO_SETTINGS,
            "about phone device model version software update"},
        {"Home App",           Settings.ACTION_HOME_SETTINGS,
            "home launcher default app"},
        {"Keyboard",           Settings.ACTION_INPUT_METHOD_SETTINGS,
            "keyboard input method typing autocorrect"},
        {"Developer Options",  Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS,
            "developer debug adb usb"},
        {"NFC",                Settings.ACTION_NFC_SETTINGS,
            "nfc tap pay contactless beam"},
    };

    private final LauncherAppState mAppState;
    private final Handler mResultHandler;
    private final boolean mAddNoResultsMessage;

    public DefaultAppSearchAlgorithm(Context context) {
        this(context, false);
    }

    public DefaultAppSearchAlgorithm(Context context, boolean addNoResultsMessage) {
        mAppState = LauncherAppState.getInstance(context);
        mResultHandler = new Handler(MAIN_EXECUTOR.getLooper());
        mAddNoResultsMessage = addNoResultsMessage;
    }

    @Override
    public void cancel(boolean interruptActiveRequests) {
        if (interruptActiveRequests) {
            mResultHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void doSearch(String query, SearchCallback<AdapterItem> callback) {
        mAppState.getModel().enqueueModelUpdateTask((taskController, dataModel, apps) -> {
            final Context context = mAppState.getContext();
            ArrayList<AdapterItem> result = getTitleMatchResult(apps.data, query);
            result.addAll(getSettingsMatchResult(context, query));
            if (mAddNoResultsMessage && result.isEmpty()) {
                result.add(getEmptyMessageAdapterItem(query));
            }
            mResultHandler.post(() -> callback.onSearchResult(query, result));
        });
    }

    private static AdapterItem getEmptyMessageAdapterItem(String query) {
        AdapterItem item = new AdapterItem(VIEW_TYPE_EMPTY_SEARCH);
        AppInfo placeHolder = new AppInfo();
        placeHolder.title = query;
        item.itemInfo = placeHolder;
        return item;
    }

    /**
     * Filters {@link AppInfo}s whose titles match the query.
     */
    @AnyThread
    public static ArrayList<AdapterItem> getTitleMatchResult(List<AppInfo> apps, String query) {
        final String queryTextLower = query.toLowerCase();
        final ArrayList<AdapterItem> result = new ArrayList<>();
        StringMatcherUtility.StringMatcher matcher =
                StringMatcherUtility.StringMatcher.getInstance();

        int resultCount = 0;
        int total = apps.size();
        for (int i = 0; i < total && resultCount < MAX_APP_RESULTS; i++) {
            AppInfo info = apps.get(i);
            if (StringMatcherUtility.matches(queryTextLower, info.title.toString(), matcher)) {
                result.add(AdapterItem.asApp(info));
                resultCount++;
            }
        }
        return result;
    }

    /**
     * Returns settings-page {@link AdapterItem}s that match the query string.
     * Each result launches the corresponding Android Settings screen when tapped.
     */
    @AnyThread
    private static ArrayList<AdapterItem> getSettingsMatchResult(Context context, String query) {
        final String q = query.toLowerCase().trim();
        if (q.isEmpty()) return new ArrayList<>();

        final PackageManager pm = context.getPackageManager();
        // Load settings icon once; fall back to LOW_RES if unavailable.
        BitmapInfo settingsIcon = BitmapInfo.LOW_RES_INFO;
        try {
            Intent probe = new Intent(Settings.ACTION_SETTINGS);
            ResolveInfo ri = pm.resolveActivity(probe, 0);
            if (ri != null) {
                Drawable d = ri.loadIcon(pm);
                try (LauncherIcons li = LauncherIcons.obtain(context)) {
                    settingsIcon = li.createBadgedIconBitmap(d);
                }
            }
        } catch (Exception ignored) { /* settings unavailable on this device */ }

        final ArrayList<AdapterItem> result = new ArrayList<>();
        for (String[] entry : SETTINGS_TABLE) {
            if (result.size() >= MAX_SETTINGS_RESULTS) break;
            final String title    = entry[0];
            final String action   = entry[1];
            final String keywords = entry[2];
            if (settingsEntryMatches(q, title, keywords)) {
                AppInfo info = new AppInfo();
                info.title = title;
                info.intent = new Intent(action)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                info.bitmap = settingsIcon;
                result.add(AdapterItem.asApp(info));
            }
        }
        return result;
    }

    /** Returns true if the query matches the settings entry title or any keyword. */
    private static boolean settingsEntryMatches(String query, String title, String keywords) {
        if (title.toLowerCase().contains(query)) return true;
        for (String kw : keywords.split(" ")) {
            if (kw.startsWith(query) || query.startsWith(kw)) return true;
        }
        return false;
    }
}
