package org.tn5250j.tools.logging;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Slf4jLoggerTest {

    @Test
    public void levelMappingRoundTrip() {
        Slf4jLogger logger = new Slf4jLogger();
        logger.initialize(getClass().getName());

        logger.setLevel(TN5250jLogger.DEBUG);
        assertTrue(logger.isDebugEnabled());
        assertEquals(TN5250jLogger.DEBUG, logger.getLevel());

        logger.setLevel(TN5250jLogger.WARN);
        assertFalse(logger.isDebugEnabled());
        assertTrue(logger.isWarnEnabled());
        assertEquals(TN5250jLogger.WARN, logger.getLevel());

        logger.setLevel(TN5250jLogger.OFF);
        assertFalse(logger.isInfoEnabled());
        assertEquals(TN5250jLogger.OFF, logger.getLevel());
    }
}
