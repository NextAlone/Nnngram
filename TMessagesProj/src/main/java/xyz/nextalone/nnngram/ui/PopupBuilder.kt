package xyz.nextalone.nnngram.ui

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.RadioColorCell
import xyz.nextalone.nnngram.ui.simplemenu.SimpleMenuPopupWindow
import xyz.nextalone.nnngram.utils.Log

object PopupBuilder {
    private var mPopupWindow: SimpleMenuPopupWindow? = null

    @JvmStatic
    fun show(
        entries: ArrayList<out CharSequence?>,
        title: String?,
        checkedIndex: Int,
        context: Context?,
        itemView: View?,
        listener: SimpleMenuPopupWindow.OnItemClickListener
    ) {
        show(entries, title, checkedIndex, context, itemView, null, listener)
    }

    @JvmStatic
    fun show(
        entries: ArrayList<out CharSequence?>,
        title: String?,
        checkedIndex: Int,
        context: Context?,
        itemView: View?,
        resourcesProvider: Theme.ResourcesProvider?,
        listener: SimpleMenuPopupWindow.OnItemClickListener
    ) {
        if (itemView == null) {
            val builder = AlertDialog.Builder(context, resourcesProvider)
            builder.setTitle(title)
            val linearLayout = LinearLayout(context)
            linearLayout.orientation = LinearLayout.VERTICAL
            builder.setView(linearLayout)
            for (a in entries.indices) {
                val cell = RadioColorCell(context)
                cell.setPadding(AndroidUtilities.dp(4f), 0, AndroidUtilities.dp(4f), 0)
                cell.tag = a
                cell.setCheckColor(
                    Theme.getColor(Theme.key_radioBackground, resourcesProvider),
                    Theme.getColor(Theme.key_dialogRadioBackgroundChecked, resourcesProvider)
                )
                cell.setTextAndValue(entries[a] as String?, checkedIndex == a)
                linearLayout.addView(cell)
                cell.setOnClickListener { v: View ->
                    val which = v.tag as Int
                    builder.dismissRunnable.run()
                    listener.onClick(which)
                }
            }
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
            builder.show()
        } else {
            val container = itemView.parent as View
            if (mPopupWindow != null) {
                try {
                    if (mPopupWindow!!.isShowing) mPopupWindow!!.dismiss()
                } catch (e: Exception) {
                    Log.e(e)
                }
            }
            mPopupWindow = SimpleMenuPopupWindow(context)
            mPopupWindow!!.onItemClickListener = listener
            mPopupWindow!!.setEntries(entries.toTypedArray())
            mPopupWindow!!.setSelectedIndex(checkedIndex)
            mPopupWindow!!.show(itemView, container, 0)
        }
    }
}
