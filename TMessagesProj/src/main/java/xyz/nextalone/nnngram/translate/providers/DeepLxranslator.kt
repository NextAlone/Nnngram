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

package xyz.nextalone.nnngram.translate.providers

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import org.telegram.tgnet.SerializedData
import org.telegram.tgnet.TLRPC
import xyz.nextalone.gen.Config
import xyz.nextalone.nnngram.translate.BaseTranslator
import xyz.nextalone.nnngram.translate.FormattedText
import xyz.nextalone.nnngram.translate.RichMessageText
import xyz.nextalone.nnngram.translate.RichMessageTextProcessor
import xyz.nextalone.nnngram.utils.Log
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Locale

/**
 * @author NextAlone
 * @date 2024/11/04 01:14
 *
 */
object DeepLxTranslator : BaseTranslator() {

    const val API_TOKEN_PLACEHOLDER = "(API_TOKEN)"
    const val MIN_MAX_CHARACTERS = 1
    const val MAX_MAX_CHARACTERS = 100_000
    const val MIN_REQUESTS_PER_SECOND = 1
    const val MAX_REQUESTS_PER_SECOND = 100

    private val rateLimitMutex = Mutex()
    private var nextRequestAtNanos = 0L

    private val targetLanguages = listOf(
        "ar", "bg", "cs", "da", "de", "de-CH", "el", "en-GB", "en-US",
        "es", "es-419", "et", "fi", "fr", "fr-CA", "he", "hu", "id", "it",
        "ja", "ko", "lt", "lv", "nb", "nl", "pl", "pt-BR", "pt-PT", "ro",
        "ru", "sk", "sl", "sv", "tr", "uk", "vi", "zh-Hans", "zh-Hant"
    )

    override fun getTargetLanguages(): List<String> = targetLanguages

    override fun convertLanguageCode(language: String, country: String?): String {
        val languageLowerCase = language.lowercase(Locale.ROOT)
        val countryUpperCase = country?.uppercase(Locale.ROOT).orEmpty()
        return when (languageLowerCase) {
            "en" -> if (countryUpperCase == "GB") "en-GB" else "en-US"
            "pt" -> if (countryUpperCase == "PT") "pt-PT" else "pt-BR"
            "zh" -> if (countryUpperCase in setOf("TW", "HK", "MO")) "zh-Hant" else "zh-Hans"
            "de" -> if (countryUpperCase == "CH") "de-CH" else "de"
            "fr" -> if (countryUpperCase == "CA") "fr-CA" else "fr"
            "no" -> "nb"
            "iw" -> "he"
            "in" -> "id"
            else -> canonicalTargetLanguage(languageLowerCase)
        }
    }

    override fun supportLanguage(language: String): Boolean =
        targetLanguages.contains(canonicalTargetLanguage(language))

    override fun getTargetLanguage(language: String): String = if (language == "app") {
        getCurrentAppLanguage()
    } else {
        canonicalTargetLanguage(language)
    }

    override fun convertLanguageCode(code: String, reverse: Boolean): String = if (reverse) {
        code.replace('_', '-').lowercase(Locale.ROOT)
    } else {
        canonicalTargetLanguage(code)
    }

    private fun canonicalTargetLanguage(language: String): String {
        val normalized = language.replace('_', '-').lowercase(Locale.ROOT)
        return when (normalized) {
            "en" -> "en-US"
            "pt" -> "pt-BR"
            "zh", "zh-cn", "zh-sg" -> "zh-Hans"
            "zh-tw", "zh-hk", "zh-mo" -> "zh-Hant"
            "no" -> "nb"
            "iw" -> "he"
            "in" -> "id"
            else -> targetLanguages.firstOrNull { it.equals(normalized, ignoreCase = true) } ?: normalized
        }
    }

