package org.mangorage.bootstrap.internal.logger;

import org.mangorage.bootstrap.api.logging.IMangoLogger;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Java Util Logging fallback implementation when SLF4J is not available.
 * Maintains the same API but uses JUL internally.
 */
public final class JulMangoLogger implements IMangoLogger {

    private final Logger delegate;
    private final String name;

    public JulMangoLogger(String name) {
        this.name = name;
        this.delegate = Logger.getLogger(name);
    }

    public JulMangoLogger(Class<?> clazz) {
        this(clazz.getName());
    }

    @Override
    public <T> T unwrap(Class<T> loggerClass) throws UnsupportedOperationException {
        return loggerClass.isInstance(delegate) ? loggerClass.cast(delegate) : null;
    }

    @Override
    public void trace(String message) {
        delegate.finest(message);
    }

    @Override
    public void trace(String message, Object... args) {
        if (delegate.isLoggable(Level.FINEST)) {
            delegate.finest(String.format(message, args));
        }
    }

    @Override
    public void trace(String message, Throwable throwable) {
        delegate.log(Level.FINEST, message, throwable);
    }

    @Override
    public void debug(String message) {
        delegate.fine(message);
    }

    @Override
    public void debug(String message, Object... args) {
        if (delegate.isLoggable(Level.FINE)) {
            delegate.fine(String.format(message, args));
        }
    }

    @Override
    public void debug(String message, Throwable throwable) {
        delegate.log(Level.FINE, message, throwable);
    }

    @Override
    public void info(String message) {
        delegate.info(message);
    }

    @Override
    public void info(String message, Object... args) {
        if (delegate.isLoggable(Level.INFO)) {
            delegate.info(String.format(message, args));
        }
    }

    @Override
    public void info(String message, Throwable throwable) {
        delegate.log(Level.INFO, message, throwable);
    }

    @Override
    public void warn(String message) {
        delegate.warning(message);
    }

    @Override
    public void warn(String message, Object... args) {
        if (delegate.isLoggable(Level.WARNING)) {
            delegate.warning(String.format(message, args));
        }
    }

    @Override
    public void warn(String message, Throwable throwable) {
        delegate.log(Level.WARNING, message, throwable);
    }

    @Override
    public void error(String message) {
        delegate.severe(message);
    }

    @Override
    public void error(String message, Object... args) {
        if (delegate.isLoggable(Level.SEVERE)) {
            delegate.severe(String.format(message, args));
        }
    }

    @Override
    public void error(String message, Throwable throwable) {
        delegate.log(Level.SEVERE, message, throwable);
    }

    // Fun methods (simplified for JUL)
    @Override
    public void rainbow(String message) {
        info("🌈 " + message);
    }

    @Override
    public void celebration(String message) {
        info("🎉 " + message + " 🎉");
    }

    @Override
    public void dramatic(String message) {
        warn("🎭 *DRAMATIC* " + message.toUpperCase() + " *END SCENE* 🎭");
    }

    @Override
    public void whisper(String message) {
        debug("🤫 *whispers* " + message.toLowerCase());
    }

    @Override
    public void shout(String message) {
        warn("📢 " + message.toUpperCase() + "!!!");
    }

    @Override
    public void withEmoji(String emoji, String message) {
        info(emoji + " " + message);
    }

    @Override
    public void withBorder(String message) {
        String border = "=".repeat(Math.min(message.length() + 4, 80));
        info("┌" + border + "┐");
        info("│ " + message + " │");
        info("└" + border + "┘");
    }

    @Override
    public void withContext(String context, String message) {
        info("[" + context + "] " + message);
    }

    @Override
    public boolean isTraceEnabled() {
        return delegate.isLoggable(Level.FINEST);
    }

    @Override
    public boolean isDebugEnabled() {
        return delegate.isLoggable(Level.FINE);
    }

    @Override
    public boolean isInfoEnabled() {
        return delegate.isLoggable(Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return delegate.isLoggable(Level.WARNING);
    }

    @Override
    public boolean isErrorEnabled() {
        return delegate.isLoggable(Level.SEVERE);
    }

    @Override
    public String getName() {
        return name;
    }
}