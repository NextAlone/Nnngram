/*
 * Copyright (C) 2019-2023 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this software.
 *  If not, see
 * <https://www.gnu.org/licenses/>
 */

package xyz.nextalone.nnngram.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import xyz.nextalone.nnngram.helpers.TranslateHelper;

public class DialogConfig {
    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("dialogconfig", Context.MODE_PRIVATE);

    public static boolean isAutoTranslateEnable(long dialogId, long topicId) {
        migrateLegacyPositiveChatKey(dialogId, topicId);
        String exactKey = AutoTranslateConfigKey.forDialog(dialogId, topicId);
        boolean hasExactValue = preferences.contains(exactKey);
        boolean exactValue = preferences.getBoolean(exactKey, false);

        String parentDialogKey = AutoTranslateConfigKey.parentDialogKey(dialogId, topicId);
        boolean hasParentDialogValue = false;
        boolean parentDialogValue = false;
        if (parentDialogKey != null) {
            migrateLegacyPositiveChatKey(dialogId, 0);
            hasParentDialogValue = preferences.contains(parentDialogKey);
            parentDialogValue = preferences.getBoolean(parentDialogKey, false);
        }

        return AutoTranslateConfigKey.resolveEffectiveValue(
                hasExactValue,
                exactValue,
                hasParentDialogValue,
                parentDialogValue,
                TranslateHelper.getAutoTranslate()
        );
    }

    public static boolean hasAutoTranslateConfig(long dialogId, long topicId) {
        migrateLegacyPositiveChatKey(dialogId, topicId);
        return preferences.contains(AutoTranslateConfigKey.forDialog(dialogId, topicId));
    }

    public static void setAutoTranslateEnable(long dialogId, long topicId, boolean enable) {
        SharedPreferences.Editor editor = preferences.edit()
                .putBoolean(AutoTranslateConfigKey.forDialog(dialogId, topicId), enable);
        markSignedDialogMigrated(editor, dialogId, topicId);
        editor.apply();
    }

    public static void removeAutoTranslateConfig(long dialogId, long topicId) {
        SharedPreferences.Editor editor = preferences.edit()
                .remove(AutoTranslateConfigKey.forDialog(dialogId, topicId));
        markSignedDialogMigrated(editor, dialogId, topicId);
        editor.apply();
    }

    /**
     * Older builds read and wrote chat overrides under a positive raw chat ID,
     * while the in-chat menu wrote a signed key that was never read. Copy the
     * legacy value once, replacing any signed value, so the effective setting
     * survives the upgrade. The legacy key is intentionally retained because it
     * may also represent a private dialog with the same raw ID.
     */
    private static synchronized void migrateLegacyPositiveChatKey(long dialogId, long topicId) {
        if (dialogId >= 0) {
            return;
        }

        String canonicalKey = AutoTranslateConfigKey.forDialog(dialogId, topicId);
        String markerKey = AutoTranslateConfigKey.signedDialogMigrationMarker(dialogId, topicId);
        String legacyKey = AutoTranslateConfigKey.legacyPositiveChatKey(dialogId, topicId);
        boolean hasMigrationMarker = preferences.getBoolean(markerKey, false);
        boolean hasLegacyValue = legacyKey != null && preferences.contains(legacyKey);

        if (hasMigrationMarker) {
            return;
        }

        SharedPreferences.Editor editor = preferences.edit().putBoolean(markerKey, true);
        if (AutoTranslateConfigKey.shouldMigrateLegacyPositiveChatKey(
                dialogId,
                hasMigrationMarker,
                hasLegacyValue
        )) {
            editor.putBoolean(canonicalKey, preferences.getBoolean(legacyKey, TranslateHelper.getAutoTranslate()));
        }
        editor.apply();
    }

    private static void markSignedDialogMigrated(SharedPreferences.Editor editor, long dialogId, long topicId) {
        if (dialogId < 0) {
            editor.putBoolean(AutoTranslateConfigKey.signedDialogMigrationMarker(dialogId, topicId), true);
        }
    }

}
