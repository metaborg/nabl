package mb.statix.solver;


public interface IDebugContext {

    boolean isRoot();

    IDebugContext subContext();

    void info(String fmt, Object... args);

    void info(String fmt, Throwable t, Object... args);

    void warn(String fmt, Object... args);

    void warn(String fmt, Throwable t, Object... args);

    void error(String fmt, Object... args);

    void error(String fmt, Throwable t, Object... args);

}