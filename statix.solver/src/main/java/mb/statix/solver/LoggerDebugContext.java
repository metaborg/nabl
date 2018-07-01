package mb.statix.solver;

import java.util.Collections;

import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.Level;

public class LoggerDebugContext implements IDebugContext {

    private final ILogger logger;
    private final Level level;
    private final int depth;

    public LoggerDebugContext(ILogger logger, Level level) {
        this(logger, level, 0);
    }

    private LoggerDebugContext(ILogger logger, Level level, int depth) {
        this.logger = logger;
        this.level = level;
        this.depth = depth;
    }

    public boolean isRoot() {
        return depth == 0;
    }

    @Override public IDebugContext subContext() {
        return new LoggerDebugContext(logger, level, depth + 1);
    }

    @Override public void info(String fmt, Object... args) {
        if(Level.Info.compareTo(level) < 0) {
            return;
        }
        logger.info(prefix(depth) + fmt, args);
    }

    @Override public void info(String fmt, Throwable t, Object... args) {
        if(Level.Info.compareTo(level) < 0) {
            return;
        }
        logger.info(prefix(depth) + fmt, t, args);
    }

    @Override public void warn(String fmt, Object... args) {
        if(Level.Warn.compareTo(level) < 0) {
            return;
        }
        logger.warn(prefix(depth) + fmt, args);
    }

    @Override public void warn(String fmt, Throwable t, Object... args) {
        if(Level.Warn.compareTo(level) < 0) {
            return;
        }
        logger.warn(prefix(depth) + fmt, t, args);
    }

    @Override public void error(String fmt, Object... args) {
        if(Level.Error.compareTo(level) < 0) {
            return;
        }
        logger.error(prefix(depth) + fmt, args);
    }

    @Override public void error(String fmt, Throwable t, Object... args) {
        if(Level.Error.compareTo(level) < 0) {
            return;
        }
        logger.error(prefix(depth) + fmt, t, args);
    }

    private String prefix(int depth) {
        return String.join("", Collections.nCopies(depth, "| "));
    }

}