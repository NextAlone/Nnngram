/*
 * Copyright (C) 2019-2024 qwq233 <qwq233@qwq2333.top>
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

package org.telegram.ui.Components;

import org.telegram.messenger.FileLog;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;

@SuppressLint("ViewConstructor")
public class SelectSendAsPremiumHintBulletinLayout extends Bulletin.MultiLineLayout implements Theme.Colorable {

    public SelectSendAsPremiumHintBulletinLayout(@NonNull Context context, Theme.ResourcesProvider resourcesProvider, boolean channels, Runnable callback) {
        super(context, resourcesProvider);

        imageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.msg_premium_prolfilestar));
        imageView.setColorFilter(new PorterDuffColorFilter(themedIconColor = getThemedColor(Theme.key_undo_infoColor), PorterDuff.Mode.SRC_IN));
        textView.setText(AndroidUtilities.replaceTags(LocaleController.getString(channels ? R.string.SelectSendAsPeerPremiumHint : R.string.SelectSendAsPeerPremiumHint)));

        Bulletin.UndoButton button = new Bulletin.UndoButton(context, true, resourcesProvider);
        button.setText(LocaleController.getString(R.string.SelectSendAsPeerPremiumOpen));
        button.setUndoAction(callback);
        setButton(button);
    }

    private int themedIconColor;

    @Override
    public void updateColors() {
        super.updateColors();
        try {
            final int color = getThemedColor(Theme.key_undo_infoColor);
            if (imageView != null && color != themedIconColor) {
                themedIconColor = color;
                imageView.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            }
            invalidate();
    
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}
