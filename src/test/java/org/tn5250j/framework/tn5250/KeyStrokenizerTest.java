package org.tn5250j.framework.tn5250;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class KeyStrokenizerTest {

    @Test
    public void tokenizesCharactersAndMnemonicsInOrder() {
        KeyStrokenizer tokenizer = new KeyStrokenizer();
        tokenizer.setKeyStrokes("ab[enter]");

        assertEquals("a", tokenizer.nextKeyStroke());
        assertEquals("b", tokenizer.nextKeyStroke());
        assertEquals("[enter]", tokenizer.nextKeyStroke());
        assertFalse(tokenizer.hasMoreKeyStrokes());
    }
}
