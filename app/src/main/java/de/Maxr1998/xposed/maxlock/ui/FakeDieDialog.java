/*
 * MaxLock, an Xposed applock module for Android
 * Copyright (C) 2014-2015  Maxr1998
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;

import de.Maxr1998.xposed.maxlock.Common;
import de.Maxr1998.xposed.maxlock.R;


public class FakeDieDialog extends Activity {

    ApplicationInfo requestPkgInfo;
    AlertDialog.Builder alertDialog;
    private String requestPkg;
    private Intent app;
    private AlertDialog.Builder reportDialog;
    private SharedPreferences prefs, prefsPackages;

    @SuppressLint("WorldReadableFiles")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Intent Extras
        requestPkg = getIntent().getStringExtra(Common.INTENT_EXTRAS_PKG_NAME);
        app = getIntent().getParcelableExtra(Common.INTENT_EXTRAS_INTENT);

        // Preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //noinspection deprecation
        prefsPackages = getSharedPreferences(Common.PREFS_PACKAGES, MODE_WORLD_READABLE);

        // Technical timer
        Long permitTimestamp = prefsPackages.getLong(requestPkg + "_tmp", 0);
        if (!prefsPackages.getBoolean(requestPkg, false) || (permitTimestamp != 0 && System.currentTimeMillis() - permitTimestamp <= 5000)) {
            fakeUnlock();
            return;
        }

        // Intika I.MoD
        boolean IMoDDelayGlobalEnabled = prefs.getBoolean(Common.IMOD_DELAY_GLOBAL_ENABLED, false);
        boolean IMoDDelayAppEnabled = prefs.getBoolean(Common.IMOD_DELAY_APP_ENABLED, false);
        long IMoDLastUnlockGlobal = prefs.getLong(Common.IMOD_LAST_UNLOCK_GLOBAL, 0);
        long IMoDLastUnlockApp = prefsPackages.getLong(requestPkg + "_imod", 0);

        if (/* Global */(IMoDDelayGlobalEnabled && (IMoDLastUnlockGlobal != 0 &&
                System.currentTimeMillis() - IMoDLastUnlockGlobal <=
                        prefs.getInt(Common.IMOD_DELAY_GLOBAL, 600000)))
                ||/* Per app */(IMoDDelayAppEnabled) && (IMoDLastUnlockApp != 0 &&
                System.currentTimeMillis() - IMoDLastUnlockApp <=
                        prefs.getInt(Common.IMOD_DELAY_APP, 600000))) {
            fakeUnlock();
            return;
        }
        // Intika I.MoD End
        getWindow().setBackgroundDrawable(new ColorDrawable(0));
        PackageManager pm = getPackageManager();
        try {
            requestPkgInfo = pm.getApplicationInfo(requestPkg, 0);
        } catch (PackageManager.NameNotFoundException e) {
            requestPkgInfo = null;
        }

        String requestPkgFullName = (String) (requestPkgInfo != null ? pm.getApplicationLabel(requestPkgInfo) : "(unknown)");
        alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage(String.format(getResources().getString(R.string.fake_die_message), requestPkgFullName))
                .setNeutralButton(R.string.report, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (prefs.getBoolean(Common.IMOD_MIN_FAKE_UNLOCK, false)) {
                            callLockScreen();
                        } else {
                            final EditText input = new EditText(FakeDieDialog.this);
                            input.setMinLines(3);
                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                            reportDialog = new AlertDialog.Builder(FakeDieDialog.this);
                            reportDialog.setView(input)
                                    .setTitle(R.string.report_problem)
                                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (input.getText().toString().equals(prefs.getString(Common.FAKE_DIE_INPUT, "start"))) {
                                                callLockScreen();
                                            }
                                            finish();
                                        }
                                    })
                                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            finish();
                                        }
                                    })
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialogInterface) {
                                            finish();
                                        }
                                    })
                                    .create().show();
                        }
                    }
                })
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .create().show();
    }

    @SuppressLint("CommitPrefEdits")
    public void fakeUnlock() {
        prefsPackages.edit()
                .putLong(requestPkg + "_tmp", System.currentTimeMillis())
                .commit();
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        am.killBackgroundProcesses("de.Maxr1998.xposed.maxlock");
        try {
            app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(app);
        } catch (Exception e) {
            Intent intent_option = getPackageManager().getLaunchIntentForPackage(requestPkg);
            intent_option.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent_option);
        } finally {
            finish();
        }
    }

    public void callLockScreen(){
        Intent it = new Intent(FakeDieDialog.this, LockActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        it.putExtra(Common.INTENT_EXTRAS_PKG_NAME, requestPkg);
        it.putExtra(Common.INTENT_EXTRAS_INTENT, app);
        startActivity(it);
    }
}
