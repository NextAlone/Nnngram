package xyz.nextalone.nnngram.config;

/**
 * Lightweight JVM regression test without Android or JUnit dependencies.
 */
public final class AutoTranslateConfigKeyTest {

    public static void main(String[] args) {
        assertEquals("private dialog key", "autoTranslate_42", AutoTranslateConfigKey.forDialog(42, 0));
        assertEquals("group dialog key", "autoTranslate_-42", AutoTranslateConfigKey.forDialog(-42, 0));
        assertEquals("topic dialog key", "autoTranslate_-42_7", AutoTranslateConfigKey.forDialog(-42, 7));
        assertEquals("topic inherits group key", "autoTranslate_-42", AutoTranslateConfigKey.parentDialogKey(-42, 7));
        assertEquals("root dialog has no parent key", null, AutoTranslateConfigKey.parentDialogKey(-42, 0));
        assertEquals("legacy group key", "autoTranslate_42", AutoTranslateConfigKey.legacyPositiveChatKey(-42, 0));
        assertEquals("legacy topic key", "autoTranslate_42_7", AutoTranslateConfigKey.legacyPositiveChatKey(-42, 7));
        assertEquals("private dialog has no legacy chat key", null, AutoTranslateConfigKey.legacyPositiveChatKey(42, 0));

        assertTrue("explicit topic enable overrides disabled group",
                AutoTranslateConfigKey.resolveEffectiveValue(true, true, true, false, false));
        assertFalse("explicit topic disable overrides enabled group",
                AutoTranslateConfigKey.resolveEffectiveValue(true, false, true, true, true));
        assertTrue("topic inherits enabled group",
                AutoTranslateConfigKey.resolveEffectiveValue(false, false, true, true, false));
        assertFalse("topic inherits disabled group",
                AutoTranslateConfigKey.resolveEffectiveValue(false, true, true, false, true));
        assertTrue("missing topic and group use enabled global value",
                AutoTranslateConfigKey.resolveEffectiveValue(false, false, false, false, true));
        assertFalse("missing topic and group use disabled global value",
                AutoTranslateConfigKey.resolveEffectiveValue(false, true, false, true, false));

        assertTrue("migrate legacy group value, replacing a never-read signed value",
                AutoTranslateConfigKey.shouldMigrateLegacyPositiveChatKey(-42, false, true));
        assertFalse("migration marker prevents restoring a removed override",
                AutoTranslateConfigKey.shouldMigrateLegacyPositiveChatKey(-42, true, true));
        assertFalse("private dialog value is never treated as legacy chat data",
                AutoTranslateConfigKey.shouldMigrateLegacyPositiveChatKey(42, false, true));
        assertFalse("missing legacy value does not create an override",
                AutoTranslateConfigKey.shouldMigrateLegacyPositiveChatKey(-42, false, false));
    }

    private static void assertEquals(String label, String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(String label, boolean actual) {
        if (!actual) {
            throw new AssertionError(label + ": expected true");
        }
    }

    private static void assertFalse(String label, boolean actual) {
        if (actual) {
            throw new AssertionError(label + ": expected false");
        }
    }
}
