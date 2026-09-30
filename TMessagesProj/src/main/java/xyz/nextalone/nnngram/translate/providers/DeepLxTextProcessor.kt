/*
 * Copyright (C) 2019-2026 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 */

package xyz.nextalone.nnngram.translate.providers

/** Pure text segmentation used by DeepLX before network requests are made. */
internal object DeepLxTextProcessor {
    data class Part(val text: String, val translate: Boolean)

    data class ProtectedRange(val start: Int, val end: Int)

    data class FormattingPlan(
        val markedText: String,
        val markerByOffset: Map<Int, Char>,
        val protectedRanges: List<ProtectedRange>
    )

    data class DecodedFormatting(
        val text: String,
        val mappedOffsets: Map<Int, Int>
    )

    /**
     * Adds one private-use marker at each formatting boundary. The markers stay inside one
     * contextual translation request and are removed after the translated offsets are known.
     */
    fun createFormattingPlan(
        text: String,
        boundaryOffsets: Collection<Int>,
        protectedRanges: Collection<ProtectedRange>
    ): FormattingPlan? {
        val normalizedRanges = mergeProtectedRanges(text.length, protectedRanges)
        val offsets = sortedSetOf<Int>()
        boundaryOffsets.filterTo(offsets) { it in 0..text.length }
        normalizedRanges.forEach { range ->
            offsets.add(range.start)
            offsets.add(range.end)
        }
        if (offsets.isEmpty()) {
            return FormattingPlan(text, emptyMap(), emptyList())
        }

        val usedCharacters = text.toHashSet()
        val markerByOffset = LinkedHashMap<Int, Char>()
        var candidate = PRIVATE_USE_START
        offsets.forEach { offset ->
            while (candidate <= PRIVATE_USE_END && usedCharacters.contains(candidate.toChar())) {
                candidate++
            }
            if (candidate > PRIVATE_USE_END) return null
            markerByOffset[offset] = candidate.toChar()
            candidate++
        }

        val marked = StringBuilder(text.length + markerByOffset.size)
        var cursor = 0
        markerByOffset.forEach { (offset, marker) ->
            marked.append(text, cursor, offset)
            marked.append(marker)
            cursor = offset
        }
        marked.append(text, cursor, text.length)
        return FormattingPlan(marked.toString(), markerByOffset, normalizedRanges)
    }

    /** Returns null when the translation service removed, duplicated, or corrupted a marker. */
    fun decodeFormatting(
        sourceText: String,
        translatedMarkedText: String,
        plan: FormattingPlan
    ): DecodedFormatting? {
        if (plan.markerByOffset.isEmpty()) {
            return DecodedFormatting(translatedMarkedText, emptyMap())
        }
        val offsetByMarker = plan.markerByOffset.entries.associate { (offset, marker) -> marker to offset }
        val markerCounts = HashMap<Char, Int>()
        translatedMarkedText.forEach { character ->
            if (offsetByMarker.containsKey(character)) {
                markerCounts[character] = (markerCounts[character] ?: 0) + 1
            }
        }
        if (plan.markerByOffset.values.any { markerCounts[it] != 1 }) return null

        val protectedByStart = plan.protectedRanges.associateBy { it.start }
        val mappedOffsets = HashMap<Int, Int>()
        val decoded = StringBuilder(translatedMarkedText.length)
        var index = 0
        while (index < translatedMarkedText.length) {
            val character = translatedMarkedText[index]
            val sourceOffset = offsetByMarker[character]
            if (sourceOffset == null) {
                decoded.append(character)
                index++
                continue
            }

            mappedOffsets[sourceOffset] = decoded.length
            val protectedRange = protectedByStart[sourceOffset]
            if (protectedRange == null) {
                index++
                continue
            }
            val endMarker = plan.markerByOffset[protectedRange.end] ?: return null
            val translatedEnd = translatedMarkedText.indexOf(endMarker, index + 1)
            if (translatedEnd < 0) return null

            val decodedStart = decoded.length
            plan.markerByOffset.keys.forEach { offset ->
                if (offset in protectedRange.start..protectedRange.end) {
                    mappedOffsets[offset] = decodedStart + offset - protectedRange.start
                }
            }
            decoded.append(sourceText, protectedRange.start, protectedRange.end)
            index = translatedEnd + 1
        }
        if (!mappedOffsets.keys.containsAll(plan.markerByOffset.keys)) return null
        return DecodedFormatting(decoded.toString(), mappedOffsets)
    }

    fun structuralWhitespaceRanges(text: String): List<ProtectedRange> {
        val ranges = ArrayList<ProtectedRange>()
        var index = 0
        while (index < text.length) {
            val end = structuralWhitespaceEnd(text, index)
            if (end > index) {
                ranges.add(ProtectedRange(index, end))
                index = end
            } else {
                index += Character.charCount(text.codePointAt(index))
            }
        }
        return ranges
    }

    fun split(text: String, maxCharacters: Int, preserveFormatting: Boolean): List<Part> {
        if (text.isEmpty()) {
            return listOf(Part("", false))
        }
        val limit = maxCharacters.coerceAtLeast(1)
        val structuralParts = if (preserveFormatting) splitStructuralWhitespace(text) else listOf(Part(text, true))
        val result = ArrayList<Part>()
        structuralParts.forEach { part ->
            if (!part.translate) {
                appendPart(result, part)
            } else {
                splitLongPart(part.text, limit, preserveFormatting).forEach { appendPart(result, it) }
            }
        }
        return result
    }

