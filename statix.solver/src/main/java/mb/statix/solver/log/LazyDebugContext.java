package mb.statix.solver.log;

import org.metaborg.util.log.Level;

/**
 * Debug context which queues messages until {@link #commit()} is called.
 */
public class LazyDebugContext implements IDebugContext {

    private final IDebugContext debug;
    private final int offset;
    private final Log log;
    private final String prefix;

    public LazyDebugContext(IDebugContext debug) {
        this(debug, 0, new Log());
    }

    private LazyDebugContext(IDebugContext debug, int offset, Log log) {
        this.debug = debug;
        this.offset = offset;
        this.log = log;
        this.prefix = prefix(offset);
    }

    @Override public Level getLevel() {
        return debug.getLevel();
    }

    @Override public int getDepth() {
        return debug.getDepth() + offset;
    }

    @Override public boolean isEnabled(Level level) {
        return debug.isEnabled(level);
    }

    @Override public IDebugContext subContext() {
        return new LazyDebugContext(debug, offset + 1, log);
    }

    @Override public void info(String fmt, Object... args) {
        if(isEnabled(Level.Info)) {
            log.append(Level.Info, prefix + fmt, args);
        }
    }

    @Override public void warn(String fmt, Object... args) {
        if(isEnabled(Level.Warn)) {
            log.append(Level.Warn, prefix + fmt, args);
        }
    }

    @Override public void error(String fmt, Object... args) {
        if(isEnabled(Level.Error)) {
            log.append(Level.Error, prefix + fmt, args);
        }
    }

    @Override public void log(Level level, String fmt, Object... args) {
        if(isEnabled(level)) {
            log.append(level, prefix + fmt, args);
        }
    }
    
    @Override public void _log(Level level, String fmt, Object... args) {
        log.append(level, prefix + fmt, args);
    }

    /**
     * Flushes the queued messages to the parent debug context.
     * 
     * <p>All subcontexts will also be flushed.</p>
     */
    public void commit() {
        log.flush(debug);
    }

    /**
     * Clears this debug log, and returns a log with all the messages that were cleared.
     * 
     * @return
     *      a log with all the messages that were cleared
     */
    public Log clear() {
        return log.clear();
    }
    
    /**
     * Returns a log with all the messages queued in this lazy debug context.
     * 
     * @return
     *      a log with all the messages
     */
    public Log copy() {
        return log.copy();
    }

    /**
     * Creates a prefix of {@code "| "} times the offset.
     * 
     * @param offset
     *      the offset
     * @return
     *      a prefix string 
     */
    private static String prefix(int offset) {
        StringBuilder sb = new StringBuilder(offset * 2);
        for (int i = 0; i < offset; i++) {
            sb.append("| ");
        }
        return sb.toString();
    }

}