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

    @Test
    public void quoteAvoidsRegexAndEscapesWhenNeeded() {
        assertEquals("simple", StoredArguments.quote("simple"));
        assertEquals("\"needs quotes\"", StoredArguments.quote("needs quotes"));
        assertEquals("\"tab\there\"", StoredArguments.quote("tab\there"));
        assertEquals("\"\"", StoredArguments.quote(""));
    }

    @Test
    public void normalizesLegacySavedSessionFlags() {
        assertArrayEquals(new String[]{"CDKDEV", "-p", "23", "-e", "-t", "--proxy-port", "1080", "-d"},
                StoredArguments.normalizeLegacy(StoredArguments.split("CDKDEV -p 23 -e -t -spp 1080 -d")));
        assertArrayEquals(new String[]{"ibmi", "--code-page", "37", "--proxy", "--proxy-host", "proxy", "--proxy-port", "1080"},
                StoredArguments.normalizeLegacy(new String[]{"ibmi", "-cp37", "-usp", "-sph", "proxy", "-spp1080"}));
        assertArrayEquals(new String[]{"ibmi", "--device-name-from-hostname", "--wide", "--new-instance"},
                StoredArguments.normalizeLegacy(new String[]{"ibmi", "-dn=hostname", "-132", "-nc"}));
    }
}
