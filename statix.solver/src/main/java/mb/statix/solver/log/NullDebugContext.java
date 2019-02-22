package mb.statix.solver.log;

import org.metaborg.util.log.Level;

/**
 * Implementation of {@link IDebugContext} which does not perform any logging.
 */
public class NullDebugContext implements IDebugContext {

    private final int depth;

    public NullDebugContext() {
        this(0);
    }

    public NullDebugContext(int depth) {
        this.depth = depth;
    }

    @Override public Level getLevel() {
        return Level.Error;
    }

    @Override public int getDepth() {
        return depth;
    }

    @Override public boolean isEnabled(Level level) {
        return false;
    }

    @Override public IDebugContext subContext() {
        return new NullDebugContext(depth + 1);
    }

    @Override public void info(String fmt, Object... args) {
    }

    @Override public void warn(String fmt, Object... args) {
    }

    @Override public void error(String fmt, Object... args) {
    }

    @Override public void log(Level level, String fmt, Object... args) {
    }

}