    /**
     * DeepL trims each request, so whitespace at chunk edges would vanish when chunks are joined.
     * Re-applies the source chunk's leading and trailing whitespace around the translation.
     */
    fun restoreEdgeWhitespace(source: String, translated: String): String {
        val leading = source.takeWhile { it.isWhitespace() }
        val trailing = source.takeLastWhile { it.isWhitespace() }
        return leading + translated.trim() + trailing
    }

    private fun splitStructuralWhitespace(text: String): List<Part> {
        val result = ArrayList<Part>()
        var textStart = 0
        var index = 0
        while (index < text.length) {
            val structuralEnd = structuralWhitespaceEnd(text, index)
            if (structuralEnd < 0) {
                index += Character.charCount(text.codePointAt(index))
                continue
            }
            if (textStart < index) {
                result.add(Part(text.substring(textStart, index), true))
            }
            result.add(Part(text.substring(index, structuralEnd), false))
            index = structuralEnd
            textStart = structuralEnd
        }
        if (textStart < text.length) {
            result.add(Part(text.substring(textStart), true))
        }
        return result
    }

    private fun structuralWhitespaceEnd(text: String, start: Int): Int {
        val codePoint = text.codePointAt(start)
        if (codePoint == '\r'.code) {
            return if (start + 1 < text.length && text[start + 1] == '\n') start + 2 else start + 1
        }
        if (codePoint == '\n'.code || codePoint == '\t'.code) {
            var end = start + Character.charCount(codePoint)
            while (end < text.length) {
                val next = text.codePointAt(end)
                if (next != '\n'.code && next != '\r'.code && next != '\t'.code) break
                end += Character.charCount(next)
            }
            return end
        }
        if (!Character.isWhitespace(codePoint)) return -1

        var end = start + Character.charCount(codePoint)
        var count = 1
        while (end < text.length) {
            val next = text.codePointAt(end)
            if (!Character.isWhitespace(next) || next == '\n'.code || next == '\r'.code || next == '\t'.code) break
            end += Character.charCount(next)
            count++
        }
        return if (count >= 2) end else -1
    }

    private fun splitLongPart(text: String, limit: Int, preserveFormatting: Boolean): List<Part> {
        if (text.codePointCount(0, text.length) <= limit) {
            return listOf(Part(text, hasTranslatableContent(text)))
        }

        val result = ArrayList<Part>()
        var start = 0
        while (start < text.length) {
            val remainingCount = text.codePointCount(start, text.length)
            if (remainingCount <= limit) {
                result.add(Part(text.substring(start), hasTranslatableContent(text.substring(start))))
                break
            }

            val hardEnd = text.offsetByCodePoints(start, limit)
            val preferredEnd = findPreferredEnd(text, start, hardEnd, limit)
            var end = if (preferredEnd > start) preferredEnd else hardEnd

            if (preserveFormatting && end < text.length && Character.isWhitespace(text.codePointAt(end))) {
                val chunk = text.substring(start, end)
                if (chunk.isNotEmpty()) result.add(Part(chunk, hasTranslatableContent(chunk)))
                var whitespaceEnd = end
                while (whitespaceEnd < text.length && Character.isWhitespace(text.codePointAt(whitespaceEnd))) {
                    whitespaceEnd += Character.charCount(text.codePointAt(whitespaceEnd))
                }
                result.add(Part(text.substring(end, whitespaceEnd), false))
                start = whitespaceEnd
                continue
            }

            if (end <= start) end = hardEnd
            val chunk = text.substring(start, end)
            result.add(Part(chunk, hasTranslatableContent(chunk)))
            start = end
        }
        return result
    }

    private fun findPreferredEnd(text: String, start: Int, hardEnd: Int, limit: Int): Int {
        val minimum = text.offsetByCodePoints(start, (limit / 2).coerceAtLeast(1))
        var index = hardEnd
        var whitespace = -1
        while (index > minimum) {
            val codePoint = text.codePointBefore(index)
            val codePointStart = index - Character.charCount(codePoint)
            if (isSentenceBoundary(codePoint)) return index
            if (whitespace < 0 && Character.isWhitespace(codePoint)) whitespace = codePointStart
            index = codePointStart
        }
        return whitespace
    }

    private fun isSentenceBoundary(codePoint: Int): Boolean = when (codePoint) {
        '.'.code, '!'.code, '?'.code, ';'.code, ':'.code,
        '。'.code, '！'.code, '？'.code, '；'.code, '：'.code -> true
        else -> false
    }

    private fun hasTranslatableContent(text: String): Boolean {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (Character.isLetterOrDigit(codePoint)) return true
            index += Character.charCount(codePoint)
        }
        return false
    }

    private fun appendPart(parts: MutableList<Part>, part: Part) {
        if (part.text.isEmpty()) return
        val last = parts.lastOrNull()
        if (last != null && !last.translate && !part.translate) {
            parts[parts.lastIndex] = Part(last.text + part.text, false)
        } else {
            parts.add(part)
        }
    }

    private fun mergeProtectedRanges(
        textLength: Int,
        ranges: Collection<ProtectedRange>
    ): List<ProtectedRange> {
        val sorted = ranges
            .map { ProtectedRange(it.start.coerceIn(0, textLength), it.end.coerceIn(0, textLength)) }
            .filter { it.end > it.start }
            .sortedWith(compareBy<ProtectedRange> { it.start }.thenBy { it.end })
        if (sorted.isEmpty()) return emptyList()

        val merged = ArrayList<ProtectedRange>()
        var current = sorted.first()
        for (index in 1 until sorted.size) {
            val next = sorted[index]
            if (next.start <= current.end) {
                current = ProtectedRange(current.start, maxOf(current.end, next.end))
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    private const val PRIVATE_USE_START = 0xE000
    private const val PRIVATE_USE_END = 0xF8FF
}
