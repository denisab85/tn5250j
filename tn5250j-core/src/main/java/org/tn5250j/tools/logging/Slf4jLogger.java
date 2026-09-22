/*
 * @(#)Slf4jLogger.java
 *
 * Copyright:    Copyright (c) 2001, 2002, 2003
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 *
 */
package org.tn5250j.tools.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4J/Logback implementation of {@link TN5250jLogger}.
 */
public final class Slf4jLogger implements TN5250jLogger {

    private Logger log;

    Slf4jLogger() {
    }

    @Override
    public void initialize(final String clazz) {
        log = (Logger) LoggerFactory.getLogger(clazz);
    }

    @Override
    public void debug(Object message) {
        log.debug(String.valueOf(message));
    }

    @Override
    public void debug(Object message, Throwable throwable) {
        log.debug(String.valueOf(message), throwable);
    }

    @Override
    public void info(Object message) {
        log.info(String.valueOf(message));
    }

    @Override
    public void info(Object message, Throwable throwable) {
        log.info(String.valueOf(message), throwable);
    }

    @Override
    public void warn(Object message) {
        log.warn(String.valueOf(message));
    }

    @Override
    public void warn(Object message, Throwable throwable) {
        log.warn(String.valueOf(message), throwable);
    }

    @Override
    public void error(Object message) {
        log.error(String.valueOf(message));
    }

    @Override
    public void error(Object message, Throwable throwable) {
        log.error(String.valueOf(message), throwable);
    }

    @Override
    public void fatal(Object message) {
        log.error(String.valueOf(message));
    }

    @Override
    public void fatal(Object message, Throwable throwable) {
        log.error(String.valueOf(message), throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public boolean isFatalEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void setLevel(int newLevel) {
        log.setLevel(toLogbackLevel(newLevel));
    }

    @Override
    public int getLevel() {
        return fromLogbackLevel(log.getLevel());
    }

    private static Level toLogbackLevel(int newLevel) {
        switch (newLevel) {
            case OFF:
                return Level.OFF;
            case DEBUG:
                return Level.DEBUG;
            case INFO:
                return Level.INFO;
            case WARN:
                return Level.WARN;
            case ERROR:
            case FATAL:
                return Level.ERROR;
            default:
                return Level.WARN;
        }
    }

    private static int fromLogbackLevel(Level level) {
        if (level == null) {
            return WARN;
        }
        switch (level.toInt()) {
            case Level.OFF_INT:
                return OFF;
            case Level.DEBUG_INT:
                return DEBUG;
            case Level.INFO_INT:
                return INFO;
            case Level.WARN_INT:
                return WARN;
            case Level.ERROR_INT:
                return ERROR;
            default:
                return WARN;
        }
    }
}
