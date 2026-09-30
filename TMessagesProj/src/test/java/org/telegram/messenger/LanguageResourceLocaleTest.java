package org.telegram.messenger;

import java.util.Locale;

/**
 * Lightweight JVM regression test. It intentionally has no Android or JUnit
 * dependency, so it can be compiled together with LanguageResourceLocale.java.
 */
public final class LanguageResourceLocaleTest {

    public static void main(String[] args) {
        assertLocale("official Traditional Chinese beta pack",
                "zh-Hant-TW", LanguageResourceLocale.resolve("zh-hant-beta", "", "zh"));
        assertLocale("translation-platform Traditional Chinese code",
                "zh-Hant-TW", LanguageResourceLocale.resolve("zhhant-tw", "", "zh"));
        assertLocale("built-in Taiwan alias",
                "zh-Hant-TW", LanguageResourceLocale.resolve("taiwan", "zh_hant_raw", "zh_tw"));
        assertLocale("Traditional Chinese region code",
                "zh-Hant-TW", LanguageResourceLocale.resolve("custom", "", "zh_hk"));
        assertLocale("official Simplified Chinese beta pack",
                "zh-Hans-CN", LanguageResourceLocale.resolve("zh-hans-beta", "", "zh"));
        assertLocale("built-in Simplified Chinese alias",
                "zh-Hans-CN", LanguageResourceLocale.resolve("moecn", "zh_hans_raw", "zh_cn"));
        assertLocale("non-Chinese regional locale",
                "pt-BR", LanguageResourceLocale.resolve("pt_br", "", "pt_br"));
        assertLocale("raw pack with ISO plural code",
                "ja", LanguageResourceLocale.resolve("ja_raw", "", "ja"));
    }

    private static void assertLocale(String label, String expected, Locale actual) {
        String actualTag = actual.toLanguageTag();
        if (!expected.equals(actualTag)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actualTag);
        }
    }
}
