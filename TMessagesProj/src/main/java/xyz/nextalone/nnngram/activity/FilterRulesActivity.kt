/*
 * Copyright (C) 2019-2026 qwq233 <qwq233@qwq2333.top>
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
package xyz.nextalone.nnngram.activity

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import org.json.JSONArray
import androidx.recyclerview.widget.RecyclerView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBarMenu
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Cells.TextSettingsCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper
import xyz.nextalone.nnngram.helpers.MessageFilterRules
import java.util.UUID
import java.util.regex.Pattern

@SuppressLint("NotifyDataSetChanged")
class FilterRulesActivity : BaseActivity() {

    private var rules: MutableList<MessageFilterRules.Rule> = mutableListOf()

    private var headerRow = -1
    private var addRow = -1
    private var rulesStartRow = -1
    private var rulesEndRow = -1
    private var hintRow = -1

    override fun getActionBarTitle(): String =
        LocaleController.getString("FilterRulesTitle", R.string.FilterRulesTitle)

    override fun getKey(): String? = null

    override fun createAdapter(context: Context): BaseListAdapter = ListAdapter(context)

    override fun onFragmentCreate(): Boolean {
        rules = MessageFilterRules.loadRaw().toMutableList()
        return super.onFragmentCreate()
    }

    override fun createView(context: Context): View {
        val view = super.createView(context)
        val menu: ActionBarMenu = actionBar.createMenu()
        menu.addItem(1, R.drawable.msg_add).setOnClickListener { showEditDialog(null) }
        menu.addItem(2, R.drawable.ic_ab_other).setOnClickListener { showImportExportMenu() }
        return view
    }

    private fun showImportExportMenu() {
        val context = parentActivity ?: return
        val exportLabel = LocaleController.getString("FilterRulesExport", R.string.FilterRulesExport)
        val importLabel = LocaleController.getString("FilterRulesImport", R.string.FilterRulesImport)
        AlertDialog.Builder(context, resourcesProvider)
            .setItems(arrayOf<CharSequence>(exportLabel, importLabel)) { _, which ->
                if (which == 0) exportRules() else showImportDialog()
            }.show()
    }

    private fun exportRules() {
        val context = parentActivity ?: return
        val json = JSONArray().apply { rules.forEach { put(it.toJson()) } }.toString(2)
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Nnngram filter rules", json))
        org.telegram.ui.Components.BulletinFactory.of(this)
            .createSimpleBulletin(R.raw.copy, LocaleController.getString("FilterRulesExportCopied", R.string.FilterRulesExportCopied))
            .show()
    }

    private fun showImportDialog() {
        val context = parentActivity ?: return
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AndroidUtilities.dp(24f), AndroidUtilities.dp(8f), AndroidUtilities.dp(24f), 0)
        }
        val edit = EditTextBoldCursor(context).apply {
            background = null
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
            setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider))
            setCursorColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider))
            setHintColor(Theme.getColor(Theme.key_dialogTextHint, resourcesProvider))
            setHintText(LocaleController.getString("FilterRulesImportHint", R.string.FilterRulesImportHint))
            setLineColors(
                Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider),
                Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider),
                Theme.getColor(Theme.key_text_RedRegular, resourcesProvider),
            )
            maxLines = 12
            setHorizontallyScrolling(false)
        }
        container.addView(edit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT))

        AlertDialog.Builder(context, resourcesProvider)
            .setTitle(LocaleController.getString("FilterRulesImport", R.string.FilterRulesImport))
            .setView(container)
            .setPositiveButton(LocaleController.getString("Save", R.string.Save)) { _, _ ->
                val json = edit.text?.toString()?.trim().orEmpty()
                val parsed = runCatching {
                    val arr = JSONArray(json)
                    (0 until arr.length()).map { MessageFilterRules.Rule.fromJson(arr.getJSONObject(it)) }
                }.getOrNull()
                if (parsed == null) {
                    org.telegram.ui.Components.BulletinFactory.of(this@FilterRulesActivity)
                        .createErrorBulletin(LocaleController.getString("FilterRulesImportFailed", R.string.FilterRulesImportFailed))
                        .show()
                } else {
                    rules = parsed.toMutableList()
                    MessageFilterRules.save(rules)
                    updateRows()
                }
            }
            .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
            .show()
    }

    override fun updateRows() {
        super.updateRows()
        headerRow = addRow()
        addRow = addRow()
        rulesStartRow = rowCount
        rowCount += rules.size
        rulesEndRow = rowCount
        hintRow = addRow()
        listAdapter?.notifyDataSetChanged()
    }

    override fun onItemClick(view: View, position: Int, x: Float, y: Float) {
        if (position == addRow) {
            showEditDialog(null)
        } else if (position in rulesStartRow until rulesEndRow) {
            showEditDialog(position - rulesStartRow)
        }
    }

    override fun onItemLongClick(view: View, position: Int, x: Float, y: Float): Boolean {
        if (position in rulesStartRow until rulesEndRow) {
            val idx = position - rulesStartRow
            val rule = rules[idx]
            val toggleLabel = if (rule.enabled)
                LocaleController.getString("FilterRulesDisable", R.string.FilterRulesDisable)
            else LocaleController.getString("FilterRulesEnable", R.string.FilterRulesEnable)
            val deleteLabel = LocaleController.getString("Delete", R.string.Delete)
            val upLabel = LocaleController.getString("FilterRulesMoveUp", R.string.FilterRulesMoveUp)
            val downLabel = LocaleController.getString("FilterRulesMoveDown", R.string.FilterRulesMoveDown)
            // 动态构建: 第一条隐藏"上移", 最后一条隐藏"下移"
            val labels = mutableListOf<CharSequence>()
            val actions = mutableListOf<() -> Unit>()
            if (idx > 0) {
                labels.add(upLabel)
                actions.add {
                    val tmp = rules[idx]; rules[idx] = rules[idx - 1]; rules[idx - 1] = tmp
                    MessageFilterRules.save(rules)
                    updateRows()
                }
            }
            if (idx < rules.size - 1) {
                labels.add(downLabel)
                actions.add {
                    val tmp = rules[idx]; rules[idx] = rules[idx + 1]; rules[idx + 1] = tmp
                    MessageFilterRules.save(rules)
                    updateRows()
                }
            }
            labels.add(toggleLabel)
            actions.add {
                rules[idx] = rule.copy(enabled = !rule.enabled)
                MessageFilterRules.save(rules)
                listAdapter?.notifyItemChanged(position)
            }
            labels.add(deleteLabel)
            actions.add {
                AlertDialog.Builder(parentActivity, resourcesProvider).apply {
                    setTitle(LocaleController.getString("FilterRulesDeleteTitle", R.string.FilterRulesDeleteTitle))
                    setMessage(LocaleController.getString("FilterRulesDeleteConfirm", R.string.FilterRulesDeleteConfirm))
                    setPositiveButton(deleteLabel) { _, _ ->
                        rules.removeAt(idx)
                        MessageFilterRules.save(rules)
                        updateRows()
                    }
                    setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                }.show()
            }
            AlertDialog.Builder(parentActivity, resourcesProvider)
                .setItems(labels.toTypedArray()) { _, which -> actions[which].invoke() }
                .show()
            return true
        }
        return false
    }

    /** 显示新增/编辑规则对话框. editIndex=null 表示新增. */
    /**
     * Plain framework CompoundButtons take text/tint colors from the system theme, which does not
     * follow the Telegram theme of the dialog background (e.g. dark text on a dark dialog under a
     * light system / Monet palette). Apply the dialog theme colors explicitly.
     */
    private fun CompoundButton.applyDialogTheme(checkedKey: Int, uncheckedKey: Int) {
        setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider))
        buttonTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(
                Theme.getColor(checkedKey, resourcesProvider),
                Theme.getColor(uncheckedKey, resourcesProvider),
            ),
        )
    }

    private fun showEditDialog(editIndex: Int?) {
        val context = parentActivity ?: return
        val editing = editIndex?.let { rules[it] }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(AndroidUtilities.dp(24f), 0, AndroidUtilities.dp(24f), 0)
        }

        fun addLabel(text: String) {
            container.addView(TextView(context).apply {
                setText(text)
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
                setTextColor(Theme.getColor(Theme.key_dialogTextGray2, resourcesProvider))
                setPadding(0, AndroidUtilities.dp(12f), 0, AndroidUtilities.dp(4f))
            })
        }

        addLabel(LocaleController.getString("FilterRulesMatchType", R.string.FilterRulesMatchType))
        val matchGroup = RadioGroup(context)
        val matchLabels = mapOf(
            MessageFilterRules.MATCH_CONTAINS to LocaleController.getString("FilterRulesMatchContains", R.string.FilterRulesMatchContains),
            MessageFilterRules.MATCH_REGEX to LocaleController.getString("FilterRulesMatchRegex", R.string.FilterRulesMatchRegex),
            MessageFilterRules.MATCH_SENDER_ID to LocaleController.getString("FilterRulesMatchSenderId", R.string.FilterRulesMatchSenderId),
            MessageFilterRules.MATCH_VIA_BOT to LocaleController.getString("FilterRulesMatchViaBot", R.string.FilterRulesMatchViaBot),
        )
        matchLabels.forEach { (type, label) ->
            matchGroup.addView(RadioButton(context).apply {
                id = type + 1000
                text = label
                isChecked = (editing?.matchType ?: MessageFilterRules.MATCH_CONTAINS) == type
                applyDialogTheme(Theme.key_dialogRadioBackgroundChecked, Theme.key_dialogRadioBackground)
            })
        }
        container.addView(matchGroup)

        addLabel(LocaleController.getString("FilterRulesMatchValue", R.string.FilterRulesMatchValue))
        val valueEdit = EditTextBoldCursor(context).apply {
            background = null
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider))
            setCursorColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider))
            setHintColor(Theme.getColor(Theme.key_dialogTextHint, resourcesProvider))
            setCursorSize(AndroidUtilities.dp(20f))
            setCursorWidth(1.5f)
            setLineColors(
                Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider),
                Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider),
                Theme.getColor(Theme.key_text_RedRegular, resourcesProvider),
            )
            setText(editing?.matchValue ?: "")
            setHintText(LocaleController.getString("FilterRulesMatchValueHint", R.string.FilterRulesMatchValueHint))
        }
        container.addView(valueEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 40))

        val caseInsensitiveBox = CheckBox(context).apply {
            text = LocaleController.getString("FilterRulesCaseInsensitive", R.string.FilterRulesCaseInsensitive)
            isChecked = editing?.caseInsensitive ?: false
            applyDialogTheme(Theme.key_dialogCheckboxSquareBackground, Theme.key_dialogCheckboxSquareUnchecked)
        }
        container.addView(caseInsensitiveBox)

        addLabel(LocaleController.getString("FilterRulesAction", R.string.FilterRulesAction))
        val actionGroup = RadioGroup(context)
        val actionLabels = mapOf(
            MessageFilterRules.ACTION_HIDE to LocaleController.getString("FilterRulesActionHide", R.string.FilterRulesActionHide),
            MessageFilterRules.ACTION_COLLAPSE to LocaleController.getString("FilterRulesActionCollapse", R.string.FilterRulesActionCollapse),
            MessageFilterRules.ACTION_SPOILER to LocaleController.getString("FilterRulesActionSpoiler", R.string.FilterRulesActionSpoiler),
        )
        actionLabels.forEach { (act, label) ->
            actionGroup.addView(RadioButton(context).apply {
                id = act + 2000
                text = label
                isChecked = (editing?.action ?: MessageFilterRules.ACTION_HIDE) == act
                applyDialogTheme(Theme.key_dialogRadioBackgroundChecked, Theme.key_dialogRadioBackground)
            })
        }
        container.addView(actionGroup)

        AlertDialog.Builder(context, resourcesProvider).apply {
            setTitle(if (editing == null)
                LocaleController.getString("FilterRulesAdd", R.string.FilterRulesAdd)
            else LocaleController.getString("FilterRulesEdit", R.string.FilterRulesEdit))
            setView(container)
            setPositiveButton(LocaleController.getString("Save", R.string.Save)) { _, _ ->
                val value = valueEdit.text?.toString()?.trim().orEmpty()
                if (value.isEmpty()) return@setPositiveButton
                val matchType = matchGroup.checkedRadioButtonId.takeIf { it != -1 }?.let { it - 1000 }
                    ?: MessageFilterRules.MATCH_CONTAINS
                val action = actionGroup.checkedRadioButtonId.takeIf { it != -1 }?.let { it - 2000 }
                    ?: MessageFilterRules.ACTION_HIDE
                if (matchType == MessageFilterRules.MATCH_REGEX) {
                    runCatching { Pattern.compile(value) }.onFailure {
                        org.telegram.ui.Components.BulletinFactory.of(this@FilterRulesActivity)
                            .createErrorBulletin(LocaleController.getString("InvalidPattern", R.string.InvalidPattern))
                            .show()
                        return@setPositiveButton
                    }
                }
                if (matchType == MessageFilterRules.MATCH_SENDER_ID && value.toLongOrNull() == null) {
                    org.telegram.ui.Components.BulletinFactory.of(this@FilterRulesActivity)
                        .createErrorBulletin(LocaleController.getString("FilterRulesSenderIdInvalid", R.string.FilterRulesSenderIdInvalid))
                        .show()
                    return@setPositiveButton
                }
                // via bot: "*" / 空 (任意) / 数字 bot id
                if (matchType == MessageFilterRules.MATCH_VIA_BOT
                    && value != "*" && value.toLongOrNull() == null) {
                    org.telegram.ui.Components.BulletinFactory.of(this@FilterRulesActivity)
                        .createErrorBulletin(LocaleController.getString("FilterRulesViaBotInvalid", R.string.FilterRulesViaBotInvalid))
                        .show()
                    return@setPositiveButton
                }
                val newRule = MessageFilterRules.Rule(
                    id = editing?.id ?: UUID.randomUUID().toString(),
                    matchType = matchType,
                    matchValue = value,
                    action = action,
                    enabled = editing?.enabled ?: true,
                    caseInsensitive = caseInsensitiveBox.isChecked,
                )
                if (editing == null) rules.add(newRule) else rules[editIndex!!] = newRule
                MessageFilterRules.save(rules)
                updateRows()
            }
            setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
        }.show()
    }

    private fun describeRule(r: MessageFilterRules.Rule): String {
        val actionLabel = when (r.action) {
            MessageFilterRules.ACTION_HIDE -> LocaleController.getString("FilterRulesActionHide", R.string.FilterRulesActionHide)
            MessageFilterRules.ACTION_COLLAPSE -> LocaleController.getString("FilterRulesActionCollapse", R.string.FilterRulesActionCollapse)
            MessageFilterRules.ACTION_SPOILER -> LocaleController.getString("FilterRulesActionSpoiler", R.string.FilterRulesActionSpoiler)
            else -> ""
        }
        val typeLabel = when (r.matchType) {
            MessageFilterRules.MATCH_CONTAINS -> LocaleController.getString("FilterRulesMatchContains", R.string.FilterRulesMatchContains)
            MessageFilterRules.MATCH_REGEX -> LocaleController.getString("FilterRulesMatchRegex", R.string.FilterRulesMatchRegex)
            MessageFilterRules.MATCH_SENDER_ID -> LocaleController.getString("FilterRulesMatchSenderId", R.string.FilterRulesMatchSenderId)
            MessageFilterRules.MATCH_VIA_BOT -> LocaleController.getString("FilterRulesMatchViaBot", R.string.FilterRulesMatchViaBot)
            else -> ""
        }
        val base = "$typeLabel · $actionLabel"
        return if (r.enabled) base
        else base + " · " + LocaleController.getString("FilterRulesDisabled", R.string.FilterRulesDisabled)
    }

    private inner class ListAdapter(context: Context) : BaseListAdapter(context) {

        override fun getItemCount(): Int = rowCount

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, partial: Boolean) {
            when (holder.itemViewType) {
                TYPE_HEADER -> (holder.itemView as HeaderCell).setText(
                    LocaleController.getString("FilterRulesTitle", R.string.FilterRulesTitle)
                )
                TYPE_SETTINGS -> {
                    val cell = holder.itemView as TextSettingsCell
                    if (position == addRow) {
                        cell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText))
                        cell.setText(LocaleController.getString("FilterRulesAdd", R.string.FilterRulesAdd), true)
                    }
                }
                TYPE_DETAIL_SETTINGS -> {
                    val cell = holder.itemView as TextDetailSettingsCell
                    val idx = position - rulesStartRow
                    val rule = rules[idx]
                    val title = if (rule.matchType == MessageFilterRules.MATCH_SENDER_ID)
                        rule.matchValue
                    else rule.matchValue.take(40)
                    cell.setTextAndValue(title, describeRule(rule), position != rulesEndRow - 1)
                }
                TYPE_INFO_PRIVACY -> {
                    val cell = holder.itemView as TextInfoPrivacyCell
                    cell.setText(LocaleController.getString("FilterRulesHint", R.string.FilterRulesHint))
                    cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow))
                }
            }
        }

        override fun getItemViewType(position: Int): Int = when {
            position == headerRow -> TYPE_HEADER
            position == addRow -> TYPE_SETTINGS
            position == hintRow -> TYPE_INFO_PRIVACY
            position in rulesStartRow until rulesEndRow -> TYPE_DETAIL_SETTINGS
            else -> TYPE_SHADOW
        }
    }
}