    override suspend fun translate(source: Any, from: String, to: String): TranslateResult {
        if (source is RichMessageText) {
            return translateRichMessage(source, canonicalTargetLanguage(to))
        }
        if (source !is FormattedText || !Config.deepLxPreserveFormatting) {
            return super.translate(source, from, to)
        }
        return translateFormattedText(source, canonicalTargetLanguage(to))
    }

    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        val parts = DeepLxTextProcessor.split(
            text,
            Config.deepLxMaxCharacters.coerceIn(MIN_MAX_CHARACTERS, MAX_MAX_CHARACTERS),
            false
        )
        val translated = StringBuilder()
        var detectedLanguage = from
        for (part in parts) {
            if (!part.translate) {
                translated.append(part.text)
                continue
            }
            val result = requestChunk(part.text, "auto", canonicalTargetLanguage(to))
            if (result.error != null || result.result == null) {
                return RequestResult(detectedLanguage, null, result.error ?: HttpStatusCode.InternalServerError)
            }
            if (result.from.isNotBlank() && !result.from.equals("auto", ignoreCase = true)) {
                detectedLanguage = result.from
            }
            translated.append(DeepLxTextProcessor.restoreEdgeWhitespace(part.text, result.result))
        }
        return RequestResult(detectedLanguage, translated.toString())
    }

    private suspend fun requestChunk(text: String, from: String, to: String): RequestResult {
        val apiUrl = resolveApiUrl()
        awaitRateLimit()
        val response = try {
            client.post(apiUrl) {
                contentType(ContentType.Application.Json)
                setBody(getRequestBody(text, from, to))
            }
        } catch (e: Exception) {
            // A request exception may contain the expanded URL. Do not leak API tokens to logs/UI.
            throw IOException("DeepLX request failed (${e.javaClass.simpleName})")
        }

        val responseBody = response.bodyAsText()
        val jsonObject = runCatching { JSONObject(responseBody) }.getOrNull()
        if (response.status == HttpStatusCode.OK && jsonObject != null) {
            val responseCode = jsonObject.optInt("code", HttpStatusCode.OK.value)
            if (jsonObject.has("error") || responseCode !in 200..299) {
                val message = jsonObject.optString("message", "DeepLX returned an error")
                return RequestResult(from, null, HttpStatusCode(responseCode, message.take(160)))
            }
            if (!jsonObject.has("data") || jsonObject.isNull("data")) {
                return RequestResult(from, null, HttpStatusCode(500, "DeepLX response is missing data"))
            }
            val data = jsonObject.getString("data")
            return RequestResult(jsonObject.optString("source_lang", from), data)
        }

        val message = jsonObject?.optString("message")
            ?.takeIf { it.isNotBlank() }
            ?.take(160)
            ?: response.status.description
        Log.w("DeepLX request failed: HTTP ${response.status.value} ($message)")
        return RequestResult(from, null, HttpStatusCode(response.status.value, message))
    }

    private suspend fun awaitRateLimit() {
        rateLimitMutex.withLock {
            val requestsPerSecond = Config.deepLxRequestsPerSecond.coerceIn(
                MIN_REQUESTS_PER_SECOND,
                MAX_REQUESTS_PER_SECOND
            )
            val intervalNanos = 1_000_000_000L / requestsPerSecond
            val now = System.nanoTime()
            val waitNanos = nextRequestAtNanos - now
            if (waitNanos > 0) {
                delay((waitNanos + 999_999L) / 1_000_000L)
            }
            nextRequestAtNanos = maxOf(nextRequestAtNanos, System.nanoTime()) + intervalNanos
        }
    }

    private suspend fun translateFormattedText(source: FormattedText, to: String): TranslateResult {
        val text = source.text
        val validEntities = source.entities.filter { entity ->
            entity.offset >= 0 && entity.length > 0 && entity.offset <= text.length &&
                entity.length <= text.length - entity.offset
        }
        val boundaries = sortedSetOf<Int>()
        val protectedRanges = ArrayList<DeepLxTextProcessor.ProtectedRange>()
        protectedRanges.addAll(DeepLxTextProcessor.structuralWhitespaceRanges(text))
        validEntities.forEach { entity ->
            boundaries.add(entity.offset)
            boundaries.add(entity.offset + entity.length)
            if (isProtectedEntity(entity)) {
                protectedRanges.add(
                    DeepLxTextProcessor.ProtectedRange(entity.offset, entity.offset + entity.length)
                )
            }
        }
        val plan = DeepLxTextProcessor.createFormattingPlan(text, boundaries, protectedRanges)
        if (plan == null) {
            Log.w("Unable to allocate DeepLX formatting markers; using unformatted translation")
            return translateFormattedFallback(text, to)
        }

        val result = translateText(plan.markedText, "auto", to)
        if (result.error != null || result.result == null) {
            return TranslateResult(result.from, null, result.error ?: HttpStatusCode.InternalServerError)
        }
        val decoded = DeepLxTextProcessor.decodeFormatting(text, result.result, plan)
        if (decoded == null) {
            Log.w("DeepLX did not preserve formatting markers; using unformatted translation")
            return translateFormattedFallback(text, to)
        }
        val translatedEntities = ArrayList<TLRPC.MessageEntity>()
        validEntities.forEach { entity ->
            val newStart = decoded.mappedOffsets[entity.offset] ?: return@forEach
            val newEnd = decoded.mappedOffsets[entity.offset + entity.length] ?: return@forEach
            if (newEnd <= newStart) return@forEach
            cloneEntity(entity)?.let { copy ->
                copy.offset = newStart
                copy.length = newEnd - newStart
                translatedEntities.add(copy)
            }
        }
        return TranslateResult(result.from, FormattedText(decoded.text, translatedEntities))
    }

    private suspend fun translateFormattedFallback(text: String, to: String): TranslateResult {
        val result = translateText(text, "auto", to)
        return TranslateResult(result.from, result.result?.let { FormattedText(it, ArrayList()) }, result.error)
    }

    private suspend fun translateRichMessage(source: RichMessageText, to: String): TranslateResult {
        val (translatedMessage, textNodes) = RichMessageTextProcessor.translatedCopy(source.richMessage)
            ?: return TranslateResult("auto", null, HttpStatusCode.InternalServerError)
        if (textNodes.isEmpty()) {
            return TranslateResult("auto", RichMessageText(translatedMessage))
        }

        val sourceText = StringBuilder()
        val nodeRanges = ArrayList<IntRange>(textNodes.size)
        val boundaries = sortedSetOf<Int>()
        textNodes.forEachIndexed { index, node ->
            if (index > 0) sourceText.append('\n')
            val start = sourceText.length
            sourceText.append(node.text)
            val end = sourceText.length
            boundaries.add(start)
            boundaries.add(end)
            nodeRanges.add(start until end)
        }

        val originalText = sourceText.toString()
        val plan = DeepLxTextProcessor.createFormattingPlan(
            originalText,
            boundaries,
            DeepLxTextProcessor.structuralWhitespaceRanges(originalText)
        )
        if (plan != null) {
            val result = translateText(plan.markedText, "auto", to)
            if (result.error != null || result.result == null) {
                return TranslateResult(result.from, null, result.error ?: HttpStatusCode.InternalServerError)
            }
            val decoded = DeepLxTextProcessor.decodeFormatting(originalText, result.result, plan)
            if (decoded != null) {
                val translatedRanges = nodeRanges.map { range ->
                    val start = decoded.mappedOffsets[range.first]
                    val end = decoded.mappedOffsets[range.last + 1]
                    if (start == null || end == null || end < start || end > decoded.text.length) null
                    else start until end
                }
                if (translatedRanges.none { it == null }) {
                    textNodes.forEachIndexed { index, node ->
                        val range = translatedRanges[index]!!
                        node.text = decoded.text.substring(range.first, range.last + 1)
                    }
                    return TranslateResult(result.from, RichMessageText(translatedMessage))
                }
            }
            Log.w("DeepLX did not preserve rich-message boundaries; translating its text blocks separately")
        }

        var detectedLanguage = "auto"
        for (node in textNodes) {
            val result = translateText(node.text, "auto", to)
            if (result.error != null || result.result == null) {
                return TranslateResult(detectedLanguage, null, result.error ?: HttpStatusCode.InternalServerError)
            }
            if (result.from.isNotBlank() && !result.from.equals("auto", ignoreCase = true)) {
                detectedLanguage = result.from
            }
            node.text = result.result
        }
        return TranslateResult(detectedLanguage, RichMessageText(translatedMessage))
    }

    private fun isProtectedEntity(entity: TLRPC.MessageEntity): Boolean = when (entity) {
        is TLRPC.TL_messageEntityBold,
        is TLRPC.TL_messageEntityItalic,
        is TLRPC.TL_messageEntityUnderline,
        is TLRPC.TL_messageEntityStrike,
        is TLRPC.TL_messageEntitySpoiler,
        is TLRPC.TL_messageEntityBlockquote,
        is TLRPC.TL_messageEntityTextUrl -> false
        else -> true
    }

    private fun cloneEntity(entity: TLRPC.MessageEntity): TLRPC.MessageEntity? {
        val writer = SerializedData(entity.objectSize)
        return try {
            entity.serializeToStream(writer)
            val reader = SerializedData(writer.toByteArray())
            try {
                TLRPC.MessageEntity.TLdeserialize(reader, reader.readInt32(true), true)
            } finally {
                reader.cleanup()
            }
        } catch (e: Exception) {
            Log.w("Unable to preserve a translated message entity: ${e.javaClass.simpleName}")
            null
        } finally {
            writer.cleanup()
        }
    }

    private fun resolveApiUrl(): String {
        val configuredUrl = Config.deepLxApi.trim()
        if (configuredUrl.isEmpty()) {
            throw IOException("DeepLX API URL is empty")
        }
        val configuredToken = Config.deepLxApiToken.trim()
        if (configuredUrl.contains(API_TOKEN_PLACEHOLDER) && configuredToken.isEmpty()) {
            throw IOException("DeepLX API token is empty")
        }
        val encodedToken = URLEncoder.encode(configuredToken, StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
        val resolvedUrl = configuredUrl.replace(API_TOKEN_PLACEHOLDER, encodedToken)
        val uri = runCatching { URI(resolvedUrl) }.getOrNull()
        if (uri == null || uri.host.isNullOrEmpty() ||
            (!uri.scheme.equals("http", ignoreCase = true) && !uri.scheme.equals("https", ignoreCase = true))) {
            throw IOException("DeepLX API URL is invalid")
        }
        return resolvedUrl
    }

    private fun getRequestBody(text: String, from: String, to: String): String {
        val params = JSONObject().apply {
            put("text", text)
            put("source_lang", from)
            put("target_lang", to)
        }

        return params.toString()
    }
}
