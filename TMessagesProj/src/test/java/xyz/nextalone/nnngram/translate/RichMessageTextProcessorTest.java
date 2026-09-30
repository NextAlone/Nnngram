package xyz.nextalone.nnngram.translate;

import org.telegram.tgnet.tl.TL_iv;

import java.util.List;

/** Lightweight JVM regression coverage for Rich Message text extraction. */
public final class RichMessageTextProcessorTest {

    public static void main(String[] args) {
        TL_iv.RichMessage message = new TL_iv.RichMessage();
        message.blocks.add(paragraph("收到新的 Issue"));
        message.blocks.add(paragraph("Global search is disabled."));
        message.blocks.add(protectedParagraph("#A0012"));
        message.blocks.add(paragraph("---"));

        List<String> texts = RichMessageTextProcessor.translatableTexts(message);
        assertEquals("translatable node count", 2, texts.size());
        assertTrue("contains Traditional Chinese node", texts.contains("收到新的 Issue"));
        assertTrue("contains English node", texts.contains("Global search is disabled."));
        assertFalse("protected hashtag is excluded", texts.contains("#A0012"));
        assertTrue("mixed rich message is translatable", RichMessageTextProcessor.hasTranslatableText(message));

        String plainText = RichMessageTextProcessor.plainText(message);
        assertTrue("language detection text includes protected display text", plainText.contains("#A0012"));

        TL_iv.RichMessage protectedOnly = new TL_iv.RichMessage();
        protectedOnly.blocks.add(protectedParagraph("#A0012"));
        assertFalse("protected-only rich message is not translatable",
                RichMessageTextProcessor.hasTranslatableText(protectedOnly));
    }

    private static TL_iv.pageBlockParagraph paragraph(String value) {
        TL_iv.textPlain text = new TL_iv.textPlain();
        text.text = value;
        TL_iv.pageBlockParagraph paragraph = new TL_iv.pageBlockParagraph();
        paragraph.text = text;
        return paragraph;
    }

    private static TL_iv.pageBlockParagraph protectedParagraph(String value) {
        TL_iv.textPlain text = new TL_iv.textPlain();
        text.text = value;
        TL_iv.textHashtag hashtag = new TL_iv.textHashtag();
        hashtag.text = text;
        TL_iv.pageBlockParagraph paragraph = new TL_iv.pageBlockParagraph();
        paragraph.text = hashtag;
        return paragraph;
    }

    private static void assertEquals(String label, int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertTrue(String label, boolean value) {
        if (!value) {
            throw new AssertionError(label);
        }
    }

    private static void assertFalse(String label, boolean value) {
        assertTrue(label, !value);
    }
}
