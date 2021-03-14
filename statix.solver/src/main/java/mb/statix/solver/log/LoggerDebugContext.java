package mb.statix.solver.log;

import java.util.Collections;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;

public class LoggerDebugContext implements IDebugContext {

    private final ILogger logger;
    private final Level debugLevel;
    private final int depth;

    public LoggerDebugContext(ILogger logger) {
        this(logger, Level.Info, 0);
    }

    public LoggerDebugContext(ILogger logger, Level debugLevel) {
        this(logger, debugLevel, 0);
    }

    private LoggerDebugContext(ILogger logger, Level debugLevel, int depth) {
        this.logger = logger;
        this.debugLevel = debugLevel;
        this.depth = depth;
    }

    @Override public int getDepth() {
        return depth;
    }

    @Override public boolean isEnabled(Level level) {
        return logger.enabled(level);
    }

    @Override public Level getDebugLevel() {
        return debugLevel;
    }

    private volatile IDebugContext subContext;

    @Override public IDebugContext subContext() {
        IDebugContext result = subContext;
        if(result == null) {
            result = new LoggerDebugContext(logger, debugLevel, depth + 1);
            subContext = result;
        }
        return result;
    }

    @Override public void debug(String fmt, Object... args) {
        if(isEnabled(getDebugLevel())) {
            logger.log(getDebugLevel(), prefix() + fmt, args);
        }
    }

    @Override public void debug(String fmt, Throwable t, Object... args) {
        if(isEnabled(getDebugLevel())) {
            logger.log(getDebugLevel(), prefix() + fmt, t, args);
        }
    }

    @Override public void warn(String fmt, Object... args) {
        if(isEnabled(Level.Warn)) {
            logger.warn(prefix() + fmt, args);
        }
    }

    @Override public void warn(String fmt, Throwable t, Object... args) {
        if(isEnabled(Level.Warn)) {
            logger.warn(prefix() + fmt, t, args);
        }
    }

    @Override public void error(String fmt, Object... args) {
        if(isEnabled(Level.Error)) {
            logger.error(prefix() + fmt, args);
        }
    }

    @Override public void error(String fmt, Throwable t, Object... args) {
        if(isEnabled(Level.Error)) {
            logger.error(prefix() + fmt, t, args);
        }
    }

    @Override public void log(Level level, String fmt, Object... args) {
        if(isEnabled(level)) {
            logger.log(level, prefix() + fmt, args);
        }
    }

    @Override public void log(Level level, String fmt, Throwable t, Object... args) {
        if(isEnabled(level)) {
            logger.log(level, prefix() + fmt, t, args);
        }
    }

    private String prefix() {
        return String.join("", Collections.nCopies(depth, "| "));
    }

}