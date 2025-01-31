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

package xyz.nextalone.nnngram.activity;

import static xyz.nextalone.nnngram.UIKt.createMessageFilterSetter;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jakewharton.processphoenix.ProcessPhoenix;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

import xyz.nextalone.gen.Config;
import xyz.nextalone.nnngram.InlinesKt;
import xyz.nextalone.nnngram.config.ConfigManager;
import xyz.nextalone.nnngram.helpers.EntitiesHelper;
import xyz.nextalone.nnngram.ui.PopupBuilder;
import xyz.nextalone.nnngram.ui.StickerSizePreviewMessagesCell;
import xyz.nextalone.nnngram.ui.sortList.ItemTouchHelperCallback;
import xyz.nextalone.nnngram.ui.sortList.SortListAdapter;
import xyz.nextalone.nnngram.ui.sortList.TextStyleListAdapter;
import xyz.nextalone.nnngram.utils.AlertUtil;
import xyz.nextalone.nnngram.utils.Defines;
import xyz.nextalone.nnngram.utils.StringUtils;

@SuppressLint("NotifyDataSetChanged")
public class ChatSettingActivity extends BaseActivity {

    private ActionBarMenuItem resetItem;
    private StickerSizeCell stickerSizeCell;
    private GifSizeCell gifSizeCell;

    private int stickerSizeHeaderRow;
    private int stickerSizeRow;
    private int stickerSize2Row;
    private int gifSizeHeaderRow;
    private int gifSizeRow;

    private int chatRow;
    private int ignoreBlockedUserMessagesRow;
    private int hideGroupStickerRow;
    private int disablePremiumStickerRow;
    private int disableAddStickerFromImage;
    private int messageMenuRow;
    private int textStyleSettingsRow;
    private int allowScreenshotOnNoForwardChatRow;
    private int labelChannelUserRow;
    private int displaySpoilerDirectlyRow;
    private int disableJumpToNextChannelRow;
    private int disableGreetingStickerRow;
    private int disableTrendingStickerRow;
    private int disablePreviewVideoSoundShortcutRow;
    private int quickToggleAnonymous;
    private int hideSendAsButtonRow;
    private int customDoubleClickTapRow;
    private int confirmToSendMediaMessagesRow;
    private int maxRecentStickerRow;
    private int unreadBadgeOnBackButtonRow;
    private int ignoreReactionMentionRow;
    private int showForwardDateRow;
    private int hideTimeForStickerRow;
    private int showMessageIDRow;
    private int hideQuickSendMediaBottomRow;
    private int customQuickMessageRow;
    private int scrollableChatPreviewRow;
    private int showTabsOnForwardRow;
    private int disableStickersAutoReorderRow;
    private int hideTitleRow;
    private int messageFiltersRow;
    private int sendLargePhotoRow;
    private int doNotUnarchiveBySwipeRow;
    private int hideInputFieldBotButtonRow;
    private int hideMessageSeenTooltipRow;
    private int disableNotificationBubbleRow;
    private int showOnlineStatusRow;
    private int disablePhotoSideActionRow;
    private int mergeMessageRow;
    private int filterZalgoRow;
    private int hideKeyboardWhenScrollingRow;
    private int searchInPlaceRow;
    private int disableChannelMuteButtonRow;
    private int disableAutoPipRow;
    private int sendMp4DocumentAsVideoRow;
    private int disableGravityDetectionInVideoRow;
    private int autoMuteAfterJoiningChannelRow;
    private int chat2Row;

    private int markdownRow;
    private int markdownDisableRow;
    private int markdownParserRow;
    private int markdownParseLinksRow;
    private int markdown2Row;


    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows();

