package mb.statix.solver.log;

import java.util.Collections;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;

public class LoggerDebugContext implements IDebugContext {

    private final ILogger logger;
    private final Level level;
    private final int depth;

    public LoggerDebugContext(ILogger logger) {
        this(logger, Level.Info, 0);
    }

    public LoggerDebugContext(ILogger logger, Level level) {
        this(logger, level, 0);
    }

    private LoggerDebugContext(ILogger logger, Level level, int depth) {
        this.logger = logger;
        this.level = level;
        this.depth = depth;
    }

    @Override public Level getLevel() {
        return level;
    }

    @Override public int getDepth() {
        return depth;
    }

    @Override public boolean isEnabled(Level level) {
        return this.level.compareTo(level) <= 0;
    }

    @Override public IDebugContext subContext() {
        return new LoggerDebugContext(logger, level, depth + 1);
    }

    @Override public void info(String fmt, Object... args) {
        if(isEnabled(Level.Info)) {
            logger.info(prefix() + fmt, args);
        }
    }

    @Override public void info(String fmt, Throwable t, Object... args) {
        if(isEnabled(Level.Info)) {
            logger.info(prefix() + fmt, t, args);
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