package mb.statix.solver.log;

import org.metaborg.util.log.Level;

public interface IDebugContext {

    default boolean isRoot() {
        return getDepth() == 0;
    }

    int getDepth();

    Level getDebugLevel();

    boolean isEnabled(Level level);

    IDebugContext subContext();

    void debug(String fmt, Object... args);

    void debug(String fmt, Throwable t, Object... args);

    void warn(String fmt, Object... args);

    void warn(String fmt, Throwable t, Object... args);

    void error(String fmt, Object... args);

    void error(String fmt, Throwable t, Object... args);

    void log(Level level, String fmt, Object... args);

    void log(Level level, String fmt, Throwable t, Object... args);

}