        return true;
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString("Chat", R.string.Chat);
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);

        ActionBarMenu menu = actionBar.createMenu();
        resetItem = menu.addItem(0, R.drawable.msg_reset);
        resetItem.setContentDescription(LocaleController.getString("ResetStickerSize", R.string.ResetStickerSize));
        resetItem.setVisibility(ConfigManager.getFloatOrDefault(Defines.stickerSize, 14.0f) != 14.0f ? View.VISIBLE : View.GONE);
        resetItem.setTag(null);
        resetItem.setOnClickListener(v -> {
            AndroidUtilities.updateViewVisibilityAnimated(resetItem, false, 0.5f, true);
            ValueAnimator animator = ValueAnimator.ofFloat(ConfigManager.getFloatOrDefault(Defines.stickerSize, 14.0f), 14.0f);
            animator.setDuration(150);
            animator.addUpdateListener(valueAnimator -> {
                ConfigManager.putFloat(Defines.stickerSize, (Float) valueAnimator.getAnimatedValue());
                stickerSizeCell.invalidate();
                gifSizeCell.invalidate();
            });
            animator.start();
        });

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        return view;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == ignoreBlockedUserMessagesRow) {
            Config.toggleIgnoreBlockedUser();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.ignoreBlockedUser);
            }
        } else if (position == hideGroupStickerRow) {
            Config.toggleHideGroupSticker();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.hideGroupSticker);
            }
        } else if (position == disablePremiumStickerRow) {
            Config.toggleDisablePremiumSticker();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disablePremiumSticker);
            }
        } else if(position == disableAddStickerFromImage) {
            Config.toggleDisableAddStickerFromImage();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disableAddStickerFromImage);
            }
        } else if (position == messageMenuRow) {
            showMessageMenuAlert();
        } else if (position == textStyleSettingsRow) {
            showTextStyleSettingsAlert();
        } else if (position == allowScreenshotOnNoForwardChatRow) {
            Config.toggleAllowScreenshotOnNoForwardChat();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.allowScreenshotOnNoForwardChat);
            }
        } else if (position == labelChannelUserRow) {
            if (!Config.channelAlias) {
                Config.toggleLabelChannelUser();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(Config.labelChannelUser);
                }
            } else {
                AndroidUtilities.shakeView(view);
                AlertUtil.showToast(LocaleController.getString("notAllowedWhenChannelAliasIsEnabled", R.string.notAllowedWhenChannelAliasIsEnabled));
            }
        } else if (position == displaySpoilerDirectlyRow) {
            Config.toggleDisplaySpoilerMsgDirectly();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.displaySpoilerMsgDirectly);
            }
        } else if (position == disableJumpToNextChannelRow) {
            Config.toggleDisableJumpToNextChannel();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disableJumpToNextChannel);
            }
        } else if (position == disableGreetingStickerRow) {
            Config.toggleDisableGreetingSticker();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disableGreetingSticker);
            }
        } else if (position == disableTrendingStickerRow) {
            Config.toggleDisableTrendingSticker();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disableTrendingSticker);
            }
        } else if (position == customDoubleClickTapRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString("Disable", R.string.Disable));
            types.add(Defines.doubleTabNone);
            arrayList.add(LocaleController.getString("Reactions", R.string.Reactions));
            types.add(Defines.doubleTabReaction);
            arrayList.add(LocaleController.getString("Reply", R.string.Reply));
            types.add(Defines.doubleTabReply);
            arrayList.add(LocaleController.getString("Edit", R.string.Edit));
            types.add(Defines.doubleTabEdit);
            arrayList.add(LocaleController.getString("saveMessages", R.string.saveMessages));
            types.add(Defines.doubleTabSaveMessages);
            arrayList.add(LocaleController.getString("Repeat", R.string.Repeat));
            types.add(Defines.doubleTabRepeat);
            arrayList.add(LocaleController.getString("RepeatAsCopy", R.string.RepeatAsCopy));
            types.add(Defines.doubleTabRepeatAsCopy);
            arrayList.add(LocaleController.getString("Reverse", R.string.Reverse));
            types.add(Defines.doubleTabReverse);
            arrayList.add(LocaleController.getString("TranslateMessage", R.string.TranslateMessage));
            types.add(Defines.doubleTabTranslate);
            PopupBuilder.show(arrayList, LocaleController.getString("customDoubleTap", R.string.customDoubleTap), types.indexOf(Config.getDoubleTab()), getParentActivity(), view, i -> {
                Config.setDoubleTab(types.get(i));
                listAdapter.notifyItemChanged(customDoubleClickTapRow, PARTIAL);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            });
        } else if (position == confirmToSendMediaMessagesRow) {
            Config.toggleConfirmToSendMediaMessages();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.confirmToSendMediaMessages);
            }
        } else if (position == maxRecentStickerRow) {
            setMaxRecentSticker(view, position);
            listAdapter.notifyItemChanged(position, PARTIAL);
        } else if (position == unreadBadgeOnBackButtonRow) {
            Config.toggleUnreadBadgeOnBackButton();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.unreadBadgeOnBackButton);
            }
        } else if (position == ignoreReactionMentionRow) {
            Config.toggleIgnoreReactionMention();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.ignoreReactionMention);
            }
        } else if (position == showForwardDateRow) {
            Config.toggleDateOfForwardedMsg();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.dateOfForwardedMsg);
            }
        } else if (position == hideTimeForStickerRow) {
            Config.toggleHideTimeForSticker();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.hideTimeForSticker);
            }
        } else if (position == showMessageIDRow) {
            Config.toggleShowMessageID();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.showMessageID);
            }
        } else if (position == hideQuickSendMediaBottomRow) {
            Config.toggleHideQuickSendMediaBottom();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.hideQuickSendMediaBottom);
            }
        } else if (position == customQuickMessageRow) {
            setCustomQuickMessage();
            listAdapter.notifyItemChanged(position, PARTIAL);
        } else if (position == scrollableChatPreviewRow) {
            Config.toggleScrollableChatPreview();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.scrollableChatPreview);
            }
        } else if (position == showTabsOnForwardRow) {
            Config.toggleShowTabsOnForward();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.showTabsOnForward);
            }
        } else if (position == disableStickersAutoReorderRow) {
            Config.toggleDisableStickersAutoReorder();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disableStickersAutoReorder);
            }
        } else if (position == disablePreviewVideoSoundShortcutRow) {
            Config.toggleDisablePreviewVideoSoundShortcut();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disablePreviewVideoSoundShortcut);
            }
        } else if (position == quickToggleAnonymous) {
            Config.toggleQuickToggleAnonymous();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.quickToggleAnonymous);
            }

            AlertDialog restart = new AlertDialog(getContext(), 0);
            restart.setTitle(LocaleController.getString("AppName", R.string.AppName));
            restart.setMessage(LocaleController.getString("RestartAppToTakeEffect", R.string.RestartAppToTakeEffect));
            restart.setPositiveButton(LocaleController.getString("OK", R.string.OK), (__, ___) -> {
                ProcessPhoenix.triggerRebirth(getContext(), new Intent(getContext(), LaunchActivity.class));
            });
            restart.show();
        } else if (position == hideSendAsButtonRow) {
            Config.toggleHideSendAsButton();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.hideSendAsButton);
            }
        } else if (position == markdownParserRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add("Nnngram");
            arrayList.add("Telegram");
            boolean oldParser = Config.newMarkdownParser;
            PopupBuilder.show(arrayList, LocaleController.getString("MarkdownParser", R.string.MarkdownParser), Config.newMarkdownParser ? 0 : 1, getParentActivity(), view, i -> {
                Config.setNewMarkdownParser(i == 0);
                listAdapter.notifyItemChanged(markdownParserRow, PARTIAL);
                if (oldParser != Config.newMarkdownParser) {
                    if (oldParser) {
                        listAdapter.notifyItemRemoved(markdownParseLinksRow);
                        updateRows();
                    } else {
                        updateRows();
                        listAdapter.notifyItemInserted(markdownParseLinksRow);
                    }
                    listAdapter.notifyItemChanged(markdown2Row, PARTIAL);
                }
            });
        } else if (position == markdownParseLinksRow) {
            Config.toggleMarkdownParseLinks();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.markdownParseLinks);
            }
            listAdapter.notifyItemChanged(markdown2Row);
        } else if (position == markdownDisableRow) {
            Config.toggleMarkdownDisabled();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.markdownDisabled);
            }
        } else if (position == hideTitleRow) {
            Config.toggleShowHideTitle();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.showHideTitle);
            }
        } else if (position == messageFiltersRow) {
            createMessageFilterSetter(this, getContext(), resourcesProvider);
        } else if (position == sendLargePhotoRow) {
            Config.toggleSendLargePhoto();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.sendLargePhoto);
            }
        } else if (position == doNotUnarchiveBySwipeRow) {
            Config.toggleDoNotUnarchiveBySwipe();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.doNotUnarchiveBySwipe);
            }
        } else if (position == hideInputFieldBotButtonRow) {
            Config.toggleHideInputFieldBotButton();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.hideInputFieldBotButton);
            }
        } else if (position == hideMessageSeenTooltipRow) {
            Config.toggleHideMessageSeenTooltip();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.hideMessageSeenTooltip);
            }
        } else if (position == disableNotificationBubbleRow) {
            Config.toggleDisableNotificationBubble();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disableNotificationBubble);
            }
        } else if (position == showOnlineStatusRow) {
            Config.toggleShowOnlineStatus();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.showOnlineStatus);
            }
        } else if (position == disablePhotoSideActionRow) {
            Config.toggleDisablePhotoSideAction();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disablePhotoSideAction);
            }
        } else if (position == mergeMessageRow) {
            Config.toggleMergeMessage();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.mergeMessage);
            }
        } else if (position == filterZalgoRow) {
            Config.toggleFilterZalgo();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.filterZalgo);
            }
        } else if (position == hideKeyboardWhenScrollingRow) {
            Config.toggleHideKeyboardWhenScrolling();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.hideKeyboardWhenScrolling);
            }
        } else if (position == searchInPlaceRow) {
            Config.toggleSearchInPlace();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.searchInPlace);
            }
        } else if (position == disableChannelMuteButtonRow) {
            Config.toggleDisableChannelMuteButton();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disableChannelMuteButton);
            }
        } else if (position == disableAutoPipRow) {
            Config.toggleDisableAutoPip();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disableAutoPip);
            }
        } else if (position == sendMp4DocumentAsVideoRow) {
            Config.toggleSendMp4DocumentAsVideo();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.sendMp4DocumentAsVideo);
            }
        } else if (position == disableGravityDetectionInVideoRow) {
            Config.toggleDisableGravityDetectionInVideo();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.disableGravityDetectionInVideo);
            }
        } else if (position == autoMuteAfterJoiningChannelRow) {
            Config.toggleAutoMuteAfterJoiningChannel();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(Config.autoMuteAfterJoiningChannel);
            }
        }
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        return false;
    }

    @Override
    protected String getKey() {
        return "c";
    }


    @Override
    protected void updateRows() {
        super.updateRows();

        stickerSizeHeaderRow = addRow();
        stickerSizeRow = addRow("stickerSize");
        stickerSize2Row = addRow();
        gifSizeHeaderRow = addRow();
        gifSizeRow = addRow("gifSize");
        chatRow = addRow();
        ignoreBlockedUserMessagesRow = addRow("ignoreBlockedUserMessages");
        hideGroupStickerRow = addRow("hideGroupSticker");
        disablePremiumStickerRow = addRow("disablePremiumSticker");
        disableAddStickerFromImage = addRow("disableAddStickerFromImage");
        messageMenuRow = addRow();
        textStyleSettingsRow = addRow("textStyleSettings");
        if (Config.showHiddenSettings) {
            allowScreenshotOnNoForwardChatRow = addRow("allowScreenshotOnNoForwardChat");
        }
        labelChannelUserRow = addRow("labelChannelUser");
        displaySpoilerDirectlyRow = addRow("displaySpoilerDirectly");
        disableJumpToNextChannelRow = addRow("disableJumpToNextChannel");
        disableGreetingStickerRow = addRow("disableGreetingSticker");
        disableTrendingStickerRow = addRow("disableTrendingSticker");
        disablePreviewVideoSoundShortcutRow = addRow("disablePreviewVideoSoundShortcut");
        customDoubleClickTapRow = addRow("customDoubleClickTap");
        confirmToSendMediaMessagesRow = addRow("confirmToSendMediaMessages");
        maxRecentStickerRow = addRow("maxRecentSticker");
        unreadBadgeOnBackButtonRow = addRow("unreadBadgeOnBackButton");
        ignoreReactionMentionRow = addRow("ignoreReactionMention");
        showForwardDateRow = addRow("showForwardDate");
        hideTimeForStickerRow = addRow("hideTimeForSticker");
        showMessageIDRow = addRow("showMessageID");
        quickToggleAnonymous = addRow("quickToggleAnonymous");
        hideSendAsButtonRow = addRow("hideSendAsButton");
        hideQuickSendMediaBottomRow = addRow("hideQuickSendMediaBottom");
        customQuickMessageRow = addRow("customQuickMessage");
        scrollableChatPreviewRow = addRow("scrollableChatPreview");
        showTabsOnForwardRow = addRow("showTabsOnForward");
        disableStickersAutoReorderRow = addRow("disableStickersAutoReorder");
        hideTitleRow = addRow("showHideTitle");
        messageFiltersRow = addRow("messageFilters");
        sendLargePhotoRow = addRow("sendLargePhoto");
        doNotUnarchiveBySwipeRow = addRow("doNotUnarchiveBySwipe");
        hideInputFieldBotButtonRow = addRow("hideInputFieldBotButton");
        hideMessageSeenTooltipRow = addRow("hideMessageSeenTooltip");
        disableNotificationBubbleRow = addRow("disableNotificationBubble");
        showOnlineStatusRow = addRow("showOnlineStatus");
        disablePhotoSideActionRow = addRow("disablePhotoSideAction");
        mergeMessageRow = addRow("mergeMessage");
        filterZalgoRow = addRow("filterZalgo");
        hideKeyboardWhenScrollingRow = addRow("hideKeyboardWhenScrolling");
        searchInPlaceRow = addRow("searchInPlace");
        disableChannelMuteButtonRow = addRow("disableChannelMuteButton");
        disableAutoPipRow = addRow("disableAutoPip");
        sendMp4DocumentAsVideoRow = addRow("sendMp4DocumentAsVideo");
        disableGravityDetectionInVideoRow = addRow("disableGravityDetectionInVideo");
        autoMuteAfterJoiningChannelRow = addRow("autoMuteAfterJoiningChannel");
        chat2Row = addRow();

        markdownRow = addRow();
        markdownDisableRow = addRow("markdownDisabled");
        markdownParserRow = addRow("markdownParser");
        markdownParseLinksRow = Config.newMarkdownParser ? addRow("markdownParseLinks") : -1;
        markdown2Row = addRow();

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class ListAdapter extends BaseListAdapter {
        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean payload) {
            switch (holder.getItemViewType()) {
                case TYPE_SHADOW: {
                    if (position == chat2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == stickerSizeRow) {
                        textCell.setTextAndValue(LocaleController.getString("StickerSize", R.string.StickerSize),
                            String.valueOf(Math.round(ConfigManager.getFloatOrDefault(Defines.stickerSize, 14.0f))), payload, true);
                    } else if (position == gifSizeHeaderRow) {
                        textCell.setTextAndValue(LocaleController.getString("gifSize", R.string.gifSize),
                            String.valueOf(ConfigManager.getIntOrDefault(Defines.gifSize, 150)), payload, true);
                    } else if (position == chatRow) {
                        textCell.setText(LocaleController.getString("Chat", R.string.Chat), false);
                    } else if (position == messageMenuRow) {
                        textCell.setText(LocaleController.getString("MessageMenu", R.string.MessageMenu), false);
                    } else if (position == textStyleSettingsRow) {
                        textCell.setText(LocaleController.getString("TextStyleSettings", R.string.TextStyleSettings), false);
                    } else if (position == maxRecentStickerRow) {
                        textCell.setTextAndValue(LocaleController.getString("maxRecentSticker", R.string.maxRecentSticker), String.valueOf(Config.getMaxRecentSticker()), payload, true);
                    } else if (position == customDoubleClickTapRow) {
                        String value;
                        switch (Config.getDoubleTab()) {
                            case Defines.doubleTabNone:
                                value = LocaleController.getString("Disable", R.string.Disable);
                                break;
                            case Defines.doubleTabReaction:
                                value = LocaleController.getString("Reactions", R.string.Reactions);
                                break;
                            case Defines.doubleTabReply:
                                value = LocaleController.getString("Reply", R.string.Reply);
                                break;
                            case Defines.doubleTabEdit:
                                value = LocaleController.getString("Edit", R.string.Edit);
                                break;
                            case Defines.doubleTabSaveMessages:
                                value = LocaleController.getString("saveMessages", R.string.saveMessages);
                                break;
                            case Defines.doubleTabRepeat:
                                value = LocaleController.getString("Repeat", R.string.Repeat);
                                break;
                            case Defines.doubleTabRepeatAsCopy:
                                value = LocaleController.getString("RepeatAsCopy", R.string.RepeatAsCopy);
                                break;
                            case Defines.doubleTabReverse:
                                value = LocaleController.getString("Reverse", R.string.Reverse);
                                break;
                            case Defines.doubleTabTranslate:
                                value = LocaleController.getString("TranslateMessage", R.string.TranslateMessage);
                                break;
                            default:
                                value = LocaleController.getString("Reactions", R.string.Reactions);
                        }
                        textCell.setTextAndValue(LocaleController.getString("customDoubleTap", R.string.customDoubleTap), value, payload, true);
                    } else if (position == customQuickMessageRow) {
                        textCell.setText(LocaleController.getString("setCustomQuickMessage", R.string.setCustomQuickMessage), true);
                    } else if (position == markdownParserRow) {
                        textCell.setTextAndValue(LocaleController.getString("MarkdownParser", R.string.MarkdownParser), Config.newMarkdownParser ? "Nnngram" : "Telegram", payload,
                            position + 1 != markdown2Row);
                    } else if (position == messageFiltersRow) {
                        textCell.setText(LocaleController.getString("MessageFilter", R.string.MessageFilter), payload);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == ignoreBlockedUserMessagesRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ignoreBlockedUser", R.string.ignoreBlockedUser), Config.ignoreBlockedUser, true);
                    } else if (position == hideGroupStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("hideGroupSticker", R.string.hideGroupSticker), Config.hideGroupSticker, true);
                    } else if (position == disablePremiumStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disablePremiumSticker", R.string.disablePremiumSticker), Config.disablePremiumSticker, true);
                    } else if (position == disableAddStickerFromImage) {
                        textCell.setTextAndCheck(LocaleController.getString("disableAddStickerFromImage", R.string.disableAddStickerFromImage), Config.disableAddStickerFromImage, true);
                    } else if (position == allowScreenshotOnNoForwardChatRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("allowScreenshotOnNoForwardChat", R.string.allowScreenshotOnNoForwardChat), LocaleController.getString("allowScreenshotOnNoForwardChatWarning", R.string.allowScreenshotOnNoForwardChatWarning), Config.allowScreenshotOnNoForwardChat, true, true);
                    } else if (position == labelChannelUserRow) {
                        if (Config.channelAlias) {
                            textCell.setEnabled(false, null);
                        }
                        textCell.setTextAndValueAndCheck(LocaleController.getString("labelChannelUser", R.string.labelChannelUser), LocaleController.getString("labelChannelUser", R.string.labelChannelUserDetails), Config.labelChannelUser, true, true);
                    } else if (position == displaySpoilerDirectlyRow) {
                        textCell.setTextAndCheck(LocaleController.getString("displaySpoilerDirectly", R.string.displaySpoilerDirectly), Config.displaySpoilerMsgDirectly, true);
                    } else if (position == disableJumpToNextChannelRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableJumpToNextChannel", R.string.disableJumpToNextChannel), Config.disableJumpToNextChannel, true);
                    } else if (position == disableGreetingStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableGreetingSticker", R.string.disableGreetingSticker), Config.disableGreetingSticker, true);
                    } else if (position == disableTrendingStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableTrendingSticker", R.string.disableTrendingSticker), Config.disableTrendingSticker, true);
                    } else if (position == confirmToSendMediaMessagesRow) {
                        textCell.setTextAndCheck(LocaleController.getString("confirmToSendMediaMessages", R.string.confirmToSendMediaMessages), Config.confirmToSendMediaMessages, true);
                    } else if (position == unreadBadgeOnBackButtonRow) {
                        textCell.setTextAndCheck(LocaleController.getString("unreadBadgeOnBackButton", R.string.unreadBadgeOnBackButton), Config.unreadBadgeOnBackButton, true);
                    } else if (position == ignoreReactionMentionRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("ignoreReactionMention", R.string.ignoreReactionMention), LocaleController.getString("ignoreReactionMentionInfo", R.string.ignoreReactionMentionInfo), Config.ignoreReactionMention, true, true);
                    } else if (position == showForwardDateRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showForwardDate", R.string.showForwardDate), Config.dateOfForwardedMsg, true);
                    } else if (position == hideTimeForStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showForwardName", R.string.hideTimeForSticker), Config.hideTimeForSticker, true);
                    } else if (position == showMessageIDRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showMessageID", R.string.showMessageID), Config.showMessageID, true);
                    } else if (position == hideQuickSendMediaBottomRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableQuickSendMediaBottom", R.string.DisableQuickSendMediaBottom),
                            Config.hideQuickSendMediaBottom, true);
                    } else if (position == scrollableChatPreviewRow) {
                        textCell.setTextAndCheck(LocaleController.getString("scrollableChatPreview", R.string.scrollableChatPreview), Config.scrollableChatPreview, true);
                    } else if (position == showTabsOnForwardRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showTabsOnForward", R.string.showTabsOnForward), Config.showTabsOnForward, true);
                    } else if (position == disablePreviewVideoSoundShortcutRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("disablePreviewVideoSoundShortcut", R.string.disablePreviewVideoSoundShortcut), LocaleController.getString("disablePreviewVideoSoundShortcutNotice", R.string.disablePreviewVideoSoundShortcutNotice), Config.disablePreviewVideoSoundShortcut, true, true);
                    } else if (position == quickToggleAnonymous) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("quickToggleAnonymous", R.string.quickToggleAnonymous), LocaleController.getString("quickToggleAnonymousNotice", R.string.quickToggleAnonymousNotice), Config.quickToggleAnonymous, true, true);
                    } else if (position == hideSendAsButtonRow) {
                        textCell.setTextAndCheck(LocaleController.getString("hideSendAsButton", R.string.hideSendAsButton),  Config.hideSendAsButton, true);
                    } else if (position == disableStickersAutoReorderRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableStickersAutoReorder", R.string.disableStickersAutoReorder),
                            Config.disableStickersAutoReorder, true);
                    } else if (position == markdownParseLinksRow) {
                        textCell.setTextAndCheck(LocaleController.getString("MarkdownParseLinks", R.string.MarkdownParseLinks), Config.markdownParseLinks, false);
                    } else if (position == markdownDisableRow) {
                        textCell.setTextAndCheck(LocaleController.getString("MarkdownDisableByDefault", R.string.MarkdownDisableByDefault), Config.markdownDisabled, true);
                    } else if (position == hideTitleRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showHideTitle", R.string.showHideTitle), Config.showHideTitle, true);
                    } else if (position == sendLargePhotoRow) {
                        textCell.setTextAndCheck(LocaleController.getString("sendLargePhoto", R.string.sendLargePhoto), Config.sendLargePhoto, true);
                    } else if (position == doNotUnarchiveBySwipeRow) {
                        textCell.setTextAndCheck(LocaleController.getString("doNotUnarchiveBySwipe", R.string.doNotUnarchiveBySwipe), Config.doNotUnarchiveBySwipe, true);
                    } else if (position == hideInputFieldBotButtonRow) {
                        textCell.setTextAndCheck(LocaleController.getString("hideInputFieldBotButton", R.string.hideInputFieldBotButton), Config.hideInputFieldBotButton, true);
                    } else if (position == hideMessageSeenTooltipRow) {
                        textCell.setTextAndCheck(LocaleController.getString("hideMessageSeenTooltip", R.string.hideMessageSeenTooltip), Config.hideMessageSeenTooltip, true);
                    } else if (position == disableNotificationBubbleRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableNotificationBubble", R.string.disableNotificationBubble), Config.disableNotificationBubble, true);
                    } else if (position == showOnlineStatusRow) {
                        textCell.setTextAndCheck(LocaleController.getString("showOnlineStatus", R.string.showOnlineStatus), Config.showOnlineStatus, true);
                    } else if (position == disablePhotoSideActionRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disablePhotoSideAction", R.string.disablePhotoSideAction), Config.disablePhotoSideAction, true);
                    } else if (position == mergeMessageRow) {
                        textCell.setTextAndCheck(LocaleController.getString("MergeMessage", R.string.MergeMessage), Config.mergeMessage, true);
                    } else if (position == filterZalgoRow) {
                        textCell.setTextAndCheck(LocaleController.getString("filterZalgo", R.string.filterZalgo), Config.filterZalgo, true);
                    } else if (position == hideKeyboardWhenScrollingRow) {
                        textCell.setTextAndCheck(LocaleController.getString("hideKeyboardWhenScrolling", R.string.hideKeyboardWhenScrolling), Config.hideKeyboardWhenScrolling, true);
                    } else if (position == searchInPlaceRow) {
                        textCell.setTextAndCheck(LocaleController.getString("searchInPlace", R.string.searchInPlace), Config.searchInPlace, true);
                    } else if (position == disableChannelMuteButtonRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableChannelMuteButton", R.string.disableChannelMuteButton), Config.disableChannelMuteButton, true);
                    } else if (position == disableAutoPipRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableAutoPip", R.string.disableAutoPip), Config.disableAutoPip, true);
                    } else if (position == sendMp4DocumentAsVideoRow) {
                        textCell.setTextAndCheck(LocaleController.getString("sendMp4DocumentAsVideo", R.string.sendMp4DocumentAsVideo), Config.sendMp4DocumentAsVideo, true);
                    } else if (position == disableGravityDetectionInVideoRow) {
                        textCell.setTextAndCheck(LocaleController.getString("disableGravityDetectionInVideo", R.string.disableGravityDetectionInVideo), Config.disableGravityDetectionInVideo, true);
                    } else if (position == autoMuteAfterJoiningChannelRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.autoMuteAfterJoiningChannel), Config.autoMuteAfterJoiningChannel, true);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == chatRow) {
                        headerCell.setText(LocaleController.getString("Chat", R.string.Chat));
                    } else if (position == stickerSizeHeaderRow) {
                        headerCell.setText(LocaleController.getString("StickerSize", R.string.StickerSize));
                    } else if (position == gifSizeHeaderRow) {
                        headerCell.setText(LocaleController.getString("gifSize", R.string.gifSize));
                    } else if (position == markdownRow) {
                        headerCell.setText(LocaleController.getString("Markdown", R.string.Markdown));
                    }
                    break;
                }
                case TYPE_NOTIFICATION_CHECK: {
                    NotificationsCheckCell textCell = (NotificationsCheckCell) holder.itemView;
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == markdown2Row) {
                        cell.getTextView().setMovementMethod(null);
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(TextUtils.expandTemplate(EntitiesHelper.parseMarkdown(Config.newMarkdownParser && Config.markdownParseLinks ?
                                LocaleController.getString("MarkdownAbout", R.string.MarkdownAbout) : LocaleController.getString("MarkdownAbout2", R.string.MarkdownAbout2)), "**", "__", "~~", "`", "||", "[", "](", ")"));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 3 || type == 6 || type == 5;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case TYPE_SHADOW:
                    view = new ShadowSectionCell(mContext);
                    break;
                case TYPE_SETTINGS:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_CHECK:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_HEADER:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_NOTIFICATION_CHECK:
                    view = new NotificationsCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_DETAIL_SETTINGS:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_INFO_PRIVACY:
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case TYPE_STICKER_SIZE:
                    view = stickerSizeCell = new StickerSizeCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case TYPE_GIF_SIZE:
                    view = gifSizeCell = new GifSizeCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            // noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == chat2Row || position == stickerSize2Row) {
                return TYPE_SHADOW;
            } else if (position == messageMenuRow || position == customDoubleClickTapRow || position == maxRecentStickerRow || position == customQuickMessageRow || position == markdownParserRow
                || position == messageFiltersRow || position == textStyleSettingsRow) {
                return TYPE_SETTINGS;
            } else if (position == chatRow || position == stickerSizeHeaderRow || position == markdownRow || position == gifSizeHeaderRow) {
                return TYPE_HEADER;
            } else if (position == stickerSizeRow) {
                return TYPE_STICKER_SIZE;
            } else if (position == gifSizeRow) {
                return TYPE_GIF_SIZE;
            } else if ((position > chatRow && position < chat2Row) || (position > markdownRow && position < markdown2Row) || (position > stickerSizeRow && position < stickerSize2Row)) {
                return TYPE_CHECK;
            } else if (position == markdown2Row) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_CHECK;
        }
    }

    private void showMessageMenuAlert() {
        if (getParentActivity() == null) {
            return;
        }
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("MessageMenu", R.string.MessageMenu));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout linearLayoutInviteContainer = new LinearLayout(context);
        linearLayoutInviteContainer.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(linearLayoutInviteContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int count = 9 + 2;
        for (int a = 0; a < count; a++) {
            TextCheckCell textCell = new TextCheckCell(context);
            switch (a) {
                case 0: {
                    textCell.setTextAndCheck(LocaleController.getString("DeleteDownloadedFile", R.string.DeleteDownloadedFile), ConfigManager.getBooleanOrFalse(Defines.showDeleteDownloadFiles), false);
                    break;
                }
                case 1: {
                    textCell.setTextAndCheck(LocaleController.getString("NoQuoteForward", R.string.NoQuoteForward), Config.showNoQuoteForward, false);
                    break;
                }
                case 2: {
                    textCell.setTextAndCheck(LocaleController.getString("saveMessages", R.string.saveMessages), Config.showSaveMessages, false);
                    break;
                }
                case 3: {
                    textCell.setTextAndCheck(LocaleController.getString("Repeat", R.string.Repeat), Config.showRepeat, false);
                    break;
                }
                case 4: {
                    textCell.setTextAndCheck(LocaleController.getString("RepeatAsCopy", R.string.RepeatAsCopy), Config.showRepeatAsCopy, false);
                    break;
                }
                case 4 + 1: {
                    textCell.setTextAndCheck(LocaleController.getString("Reverse", R.string.Reverse), Config.showReverse, false);
                    break;
                }
                case 4 + 1 + 1: {
                    textCell.setTextAndCheck(LocaleController.getString("ViewHistory", R.string.ViewHistory), Config.showViewHistory, false);
                    break;
                }
                case 5+ 1 + 1: {
                    textCell.setTextAndCheck(LocaleController.getString("MessageDetails", R.string.MessageDetails), Config.showMessagesDetail, false);
                    break;
                }
                case 6+ 1 + 1: {
                    textCell.setTextAndCheck(LocaleController.getString("CopyPhoto", R.string.CopyPhoto), Config.showCopyPhoto, false);
                    break;
                }
                case 7+ 1 + 1: {
                    textCell.setTextAndCheck(LocaleController.getString("Reactions", R.string.Reactions), Config.showReactions, false);
                    break;
                }
                case 8+ 1 + 1: {
                    textCell.setTextAndCheck(LocaleController.getString("ReportChat", R.string.ReportChat), Config.showReport, false);
                }
            }
            textCell.setTag(a);
            textCell.setBackground(Theme.getSelectorDrawable(false));
            linearLayoutInviteContainer.addView(textCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            textCell.setOnClickListener(v2 -> {
                Integer tag = (Integer) v2.getTag();
                switch (tag) {
                    case 0: {
                        Config.toggleShowDeleteDownloadFiles();
                        textCell.setChecked(Config.showDeleteDownloadFiles);
                        break;
                    }
                    case 1: {
                        Config.toggleShowNoQuoteForward();
                        textCell.setChecked(Config.showNoQuoteForward);
                        break;
                    }
                    case 2: {
                        Config.toggleShowSaveMessages();
                        textCell.setChecked(Config.showSaveMessages);
                        break;
                    }
                    case 3: {
                        Config.toggleShowRepeat();
                        textCell.setChecked(Config.showRepeat);
                        break;
                    }
                    case 4: {
                        Config.toggleShowRepeatAsCopy();
                        textCell.setChecked(Config.showRepeatAsCopy);
                        break;
                    }
                    case 4+ 1: {
                        Config.toggleShowReverse();
                        textCell.setChecked(Config.showReverse);
                        break;
                    }
                    case 4 + 1 + 1: {
                        Config.toggleShowViewHistory();
                        textCell.setChecked(Config.showViewHistory);
                        break;
                    }
                    case 5 + 1 + 1: {
                        Config.toggleShowMessagesDetail();
                        textCell.setChecked(Config.showMessagesDetail);
                        break;
                    }
                    case 6 + 1 + 1: {
                        Config.toggleShowCopyPhoto();
                        textCell.setChecked(Config.showCopyPhoto);
                        break;
                    }
                    case 7 + 1 + 1: {
                        Config.toggleShowReactions();
                        textCell.setChecked(Config.showReactions);
                        break;
                    }
                    case 8 + 1 + 1: {
                        Config.toggleShowReport();
                        textCell.setChecked(Config.showReport);
                        break;
                    }
                }
            });
        }
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        builder.setView(linearLayout);
        showDialog(builder.create());
    }

    private void showTextStyleSettingsAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("TextStyleSettings", R.string.TextStyleSettings));

        RecyclerView recyclerView = new RecyclerView(getParentActivity());
        recyclerView.setLayoutManager(new LinearLayoutManager(getParentActivity()));
        recyclerView.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);

        SortListAdapter adapter = new TextStyleListAdapter();
        recyclerView.setAdapter(adapter);

        ItemTouchHelperCallback itemTouchHelperCallback = new ItemTouchHelperCallback(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        builder.setNeutralButton(LocaleController.getString("Default", R.string.Default), (dialog, which) -> adapter.reset());

        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        builder.setView(recyclerView);
        showDialog(builder.create());
    }

    @SuppressLint("SetTextI18n")
    private void setMaxRecentSticker(View view, int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("setMaxRecentSticker", R.string.setMaxRecentSticker));

        final EditTextBoldCursor editText = new EditTextBoldCursor(getParentActivity()) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
            }
        };
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editText.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        editText.setHintText(LocaleController.getString("Number", R.string.Number));
        editText.setText(Config.getMaxRecentSticker() + "");
        editText.setHeaderHintColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        editText.setSingleLine(true);
        editText.setFocusable(true);
        editText.setTransformHintToHeader(true);
        editText.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setBackgroundDrawable(null);
        editText.requestFocus();
        editText.setPadding(0, 0, 0, 0);
        builder.setView(editText);

        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
            if (editText.getText().toString().trim().equals("")) {
                Config.setMaxRecentSticker(20);
            } else {
                if (!InlinesKt.isNumber(editText.getText().toString())) {
                    AndroidUtilities.shakeView(view);
                    AlertUtil.showToast(LocaleController.getString("notANumber", R.string.notANumber));
                } else {
                    final int targetNum = Integer.parseInt(editText.getText().toString().trim());
                    if (targetNum > 200 || targetNum < 20)
                        AlertUtil.showToast(LocaleController.getString("numberInvalid", R.string.numberInvalid));
                    else
                        Config.setMaxRecentSticker(Integer.parseInt(editText.getText().toString()));
                }
            }
            listAdapter.notifyItemChanged(pos, PARTIAL);
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show().setOnShowListener(dialog -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        });
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) editText.getLayoutParams();
        if (layoutParams != null) {
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) layoutParams).gravity = Gravity.CENTER_HORIZONTAL;
            }
            layoutParams.rightMargin = layoutParams.leftMargin = AndroidUtilities.dp(24);
            layoutParams.height = AndroidUtilities.dp(36);
            editText.setLayoutParams(layoutParams);
        }
        editText.setSelection(0, editText.getText().length());
    }

    @SuppressLint("SetTextI18n")
    private void setCustomQuickMessage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("setCustomQuickMessage", R.string.setCustomQuickMessage));

        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditTextBoldCursor setDisplayNameEditText = new EditTextBoldCursor(getParentActivity()) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
            }
        };

        setDisplayNameEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        setDisplayNameEditText.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        setDisplayNameEditText.setHintText(LocaleController.getString("Name", R.string.Name));
        setDisplayNameEditText.setText(ConfigManager.getStringOrDefault(Defines.customQuickMessageDisplayName, ""));
        setDisplayNameEditText.setHeaderHintColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        setDisplayNameEditText.setSingleLine(true);
        setDisplayNameEditText.setFocusable(true);
        setDisplayNameEditText.setTransformHintToHeader(true);
        setDisplayNameEditText.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        setDisplayNameEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        setDisplayNameEditText.setBackgroundDrawable(null);
        setDisplayNameEditText.setPadding(AndroidUtilities.dp(36), AndroidUtilities.dp(16), AndroidUtilities.dp(36), AndroidUtilities.dp(16));
        layout.addView(setDisplayNameEditText);

        final EditTextBoldCursor setMessageEditText = new EditTextBoldCursor(getParentActivity()) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
            }
        };
        setMessageEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        setMessageEditText.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        setMessageEditText.setHintText(LocaleController.getString("Message", R.string.Message));
        setMessageEditText.setText(ConfigManager.getStringOrDefault(Defines.customQuickMessage, ""));
        setMessageEditText.setHeaderHintColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader));
        setMessageEditText.setSingleLine(false);
        setMessageEditText.setFocusable(true);
        setMessageEditText.setTransformHintToHeader(true);
        setMessageEditText.setLineColors(getThemedColor(Theme.key_windowBackgroundWhiteInputField), getThemedColor(Theme.key_windowBackgroundWhiteInputFieldActivated), getThemedColor(Theme.key_text_RedRegular));
        setMessageEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        setMessageEditText.setBackgroundDrawable(null);
        setMessageEditText.setPadding(AndroidUtilities.dp(36), AndroidUtilities.dp(16), AndroidUtilities.dp(36), AndroidUtilities.dp(16));
        layout.addView(setMessageEditText);

        CheckBoxCell cell = new CheckBoxCell(getParentActivity(), 1);
        cell.setBackground(Theme.getSelectorDrawable(false));
        cell.setText(LocaleController.getString("SendAsReply", R.string.SendAsReply), "", Config.customQuickMsgSAR, false);
        cell.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(16) : AndroidUtilities.dp(8), 0, LocaleController.isRTL ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16), 0);
        cell.setOnClickListener(v -> {
            CheckBoxCell cell1 = (CheckBoxCell) v;
            cell1.setChecked(!cell1.isChecked(), true);
        });
        layout.addView(cell);

        builder.setView(layout);


        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
            if (StringUtils.isBlank(setDisplayNameEditText.getText().toString())|| StringUtils.isBlank(setMessageEditText.getText().toString())) {
                AlertUtil.showToast(LocaleController.getString("emptyInput", R.string.emptyInput));
            } else {
                ConfigManager.putString(Defines.customQuickMessageDisplayName, setDisplayNameEditText.getText().toString());
                ConfigManager.putString(Defines.customQuickMessage, setMessageEditText.getText().toString());
                Config.setCustomQuickMessageEnabled(true);
                Config.setCustomQuickMsgSAR(true);
            }
        });


        builder.setNeutralButton(LocaleController.getString("Reset", R.string.Reset), (dialogInterface, i) -> {
            ConfigManager.deleteValue(Defines.customQuickMessage);
            ConfigManager.deleteValue(Defines.customQuickMessageDisplayName);
            Config.setCustomQuickMessageEnabled(false);
            Config.setCustomQuickMsgSAR(false);
        });

        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show().setOnShowListener(dialog -> {
            setDisplayNameEditText.requestFocus();
            AndroidUtilities.showKeyboard(setDisplayNameEditText);
        });

        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) setDisplayNameEditText.getLayoutParams();
        if (layoutParams != null) {
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) layoutParams).gravity = Gravity.CENTER_HORIZONTAL;
            }
            layoutParams.rightMargin = layoutParams.leftMargin = AndroidUtilities.dp(24);
            layoutParams.height = AndroidUtilities.dp(36);
            setDisplayNameEditText.setLayoutParams(layoutParams);
        }
        setDisplayNameEditText.setSelection(0, setDisplayNameEditText.getText().length());

        layoutParams = (ViewGroup.MarginLayoutParams) setMessageEditText.getLayoutParams();
        if (layoutParams != null) {
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                ((FrameLayout.LayoutParams) layoutParams).gravity = Gravity.CENTER_HORIZONTAL;
            }
            layoutParams.rightMargin = layoutParams.leftMargin = AndroidUtilities.dp(24);
            layoutParams.height = AndroidUtilities.dp(36);
            setDisplayNameEditText.setLayoutParams(layoutParams);
        }
        setMessageEditText.setSelection(0, setMessageEditText.getText().length());
    }

    private class StickerSizeCell extends FrameLayout {

        private final StickerSizePreviewMessagesCell messagesCell;
        private final SeekBarView sizeBar;
        private final int startStickerSize = 2;
        private final int endStickerSize = 20;

        private final TextPaint textPaint;
        private int lastWidth;

        public StickerSizeCell(Context context) {
            super(context);

            setWillNotDraw(false);

            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(AndroidUtilities.dp(16));

            sizeBar = new SeekBarView(context);
            sizeBar.setReportChanges(true);
            sizeBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    sizeBar.getSeekBarAccessibilityDelegate().postAccessibilityEventRunnable(StickerSizeCell.this);
                    ConfigManager.putFloat(Defines.stickerSize, startStickerSize + (endStickerSize - startStickerSize) * progress);
                    StickerSizeCell.this.invalidate();
                    if (resetItem.getVisibility() != VISIBLE) {
                        AndroidUtilities.updateViewVisibilityAnimated(resetItem, true, 0.5f, true);
                    }
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {

                }
            });
            sizeBar.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 9, 5, 43, 11));

            messagesCell = new StickerSizePreviewMessagesCell(context, parentLayout, resourcesProvider);
            messagesCell.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            addView(messagesCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 53, 0, 0));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            canvas.drawText(String.valueOf(Math.round(ConfigManager.getFloatOrDefault(Defines.stickerSize, 14.0f))), getMeasuredWidth()- AndroidUtilities.dp(39), AndroidUtilities.dp(28), textPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int width = MeasureSpec.getSize(widthMeasureSpec);
            if (lastWidth != width) {
                sizeBar.setProgress((ConfigManager.getFloatOrDefault(Defines.stickerSize, 14.0f) - startStickerSize)/ (float) (endStickerSize - startStickerSize));
                lastWidth = width;
            }
        }

        @Override
        public void invalidate() {
            super.invalidate();
            lastWidth = -1;
            messagesCell.invalidate();
            sizeBar.invalidate();
        }

        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityEvent(this, event);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityNodeInfoInternal(this, info);
        }

        @Override
        public boolean performAccessibilityAction(int action, Bundle arguments) {
            return super.performAccessibilityAction(action, arguments) || sizeBar.getSeekBarAccessibilityDelegate().performAccessibilityActionInternal(this, action, arguments);
        }
    }
    
    public class GifSizeCell extends FrameLayout {
        
        private final SeekBarView sizeBar;
        private final int startGifSize = 50;
        private final int endGifSize = 100;
        
        private final TextPaint textPaint;
        private int lastWidth;
        
        public GifSizeCell(Context context) {
            super(context);
            
            setWillNotDraw(false);
            
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(AndroidUtilities.dp(16));
            
            sizeBar = new SeekBarView(context);
            sizeBar.setReportChanges(true);
            sizeBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    sizeBar.getSeekBarAccessibilityDelegate().postAccessibilityEventRunnable(GifSizeCell.this);
                    ConfigManager.putInt(Defines.gifSize, (int) (startGifSize + (endGifSize - startGifSize) * progress));
                    GifSizeCell.this.invalidate();
                }
                
                @Override
                public void onSeekBarPressed(boolean pressed) {
                
                }
            });
            sizeBar.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 9, 5, 43, 11));
        }
        
        @Override
        protected void onDraw(Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            canvas.drawText(ConfigManager.getIntOrDefault(Defines.gifSize, 100) + "%", getMeasuredWidth() - AndroidUtilities.dp(45), AndroidUtilities.dp(28), textPaint);
        }
        
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int width = MeasureSpec.getSize(widthMeasureSpec);
            if (lastWidth != width) {
                sizeBar.setProgress((ConfigManager.getIntOrDefault(Defines.gifSize, 100) - startGifSize) / (float) (endGifSize - startGifSize));
                lastWidth = width;
            }
        }
        
        @Override
        public void invalidate() {
            super.invalidate();
            lastWidth = -1;
            sizeBar.invalidate();
        }
        
        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityEvent(this, event);
        }
        
        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityNodeInfoInternal(this, info);
        }
        
        @Override
        public boolean performAccessibilityAction(int action, Bundle arguments) {
            return super.performAccessibilityAction(action, arguments) || sizeBar.getSeekBarAccessibilityDelegate().performAccessibilityActionInternal(this, action, arguments);
        }
    }
}
