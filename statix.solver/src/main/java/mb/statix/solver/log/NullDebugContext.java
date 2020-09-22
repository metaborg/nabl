package mb.statix.solver.log;

import org.metaborg.util.log.Level;

public class NullDebugContext implements IDebugContext {

    private final int depth;

    public NullDebugContext() {
        this(0);
    }

    public NullDebugContext(int depth) {
        this.depth = depth;
    }

    @Override public Level getDebugLevel() {
        return Level.Trace;
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

    @Override public void debug(String fmt, Object... args) {
    }

    @Override public void debug(String fmt, Throwable t, Object... args) {
    }

    @Override public void warn(String fmt, Object... args) {
    }

    @Override public void warn(String fmt, Throwable t, Object... args) {
    }

    @Override public void error(String fmt, Object... args) {
    }

    @Override public void error(String fmt, Throwable t, Object... args) {
    }

    @Override public void log(Level level, String fmt, Object... args) {
    }

    @Override public void log(Level level, String fmt, Throwable t, Object... args) {
    }
}