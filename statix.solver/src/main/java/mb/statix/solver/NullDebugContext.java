package mb.statix.solver;

public class NullDebugContext implements IDebugContext {

    public NullDebugContext() {
    }

    public boolean isRoot() {
        return false;
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