package xyz.nextalone.nnngram.translate.providers

import io.ktor.http.HttpStatusCode
import xyz.nextalone.nnngram.translate.BaseTranslator

object TelegramTranslator : BaseTranslator() {
    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
/*        val result = AtomicReference<RequestResult>()
        val latch = CountDownLatch(1)
        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(TL_messages_translateText().apply {
            flags = flags or 2
            to_lang = to
            this.text = text
        }) { res: TLObject?, error: TL_error? ->
            if (error == null) {
                if (res is TL_messages_translateResultText) {
                    result.set(RequestResult(from, res.text))
                } else {
                    result.set(RequestResult(from, null, HttpStatusCode.TooManyRequests))
                }
            } else {
                result.set(RequestResult(from, null, HttpStatusCode(500, error.text)))
            }
            latch.countDown()
        }
        latch.await()
        return result.get()*/

        return RequestResult(from, null, HttpStatusCode(500, "Not implemented"))
    }

    override fun getTargetLanguages(): List<String> = GoogleTranslator.getTargetLanguages()
    override fun convertLanguageCode(language: String, country: String?): String = GoogleTranslator.convertLanguageCode(language, country)


}
