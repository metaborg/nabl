package mb.statix.solver.log;

import org.metaborg.util.log.Level;

public interface IDebugContext {

    default boolean isRoot() {
        return getDepth() == 0;
    }

    Level getLevel();

    int getDepth();

    boolean isEnabled(Level level);

    IDebugContext subContext();

    void info(String fmt, Object... args);

    void info(String fmt, Throwable t, Object... args);

    void warn(String fmt, Object... args);

    void warn(String fmt, Throwable t, Object... args);

    void error(String fmt, Object... args);

    void error(String fmt, Throwable t, Object... args);

    void log(Level level, String fmt, Object... args);

    void log(Level level, String fmt, Throwable t, Object... args);

}