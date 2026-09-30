/*
 * Copyright (C) 2019-2026 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */
package xyz.nextalone.nnngram.translate

import org.telegram.tgnet.SerializedData
import org.telegram.tgnet.tl.TL_iv
import xyz.nextalone.nnngram.utils.Log
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap

data class RichMessageText(val richMessage: TL_iv.RichMessage)

/** Utilities for translating the text leaves of a Telegram Rich Message without flattening it. */
object RichMessageTextProcessor {

    @JvmStatic
    fun copy(source: TL_iv.RichMessage): TL_iv.RichMessage? {
        val writer = SerializedData(source.objectSize)
        return try {
            source.serializeToStream(writer)
            val reader = SerializedData(writer.toByteArray())
            try {
                TL_iv.RichMessage.TLdeserialize(reader, reader.readInt32(true), true)
            } finally {
                reader.cleanup()
            }
        } catch (e: Exception) {
            Log.w("Unable to copy a rich message for translation: ${e.javaClass.simpleName}")
            null
        } finally {
            writer.cleanup()
        }
    }

    @JvmStatic
    fun plainText(source: TL_iv.RichMessage?): String {
        if (source == null) return ""
        return collectTextNodes(source, includeProtected = true)
            .map { it.text }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    @JvmStatic
    fun hasTranslatableText(source: TL_iv.RichMessage?): Boolean {
        return translatableTexts(source).isNotEmpty()
    }

    /** Text leaves that may be sent to the translator, excluding links and other protected values. */
    @JvmStatic
    fun translatableTexts(source: TL_iv.RichMessage?): List<String> {
        if (source == null) return emptyList()
        return collectTextNodes(source, includeProtected = false)
            .map { it.text }
            .filter { hasLettersOrDigits(it) }
    }

    /** Builds the translated view used when the user asks to keep the original text visible. */
    @JvmStatic
    fun withOriginal(
        original: TL_iv.RichMessage,
        translated: TL_iv.RichMessage
    ): TL_iv.RichMessage {
        val combined = copy(original) ?: return translated
        if (combined.blocks.isNotEmpty() && translated.blocks.isNotEmpty()) {
            combined.blocks.add(TL_iv.pageBlockDivider())
        }
        combined.blocks.addAll(translated.blocks)
        return combined
    }

    internal fun translatedCopy(source: TL_iv.RichMessage): Pair<TL_iv.RichMessage, List<TL_iv.textPlain>>? {
        val translated = copy(source) ?: return null
        return translated to collectTextNodes(translated, includeProtected = false)
            .filter { hasLettersOrDigits(it.text) }
    }

    private fun collectTextNodes(
        source: TL_iv.RichMessage,
        includeProtected: Boolean
    ): List<TL_iv.textPlain> {
        val result = ArrayList<TL_iv.textPlain>()
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        collect(source, protected = false, includeProtected, visited, result)
        return result
    }

    private fun collect(
        value: Any?,
        protected: Boolean,
        includeProtected: Boolean,
        visited: MutableSet<Any>,
        result: MutableList<TL_iv.textPlain>
    ) {
        if (value == null) return
        if (value is TL_iv.textPlain) {
            if (includeProtected || !protected) result.add(value)
            return
        }
        if (!isRichMessageObject(value) || !visited.add(value)) return

        val protectChildren = protected || isProtectedRichText(value)
        value.javaClass.fields.forEach { field ->
            if (Modifier.isStatic(field.modifiers) || field.name == "parentRichText") return@forEach
            val child = runCatching { field.get(value) }.getOrNull() ?: return@forEach
            when (child) {
                is Iterable<*> -> child.forEach {
                    collect(it, protectChildren, includeProtected, visited, result)
                }
                else -> collect(child, protectChildren, includeProtected, visited, result)
            }
        }
    }

    private fun isRichMessageObject(value: Any): Boolean =
        value.javaClass.name.startsWith("org.telegram.tgnet.tl.TL_iv\$")

    private fun isProtectedRichText(value: Any): Boolean = when (value) {
        is TL_iv.textFixed,
        is TL_iv.textEmail,
        is TL_iv.textPhone,
        is TL_iv.textMath,
        is TL_iv.textMention,
        is TL_iv.textHashtag,
        is TL_iv.textBotCommand,
        is TL_iv.textCashtag,
        is TL_iv.textAutoUrl,
        is TL_iv.textAutoEmail,
        is TL_iv.textAutoPhone,
        is TL_iv.textBankCard,
        is TL_iv.textMentionName,
        is TL_iv.textDate -> true
        else -> false
    }

    private fun hasLettersOrDigits(text: String): Boolean {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (Character.isLetterOrDigit(codePoint)) return true
            index += Character.charCount(codePoint)
        }
        return false
    }
}
