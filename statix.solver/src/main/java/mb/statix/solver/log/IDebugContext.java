package mb.statix.solver.log;

import org.metaborg.util.log.Level;

/**
 * Interface for logging debug messages.
 */
public interface IDebugContext {

    /**
     * The root debug context is the context at the top level. It will do the actual logging.
     * 
     * @return
     *      true if this debug context is a root, false otherwise
     */
    default boolean isRoot() {
        return getDepth() == 0;
    }

    /**
     * Returns the level of this debug context. All messages with a level lower than this level
     * will not be logged.
     * 
     * @return
     *      the level of this debug context
     */
    Level getLevel();

    /**
     * The depth of a debug context indicates how many levels from the parent it is.
     * Messages will get {@code "| "} prefixed as many times as the depth.
     * 
     * @return
     *      the depth of this debug context
     */
    int getDepth();

    /**
     * @param level
     *      the level to check
     * @return
     *      true if messages at the given level would be logged, false otherwise
     */
    boolean isEnabled(Level level);

    /**
     * @return
     *      creates a debug context with a depth + 1
     */
    IDebugContext subContext();

    /**
     * Logs the given message at the info level, formatting it with the given arguments.
     * 
     * @param fmt
     *      the message to format and log
     * @param args
     *      the arguments to format with
     */
    void info(String fmt, Object... args);

    /**
     * Logs the given message at the warn level, formatting it with the given arguments.
     * 
     * @param fmt
     *      the message to format and log
     * @param args
     *      the arguments to format with
     */
    void warn(String fmt, Object... args);

    /**
     * Logs the given message at the error level, formatting it with the given arguments.
     * 
     * @param fmt
     *      the message to format and log
     * @param args
     *      the arguments to format with
     */
    void error(String fmt, Object... args);

    /**
     * Logs the given message at the given level, formatting it with the given arguments.
     * 
     * @param level
     *      the level to log at
     * @param fmt
     *      the message to format and log
     * @param args
     *      the arguments to format with
     */
    void log(Level level, String fmt, Object... args);

}