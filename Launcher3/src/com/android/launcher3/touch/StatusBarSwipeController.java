/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.launcher3.touch;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.util.TouchController;

/**
 * A TouchController that detects a downward swipe on the home screen and expands
 * the system notifications panel. Only active in the NORMAL (home screen) state.
 */
public class StatusBarSwipeController implements TouchController {

    private static final String TAG = "StatusBarSwipeController";

    /** Minimum downward velocity (px/sec) required to trigger the notification panel. */
    private static final float MIN_FLING_VELOCITY = 300f;

    private final Launcher mLauncher;
    private final GestureDetector mGestureDetector;

    public StatusBarSwipeController(Launcher launcher) {
        mLauncher = launcher;
        mGestureDetector = new GestureDetector(launcher,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                            float velocityX, float velocityY) {
                        // Require a predominantly downward fling
                        if (velocityY > MIN_FLING_VELOCITY
                                && Math.abs(velocityY) > Math.abs(velocityX) * 1.5f) {
                            expandNotificationsPanel();
                            return true;
                        }
                        return false;
                    }
                });
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (mLauncher.isInState(LauncherState.NORMAL)
                && !AbstractFloatingView.hasOpenView(
                        mLauncher, AbstractFloatingView.TYPE_STATUS_BAR_SWIPE_DOWN_DISALLOW)) {
            mGestureDetector.onTouchEvent(ev);
        }
        // Never claim the event; just observe it.
        return false;
    }

    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        return false;
    }

    @SuppressWarnings({"JavaReflectionMemberAccess", "DiscouragedPrivateApi"})
    private void expandNotificationsPanel() {
        try {
            // expandNotificationsPanel() is a @SystemApi — use the internal service via
            // reflection, which works for all API levels the launcher supports.
            Object service = mLauncher.getSystemService("statusbar");
            if (service != null) {
                service.getClass().getMethod("expandNotificationsPanel").invoke(service);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to expand notifications panel", e);
        }
    }
}
