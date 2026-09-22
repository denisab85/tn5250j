package org.tn5250j.cli;

import org.junit.Test;
import static org.junit.Assert.*;

public class StoredArgumentsTest {
    @Test
    public void savedSettingsAndForwardedArgumentsRoundTrip() {
        String[] args = {"ibmi", "--session", "Production office", "--config", "C:\\My files\\display.props", "", "say \"hello\"", "a'b", "a\\b"};
        assertArrayEquals(args, StoredArguments.split(StoredArguments.join(args)));
    }

    @Test
    public void readsWhitespaceAndWindowsPaths() {
        assertArrayEquals(new String[]{"ibmi", "-f", "C:\\tn5250j\\session.props", "--code-page", "37"},
                StoredArguments.split("ibmi  -f C:\\tn5250j\\session.props --code-page 37 "));
        assertArrayEquals(new String[]{"ibmi", "--config", "my session.props"},
                StoredArguments.split("ibmi --config 'my session.props'"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnclosedQuotes() {
        StoredArguments.split("ibmi --config \"unfinished");
    }
}
