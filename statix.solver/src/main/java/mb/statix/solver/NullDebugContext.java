package mb.statix.solver;

public class NullDebugContext implements IDebugContext {

    private final boolean isRoot;

    public NullDebugContext() {
        this(false);
    }

    public NullDebugContext(boolean isRoot) {
        this.isRoot = isRoot;
    }

    public boolean isRoot() {
        return isRoot;
    }

    @Override public IDebugContext subContext() {
        return this;
    }

    @Override public void info(String fmt, Object... args) {
    }

    @Override public void info(String fmt, Throwable t, Object... args) {
    }

    @Override public void warn(String fmt, Object... args) {
    }

    @Override public void warn(String fmt, Throwable t, Object... args) {
    }

    @Override public void error(String fmt, Object... args) {
    }

    @Override public void error(String fmt, Throwable t, Object... args) {
    }

}