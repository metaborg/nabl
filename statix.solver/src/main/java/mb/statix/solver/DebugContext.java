package mb.statix.solver;

import java.util.Collections;

import org.metaborg.util.log.ILogger;

public class DebugContext implements IDebugContext {

    private final ILogger logger;
    private final int depth;

    public DebugContext(ILogger logger) {
        this(logger, 0);
    }

    private DebugContext(ILogger logger, int depth) {
        this.logger = logger;
        this.depth = depth;
    }

    public boolean isRoot() {
        return depth == 0;
    }

    @Override public IDebugContext subContext() {
        return new DebugContext(logger, depth + 1);
    }

    @Override public void info(String fmt, Object... args) {
        logger.info(prefix(depth) + fmt, args);
    }

    @Override public void info(String fmt, Throwable t, Object... args) {
        logger.info(prefix(depth) + fmt, t, args);
    }

    @Override public void warn(String fmt, Object... args) {
        logger.warn(prefix(depth) + fmt, args);
    }

    @Override public void warn(String fmt, Throwable t, Object... args) {
        logger.warn(prefix(depth) + fmt, t, args);
    }

    @Override public void error(String fmt, Object... args) {
        logger.error(prefix(depth) + fmt, args);
    }

    @Override public void error(String fmt, Throwable t, Object... args) {
        logger.error(prefix(depth) + fmt, t, args);
    }

    private String prefix(int depth) {
        return String.join("", Collections.nCopies(depth, "| "));
    }

}