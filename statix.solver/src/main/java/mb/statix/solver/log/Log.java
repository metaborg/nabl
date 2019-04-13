package mb.statix.solver.log;

import org.metaborg.util.log.Level;

/**
 * Log class which keeps a doubly linked list of {@link Entry} objects.
 * 
 * <p>Logs can be flushed to an {@link IDebugContext} to actually log all the messages.</p>
 */
public class Log {

    private Entry first;
    private Entry last;

    public Log() {
        this(null, null);
    }

    /**
     * Creates new log with the given entries as head and tail of the linked list.
     * 
     * @param first
     *      the head element
     * @param last
     *      the tail element
     */
    private Log(Entry first, Entry last) {
        this.first = first;
        this.last = last;
    }

    /**
     * @return
     *      true if this log has no messages, false otherwise
     */
    public boolean isEmpty() {
        return first == null;
    }

    /**
     * Appends a new message to this log. All parameters are required and non null.
     * 
     * @param level
     *      the level to log at
     * @param message
     *      the message
     * @param args
     *      the arguments of the message
     */
    public void append(Level level, String message, Object[] args) {
        final Entry next = new Entry(level, message, args);
        if(isEmpty()) {
            first = next;
            last = next;
        } else {
            last.next = next;
            last = next;
        }
    }

    /**
     * Flushes all messages in this log to the given debug context.
     * 
     * @param debug
     *      the debug context
     */
    public void flush(IDebugContext debug) {
        while(!isEmpty()) {
            debug.log(first.level, first.message, first.args);
            first = first.next;
        }
    }

    /**
     * Absorbs all the messages from the given log into this log.
     * The given log is cleared when this call completes.
     * 
     * @param log
     *      the log to absorb messages from
     */
    public void absorb(final Log log) {
        if(!log.isEmpty()) {
            if(isEmpty()) {
                first = log.first;
                last = log.last;
            } else {
                last.next = log.first;
                last = log.last;
            }
            log.first = null;
            log.last = null;
        }
    }

    /**
     * Creates a copy of this log, and then clears this log.
     * 
     * @return
     *      the copy of this log
     */
    public Log clear() {
        final Log log = new Log(first, last);
        first = null;
        last = null;
        return log;
    }
    
    /**
     * Creates a copy of this log.
     * 
     * @return
     *      a copy of this log
     */
    public Log copy() {
        return new Log(first, last);
    }

    /**
     * Class representing a log entry.
     * A log entry consists of a level, message and arguments.
     */
    private static class Entry {

        private final Level level;
        private final String message;
        private final Object[] args;
        private Entry next;

        private Entry(Level level, String message, Object[] args) {
            this.level = level;
            this.message = message;
            this.args = args;
        }

    }

}