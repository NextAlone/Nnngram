package org.telegram.messenger;

import java.util.Locale;

/**
 * Converts Telegram language-pack identifiers to an Android resource locale.
 *
 * Telegram language packs are not limited to BCP 47 tags. Official beta packs,
 * built-in aliases and plural-rule codes can all describe the same UI language.
 * In particular, both Chinese scripts use {@code zh} plural rules, so the plural
 * code must not erase the Hant/Hans distinction carried by the pack identifier.
 */
final class LanguageResourceLocale {

    private static final int CHINESE_SCRIPT_UNSPECIFIED = 0;
    private static final int CHINESE_SCRIPT_TRADITIONAL = 1;
    private static final int CHINESE_SCRIPT_SIMPLIFIED = 2;

    private LanguageResourceLocale() {
    }

    static Locale resolve(String shortName, String baseLangCode, String pluralLangCode) {
        String normalizedShortName = normalize(shortName);
        String normalizedBaseLangCode = normalize(baseLangCode);
        String normalizedPluralLangCode = normalize(pluralLangCode);

        int chineseScript = getChineseScript(normalizedShortName);
        if (chineseScript == CHINESE_SCRIPT_UNSPECIFIED) {
            chineseScript = getChineseScript(normalizedBaseLangCode);
        }
        if (chineseScript == CHINESE_SCRIPT_UNSPECIFIED) {
            chineseScript = getChineseScript(normalizedPluralLangCode);
        }
        if (chineseScript == CHINESE_SCRIPT_TRADITIONAL) {
            return Locale.forLanguageTag("zh-Hant-TW");
        }
        if (chineseScript == CHINESE_SCRIPT_SIMPLIFIED) {
            return Locale.forLanguageTag("zh-Hans-CN");
        }

        String languageCode;
        if (!normalizedPluralLangCode.isEmpty()) {
            languageCode = normalizedPluralLangCode;
        } else if (!normalizedBaseLangCode.isEmpty()) {
            languageCode = normalizedBaseLangCode;
        } else {
            languageCode = normalizedShortName;
        }
        return Locale.forLanguageTag(languageCode.replace('_', '-'));
    }

    private static int getChineseScript(String languageCode) {
        if (languageCode.equals("taiwan")
                || isCodeOrVariant(languageCode, "zh_hant")
                || isCodeOrVariant(languageCode, "zhhant")
                || isCodeOrVariant(languageCode, "zh_tw")
                || isCodeOrVariant(languageCode, "zh_hk")
                || isCodeOrVariant(languageCode, "zh_mo")) {
            return CHINESE_SCRIPT_TRADITIONAL;
        }
        if (languageCode.equals("moecn")
                || isCodeOrVariant(languageCode, "zh_hans")
                || isCodeOrVariant(languageCode, "zhhans")
                || isCodeOrVariant(languageCode, "zh_cn")
                || isCodeOrVariant(languageCode, "zh_sg")) {
            return CHINESE_SCRIPT_SIMPLIFIED;
        }
        return CHINESE_SCRIPT_UNSPECIFIED;
    }

    private static boolean isCodeOrVariant(String languageCode, String expectedCode) {
        return languageCode.equals(expectedCode) || languageCode.startsWith(expectedCode + "_");
    }

    private static String normalize(String languageCode) {
        return languageCode == null ? "" : languageCode.trim().replace('-', '_').toLowerCase(Locale.US);
    }
}
